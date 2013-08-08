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

import java.io.File;
import java.io.PrintStream;
import java.io.StringReader;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
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

public class VDDReporter {

   /**
    * File header for summary.html
    */

   private final String SUMMARY_HEADER = "summaryreporter-header.txt";

   /**
    * Filename of summary.html
    */

   private final String SUMMARY_FILENAME = "summary.html";

   /**
    * File header for issues.html
    */

   private final String ISSUES_HEADER = "issues-header.txt";

   /**
    * Filename of issues.html
    */

   private final String ISSUES_FILENAME = "issues.html";

   /**
    * List of input XML files.
    */

   private ArrayList<File> xmlFiles;

   /**
    * Base directory of test result files.
    */

   private File basedir;

   /**
    * Collection of counts of errors, warnings, and exceptions from
    * all suites.
    */

   private Issues issues;

   /**
    * Running count of the rows being output in summary.html.
    */

   private int rowCount = 0;


   /**
    * Summary data from the suite log.
    */

   private class SuiteData {
      public int passed = 0;
      public int failed = 0;
      public int blocked = 0;
      public int asserts = 0;
      public int failedAsserts = 0;
      public int errors = 0;
      public int exceptions = 0;
      public int watchdog = 0;
      public double runtime = 0;
      public boolean truncated = false;
      public String suitename = null;
      public ArrayList<File> testlogs = null;

      public SuiteData() {
         this.testlogs = new ArrayList<File>();
      }

      /**
       * Add the values from a SuiteData object to this object.
       *
       * @param n  the new SuiteData values
       */

      public void append(SuiteData n) {
         this.passed        += n.passed;
         this.failed        += n.failed;
         this.blocked       += n.blocked;
         this.asserts       += n.asserts;
         this.failedAsserts += n.failedAsserts;
         this.errors        += n.errors;
         this.exceptions    += n.exceptions;
         this.watchdog      += n.watchdog;
         this.runtime       += n.runtime;
         this.truncated      = this.truncated || n.truncated;
         this.testlogs.addAll(n.testlogs);
      }
   }


   /**
    * VDDReporter entry point.
    *
    * @param args  command line arguments
    */

   public static void main(String[] args) {
      ArrayList<File> xml = null;
      File dir = null;
      HashMap<String,File> opts = new HashMap<String,File>();

      for (String arg: args) {
         if (arg.startsWith("--suitefile=")) {
            opts.put("suitefile", new File(arg.replace("--suitefile=", "")));
         } else if (arg.startsWith("--suitedir=")) {
            opts.put("suitedir", new File(arg.replace("--suitedir=", "")));
         } else if (arg.equals("--help")) {
            System.out.println("Usage:\n" +
                               "   VDDReporter --suitefile=<suite.xml>\n" +
                               "   VDDReporter --suitedir=<suite dir>");
            System.exit(0);
         }
      }

      if (opts.containsKey("suitefile")) {
         File f = opts.get("suitefile");

         if (!f.exists()) {
            System.out.println("(!)Suite file '" + f + "' does not exist");
            System.exit(3);
         }

         System.out.println("(*)Processing suite file: '" + f + "'...");
         xml = new ArrayList<File>();
         xml.add(f);
         dir = f.getAbsoluteFile().getParentFile();
      } else if (opts.containsKey("suitedir")) {
         dir = opts.get("suitedir");
         System.out.println("(*)Processing suite directory: '" + dir + "'.");

         File fs[] = dir.listFiles(new java.io.FilenameFilter() {
               public boolean accept(File dir, String name) {
                  boolean ok = name.toLowerCase().endsWith(".xml");
                  if (ok) {
                     System.out.println("(*)Found Suite File: '" + name + "'.");
                  }
                  return ok;
               }
            });

         if (fs == null) {
            System.out.println("(!)Suite directory '" + dir + "' is not valid");
            System.exit(4);
         }

         xml = new ArrayList<File>(Arrays.asList(fs));

      } else {
         System.out.println("(!)Missing --suitefile or --suitedir!");
         System.exit(2);
      }

      System.out.println("(*)Generating Summary file...");
      VDDReporter r = new VDDReporter(xml, dir);
      r.generateReport();
   }


   /**
    * Instantiate a VDDReporter object
    *
    * @param xmlFiles  list of VDD suite output files
    * @param path      directory containing the XML files
    */

   public VDDReporter(ArrayList<File> xmlFiles, File path) {
      this.xmlFiles = xmlFiles;
      this.basedir = path;
      this.issues = new Issues();
   }


   /**
    * Convert the VDD output logs into a report.
    */

   public void generateReport() {
      HashMap<String,String> summaryRows = new HashMap<String,String>();
      SuiteData totals = new SuiteData();

      for (File xml: this.xmlFiles) {
         boolean truncated = false;
         ArrayList<SuiteData> suites = new ArrayList<SuiteData>();

         Document doc = loadSuiteSummary(xml);
         if (doc == null) {
            continue;
         }

         /*
          * Reading each XML file and processing the results needs to
          * be done in two steps because the <truncated/> tag, if
          * present, will be at the bottom of the file but applies to
          * all entries.
          */

         NodeList suiteNodes = doc.getDocumentElement().getChildNodes();
         for (int k = 0; k < suiteNodes.getLength(); k++) {
            Node suite = suiteNodes.item(k);

            if (suite.getNodeType() != Node.ELEMENT_NODE) {
               continue;
            }

            String nm = suite.getNodeName().toLowerCase();

            if (nm.equals("truncated")) {
               truncated = true;
            }

            if (!nm.equals("suite")) {
               continue;
            }

            suites.add(getSuiteData(suite));
         }

         for (SuiteData suiteData: suites) {
            suiteData.truncated = suiteData.truncated || truncated;
            totals.append(suiteData);
            summaryRows.put(suiteData.suitename, processSuite(suiteData));
         }
      }

      this.createSummary(summaryRows, totals);
      this.createIssues();
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

   private InputSource endTagHack(File xml)
      throws java.io.FileNotFoundException, java.io.IOException {
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
      java.io.FileReader f = new java.io.FileReader(xml);
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


   /**
    * Read the suite summary log file.
    *
    * @param  the suite summary log file
    * @return suite summary data
    */

   private Document loadSuiteSummary(File xml) {
      DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      DocumentBuilder db = null;
      Document dom = null;

      try {
         db = dbf.newDocumentBuilder();
      } catch (javax.xml.parsers.ParserConfigurationException e) {
         System.out.println("(!)Failed to instantiate XML DB: " + e);
         return null;
      }

      db.setErrorHandler(new XMLErrorHandler());

      try {
         try {
            dom = db.parse(xml);
         } catch(SAXParseException e) {
            System.out.println("(!)Error parsing log file " + xml +
                               " (" + e.getMessage() + "). " +
                               "Retrying with end tag hack...");
            InputSource is = endTagHack(xml);
            dom = db.parse(is);
            System.out.println("(*)Success!");
         }
      } catch (java.io.FileNotFoundException e) {
         System.out.println("(!)Suite file '" + xml + "' not found: " + e);
      } catch (java.io.IOException e) {
         System.out.println("(!)Error reading " + xml + ": " + e);
      } catch (org.xml.sax.SAXException e) {
         System.out.println("(!)XML error in " + xml + ": " + e);
      }

      return dom;
   }


   /**
    * Read the summary data from the suite log.
    *
    * @param doc  XML document representing the suite log
    * @return SuiteData object with the data
    */

   private SuiteData getSuiteData(Node suite) {
      SuiteData d = new SuiteData();
      
      NodeList nodes = suite.getChildNodes();
      for (int k = 0; k < nodes.getLength(); k++) {
         Node node = nodes.item(k);

         if (node.getNodeType() != Node.ELEMENT_NODE) {
            continue;
         }

         Element e = (Element)node;
         String nm = e.getNodeName().toLowerCase();

         if (nm.equals("suitefile")) {
            d.suitename = e.getTextContent().replaceFirst("^(.+).xml$", "$1");
         } else if (nm.equals("runtime")) {
            String rt = e.getTextContent();

            d.runtime = (new Double(rt.substring(0, rt.indexOf(":"))) * 3600 +
                         new Double(rt.substring(2, rt.lastIndexOf(":"))) * 60 +
                         new Double(rt.substring(rt.lastIndexOf(":") + 1,
                                                 rt.length())));
         } else if (nm.equals("test")) {
            /* per-test values */
            boolean islib = false;
            File testlog = null;
            boolean passed = false;
            boolean isrestart = false;
            int failedAsserts = 0;
            int exceptions = 0;
            int errors = 0;
            boolean blocked = false;
            int asserts = 0;
            int watchdog = 0;
            boolean truncated = false;
            NodeList testNodes = e.getChildNodes();

            for (int t = 0; t < testNodes.getLength(); t++) {
               Node tn = testNodes.item(t);

               if (tn.getNodeType() != Node.ELEMENT_NODE) {
                  continue;
               }

               String tnn = tn.getNodeName().toLowerCase();

               if (tnn.equals("testfile")) {
                  String p = (new File(tn.getTextContent())).getParent();
                  islib = (p != null) && p.contains("lib");
               } else if (tnn.equals("testlog")) {
                  testlog = new File(tn.getTextContent());
               } else if (tnn.equals("result")) {
                  passed = tn.getTextContent().toLowerCase().equals("passed");
               } else if (tnn.equals("isrestart")) {
                  isrestart = new Boolean(tn.getTextContent());
               } else if (tnn.equals("failedasserts")) {
                  failedAsserts = new Integer(tn.getTextContent());
               } else if (tnn.equals("exceptions")) {
                  exceptions = new Integer(tn.getTextContent());
               } else if (tnn.equals("errors")) {
                  errors = new Integer(tn.getTextContent());
               } else if (tnn.equals("blocked")) {
                  blocked = tn.getTextContent().equals("1");
               } else if (tnn.equals("passedasserts")) {
                  asserts = new Integer(tn.getTextContent());
               } else if (tnn.equals("watchdog")) {
                  watchdog = new Integer(tn.getTextContent());
               } else if (tnn.equals("truncated")) {
                  truncated = true;
               }
            }

            d.truncated = d.truncated || truncated;

            if (isrestart) {
               continue;
            }

            if (!blocked && !islib) {
               if (passed) {
                  d.passed++;
               } else {
                  d.failed++;
               }
            }

            d.blocked       += blocked ? 1 : 0;
            d.watchdog      += watchdog;
            d.asserts       += asserts;
            d.failedAsserts += failedAsserts;
            d.exceptions    += exceptions;
            d.errors        += errors;
            d.testlogs.add(testlog);
         }
      }

      return d;
   }


   /**
    * Process a single suite
    *
    * <p>This method causes the suite/test report heirarchy to be
    * generated for a single suite.  It also generates the
    * corresponding row for the suite summary report.</p>
    *
    * @param d data for this test suite
    * @return a single row for output to summary.html
    */

   private String processSuite(SuiteData d) {
      Suite r = new Suite(d.suitename, this.basedir, d.testlogs);
      r.generateReport();
      this.issues.append(r.getIssues());

      return summaryRow(d);
   }


   /**
    * Read an entire file into a String
    *
    * <p>The file will be opened from either the directory tree or
    * VDDReporter's jar file.</p>
    *
    * @param name  name of the file to open
    * @return the contents of the file
    */

   private String readFile(String name) {
      Class c = VDDReporter.class;
      String nm = c.getName().replace('.', '/');
      String jar = c.getResource("/" + nm + ".class").toString();
      java.io.InputStream is = null;

      if (jar.startsWith("jar:")) {
         is = c.getResourceAsStream(name);
      } else {
         try {
            is = new java.io.FileInputStream(new File(c.getResource(name).getFile()));
         } catch (java.io.FileNotFoundException e) {
            System.out.println("(!)" + name + " not found: " + e);
            return "";
         }
      }

      java.io.BufferedReader b =
         new java.io.BufferedReader(new java.io.InputStreamReader(is));

      String out = "", line;
      try {
         while ((line = b.readLine()) != null) {
            out += line + "\n";
         }
      } catch (java.io.IOException e) {
         System.out.println("(!)Error reading " + name + ": " + e);
      }

      try { b.close(); } catch (java.io.IOException e) {}

      return out;
   }


   /**
    * Format a runtime value.
    *
    * @param tm  runtime in seconds
    * @return formatted runtime
    */

   private String fmtTime(double tm) {
      return String.format("%02.0f:%02.0f:%02d", Math.floor(tm / 3600),
                           Math.floor((tm % 3600) / 60), Math.round(tm % 60));
   }


   /**
    * Create summary.html
    *
    * @param summaryRows  non-sorted per-suite summary information
    * @param totals       collated suite data
    */

   private void createSummary(HashMap<String,String> summaryRows,
                              SuiteData totals) {
      File summaryFile = new File(this.basedir, SUMMARY_FILENAME);
      System.out.println("(*)SummaryFile: " + summaryFile);
      PrintStream s = null;

      try {
         s = new PrintStream(new java.io.FileOutputStream(summaryFile));
      } catch (java.io.FileNotFoundException e) {
         System.out.println("(!)Unable to create summary.html: " + e);
         return;
      }

      s.print(readFile(SUMMARY_HEADER));

      String suiteNames[] = summaryRows.keySet().toArray(new String[0]);
      Arrays.sort(suiteNames);

      for (String suiteName: suiteNames) {
         s.print(summaryRows.get(suiteName));
      }

      s.print(summaryTotals(totals));
      s.print("</table>\n\n</body>\n</html>\n");

      if (s.checkError()) {
         System.out.println("(!)Error occurred when writing " + summaryFile);
      }

      s.close();
   }


   /**
    * Generate a row in summary.html
    *
    * @param d  data for this row
    * @return formatted HTML row
    */

   private String summaryRow(SuiteData d) {
      int total  = d.failedAsserts + d.exceptions + d.errors;
      String hl  = "highlight";
      String uhl = "unhighlight";
      String row;
      String cls;

      if (d.truncated || d.passed + d.failed + d.blocked == 0) {
         hl += "_truncated";
         uhl += "_truncated";
      }

      /* Row prologue. */
      row = ("  <tr id=\"" + this.rowCount++ + "\" class=\"" + uhl + "\"" +
             " onmouseover=\"this.className='" + hl + "'\"" +
             " onmouseout=\"this.className='" + uhl + "'\">\n" +
             "    <td class=\"td_file_data\">\n" +
             "      <a href=\"" + d.suitename + "/" + d.suitename + ".html\">" +
             d.suitename + ".xml</a>\n" +
             "    </td>\n");

      /* Tests column (passed/failed/blocked). */
      cls = (d.blocked != 0) ? "td_run_data_error" : "td_run_data";
      row += ("    <td class=\"" + cls + "\">" +
              (d.passed + d.failed) + "/" + (d.passed + d.failed + d.blocked) +
              "</td>\n" +
              "    <td class=\"td_passed_data\">" + d.passed + "</td>\n" + 
              "    <td class=\"td_failed_data\">" + d.failed + "</td>\n" +
              "    <td class=\"td_blocked_data\">" + d.blocked + "</td>\n");

      /* Results column */

      /* Watchdog timer expiries */
      cls = (d.watchdog != 0) ? "td_watchdog_error_data" : "td_watchdog_data";
      row += "    <td class=\"" + cls + "\">" + d.watchdog + "</td>\n";

      /* Passed asserts */
      row += "    <td class=\"td_assert_data\">" + d.asserts + "</td>\n";

      /* Failed asserts */
      cls = (d.failedAsserts != 0) ? "td_assert_error_data" : "td_assert_data";
      row += "    <td class=\"" + cls + "\">" + d.failedAsserts + "</td>\n";

      /* Exceptions */
      cls = (d.exceptions != 0) ? "td_exceptions_error_data" :
                                  "td_exceptions_data";
      row += "    <td class=\"" + cls + "\">" + d.exceptions + "</td>\n";

      /* Errors */
      cls = (d.errors != 0) ? "td_exceptions_error_data" : "td_exceptions_data";
      row += "    <td class=\"" + cls + "\">" + d.errors + "</td>\n";

      /* Total Failures */
      cls = (total != 0) ? "td_total_error_data" : "td_total_data";
      row += "    <td class=\"" + cls + "\">" + total + "</td>\n";

      /* Runtime */
      row += "    <td class=\"td_time_data\">" + fmtTime(d.runtime) + "</td>\n";

      /* Row epilogue */
      row += "  </tr>\n";

      return row;
   }


   /**
    * Create the HTML table footer for the summary report
    *
    * @param t  test run totals
    * @return the table footer
    */

   private String summaryTotals(SuiteData t) {
      String footerrun = (t.blocked == 0 ? "td_footer_run" :
                                           "td_footer_run_err");
      String failedtd = (t.failed == 0 ? "td_footer_failed_zero" :
                                         "td_footer_failed");
      String blockedtd = (t.blocked == 0 ? "td_footer_blocked_zero" :
                                           "td_footer_blocked");

      return ("  <tr id=\"totals\">\n" +
              "    <td class=\"td_header_master\">Totals:</td>\n" +
              "    <td class=\"" + footerrun + "\">" +
              (t.passed + t.failed) + "/" + (t.passed + t.failed + t.blocked) +
              "</td>\n" +
              "    <td class=\"td_footer_passed\">" + t.passed + "</td>\n" +
              "    <td class=\"" + failedtd + "\">" + t.failed + "</td>\n" +
              "    <td class=\"" + blockedtd + "\">" + t.blocked + "</td>\n" +
              "    <td class=\"td_footer_watchdog\">" + t.watchdog + "</td>\n" +
              "    <td class=\"td_footer_passed\">" + t.asserts + "</td>\n" +
              "    <td class=\"td_footer_assert\">" + t.failedAsserts + "</td>\n" +
              "    <td class=\"td_footer_exceptions\">" + t.exceptions + "</td>\n" +
              "    <td class=\"td_footer_watchdog\">" + t.errors + "</td>\n" +
              "    <td class=\"td_footer_total\">" +
              (t.failedAsserts + t.exceptions + t.errors) + "</td>\n" +
              "    <td class=\"td_footer_times\">" + fmtTime(t.runtime) + "</td>\n" +
              "  </tr>\n");
   }


   /**
    * Create issues.html
    */

   private void createIssues() {
      File issuesFile = new File(this.basedir, ISSUES_FILENAME);
      PrintStream i = null;

      System.out.println("(*)Issues file: " + issuesFile);

      try {
         i = new PrintStream(new java.io.FileOutputStream(issuesFile));
      } catch (java.io.FileNotFoundException e) {
         System.out.println("(!)Unable to create " + issuesFile + ": " + e);
         return;
      }

      i.print(readFile(ISSUES_HEADER));
      i.print(issuesTable("Errors"));
      i.print("<hr/>\n");
      i.print(issuesTable("Exceptions"));
      i.print("<hr/>\n");
      i.print(issuesTable("Warnings"));
      i.print("</body></html>\n");

      if (i.checkError()) {
         System.out.println("(*)Error occurred when writing " + issuesFile);
      }

      i.close();
   }


   /**
    * Create a table of sorted, collated issues.
    *
    * @param type  type of issue (errors, exceptions, warnings) in title case
    * @return formatted HTML table of issues
    */

   private String issuesTable(String type) {
      final HashMap<String,Integer> m = this.issues.get(type.toLowerCase());
      String t;
      String keys[] = m.keySet().toArray(new String[0]);

      Arrays.sort(keys, new Comparator<String>() {
            public int compare(String o1, String o2) {
               return m.get(o1).compareTo(m.get(o2));
            }
         });

      t = ("<table>\n" +
           "  <tr>\n" +
           "    <td class=\"td_header_master\" colspan=\"2\">" + type + ":</td>\n" +
           "  </tr>\n" +
           "  <tr>\n" +
           "    <td class=\"td_header_count\">Count:</td>\n" +
           "    <td class=\"td_header_sub\">Issue:</td>\n" +
           "  </tr>\n");

      for (String key: keys) {
         t += ("  <tr class=\"unhighlight\"" +
               " onmouseout=\"this.className='unhighlight'\"" +
               " onmouseover=\"this.className='highlight'\">\n" +
               "    <td class=\"td_count_data\">" + m.get(key) + "</td>\n" +
               "    <td class=\"td_file_data\">" + key.replaceAll("&", "&quot;")
                                                      .replaceAll("<", "&lt;")
                                                      .replaceAll(">", "&gt;") +
               "</td>\n" +
               "  </tr>\n");
      }

      t += "</table>\n";

      return t;
   }
}
