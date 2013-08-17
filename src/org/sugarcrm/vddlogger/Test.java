/*
 * Copyright 2011-2013 SugarCRM Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you
 * may not use this file except in compliance with the License.  You
 * may may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  Please see the License for the specific language
 * governing permissions and limitations under the License.
 */

package org.sugarcrm.vddlogger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Process results from an individual VDD test.
 */

public class Test {

   /**
    * File header for per-test HTML report.
    */

   private final String TEST_HEADER = "reportlogheader.txt";

   /**
    * Input file.
    */

   private File input;

   /**
    * Output file.
    */

   private File output;

   /**
    * Issues in this test.
    */

   private Issues issues;

   /**
    * One up number used for generating HTML IDs for backtraces.
    */

   private int backTraceID = 0;


   /**
    * Create a Test object.
    *
    * @param input  VDD per-test log file
    */

   public Test(File input) {
      this.issues = new Issues();
      this.input = input;

      /*
       * The output file name is the same as the input file name with
       * "Report-" prepended and the extension changed from .log to
       * .html.
       */
      this.output = new File(input.getParent(),
                             "Report-" +
                             input.getName().replaceAll(".log$", ".html"));

      System.out.println("(*)Test report file: " + this.output);
   }


   /**
    * Get the HTML relative path to the report file.
    *
    * @return the report path
    */

   public String getReport() {
      return this.output.getName();
   }


   /**
    * Generate an HTML report file.
    */

   public void generateReport() {
      BufferedReader in = null;
      ArrayList<String> report = new ArrayList<String>();

      try {
         in = new BufferedReader(new FileReader(this.input));
      } catch (FileNotFoundException e) {
         System.out.println("(!)Failed to open input " + this.input + ": " + e);
         return;
      }

      report.add(VDDReporter.readFile(TEST_HEADER));

      try {
         String line;
         while ((line = in.readLine()) != null) {
            processIssues(line);
            report.add(processLine(line));
         }
      } catch (IOException e) {
         System.out.println("(!)Input error in " + this.input + ": " + e);
      }

      report.add("  </table>\n" +
                 "</center>\n" +
                 "</body>\n" +
                 "</html>\n");

      VDDReporter.writeFile(this.output, report);
   }


   /**
    * Add issues to the Issues data structure as they're found
    *
    * XXX: Fold this into processLine
    *
    * @param line  the current log file line
    */

   private void processIssues(String line) {
      if (line.contains("(*")) {
         return;
      }

      line = line.replaceFirst("\\[.*\\]", "");

      if (line.startsWith("(!")) {
         line = line.replaceFirst("\\(\\!\\)", "");
         if (line.startsWith("--Exception")) {
            return;
         } else if (line.startsWith("Exception")) {
            this.issues.exception(line);
         } else {
            this.issues.error(line);
         }
      } else if (line.startsWith("(W")) {
         line = line.replaceFirst("\\(W\\)", "");
         this.issues.warning(line);
      }
   }


   /**
    * Get the Issues data structure
    *
    * @return Issues data structure
    */

   public Issues getIssues() {
      return this.issues;
   }


   /**
    * Generate an HTML table row from a log file line
    *
    * @param line - A line from the raw .log file
    * @return HTML table row
    */

   private String processLine(String line){
      /* Split line into 4 components: [date-time](type)message */
      Pattern p = Pattern.compile("^\\[([^-]+)-" + // [date-
                                  "([^]]+)\\]" +   // time]
                                  "\\((.)\\)" +    // (status)
                                  "(.+)$");        // message
      Matcher m = p.matcher(line);
      if (!m.find()) {
         System.out.println("(!)Ill-formatted line in file '" + line + "'");
         return "";
      }

      String dtg = m.group(1) + "<br/>" + m.group(2);
      String msg = m.group(4).replaceAll("&",  "&amp;")
                             .replaceAll("\"", "&quot;")
                             .replaceAll("<",  "&lt;")
                             .replaceAll(">",  "&gt;");
      String trStyle, logType;

      if (m.group(3).equals("!")) {
         trStyle =  "tr_error";
         logType = "Failure";
      } else if (m.group(3).equals("W")) {
         trStyle = "tr_warning";
         logType = "Warning";
      } else if (msg.contains("Assert Passed")) {
         trStyle = "tr_assert_passed";
         logType = "Assertion Passed";
      } else if (msg.matches("^(Lib|Test|Module):")) {
         trStyle = "tr_module";
         logType = "Un/Load";
      } else {
         trStyle = "tr_normal";
         logType = "Log";
      }

      /* Log message types that require more extensive processing. */
      if (msg.startsWith("--Exception Backtrace")) {
         msg = formatBT(msg);
         logType = "Backtrace";
      } else if (msg.matches("^(HTML Saved|Screenshot file).*")) {
         msg = formatFileAnchor(msg);
      } else if (msg.startsWith("Soda Test Report")) {
         msg = formatResults(msg);
         logType = "Results";
      }

      /* Bold Test/Lib/Module and everything in single quotes. */
      if (!logType.equals("Backtrace")) {
         msg = msg.replaceAll("(^(Test|Lib|Module):|'[^']+')", "<b>$1</b>");
      }

      return ("    <tr class=\"" + trStyle + "\"" +
              " onMouseOver=\"this.className='highlight';\"" +
              " onMouseOut=\"this.className='" + trStyle + "';\">\n" +
              "      <td class=\"td_date\">" + dtg + "</td>\n" +
              "      <td class=\"td_msgtype\">" + logType + "</td>\n" +
              "      <td>" + msg + "</td>\n" +
              "    </tr>\n");
   }


   /**
    * Format a backtrace.
    *
    * <p>Backtraces can be expanded and collapsed and have each stack
    * frame on a single line.</p>
    *
    * @param msg  the log message with a backtrace
    * @return formatted backtrace
    */

   private String formatBT(String msg) {
      String btID = "bt_div_" + backTraceID;
      String hrefID = "href_div_" + backTraceID;
      backTraceID += 1;

      return ("<a id=\"" + hrefID + "\" " +
              "href=\"javascript:showdiv('" + btID + "','" + hrefID + "');\">" +
              "[ Expand Backtrace ]<b>+</b></a>" +
              "<div id=\"" + btID + "\" style=\"display: none;\">" +
              msg.replaceAll("--", "<br/>") + "<br/>" +
              "<a " +
              "href=\"javascript:hidediv('" + btID + "','" + hrefID + "');\">" +
              "[ Collapse Backtrace ]<b>-</b></a></div>");
   }


   /**
    * Format an &quot;HTML saved&quot; or a screenshot message
    *
    * <p>This creates a link to the saved file.</p>
    *
    * @param msg  the log message
    * @return the log message with a link to the file
    */

   private String formatFileAnchor(String msg) {
      Pattern p = Pattern.compile("^(HTML Saved|Screenshot file).+" +
                                  "(saved-html|screenshots)." +
                                  "([^/\\\\]+)$");
      Matcher m = p.matcher(msg);

      if (!m.find()) {
         return msg;
      }

      String t = "<b>" + m.group(1) + "</b>";
      String f = m.group(2) + "/" + m.group(3);

      return t + ": <a href=\"" + f + "\" target=\"_blank\">" + f + "</a>";
   }


   /**
    * Emit a TD with red or default text, depending on the input
    *
    * @param val  if non-zero the color is red, otherwise it's the default
    * @return a formatted TD
    */

   private String checkedTD(int val) {
      return (val == 0 ? "<td>" : "<td style=\"color: red;\">") + val + "</td>";
   }


   /**
    * Format the test results line into a nested table.
    *
    * @param msg  the test results line
    * @return an HTML table with the test results
    */

   private String formatResults(String msg) {
      /*
       * XXX: Parse the string in this less-efficient manner to enable
       * passing values to Suite in a future change.
       */
      Pattern p = Pattern.compile("--testlog:(([^-]|-[^-])+)" +
                                  "--result:(-?\\d+)" +
                                  "--isrestart:(true|false)" +
                                  "--failedasserts:(\\d+)" +
                                  "--exceptions:(\\d+)" +
                                  "--errors:(\\d+)" +
                                  "--blocked:(\\d+)" +
                                  "--passedasserts:(\\d+)" +
                                  "--watchdog:(\\d+)");
      Matcher m = p.matcher(msg);
      if (!m.find()) {
         return msg;
      }

      int res = Integer.valueOf(m.group(3));
      int fa  = Integer.valueOf(m.group(5));
      int exc = Integer.valueOf(m.group(6));
      int err = Integer.valueOf(m.group(7));
      int b   = Integer.valueOf(m.group(8));
      int wd  = Integer.valueOf(m.group(10));

      String rs = ("          <tr class=\"tr_normal\"" +
                   " onMouseOver=\"this.className='highlight_report'\"" +
                   " onMouseOut=\"this.className='tr_normal'\">\n");
      String re = "\n          </tr>\n";
      String i = "            ";

      return ("\n" +
              "        <table style=\"font-weight: bold;\">\n" +
              rs + i + "<td>Test Log:</td>\n" +
              i + "<td>" + m.group(1) + "</td>" + re +
              rs + i + "<td>Result:</td>\n" +
              i + checkedTD(res) + re +
              rs + i + "<td>Restart:</td>\n" +
              i + "<td>" + m.group(4) + "</td>" + re +
              rs + i + "<td>Failed Asserts:</td>\n" +
              i + checkedTD(fa) + re +
              rs + i + "<td>Exceptions:</td>\n" +
              i + checkedTD(exc) + re +
              rs + i + "<td>Errors:</td>\n" +
              i + checkedTD(err) + re +
              rs + i + "<td>Blocked:</td>\n" +
              i + checkedTD(b) + re +
              rs + i + "<td>Passed Asserts:</td>\n" +
              i + "<td>" + m.group(9) + "</td>" + re +
              rs + i + "<td>Watchdog:</td>\n" +
              i + checkedTD(wd) + re +
              "        </table>\n");
   }
}
