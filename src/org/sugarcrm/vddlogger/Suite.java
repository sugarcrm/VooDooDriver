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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.PrintStream;
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


   private int count = 0;
   private FileReader input = null;
   private BufferedReader br = null;
   private String strLine, tmp = null;


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
         System.out.println("(!)Failed to create suite report '" + rf + "'.");
         return;
      }

      generateHTMLHeader();


      for (File file: this.logs) {
         /*
          * Skip directories, hidden files, and files without .log extensions.
          */
         if (!file.isFile() ||
             file.isHidden() ||
             !file.getName().endsWith(".log")) {
            System.out.println("(!)Logfile (" + file + ") is not valid.");
            continue;
         }

         readNextLog(file);

         String baseName = file.getName().replaceAll(".log$", "");

         /* XXX: Wow. Just wow. */
         //get last line
         try {
            while ((tmp = br.readLine()) != null) {
               strLine = tmp;
            }
         } catch (IOException e) {
            e.printStackTrace();
         }
         //find log status, generate table row

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

   /**
    * generates the html header for the report file
    */
   private void generateHTMLHeader() {
      String title = "suite "+suiteName+".xml test results";
      String header = "";
      InputStream stream = null;
      String line = null;
      boolean found_title = false;
      boolean found_suitename = false;

      try {
         String className = this.getClass().getName().replace('.', '/');
         String classJar =  this.getClass().getResource("/" + className + ".class").toString();

         if (classJar.startsWith("jar:")) {
            stream = getClass().getResourceAsStream(SUITE_HEADER);
         } else {
            File header_fd = new File(getClass().getResource(SUITE_HEADER).getFile());
            stream = new FileInputStream(header_fd);
         }

         InputStreamReader in = new InputStreamReader(stream);
         BufferedReader br = new BufferedReader(in);

         while ((line = br.readLine()) != null) {
            if ( (found_title != true) && (line.contains("__TITLE__"))) {
               line = line.replace("__TITLE__", title);
               found_title = true;
            }

            if ((found_suitename != true) && (line.contains("__SUITENAME__")) ) {
               line = line.replace("__SUITENAME__", suiteName);
               found_suitename = true;
            }

            header += line;
            header += "\n";
         }

      } catch (Exception exp) {
         exp.printStackTrace();
      }

      repFile.print(header);

   }

   public Issues getIssues() {
      return this.issues;
   }

   /**
    * sets up FileReader and BufferedReader for the next report file
    * @param inputFile - a properly formatted .log SODA report file
    */
   private void readNextLog(File inputFile) {

      try {
         input = new FileReader(inputFile);
         br = new BufferedReader(input);
      } catch (FileNotFoundException e) {
         System.out.printf("(!)Error: Failed to find file: '%s'!\n", inputFile);
      } catch (Exception e) {
         System.out.printf("(!)Error: Reading file: '%s'!\n", inputFile);
      }
   }

}
