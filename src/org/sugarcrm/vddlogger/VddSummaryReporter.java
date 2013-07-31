/*
 * Copyright 2011-2013 SugarCRM Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * Please see the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.sugarcrm.vddlogger;

import java.io.*;
import java.util.*;
import java.nio.CharBuffer;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXParseException;
import org.xml.sax.InputSource;


/**
 * Entry point for VDD report generation.
 */

public class VddSummaryReporter {

   private String HTML_HEADER_RESOURCE = "summaryreporter-header.txt";
   private String HTML_HEADER_ISSUES_RESOURCE = "issues-header.txt";
   private int count;
   private ArrayList<File> xmlFiles;
   private int passedTests = 0;
   private int failedTests = 0;
   private int blockedTests = 0;
   private int failedAsserts = 0;
   private int passedAsserts = 0;
   private int exceptions = 0;
   private int errors = 0;
   private int watchdog = 0;
   private int hours = 0;
   private int minutes = 0;
   private int seconds = 0;
   private FileOutputStream output;
   private PrintStream repFile;
   private Document dom;
   private String basedir = "";
   private VddLogIssues issues = null;
   private String issuesHtmlFile = null;


   /**
    * Instantiate a VddSummaryReporter object
    *
    * @param xmlFiles  list of VDD suite output files
    * @param path      directory containing the XML files
    */

   public VddSummaryReporter(ArrayList<File> xmlFiles, File path) {
      this.count = 0;
      this.xmlFiles = xmlFiles;
      passedTests = 0;
      failedTests = 0;
      blockedTests = 0;
      failedAsserts = 0;
      passedAsserts = 0;
      exceptions = 0;
      errors = 0;
      watchdog = 0;
      hours = 0;
      minutes = 0;
      seconds = 0;


      String summaryFile = String.format("%s%s%s", path, File.separatorChar, "summary.html");
      this.issuesHtmlFile = String.format("%s%s%s", path, File.separatorChar, "issues.html");
      this.basedir = path.toString();

      this.issues = new VddLogIssues();

      try {
         output = new FileOutputStream(summaryFile);
         System.out.printf("(*)SummaryFile: %s\n", summaryFile);
         repFile = new PrintStream(output);
      } catch (Exception e) {
         System.out.printf("(!)Error: Failed trying to write file: '%s'!\n", summaryFile);
         e.printStackTrace();
      }
   }


   /**
    * Convert the VDD output logs into a report.
    */

   public void generateReport() {
      HashMap<String, HashMap<String, Object>> list = new HashMap<String, HashMap<String,Object>>();
      repFile.print(generateHTMLHeader());
      String name = "";
      String[] keys = null;


      for (int i = 0; i < xmlFiles.size(); i ++) {
         HashMap<String, Object> suiteData = null;
         try {
            suiteData = parseXMLFile(xmlFiles.get(i));
         } catch (Exception e) {
            e.printStackTrace();
            System.out.println("(!)Failed to parse " + xmlFiles.get(i) + ": " + e);
            continue;
         }
         name = suiteData.get("suitename").toString();
         list.put(name, suiteData);
      }

      keys = list.keySet().toArray(new String[0]);
      java.util.Arrays.sort(keys);

      for (int i = 0; i <= keys.length -1; i++) {
         String key = keys[i];
         repFile.print(generateTableRow(key, list.get(key)));
      }

      repFile.print(generateHTMLFooter());
      repFile.print("\n</body>\n</html>\n");
      repFile.close();

      this.writeIssues();
   }


   private void writeIssues() {
      String[] errors_keys = null;
      String[] warnings_keys = null;
      String[] except_keys = null;
      String line = "";
      InputStream stream = null;
      HashMap<String, Integer> tmpMap = null;

      System.out.printf("(*)Writing issues file...\n");

      errors_keys = sortIssue(this.issues.getData().get("errors"));
      warnings_keys = sortIssue(this.issues.getData().get("warnings"));
      except_keys = sortIssue(this.issues.getData().get("exceptions"));

      try {
         String className = this.getClass().getName().replace('.', '/');
         String classJar =  this.getClass().getResource("/" + className + ".class").toString();

         if (classJar.startsWith("jar:")) {
            stream = getClass().getResourceAsStream(this.HTML_HEADER_ISSUES_RESOURCE);
         } else {
            File header_fd = new File(getClass().getResource(this.HTML_HEADER_ISSUES_RESOURCE).getFile());
            stream = new FileInputStream(header_fd);
         }

         InputStreamReader in = new InputStreamReader(stream);
         BufferedReader br = new BufferedReader(in);
         File fd = new File(this.issuesHtmlFile);
         BufferedWriter out = new BufferedWriter(new FileWriter(fd));

         while ((line = br.readLine()) != null) {
            out.write(line + "\n");
         }
         br.close();
         in.close();

         tmpMap = this.issues.getData().get("errors");
         out.write("<table>\n");
         out.write("<tr>\n<td class=\"td_header_master\" colspan=\"2\">Errors:</td>\n</tr>\n");
         out.write("<tr>\n\t<td class=\"td_header_count\">Count:</td>\n\t<td class=\"td_header_sub\">Issue:</td>\n</tr>\n");
         for (int i = errors_keys.length -1; i >= 0 ; i--) {
            int count = tmpMap.get(errors_keys[i]);
            errors_keys[i] = errors_keys[i].replaceAll("<", "&lt");
            errors_keys[i] = errors_keys[i].replaceAll(">", "&gt");
            out.write("<tr class=\"unhighlight\" onmouseout=\"this.className='unhighlight'\" onmouseover=\"this.className='highlight'\">\n");
            String n = String.format("\t<td class=\"td_count_data\">%d</td>\n\t<td class=\"td_file_data\">%s</td>\n", count, errors_keys[i]);
            out.write(n);
            out.write("</tr>\n");
         }
         out.write("</table>\n");
         out.write("\n<hr></hr>\n");

         tmpMap = this.issues.getData().get("exceptions");
         out.write("<table>\n");
         out.write("<tr>\n<td class=\"td_header_master\" colspan=\"2\">Exceptions:</td>\n</tr>\n");
         out.write("<tr>\n\t<td class=\"td_header_count\">Count:</td>\n\t<td class=\"td_header_sub\">Issue:</td>\n</tr>\n");
         for (int i = except_keys.length -1; i >= 0 ; i--) {
            int count = tmpMap.get(except_keys[i]);
            out.write("<tr class=\"unhighlight\" onmouseout=\"this.className='unhighlight'\" onmouseover=\"this.className='highlight'\">\n");
            except_keys[i] = except_keys[i].replaceAll("<", "&lt");
            except_keys[i] = except_keys[i].replaceAll(">", "&gt");
            String n = String.format("\t<td class=\"td_count_data\">%d</td>\n\t<td class=\"td_file_data\">%s</td>\n", count, except_keys[i]);
            out.write(n);
            out.write("</tr>\n");
         }
         out.write("</table>\n");
         out.write("\n<hr></hr>\n");

         tmpMap = this.issues.getData().get("warnings");
         out.write("<table>\n");
         out.write("<tr>\n\t<td class=\"td_header_master\" colspan=\"2\">Warnings:</td>\n</tr>\n");
         out.write("<tr>\n\t<td class=\"td_header_count\">Count:</td>\n\t<td class=\"td_header_sub\">Issue:</td>\n</tr>\n");
         for (int i = warnings_keys.length -1; i >= 0 ; i--) {
            int count = tmpMap.get(warnings_keys[i]);
            out.write("<tr class=\"unhighlight\" onmouseout=\"this.className='unhighlight'\" onmouseover=\"this.className='highlight'\">\n");
            warnings_keys[i] = warnings_keys[i].replaceAll("<", "&lt");
            warnings_keys[i] = warnings_keys[i].replaceAll(">", "&gt");
            String n = String.format("\t<td class=\"td_count_data\">%d</td>\n\t<td class=\"td_file_data\">%s</td>\n", count, warnings_keys[i]);
            out.write(n);
            out.write("</tr>\n");
         }
         out.write("</table>\n");

         out.write("</body></html>\n");
         out.close();
      } catch (Exception exp ) {
         exp.printStackTrace();
      }

      System.out.printf("(*)Finished writing issues file.\n");

   }

   private String[] sortIssue(HashMap<String, Integer> map) {
      String[] keys = null;

      keys = map.keySet().toArray(new String[0]);

      for (int i = 0; i <= keys.length -1; i++) {
         int count = map.get(keys[i]);
         keys[i] = String.format("%d:%s", count, keys[i]);
      }

      Arrays.sort(keys);
      for (int i = 0; i <= keys.length -1; i++) {
         keys[i] = keys[i].replaceFirst("\\d+:", "");
      }

      return keys;
   }


   private boolean isRestart(Node node) {
      boolean result = false;
      NodeList parent = node.getParentNode().getChildNodes();

      for (int i = 0; i <= parent.getLength() -1; i++) {
         Node tmp = parent.item(i);
         String name = tmp.getNodeName();
         if (name.contains("isrestart")) {
            result = Boolean.valueOf(tmp.getTextContent());
            break;
         }
      }

      return result;
   }

   private boolean isLibTest(Node node) {
      NodeList parent = node.getParentNode().getChildNodes();

      for (int i = 0; i <= parent.getLength() -1; i++) {
         Node tmp = parent.item(i);
         String name = tmp.getNodeName();
         if (name.contains("testfile")) {
            File fd = new File(tmp.getTextContent());
            String path = fd.getParent();
            if (path == null) {
               /*
                * Filename contains no path information, so it's
                * impossible to know whether this is in the lib.
                */
               return false;
            }
            path = path.toLowerCase();

            if (path.contains("lib")) {
               return true;
            }
         }
      }

      return false;
   }

   private boolean isBlocked(Node node) {
      boolean result = false;
      NodeList parent = node.getParentNode().getChildNodes();

      for (int i = 0; i <= parent.getLength() -1; i++) {
         Node tmp = parent.item(i);
         String name = tmp.getNodeName();
         if (name.contains("blocked")) {
            int blocked = Integer.valueOf(tmp.getTextContent());
            if (blocked != 0) {
               result = true;
            } else {
               result = false;
            }
            break;
         }
      }

      return result;
   }

   private HashMap<String, Object> getSuiteData(Document doc) {
      HashMap<String, Object> result = new HashMap<String, Object>();
      int passed = 0, failed = 0, blocked = 0, asserts = 0, assertsF = 0, errors = 0, exceptions = 0, wd = 0, total = 0;
      boolean truncated;
      String runtime = "";
      String suiteName = getSuiteName(doc);

      passed = getAmtPassed(doc);
      blocked = getAmtBlocked(doc);
      failed = getAmtFailed(doc);
      wd = getAmtwatchdog(doc);
      asserts = getAmtAsserts(doc);
      assertsF = getAmtAssertsF(doc);
      exceptions = getAmtExceptions(doc);
      errors = getAmtErrors(doc);
      total = assertsF + exceptions + errors;
      runtime = getRunTime(doc);
      truncated = getTruncated(doc);

      result.put("passed", passed);
      result.put("blocked", blocked);
      result.put("failed", failed);
      result.put("wd", wd);
      result.put("asserts", asserts);
      result.put("assertsF", assertsF);
      result.put("exceptions", exceptions);
      result.put("errors", errors);
      result.put("total", total);
      result.put("runtime", runtime);
      result.put("suitename", suiteName);
      result.put("testlogs", this.getTestLogs(doc));
      result.put("truncated", truncated);

      return result;
   }

   /**
    * Generate a row in the suite summary report
    *
    * @param suiteName  name of the test suite being processed
    * @param data       data for this test suite
    * @return a single row for output to summary.html
    */

   @SuppressWarnings("unchecked")
   private String generateTableRow(String suiteName,
                                   HashMap<String, Object> data) {
      int passed        = (Integer)data.get("passed");
      int failed        = (Integer)data.get("failed");
      int blocked       = (Integer)data.get("blocked");
      int wd            = (Integer)data.get("wd");
      int asserts       = (Integer)data.get("asserts");
      int assertsF      = (Integer)data.get("assertsF");
      int exceptions    = (Integer)data.get("exceptions");
      int errors        = (Integer)data.get("errors");
      int total         = assertsF + exceptions + errors;
      String runtime    = data.get("runtime").toString();
      boolean truncated = (Boolean)data.get("truncated");
      String hl         = "highlight";
      String uhl        = "unhighlight";
      String runclass   = (blocked == 0) ? "td_run_data_error" : "td_run_data";
      String html;
      String cls;

      if (truncated || passed + failed + blocked == 0) {
         hl += "_truncated";
         uhl += "_truncated";
      }

      /* Row prologue. */
      html = ("<tr id=\"" + count + "\" class=\"" + uhl + "\"" +
                     "    onmouseover=\"this.className='" + hl + "'\"" +
              "    onmouseout=\"this.className='" + uhl + "'\">\n" +
              "   <td class=\"td_file_data\">\n" +
              "      <a href=\"" + suiteName + "/" + suiteName + ".html\">" +
              suiteName + ".xml</a>\n" +
              "   </td>");

      /* Tests column (passed/failed/blocked). */
      html += ("   <td class=\"" + runclass + "\">" +
               (passed + failed) + "/" + (passed + failed + blocked) +
               "</td>\n" +
               "   <td class=\"td_passed_data\">" + passed + "</td>\n" + 
               "   <td class=\"td_failed_data\">" + failed + "</td>\n" +
               "   <td class=\"td_blocked_data\">" + blocked + "</td> \n");

      /* Results column */

      /* Watchdog timer expiries */
      cls = (wd != 0) ? "td_watchdog_error_data" : "td_watchdog_data";
      html += "   <td class=\"" + cls + "\">" + wd + "</td>\n";

      /* Passed asserts */
      html += "   <td class=\"td_assert_data\">"+asserts+"</td>\n";

      /* Failed asserts */
      cls = (assertsF != 0) ? "td_assert_error_data" : "td_assert_data";
      html += "   <td class=\"" + cls + "\">" + assertsF + "</td>\n";

      /* Exceptions */
      cls = (exceptions != 0) ? "td_exceptions_error_data" :
                                "td_exceptions_data";
      html += "   <td class=\"" + cls + "\">" + exceptions + "</td>\n";

      /* Errors */
      cls = (errors != 0) ? "td_exceptions_error_data" : "td_exceptions_data";
      html += "   <td class=\"" + cls + "\">" + errors + "</td>\n";

      /* Total Failures */
      cls = (total != 0) ? "td_total_error_data" : "td_total_data";
      html += "   <td class=\"" + cls + "\">" + total + "</td>\n";

      /* Runtime */
      html += "   <td class=\"td_time_data\">" + runtime + "</td>\n";

      /* Row epilogue */
      html += "</tr>";

      /*
       * Generate the suite report.
       */
      ArrayList<HashMap<String, String>> logs =
         (ArrayList<HashMap<String, String>>)data.get("testlogs");
      VddSuiteReporter reporter = new VddSuiteReporter(suiteName,
                                                       this.basedir, logs);
      reporter.generateReport();
      this.issues.appendIssues(reporter.getIssues());

      return html;
   }

   /**
    * generates the HTML table header for summary report, then returns it
    * @return - String of html table header
    */
   private String generateHTMLHeader() {
      String header = "";
      String line = "";
      InputStream stream = null;

      try {
         String className = this.getClass().getName().replace('.', '/');
         String classJar =  this.getClass().getResource("/" + className + ".class").toString();

         if (classJar.startsWith("jar:")) {
            stream = getClass().getResourceAsStream(this.HTML_HEADER_RESOURCE);
         } else {
            File header_fd = new File(getClass().getResource(this.HTML_HEADER_RESOURCE).getFile());
            stream = new FileInputStream(header_fd);
         }

         InputStreamReader in = new InputStreamReader(stream);
         BufferedReader br = new BufferedReader(in);

         while ((line = br.readLine()) != null) {
            header += line;
            header += "\n";
         }

         br.close();
         in.close();
      } catch (Exception exp ) {
         exp.printStackTrace();
      }

      return header;
   }

   /**
    * generates the HTML table footer for summary report, then returns it
    * @return - String of html table footer
    */
   private String generateHTMLFooter(){
      int n1 = 0;
      int n2 = 0;
      String footerrun = "td_footer_run";
      String failedtd = "td_footer_failed";
      String blockedtd = "td_footer_blocked";

      n1 = passedTests + failedTests;
      n2 = passedTests + failedTests + blockedTests;
      if (n1 != n2) {
         footerrun = "td_footer_run_err";
      }

      if (failedTests == 0) {
         failedtd = "td_footer_failed_zero";
      }

      if (blockedTests == 0) {
         blockedtd = "td_footer_blocked_zero";
      }

      String footer = "<tr id=\"totals\"> \n" +
            "\t <td class=\"td_header_master\">Totals:</td>" +
            String.format("\t <td class=\"%s\">"+(passedTests + failedTests)+"/"+(passedTests + failedTests + blockedTests)+"</td>", footerrun) +
            "\t <td class=\"td_footer_passed\">"+passedTests+"</td>" +
            String.format("\t <td class=\"%s\">"+failedTests+"</td>", failedtd) +
            String.format("\t <td class=\"%s\">"+blockedTests+"</td>", blockedtd) +
            "\t <td class=\"td_footer_watchdog\">"+watchdog+"</td>" +
            "\t <td class=\"td_footer_passed\">"+passedAsserts+"</td>" +
            "\t <td class=\"td_footer_assert\">"+failedAsserts+"</td>" +
            "\t <td class=\"td_footer_exceptions\">"+exceptions+"</td>" +
            "\t <td class=\"td_footer_watchdog\">"+errors+"</td>" +
            "\t <td class=\"td_footer_total\">"+(failedAsserts + exceptions + errors)+"</td>" +
            "\t <td class=\"td_footer_times\">"+printTotalTime(hours, minutes, seconds)+"</td>" +
            "</tr>" +
            "</tbody>" +
            "</table>";

      return footer;
   }

   /**
    * Deal with log files that are missing end tags.
    *
    * <p>This is called when xml processing has failed, and that
    * only happens when the datafile ends prematurely.  From this
    * point of view, there are three scenarios:</p>
    *
    * <ol>
    *   <li>File is empty</li>
    *   <li>File is correctly formed xml, but missing end tags</li>
    *   <li>File ends mid-tag</li>
    * </ol>
    *
    * <p>For the first case, we create an almost empty file with
    * the bare minimum of tags and use the file name for the
    * suite file name.  This should be noticable by the user as a
    * problem.</p>
    *
    * <p>For the second and third, we find the last valid entry
    * and cut the file off there, then add terminating tags.</p>
    *
    * <p>For all cases, a &lt;truncated/&gt; tag is added.  This
    * tells the report generation engine to highlight this file's
    * entry so the user will be incited to look into the problem
    * (e.g. did the test machine crash midway through the
    * test?)</p>
    *
    * @param xml a log file that has already failed parsing
    * @return an InputSource with added end tags suitable for re-parsing
    */

   private InputSource endTagHack(File xml) throws Exception {
      if (xml.length() >= (2L << 31)) {
         /*
          * The CharBuffer below uses an int for its
          * buffer size.  This limits us to files less
          * than 2GB.  That's probably the least of the
          * problems if this code is getting called,
          * but make sure here it's flagged.  Should
          * the user notice and file a bug, this code
          * can be revisited.
          */
         System.out.println("(!)Warning: File > 2GB (" + xml.length() + "). Further truncation will occur.");
      }
      CharBuffer cbuf = CharBuffer.allocate((int)xml.length());
      FileReader f = new FileReader(xml);
      f.read(cbuf);
      cbuf.rewind();

      /* First case */
      if (cbuf.length() == 0) {
         String contents = "<data><truncated/><suite><suitefile>" + xml.getName() + "</suitefile></suite></data>\n";
         return new InputSource(new StringReader(contents));
      }

      /* Second and third cases. */
      String mungedXml = cbuf.toString();

      /* Scan backward through the file, looking for an appropriate end tag. */
      String endTags[] = {"</test>", "</suite>", "</data>"}; // Yes, the order is significant.
      for (int k = cbuf.length() - 1; k >= 0; k--) {
         if (cbuf.charAt(k) == '<') {
            Boolean foundEndTag = false;
            for (int t = 0; t < endTags.length; t++) {
               if (mungedXml.regionMatches(k, endTags[t], 0, endTags[t].length())) {
                  foundEndTag = true;
                  mungedXml = mungedXml.substring(0, k);
               }
               if (foundEndTag) {
                  if (t == 2) {
                     mungedXml += "<truncated/>";
                  }
                  mungedXml += endTags[t];
               }
            }
            if (foundEndTag) {
               break;
            }
         }
      }

      return new InputSource(new StringReader(mungedXml));
   }

   private HashMap<String, Object> parseXMLFile(File xml) throws Exception {
      DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      HashMap<String, Object> result = new HashMap<String, Object>();
      DocumentBuilder db = dbf.newDocumentBuilder();
      db.setErrorHandler(new VddErrorHandler());

      try {
         dom = db.parse(xml);
      } catch(SAXParseException e) {
         System.out.println("(!)Error parsing log file (" + e.getMessage() + ").  Retrying with end tag hack...");
         InputSource is = endTagHack(xml);
         dom = db.parse(is);
         System.out.println("(*)Success!");
      }

      result = getSuiteData(dom);
      return result;
   }

   /**
    * get the number of tests that passed within this suite document
    * @ param d - the Document containing suite run data
    * @ return the number of tests that passed
    *
    */
   private int getAmtPassed(Document d) {
      int n = 0;
      Element el;
      NodeList nl = d.getElementsByTagName("result");
      boolean isrestart = false;
      boolean islibfile = false;

      for (int i = 0; i < nl.getLength(); i ++) {
         el = (Element)nl.item(i);
         if (el.getFirstChild().getNodeValue().compareToIgnoreCase("Passed") == 0) {
            islibfile = isLibTest(nl.item(i));
            isrestart = isRestart(nl.item(i));

            if (isrestart) {
               continue;
            }

            if (islibfile) {
               continue;
            }

            n ++;
         }
      }

      //global passedTests variable
      passedTests += n;
      return n;
   }

   /**
    * get the number of tests that failed within this suite document
    * @ param d - the Document containing suite run data
    * @ return the number of tests that failed
    *
    */
   private int getAmtFailed(Document d) {
      int n = 0;
      Element el;
      boolean isrestart = false;
      boolean isblocked = false;
      NodeList nl = d.getElementsByTagName("result");
      boolean islibfile = false;

      for (int i = 0; i < nl.getLength(); i ++){
         el = (Element)nl.item(i);
         if (el.getFirstChild().getNodeValue().compareToIgnoreCase("Failed") == 0) {
            isrestart = isRestart(nl.item(i));
            isblocked = isBlocked(nl.item(i));
            islibfile = isLibTest(nl.item(i));
            if (isrestart) {
               continue;
            }

            if (isblocked) {
               continue;
            }

            if (islibfile) {
               continue;
            }

            n ++;
         }
      }

      //global failedTests variable
      failedTests += n;
      return n;
   }

   /**
    * get the number of tests that was blocked within this suite document
    * @ param d - the Document containing suite run data
    * @ return the number of tests that was blocked
    *
    */
   private int getAmtBlocked(Document d) {
      int n = 0;
      Element el;
      boolean isrestart = false;
      boolean islibfile = false;
      NodeList nl = d.getElementsByTagName("blocked");

      for (int i = 0; i < nl.getLength(); i ++) {
         el = (Element)nl.item(i);
         if (el.getFirstChild().getNodeValue().compareToIgnoreCase("1") == 0) {
            isrestart = isRestart(nl.item(i));
            islibfile = isLibTest(nl.item(i));
            if (isrestart) {
               continue;
            }

            if (islibfile) {
               continue;
            }

            n ++;
         }
      }

      //global blockedTests variable
      blockedTests += n;
      return n;
   }

   private ArrayList<HashMap<String, String>> getTestLogs(Document d) {
      ArrayList<HashMap<String, String>> result = new ArrayList<HashMap<String,String>>();
      NodeList nodes = d.getElementsByTagName("test");

      for (int i = 0; i <= nodes.getLength() -1; i++) {
         Node currNode = nodes.item(i);
         HashMap<String, String> newHash = new HashMap<String, String>();
         NodeList kids = currNode.getChildNodes();

         for (int x = 0; x <= kids.getLength() -1; x++) {
            Node kidNode = kids.item(x);
            if (kidNode.getNodeName().contains("testlog")) {
               newHash.put(kidNode.getNodeName(), kidNode.getTextContent());
            } else if (kidNode.getNodeName().contains("isrestart")) {
               newHash.put(kidNode.getNodeName(), kidNode.getTextContent().toLowerCase());
            }
         }
         result.add(newHash);
      }

      return result;
   }

   /**
    * get the number of assertions that passed within this suite document
    * @ param d - the Document containing suite run data
    * @ return the number of passed assertions
    *
    */
   private int getAmtAsserts(Document d) {
      int n = 0;
      Element el;
      boolean isrestart = false;
      NodeList nl = d.getElementsByTagName("passedasserts");

      for (int i = 0; i < nl.getLength(); i ++) {
         el = (Element)nl.item(i);
         if (Integer.parseInt(el.getFirstChild().getNodeValue()) > 0){
            isrestart = isRestart(nl.item(i));
            if (isrestart) {
               continue;
            }
            n += Integer.parseInt(el.getFirstChild().getNodeValue());
         }
      }
      //global passedAsserts
      passedAsserts += n;
      return n;
   }

   /**
    * get the number of assertions that failed within this suite document
    * @ param d - the Document containing suite run data
    * @ return the number of failed assertions
    *
    */
   private int getAmtAssertsF(Document d) {
      int n = 0;
      Element el;
      boolean isrestart = false;
      NodeList nl = d.getElementsByTagName("failedasserts");

      for (int i = 0; i < nl.getLength(); i ++) {
         el = (Element)nl.item(i);
         if (Integer.parseInt(el.getFirstChild().getNodeValue()) > 0) {
            isrestart = isRestart(nl.item(i));
            if (isrestart) {
               continue;
            }
            n += Integer.parseInt(el.getFirstChild().getNodeValue());
         }
      }
      //global failedAsserts
      failedAsserts += n;
      return n;
   }

   /**
    * get the number of watchdogs within this suite document
    * @ param d - the Document containing suite run data
    * @ return the number of watchdogs
    *
    */
   private int getAmtwatchdog(Document d) {
      int n = 0;
      Element el;
      boolean isrestart = false;
      NodeList nl = d.getElementsByTagName("watchdog");

      for (int i = 0; i < nl.getLength(); i ++) {
         el = (Element)nl.item(i);
         if (Integer.parseInt(el.getFirstChild().getNodeValue()) > 0) {
            isrestart = isRestart(nl.item(i));
            if (isrestart) {
               continue;
            }
            n += Integer.parseInt(el.getFirstChild().getNodeValue());
         }
      }

      watchdog += n;
      return n;
   }

   /**
    * get the number of exceptions within this suite document
    * @ param d - the Document containing suite run data
    * @ return the number of exceptions
    *
    */
   private int getAmtExceptions(Document d) {
      int n = 0;
      Element el;
      boolean isrestart = false;
      NodeList nl = d.getElementsByTagName("exceptions");

      for (int i = 0; i < nl.getLength(); i ++) {
         el = (Element)nl.item(i);
         if (Integer.parseInt(el.getFirstChild().getNodeValue()) > 0) {
            isrestart = isRestart(nl.item(i));
            if (isrestart) {
              continue;
            }
            n += Integer.parseInt(el.getFirstChild().getNodeValue());
         }
      }
      //global exceptions
      exceptions += n;
      return n;
   }

   private int getAmtErrors(Document d) {
      int n = 0;
      Element el;
      boolean isrestart = false;
      NodeList nl = d.getElementsByTagName("errors");
      for (int i = 0; i < nl.getLength(); i ++) {
         el = (Element)nl.item(i);
         if (Integer.parseInt(el.getFirstChild().getNodeValue()) > 0) {
            isrestart = isRestart(nl.item(i));
            if (isrestart) {
               continue;
            }
            n += Integer.parseInt(el.getFirstChild().getNodeValue());
         }
      }
      //global errors
      errors += n;
      return n;
   }

   /**
    * calculates the running time fromt a suite xml file, and return it in a html-friendly format
    * @param d - document to get time data from
    * @return - total run time for this suite test in String
    */
   private String getRunTime(Document d) {
      String  temp;
      int h = 0, m = 0, s = 0;
      Element el;
      NodeList nl = d.getElementsByTagName("totaltesttime");

      for (int i = 0; i < nl.getLength(); i ++) {
         el = (Element)nl.item(i);
         temp = el.getFirstChild().getNodeValue();
         h += Integer.parseInt(temp.substring(0, temp.indexOf(":")));
         m += Integer.parseInt(temp.substring(2, temp.lastIndexOf(":")));
         s += Integer.parseInt(temp.substring(temp.lastIndexOf(":")+1, temp.indexOf(".")));
      }

      this.hours += h;
      this.minutes += m;
      this.seconds += s;
      return printTotalTime(h, m , s);
   }

   /**
    * formats and returns a correct String representation from inputs of hours, minutes and seconds
    * @param hours
    * @param minutes
    * @param seconds
    * @return correctly formatted time in String
    */
   private String printTotalTime(int h, int m, int s) {
      String time = "";

      //carry over seconds
      while (s >= 60) {
         m ++;
         s -= 60;
      }
      //carry over minutes
      while(m >= 60) {
         h ++;
         m -= 60;
      }

      String ms = ""+ m, ss = ""+ s;
      if (m < 10) {
         ms = "0"+m;
      }

      if (s < 10) {
         ss = "0"+s;
      }
      time = "0"+h+":"+ms+":"+ss;
      return time;
   }

   /**
    * get the name of the suite, without extension
    * @param d
    * @return suite name
    */
   private String getSuiteName(Document d) {
      String name = "";
      NodeList nl = d.getElementsByTagName("suitefile");

      if (nl != null && nl.getLength() > 0) {
         Element el = (Element)nl.item(0);
         name = el.getFirstChild().getNodeValue();
      }

      name = name.substring(0, name.indexOf("."));
      return name;
   }

   /**
    * Get whether the log file was truncated
    */
   private boolean getTruncated(Document d) {
      NodeList nl = d.getElementsByTagName("truncated");
      return nl.getLength() > 0;
   }
}
