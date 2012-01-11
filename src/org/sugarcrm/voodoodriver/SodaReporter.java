/*
Copyright 2011 SugarCRM Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
Please see the License for the specific language governing permissions and
limitations under the License.
*/

package voodoodriver;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.io.FilenameUtils;

public class SodaReporter {

   private String resultDir = "";
   private String reportLog = null;
   private FileOutputStream reportFD = null;
   private int Blocked = 0;
   private int Exceptions = 0;
   private int FailedAsserts = 0;
   private int PassedAsserts = 0;
   private int OtherErrors = 0;
   private int WatchDog = 0;
   private String LineSeparator = null;
   private int SavePageNum = 0;
   private SodaBrowser browser = null;
   private boolean isRestart = false;
   private String testName = null;
   private boolean saveOnWarning = false;
   private boolean saveOnError = false;
   private boolean saveOnAssertFailed = false;
   private boolean saveOnException = false;
   private boolean saveOnWatchDog = false;
   private boolean saveHTML = false;

   public SodaReporter(String reportName, String resultDir) {
      Date now = new Date();
      String frac = String.format("%1$tN", now);
      String date_str = String.format("%1$tm-%1$td-%1$tY-%1$tI-%1$tM-%1$tS", now);
      frac = frac.subSequence(0, 3).toString();
      date_str += String.format("-%s", frac);

      this.LineSeparator = System.getProperty("line.separator");

      if (resultDir != null) {
         File dir = new File(resultDir);
         if (!dir.exists()) {
            dir.mkdirs();
         }

         this.resultDir = resultDir;
      } else {
         this.resultDir = System.getProperty("user.dir");
      }

      reportLog = this.resultDir + "/" + reportName + "-" + date_str + ".log";
      reportLog = FilenameUtils.separatorsToSystem(reportLog);
      System.out.printf("ReportFile: %s\n", reportLog);

      try {
         reportFD = new FileOutputStream(reportLog);
      } catch (Exception exp) {
         exp.printStackTrace();
      }
   }

   public void setTestName(String name) {
      this.testName = name;
   }

   public void setIsRestTest(boolean restart) {
      this.isRestart = restart;
   }

   public void setSaveHTML(String setting, SodaBrowser browser) {
      this.browser = browser;
      this.saveHTML = true;
      String[] opts = setting.split(",");

      for (int i = 0; i <= opts.length -1; i++) {
         opts[i] = opts[i].toLowerCase();
         if (opts[i].contains("warning")) {
            this.saveOnWarning = true;
         } else if (opts[i].contains("error")) {
            this.saveOnError = true;
         } else if (opts[i].contains("assertfail")) {
            this.saveOnAssertFailed = true;
         } else if (opts[i].contains("exception")) {
            this.saveOnException = true;
         } else if (opts[i].contains("watchdog")) {
            this.saveOnWatchDog = true;
         } else if (opts[i].contains("all")) {
            this.saveOnWatchDog = true;
            this.saveOnException = true;
            this.saveOnAssertFailed = true;
            this.saveOnError = true;
            this.saveOnWarning = true;
         }
      }
   }

   public String getLogFileName() {
      return this.reportLog;
   }

   public SodaTestResults getResults() {
      SodaTestResults result = null;
      Integer res = 0;

      result = new SodaTestResults();
      result.put("testlog", this.reportLog);
      result.put("blocked", this.Blocked);
      result.put("exceptions", this.Exceptions);
      result.put("failedasserts", this.FailedAsserts);
      result.put("passedasserts", this.PassedAsserts);
      result.put("watchdog", this.WatchDog);
      result.put("errors", this.OtherErrors);
      result.put("isrestart", this.isRestart);

      if (this.Blocked > 0 || this.Exceptions > 0 || this.FailedAsserts > 0 || this.OtherErrors > 0) {
         res = -1;
      }

      result.put("result", res);

      return result;
   }

   private String replaceLineFeed(String str) {
      str = str.replaceAll("\n", "\\\\n");
      return str;
   }

   private void _log(String msg) {
      Date now = new Date();
      String frac = String.format("%1$tN", now);
      String date_str = String.format("%1$tm/%1$td/%1$tY-%1$tI:%1$tM:%1$tS", now);

      frac = frac.subSequence(0, 3).toString();
      date_str += String.format(".%s", frac);

      msg = replaceLineFeed(msg);
      String logstr = "[" + date_str + "]" + msg + this.LineSeparator;

      if (msg.isEmpty()) {
         msg = "Found empty message!";
      }

      try {
         this.reportFD.write(logstr.getBytes());
         System.out.printf("%s\n", msg);
      } catch (Exception exp) {
         exp.printStackTrace();
      }
   }

   public void closeLog() {
      try {
         this.reportFD.close();
         this.reportFD = null;
      } catch (Exception exp) {
         exp.printStackTrace();
      }
   }

   public void Log(String msg) {
      this._log("(*)" + msg);
   }

   public void Warn(String msg) {
      this._log("(W)" + msg);

      if (this.saveOnWarning) {
         this.SavePage();
      }
   }

   public void ReportError(String msg) {
      this._log(String.format("(!)%s", msg));
      this.OtherErrors += 1;

      if (this.saveOnError) {
         this.SavePage();
      }
   }

   public void ReportWatchDog() {
      this.WatchDog = 1;

      if (this.saveOnWatchDog) {
         this.SavePage();
      }
   }

   public void ReportBlocked() {
      this.Blocked = 1;
   }

   /*
    * ReportException -- Method
    *    This method formats a java exception class into a SODA log entry.  Both the message and the stack
    *    trace are reformatted and printed to the SODA log file, and the console.
    *
    * Input:
    *    e: The exception that happened.
    *
    * Output:
    *    None.
    *
    */
   public void ReportException(Exception e) {
      this.Exceptions += 1;
      String msg = "--Exception Backtrace: ";
      StackTraceElement[] trace = e.getStackTrace();
      String message = "";

      if (e.getMessage() != null) {
         String[] msg_lines = e.getMessage().split("\\n");
         for (int i = 0; i <= msg_lines.length -1; i++) {
            message += msg_lines[i] + "  ";
         }

         this._log("(!)Exception raised: " + message);

         for (int i = 0; i <= trace.length -1; i++) {
            String tmp = trace[i].toString();
            msg += "--" + tmp;
         }
      } else {
         msg = "Something really bad happened here and the exception is null!!!";
         e.printStackTrace();
      }

      this._log("(!)" + msg);
      if (this.saveOnException) {
         this.SavePage();
      }
   }

   public boolean isRegex(String str) {
      boolean result = false;
      Pattern p = Pattern.compile("^\\/");
      Matcher m = p.matcher(str);

      p = Pattern.compile("\\/$|\\/\\w+$");
      Matcher m2 = p.matcher(str);

      if (m.find() && m2.find()) {
         result = true;
      } else {
         result = false;
      }

      return result;
   }

   public String strToRegex(String val) {
      String result = "";
      val = val.replaceAll("\\\\", "\\\\\\\\");
      val = val.replaceAll("^/", "");
      val = val.replaceAll("/$", "");
      val = val.replaceAll("/\\w$", "");
      result = val;
      return result;
   }

   public boolean Assert(String msg, boolean state, boolean expected) {
      boolean result = false;
      boolean fail = false;
      String status = "";

      if (state == expected) {
         this.PassedAsserts += 1;
         status = "(*)Assert Passed: ";
      } else {
         this.FailedAsserts += 1;
         status = "(!)Assert Failed: ";
         fail = true;
      }

      status = status.concat(msg);
      this._log(status);

      if (fail && this.saveOnAssertFailed) {
         this.SavePage();
      }

      return result;
   }

   public boolean Assert(String value, String src) {
      boolean result = false;
      String msg = "";

      if (isRegex(value)) {
         value = this.strToRegex(value);
         Pattern p = Pattern.compile(value, Pattern.MULTILINE);
         Matcher m = p.matcher(src);
         if (m.find()) {
            this.PassedAsserts += 1;
            msg = String.format("Assert Passed, Found: '%s'.", value);
            this.Log(msg);
            result = true;
         } else {
            this.FailedAsserts += 1;
            msg = String.format("(!)Assert Failed for find: '%s'!", value);
            this._log(msg);

            if (this.saveOnAssertFailed) {
               this.SavePage();
            }

            result = false;
         }
      } else {
         if (src.contains(value)) {
            this.PassedAsserts += 1;
            msg = String.format("Assert Passed, Found: '%s'.", value);
            this.Log(msg);
            result = true;
         } else {
            this.FailedAsserts += 1;
            msg = String.format("(!)Assert Failed for find: '%s'!", value);
            this._log(msg);

            if (this.saveOnAssertFailed) {
               this.SavePage();
            }
            result = false;
         }
      }

      return result;
   }

   public boolean AssertNot(String value, String src) {
      boolean result = false;
      String msg = "";

      if (isRegex(value)) {
         value = this.strToRegex(value);
         if (src.matches(value)) {
            this.FailedAsserts += 1;
            msg = String.format("(!)Assert Failed, Found Unexpected text: '%s'.", value);
            this._log(msg);
            if (this.saveOnAssertFailed) {
               this.SavePage();
            }
            result = false;
         } else {
            this.PassedAsserts += 1;
            msg = String.format("Assert Passed did not find: '%s' as expected.", value);
            this.Log(msg);
            result = true;
         }
      } else {
         if (src.contains(value)) {
            this.FailedAsserts += 1;
            msg = String.format("(!)Assertnot Failed: Found: '%s'.", value);
            this._log(msg);

            if (this.saveOnAssertFailed) {
               this.SavePage();
            }

            result = false;
         } else {
            this.PassedAsserts += 1;
            msg = String.format("Assert Passed did not find: '%s' as expected.", value);
            this.Log(msg);
            result = true;
         }
      }

      return result;
   }

   public void SavePage() {
      File dir = null;
      File newfd = null;
      String htmldir = this.resultDir;
      String new_save_file = "";
      String src = "";
      String testname = "";

      if (!this.saveHTML) {
         return;
      }

      src = this.browser.getPageSource();

      htmldir = htmldir.concat("/saved-html");
      dir = new File(htmldir);
      if (!dir.exists()) {
         dir.mkdir();
      }

      if (this.testName != null) {
         File tmp = new File(this.testName);
         testname = tmp.getName();
         testname = testname.substring(0, testname.length() -4);
         testname = String.format("%s-", testname);
         tmp = null;
      }

      new_save_file = htmldir;
      new_save_file = new_save_file.concat(String.format("/%ssavedhtml-%d.html", testname, this.SavePageNum));
      new_save_file = FilenameUtils.separatorsToSystem(new_save_file);
      this.SavePageNum += 1;

      newfd = new File(new_save_file);

      try {
         BufferedWriter bw = new BufferedWriter(new FileWriter(newfd));
         bw.write(src);
         bw.close();
         //this.LastSavedPage = new_save_file;
         //this.CurrentMD5 = SodaUtils.MD5(src);
         this.Log(String.format("HTML Saved: %s", new_save_file));
      } catch (Exception exp) {
         this.ReportException(exp);
      }
   }

   protected void finalize() throws Throwable {
       try {
          if (this.reportFD != null) {
             this.reportFD.close();
          }
       } finally {
           super.finalize();
       }
   }
}
