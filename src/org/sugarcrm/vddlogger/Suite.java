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
    * Output results file.
    */

   private File reportFile;


   /**
    * Relative path to suite and test output files.
    */

   private String htmlPath;


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
    * @param suiteName  name of suite
    * @param base       base directory of suite
    * @param logs       ArrayList of log files
    */

   public Suite(String suiteName, File base, ArrayList<File> logs) {
      this.suiteName = suiteName;
      this.logs = logs;
      this.issues = new Issues();

      /*
       * If there is a suite directory (VDD was running against suites
       * instead of just tests), then the HTML relative path will have
       * the the suite name prepended and the report file path will
       * also incorporate it.  If there is no such directory, then the
       * suite name must be absent.
       */
      File suiteDir = new File(base, suiteName);
      this.htmlPath = suiteName + "/";
      if (!suiteDir.exists()) {
         suiteDir = base;
         this.htmlPath = "";
      }

      this.reportFile = new File(suiteDir, this.suiteName + ".html");

      System.out.println("(*)Suite directory: " + suiteDir);
      System.out.println("(*)Suite report file: " + this.reportFile);
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

         Test t = new Test(file);
         t.generateReport();
         report.add(testSummary(++n, t));
         this.issues.append(t.getIssues());
      }

      report.add("  </table>\n" +
                 "</body>\n" +
                 "</html>\n");

      VDDReporter.writeFile(this.reportFile, report);
   }


   /**
    * Create a per-test summary error table for the suite report
    *
    * @param t  the test being processed
    * @return a formatted HTML table within a cell of error info
    */

   private String errorSummary(Test t) {
      int fa  = t.getFailedAssertCount();
      int exc = t.getExceptionCount();
      int err = t.getErrorCount();
      int wd  = t.getWatchdogCount();

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
    * @param n  one-up number of the current file
    * @param t  the test being processed
    * @return a formatted HTML row for the suite summary file
    */

   public String testSummary(int n, Test t) {
      String html = ("    <tr id=\"" + n + "\"" +
                     " onMouseOver=\"this.className='highlight'\"" +
                     " onMouseOut=\"this.className='tr_normal'\"" +
                     " class=\"tr_normal\">\n" +
                     "      <td class=\"td_file_data\">" + n + "</td>\n" +
                     "      <td class=\"td_file_data\">" +
                     t.getLogfileName().replaceAll("(-\\d+)*\\.log$", ".xml") +
                     "</td>\n");

      if (t.getBlocked()) {
         html += ("      <td class=\"td_issues_data\"></td>\n" +
                  "      <td class=\"_data\">Blocked</td>\n");
      } else if (t.getResult() == -1) {
         html += (errorSummary(t) +
                  "      <td class=\"td_failed_data\">Failed</td>\n");
      } else {
         html += ("      <td class=\"td_issues_data\"></td>\n" +
                  "      <td class=\"td_passed_data\">Passed</td>\n");
      }

      html += ("      <td class=\"td_report_data\">\n" +
               "        <a href=\"" + t.getReport() + "\">Report Log</a>\n" +
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


   /**
    * Get the path to this suite's report.
    *
    * @return path to the suite report
    */

   public String getReport() {
      return this.htmlPath + this.reportFile.getName();
   }
}
