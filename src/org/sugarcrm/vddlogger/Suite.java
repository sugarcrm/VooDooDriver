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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Process VDD suite log files.
 */

public class Suite {

   /**
    * File header for per-suite HTML report.
    */

   private static String SUITE_HEADER = "suitereporter-header.txt";

   /**
    * Name of the suite being processed.
    */

   private String suiteName;

   /**
    * ArrayList of suite log files.
    */

   private ArrayList<File> logs;

   /**
    * Directory with suite log files.
    */

   private File suiteDir;

   /**
    * Issues in this suite.
    */

   private Issues issues;

   /**
    * Maximium length of the results line in the VDD log file.
    */

   private final int RESULTS_LINE_LENGTH = 320;


   /**
    * Instantiate a Suite object
    *
    * @param nm    name of suite
    * @param base  base directory of suite
    * @param logs  ArrayList of log files
    */

   public Suite(String nm, File base, ArrayList<File> logs) {
      this.suiteName = nm;
      this.logs = logs;
      this.issues = new Issues();
      this.suiteDir = new File(base, nm);

      System.out.println("(*)Suite directory: " + this.suiteDir);

      if (!this.suiteDir.exists()) {
         /* The log file being processed is completely empty. */
         System.out.println("(!)Warning: Creating missing output directory '" +
                            this.suiteDir + "'.");
         this.suiteDir.mkdir();
      }
   }


   /**
    * Read the summary line from the bottom of the log file.
    *
    * @param f  the log file
    * @return the last line in the file
    */

   private String readSummary(File f) {
      byte b[] = new byte[RESULTS_LINE_LENGTH];;

      try {
         RandomAccessFile lf = new RandomAccessFile(f, "r");

         if (lf.length() > RESULTS_LINE_LENGTH) {
            lf.seek(lf.length() - RESULTS_LINE_LENGTH);
         }

         lf.readFully(b);
      } catch (FileNotFoundException e) {
         System.out.println("(!)Could not open " + f + ": " + e);
      } catch (IOException e) {
         System.out.println("(!)Failed to read " + f + ": " + e);
      }

      return new String(b);
   }


   /**
    * Generate an HTML report file.
    */

   public void generateReport() {
      ArrayList<String> report = new ArrayList<String>();

      report.add(VDDReporter.readFile(SUITE_HEADER)
                 .replace("__TITLE__", "Suite " + this.suiteName +
                          ".xml test results")
                 .replace("__SUITENAME__", this.suiteName));
      int n = 0;

      for (File file: this.logs) {
         /* Skip directories and files without log extensions. */
         if (!file.isFile() || !file.getName().endsWith(".log")) {
            System.out.println("(!)Log file (" + file + ") is not valid.");
            continue;
         }

         System.out.println("(*)Log File: " + file);
         Test t = new Test(file);

         String summary = readSummary(file);
         report.add(testSummary(++n, file, t.getReport(), summary));

         t.generateReport();
         this.issues.append(t.getIssues());
      }

      report.add("  </table>\n" +
                 "</body>\n" +
                 "</html>\n");

      VDDReporter.writeFile(new File(this.suiteDir, this.suiteName + ".html"),
                            report);
   }


   /**
    * Create a per-test summary error table for the suite report
    *
    * @param summary  summary line from log file
    * @return a formatted HTML table within a cell of error info
    */

   private String errorSummary(String summary) {
      Pattern p = Pattern.compile("--failedasserts:(\\d+)" +
                                  "--exceptions:(\\d+)" +
                                  "--errors:(\\d+)" +
                                  "--blocked:(\\d+)" +
                                  "--passedasserts:(\\d+)" +
                                  "--watchdog:(\\d+)");
      Matcher m = p.matcher(summary);
      if (!m.find() || m.groupCount() != 6) {
         return ("      <td class=\"td_issues_data\">\n" +
                 "        <span style=\"font-weight: bold; color: red;\">\n" +
                 "          Invalid results summary line in log file.\n" +
                 "        </span>\n" +
                 "      </td>\n");
      }

      int fa  = Integer.valueOf(m.group(1));
      int exc = Integer.valueOf(m.group(2));
      int err = Integer.valueOf(m.group(3));
      int wd  = Integer.valueOf(m.group(6));

      return ("      <td class=\"td_issues_data\">\n" +
              "        <table class=\"table_sub\">\n" +
              "          <tr>\n" +
              "            <td class=\"td_sub\">" +
              "WD:&nbsp;<span style=\"color: " + (wd == 0 ? "black" : "red") +
              ";\">" + wd + "</span></td>\n" +
              "            <td class=\"td_sub\">" +
              "Exp:&nbsp;<span style=\"color: " + (exc == 0 ? "black" : "red") +
              ";\">" + exc + "</span></td>\n" +
              "            <td class=\"td_sub\">" +
              "FA:&nbsp;<span style=\"color: " + (fa == 0 ? "black" : "red") +
              ";\">" + fa + "</span></td>\n" +
              "            <td class=\"td_sub\">" +
              "E:&nbsp;<span style=\"color: " + (err == 0 ? "black" : "red") +
              ";\">" + err + "</span></td>\n" +
              "          </tr>\n" +
              "         </table\n" +
              "      </td>\n");
   }


   /**
    * Generate an HTML table row based on data from .log report file.
    *
    * @param n        one-up number of the current file
    * @param logfile  the current log file
    * @param report   the current report file
    * @param summary  summary line from log file
    * @return a formatted HTML row for the suite summary file
    */

   public String testSummary(int n, File logfile, File report, String summary) {
      String html = ("    <tr id=\"" + n + "\"" +
                     " onMouseOver=\"this.className='highlight'\"" +
                     " onMouseOut=\"this.className='tr_normal'\"" +
                     " class=\"tr_normal\">\n" +
                     "      <td class=\"td_file_data\">" + n + "</td>\n" +
                     "      <td class=\"td_file_data\">" +
                     logfile.getName().replaceAll("(-\\d+)*\\.log$", ".xml") +
                     "</td>\n");

      if (summary.contains("blocked:1")) {
         html += ("      <td class=\"td_issues_data\"></td>\n" +
                  "      <td class=\"_data\">Blocked</td>\n");
      } else if (summary.contains("result:-1")) {
         html += (errorSummary(summary) +
                  "      <td class=\"td_failed_data\">Failed</td>\n");
      } else {
         html += ("      <td class=\"td_issues_data\"></td>\n" +
                  "      <td class=\"td_passed_data\">Passed</td>\n");
      }

      html += ("      <td class=\"td_report_data\">\n" +
               "        <a href=\"" + report + "\">Report Log</a>\n" +
               "      </td>\n" +
               "    </tr>\n");

      return html;
   }


   /**
    * Get this suite's Issues data structure
    *
    * @return  Issues for this suite
    */

   public Issues getIssues() {
      return this.issues;
   }
}
