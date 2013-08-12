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
    * Output HTML report.
    */

   private PrintStream repFile;

   /**
    * Maximium length of the results line in the VDD log file.
    */

   private final int RESULTS_LINE_LENGTH = 320;

   private int count = 0;


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
    * Generate an HTML report file.
    */

   public void generateReport() {

      File rf = new File(this.suiteDir, this.suiteName + ".html");
      try {
         this.repFile = new PrintStream(new FileOutputStream(rf));
      } catch (FileNotFoundException e) {
         System.out.println("(!)Failed to create suite report '" + rf +
                            "': " + e);
         return;
      }

      this.repFile.print(VDDReporter.readFile(SUITE_HEADER)
                         .replace("__TITLE__", "Suite " + this.suiteName +
                                  ".xml test results")
                         .replace("__SUITENAME__", this.suiteName));

      for (File file: this.logs) {
         /* Skip directories and files without log extensions. */
         if (!file.isFile() || !file.getName().endsWith(".log")) {
            System.out.println("(!)Log file (" + file + ") is not valid.");
            continue;
         }

         String baseName = file.getName().replaceAll(".log$", "");
         byte b[] = new byte[RESULTS_LINE_LENGTH];;

         try {
            RandomAccessFile lf = new RandomAccessFile(file, "r");

            if (lf.length() > RESULTS_LINE_LENGTH) {
               lf.seek(lf.length() - RESULTS_LINE_LENGTH);
            }

            lf.readFully(b);
         } catch (FileNotFoundException e) {
            System.out.println("(!)Could not open " + file + ": " + e);
            continue;
         } catch (IOException e) {
            System.out.println("(*)Failed to read " + file + ": " + e);
            continue;
         }

         String strLine = new String(b);

         if (strLine.contains("blocked:1")) {
            generateTableRow(baseName, 2, null);
         } else if (strLine.contains("result:-1")) {
            generateTableRow(baseName, 0, strLine);
         } else {
            generateTableRow(baseName, 1, null);
         }

         try {
            System.out.println("(*)Log File: " + file);
            VddLogToHTML log2html = new VddLogToHTML(file.toString());
            log2html.generateReport();
            Issues tmpissues = log2html.getIssues();
            this.issues.append(tmpissues);
            tmpissues = null;
         } catch (VDDLogException e) {
            System.err.println("Failed to process " + file + ": " +
                               e.getMessage());
         }
      }

      repFile.print("\n</table>\n</body>\n</html>\n");
      repFile.close();
   }


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
    * @param fileName name of the .log report file this table row represents
    * @param status   0 == passed, 1 == failed, otherwise == blocked
    * @param line     line in log file
    */

   public void generateTableRow(String fileName, int status, String line){
      String html = "\t<td class=\"td_issues_data\"></td>\n";
      String tmp_filename = fileName;

      tmp_filename = tmp_filename.replaceAll("-\\d+-\\d+-\\d+-\\d+-\\d+-\\d+-\\d+", "");

      count ++;
      repFile.println("<tr id=\""+count+"\" onMouseOver=\"this.className='highlight'\" "+
            "onMouseOut=\"this.className='tr_normal'\" class=\"tr_normal\" >");
      repFile.println("\t<td class=\"td_file_data\">"+count+"</td>");
      repFile.println("\t<td class=\"td_file_data\">"+tmp_filename+".xml</td>");

      switch (status) {
         case 0:
            html = GenMiniErrorTable(line);
            html += "\t<td class=\"td_failed_data\">Failed</td>";
            repFile.println(html);
         break;

         case 1:
            html += "\t<td class=\"td_passed_data\">Passed</td>";
            repFile.println(html);
         break;

         default:
            html += "\t<td class=\"_data\">Blocked</td>";
            repFile.println(html);
      }

      repFile.println("\t<td class=\"td_report_data\"><a href='Report-"+fileName+".html'>Report Log</a></td>");
      repFile.println("</tr>");
   }


   public Issues getIssues() {
      return this.issues;
   }
}
