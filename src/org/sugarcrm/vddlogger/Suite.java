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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap;
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
    * Write a file
    *
    * @param file   the file to create
    * @param lines  the contents of the file
    */

   private void writeFile(File file, ArrayList<String> lines) {
      PrintStream rf = null;

      try {
         rf = new PrintStream(new FileOutputStream(file));
      } catch (FileNotFoundException e) {
         System.out.println("(!)Failed to create suite report '" + rf +
                            "': " + e);
         return;
      }

      for (String line: lines) {
         rf.print(line);
      }

      rf.close();
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

         String summary = readSummary(file);
         report.add(testSummary(++n,
                                file.getName().replaceAll(".log$", ""),
                                summary));

         VddLogToHTML log2html = null;

         try {
            log2html = new VddLogToHTML(file.toString());
         } catch (VDDLogException e) {
            System.err.println("Failed to process " + file + ": " + e);
            continue;
         }

         log2html.generateReport();
         this.issues.append(log2html.getIssues());
      }

      report.add("  </table>\n" +
                 "</body>\n" +
                 "</html>\n");

      writeFile(new File(this.suiteDir, this.suiteName + ".html"),
                report);
   }


   /**
    *
    */

   private HashMap<String, String>findErrorInfo(String line) {
      HashMap<String, String> result = new HashMap<String, String>();
      String[] items = {
         "failedasserts",
         "exceptions",
         "errors",
         "watchdog"
      };
      Pattern p = null;
      Matcher m = null;

      for (int i = 0; i <= items.length -1; i++) {
         String value = "";
         String reg = String.format("%s:(\\d+)", items[i]);
         p = Pattern.compile(reg, Pattern.CASE_INSENSITIVE);

         m = p.matcher(line);
         if (!m.find()) {
            System.out.printf("(!)Error: Failed to find needed matches when parsing a failed tests results line!\n");
            System.out.printf("--)Line: '%s'\n\n", line);
            value = "";
         } else {
            value = m.group(1);
         }

         result.put(items[i], value);
      }

      return result;
   }


   /**
    *
    */

   private String GenMiniErrorTable(String line) {
      String result = "";
      String exceptions = "";
      String watchdog = "";
      String fasserts = "";
      String errors = "";
      String color = "";

      findErrorInfo(line); // XXX

      try {
         HashMap<String, String> data = this.findErrorInfo(line);
         fasserts = data.get("failedasserts");
         exceptions = data.get("exceptions");
         watchdog = data.get("watchdog");
         errors = data.get("errors");

         result = "\t<td class=\"td_issues_data\">\n\t<table class=\"table_sub\">\n"+
         "\t<tr>\n";

         if (Integer.valueOf(watchdog) > 0) {
            color = "#FF0000";
         } else {
            color = "#000000";
         }

         result += "\t\t<td class=\"td_sub\">"+
               "WD:&nbsp;<font color=\"" + color + "\">" + watchdog + "</font></td>\n";

         if (Integer.valueOf(exceptions) > 0) {
            color = "#FF0000";
         } else {
            color = "#000000";
         }

         result += "\t\t<td class=\"td_sub\">Exp:&nbsp;<font color=\"" +
               color + "\">" + exceptions + "</font></td>\n";

         if (Integer.valueOf(fasserts) > 0) {
            color = "#FF0000";
         } else {
            color = "#000000";
         }

         result += "\t\t<td class=\"td_sub\">"+
               "FA:&nbsp;<font color=\"" + color + "\">" + fasserts + "</font></td>\n";

         if (Integer.valueOf(errors) > 0) {
            color = "#FF0000";
         } else {
            color = "#000000";
         }

         result += "\t\t<td class=\"td_sub\">"+
               "E:&nbsp;<font color=\"" + color + "\">" + errors + "</font></td>\n"+
               "\t</tr>\n\t</table>\n\t</td>\n";

      } catch (Exception exp) {
         exp.printStackTrace();
      }

      return result;
   }


   /**
    * Generate an HTML table row based on data from .log report file.
    *
    * @param n        one-up number of the current file
    * @param file     name of the current log report file
    * @param summary  summary line from log file
    * @return a formatted HTML row for the suite summary file
    */

   public String testSummary(int n, String file, String summary) {
      String sf = file.replaceAll("-\\d+-\\d+-\\d+-\\d+-\\d+-\\d+-\\d+", "");

      String html = ("    <tr id=\"" + n + "\"" +
                     " onMouseOver=\"this.className='highlight'\"" +
                     " onMouseOut=\"this.className='tr_normal'\"" +
                     " class=\"tr_normal\">\n" +
                     "      <td class=\"td_file_data\">" + n + "</td>\n" +
                     "      <td class=\"td_file_data\">" + sf + ".xml</td>\n");

      if (summary.contains("blocked:1")) {
         html += ("      <td class=\"td_issues_data\"></td>\n" +
                  "      <td class=\"_data\">Blocked</td>\n");
      } else if (summary.contains("result:-1")) {
         html += GenMiniErrorTable(summary);
         html += "      <td class=\"td_failed_data\">Failed</td>\n";
      } else {
         html += ("      <td class=\"td_issues_data\"></td>\n" +
                  "      <td class=\"td_passed_data\">Passed</td>\n");
      }

      html += ("      <td class=\"td_report_data\">\n" +
               "        <a href='Report-" + file + ".html'>Report Log</a>\n" +
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
