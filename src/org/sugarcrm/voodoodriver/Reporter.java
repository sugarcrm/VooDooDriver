/*
 * Copyright 2011-2012 SugarCRM Inc.
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

package org.sugarcrm.voodoodriver;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.util.Date;
import org.apache.commons.io.FilenameUtils;
import org.openqa.selenium.Alert;

public class Reporter {

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
   private Browser browser = null;
   private boolean isRestart = false;
   private String testName = null;

   /**
    * When to save the current HTML page.  Keys are the possible
    * events, and the values are true or false.
    */

   private VDDHash saveHtmlOn;

   /**
    * Saved HTML page file name index.
    */

   private int saveHtmlIdx = 0;

   /**
    * When to take a screenshot.  Keys are the possible events, and
    * the values are true or false.
    */

   private VDDHash screenshotOn;

   /**
    * Screenshot file name index.
    */

   private int screenshotIdx = 0;


   /**
    * Whether to terminate the current thread on error.
    */

   private boolean haltOnFailure = false;


   /**
    * Instantiate a Reporter object.
    *
    * @param reportName
    * @param resultDir
    */

   public Reporter(String reportName, String resultDir, VDDHash config) {
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
      } catch (java.io.FileNotFoundException e) {
         System.err.println("(!)Unable to create report file: " + e);
      }

      /* Initialize screenshot and savehtml events */
      String ssEvents[] = {"warning", "error", "assertfail", "exception",
                           "watchdog"};
      this.saveHtmlOn = new VDDHash();
      this.screenshotOn = new VDDHash();
      for (String ssEvent: ssEvents) {
         this.saveHtmlOn.put(ssEvent, false);
         this.screenshotOn.put(ssEvent, false);
      }

      this.haltOnFailure = (Boolean)config.get("haltOnFailure");
   }

   public void setTestName(String name) {
      this.testName = name;
   }

   public void setIsRestTest(boolean restart) {
      this.isRestart = restart;
   }

   public void setBrowser(Browser browser) {
      this.browser = browser;
   }


   /**
    * Set the events for saving the current HTML page.
    *
    * Input is expected to a string that is either "all" or a
    * comma-separated list of events.  Valid events are:
    *
    * <ul><li>warning</li>
    *     <li>error</li>
    *     <li>assertfail</li>
    *     <li>exception</li>
    *     <li>watchdog</li></ul>
    *
    * @param events  list of events
    */

   public void setSaveHTML(String events) {
      if (events.equals("all")) {
         for (String key: this.saveHtmlOn.keySet()) {
            this.saveHtmlOn.put(key, true);
         }
      } else {
         for (String event: events.split(",")) {
            if (!this.saveHtmlOn.containsKey(event)) {
               System.out.println("(!)Unrecognized event in savehtml list: " +
                                  event);
               continue;
            }
            this.saveHtmlOn.put(event, true);
         }
      }
   }


   /**
    * Set the events for taking a screenshot.
    *
    * Input is expected to a string that is either "all" or a
    * comma-separated list of events.  Valid events are:
    *
    * <ul><li>warning</li>
    *     <li>error</li>
    *     <li>assertfail</li>
    *     <li>exception</li>
    *     <li>watchdog</li></ul>
    *
    * @param events  list of events
    */

   public void setScreenshot(String events) {
      if (events.equals("all")) {
         for (String key: this.screenshotOn.keySet()) {
            this.screenshotOn.put(key, true);
         }
      } else {
         for (String event: events.split(",")) {
            if (!this.screenshotOn.containsKey(event)) {
               System.out.println("(!)Unrecognized event in screenshot list: " +
                                  event);
               continue;
            }
            this.screenshotOn.put(event, true);
         }
      }
   }


   /**
    * Return the name of the log file.
    *
    * @return the name of the log file
    */

   public String getLogFileName() {
      return this.reportLog;
   }


   /**
    * Return the results of this VDD run.
    *
    * @return VDD test results
    */

   public TestResults getResults() {
      TestResults result = null;
      Integer res = 0;

      result = new TestResults();
      result.put("testlog", this.reportLog);
      result.put("blocked", this.Blocked);
      result.put("exceptions", this.Exceptions);
      result.put("failedasserts", this.FailedAsserts);
      result.put("passedasserts", this.PassedAsserts);
      result.put("watchdog", this.WatchDog);
      result.put("errors", this.OtherErrors);
      result.put("isrestart", this.isRestart);

      if (this.Blocked > 0 ||
          this.Exceptions > 0 ||
          this.FailedAsserts > 0 ||
          this.OtherErrors > 0) {
         res = -1;
      }

      result.put("result", res);

      return result;
   }


   /**
    * Terminate the current thread.
    *
    * There's a long discussion in the Java documentation about why
    * using Thread.stop() is a terrible, terrible thing.  However, the
    * alternate offered, in combination with the klunky way Java
    * handles exceptions means that any program wanting to terminate a
    * thread abnormally must design this ability in from the ground up
    * -- not the case with VDD.  Consequently, despite the "dangers"
    * involved in stopping a running thread, it's done here.  It won't
    * matter anyways as VDD will be terminating itself shortly.
    */

   private void killTestThread() {
      System.err.println("(!)Error seen and haltOnFailure set: terminating.");
      Thread.currentThread().stop();
   }


   /**
    * Escape the ASCII character 0xa.
    *
    * @param str  string with possibly unescaped line feeds
    * @return string with all line feeds replaced by '\n'
    */

   private String replaceLineFeed(String str) {
      str = str.replaceAll("\n", "\\\\n");
      return str;
   }


   /**
    * Write a string to the log file.
    *
    * <p>This is the only routine that writes to the log file.</p>
    *
    * @param msg  string to log
    */

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

      System.out.printf("%s\n", msg);

      try {
         this.reportFD.write(logstr.getBytes());
      } catch (java.io.IOException e) {
         System.err.println("(!)Error writing to report file: " + e);
      }
   }


   /**
    * Close the log file.
    */

   public void closeLog() {
      try {
         this.reportFD.close();
      } catch (java.io.IOException e) {
         System.err.println("(!)Error closing report file: " + e);
      }
      this.reportFD = null;
   }


   /**
    * Log a normal message.
    *
    * <p>Normal messages are prepended with '(*)'.</p>
    *
    * @param msg  the message to be logged
    */

   public void log(String msg) {
      this._log("(*)" + msg);
   }


   /**
    * Log a warning.
    *
    * <p>Warning messages are prepended with '(W)'.</p>
    *
    * @param msg  the warning to be logged
    */

   public void warning(String msg) {
      this._log("(W)" + msg);

      if ((Boolean)this.saveHtmlOn.get("warning")) {
         this.SavePage();
      }
      if ((Boolean)this.screenshotOn.get("warning")) {
         this.screenshot();
      }
   }


   /**
    * Log an error.
    *
    * <p>Error messages are prepended with '(!)'.</p>
    *
    * @param msg  the error to be logged.
    */

   public void error(String msg) {
      this._log(String.format("(!)%s", msg));
      this.OtherErrors += 1;

      if ((Boolean)this.saveHtmlOn.get("error")) {
         this.SavePage();
      }
      if ((Boolean)this.screenshotOn.get("error")) {
         this.screenshot();
      }

      if (this.haltOnFailure) {
         killTestThread();
      }
   }


   /**
    * Log the expiration of the watchdog timer.
    *
    * <p>The watchdog timer runs in the main VDD thread.
    * Periodically, the <code>threadTime</code> value of the test
    * thread is polled.  If the last update time is over the watchdog
    * threshold, then the thread is terminated and this method is
    * called.</p>
    *
    * @param seconds  the time since last update of <code>threadTime</code>
    */

   public void watchdog(long seconds) {
      this._log(String.format("(!)Test watchdogged out after: '%d' seconds!",
                              seconds));

      this.WatchDog = 1;

      if ((Boolean)this.saveHtmlOn.get("watchdog")) {
         this.SavePage();
      }
      if ((Boolean)this.screenshotOn.get("watchdog")) {
         this.screenshot();
      }
   }


   /**
    * Indicate that the current test is on the block list.
    */

   public void setBlocked() {
      this.Blocked = 1;
   }


   /**
    * Accept and log an unhandled alert.
    *
    * <p>Should the test script leave an alert up, an
    * UnhandledAlertException will eventually be thrown by selenium.
    * While from a test's point of view, the timing of this exception
    * seems non-deterministic, it is thrown when the first "do
    * something" call is made to selenium after the alert's appearance
    * (just retrieving data from the page won't trigger it).</p>
    *
    * <p>Since the exception it generates needs to be logged and
    * logging an exception can fetch the page source which will throw
    * an exception, the alert is handled by the Reporter class which
    * has ways to log exceptions without triggering a page fetch.</p>
    *
    * @param e  the UnhandledAlertException
    */

   public void unhandledAlert(org.openqa.selenium.WebDriverException e) {
      try {
         Alert alert = this.browser.getDriver().switchTo().alert();
         String alertText = alert.getText();
         /*
          * Presumably, accept will be more likely to Do The Right
          * Thing(TM) WRT getting rid of alerts and moving on, but it
          * depends entirely on how the page in question is written.
          */
         alert.accept();

      } finally {
         justLogTheException("(!)Unhandled alert found and dismissed. " +
                             "Alert text is \"" + alertText + "\"", e);
      }
   }


   /**
    * Log the exception only.
    *
    * This helper routine is needed since some of the Reporter methods
    * could need to report an exception.
    *
    */

   private void justLogTheException(String msg, Throwable e) {
      String em = e.getMessage();
      String estr = "ReportException: Exception message is null!";

      if (em != null) {
         em = em.replaceAll("\\n", "  ");

         if (msg == null) {
            estr = em;
         } else {
            estr = msg + ": " + em;
         }
      } else if (msg != null) {
         estr = msg;
      }

      this._log("(!)" + estr);

      String bt = "--Exception Backtrace: ";
      for (StackTraceElement el: e.getStackTrace()) {
         bt += "--" + el.toString();
      }

      this._log("(!)" + bt);
      this.Exceptions += 1;
   }


   /**
    * Log an exception.
    *
    * This method formats a java exception into a log entry.  Both the
    * message and the stack trace are reformatted and printed to the
    * SODA log file and the console.
    *
    * @param e  the exception to report
    */

   public void exception(Throwable e) {
      this.exception(null, e);
   }


   /**
    * Log an exception and additional information.
    *
    * This method formats a java exception into a log entry.  Both the
    * message and the stack trace are reformatted and printed to the
    * SODA log file and the console.
    *
    * @param msg  additional information about the exception
    * @param e    the exception to report
    */

   public void exception(String msg, Throwable e) {
      justLogTheException(msg, e);

      if ((Boolean)this.saveHtmlOn.get("exception")) {
         this.SavePage();
      }
      if ((Boolean)this.screenshotOn.get("exception")) {
         this.screenshot();
      }

      if (this.haltOnFailure) {
         killTestThread();
      }
   }


   /**
    * Assert that two boolean values are equal to each other.
    *
    * <p>As with all Assert* methods, this logs failures as assertion
    * failures which are treated specially.</p>
    *
    * @param msg  string to log along with the results of the assert
    * @param state  the value under test
    * @param expected  the expected value
    * @return whether the assertion passed
    */

   public boolean Assert(String msg, boolean state, boolean expected) {
      boolean result = false;
      String status = "";

      if (state == expected) {
         this.PassedAsserts += 1;
         status = "(*)Assert Passed: ";
         result = true;
      } else {
         this.FailedAsserts += 1;
         status = "(!)Assert Failed: ";
         result = false;
      }

      status = status.concat(msg);
      this._log(status);

      if (result == false && (Boolean)this.saveHtmlOn.get("assertfail")) {
         this.SavePage();
      }
      if (result == false && (Boolean)this.screenshotOn.get("assertfail")) {
         this.screenshot();
      }
      if (result == false && this.haltOnFailure) {
         killTestThread();
      }

      return result;
   }


   /**
    * Assert that one string is found within another.
    *
    * <p>As with all Assert* methods, this logs failures as assertion
    * failures which are treated specially.</p>
    *
    * @param search  the search string
    * @param src     the string in which to search
    * @return whether the assertion passed
    */

   public boolean Assert(String search, String src) {
      TextFinder f = new TextFinder(search);
      boolean found = f.find(src);

      if (found) {
         this.PassedAsserts += 1;
         this.log("Assert Passed, found: '" + search + "'.");
      } else {
         this.FailedAsserts += 1;
         this._log("(!)Assert Failed for: '" + search + "'!");
      }

      if (!found && (Boolean)this.saveHtmlOn.get("assertfail")) {
         this.SavePage();
      }
      if (!found && (Boolean)this.screenshotOn.get("assertfail")) {
         this.screenshot();
      }
      if (!found && this.haltOnFailure) {
         killTestThread();
      }

      return found;
   }


   /**
    * Assert that one string is not found within another.
    *
    * <p>As with all Assert* methods, this logs failures as assertion
    * failures which are treated specially.</p>
    *
    * @param search  the search string
    * @param src     the string in which to search
    * @return whether the assertion passed
    */

   public boolean AssertNot(String search, String src) {
      TextFinder f = new TextFinder(search);
      boolean found = f.find(src);

      if (found) {
         this.FailedAsserts += 1;
         this._log("(!)Assert Failed, found unexpected text: '" + search + "'.");
      } else {
         this.PassedAsserts += 1;
         this.log("Assert Passed, did not find: '" + search + "'!");
      }

      if (found && (Boolean)this.saveHtmlOn.get("assertfail")) {
         this.SavePage();
      }
      if (found && (Boolean)this.screenshotOn.get("assertfail")) {
         this.screenshot();
      }
      if (found && this.haltOnFailure) {
         killTestThread();
      }

      return !found;
   }


   /**
    * Create a file name.
    *
    * Use the specified directory, file name root, and file index to
    * create the filename.  The directory is assumed to be relative to
    * resultDir.
    *
    * @param dir   the directory in which to create the file name
    * @param file  the file name root
    * @param idx   one-up index of this file
    * @param ext   file extension
    * @return path and file name
    */

   private String makeFilename(String dir, String file, int idx, String ext) {
      String test = "";

      String outfile = this.resultDir + "/" + dir;

      File checkDir = new File(outfile);
      if (!checkDir.exists()) {
         checkDir.mkdir();
      }

      if (this.testName != null) {
         File tmp = new File(this.testName);
         test = tmp.getName();
         test = String.format("%s-", test.substring(0, test.length() - 4));
      }

      outfile += String.format("/%s%s-%d.%s", test, file, idx, ext);

      return FilenameUtils.separatorsToSystem(outfile);
   }


   /**
    * Save the current HTML page.
    */

   public void SavePage() {
      String htmlFile = makeFilename("saved-html", "savedhtml",
                                     this.saveHtmlIdx, "html");
      this.saveHtmlIdx += 1;

      String pageSource = this.browser.getPageSource();

      try {
         File f = new File(htmlFile);
         BufferedWriter bw = new BufferedWriter(new FileWriter(f));
         bw.write(pageSource);
         bw.close();
         this.log(String.format("HTML Saved: %s", htmlFile));
      } catch (java.io.IOException e) {
         this.justLogTheException("Failed to save HTML", e);
      }
   }


   /**
    * Take a screenshot of the current page.
    */

   public void screenshot() {
      String screenshotFile = makeFilename("screenshots", "screenshot",
                                           this.screenshotIdx, "png");
      this.screenshotIdx += 1;

      Utils.takeScreenShot(screenshotFile, this, false);
   }


   /**
    * Clean up on object destruction.
    *
    * This method simply makes sure that the output file handle is
    * properly closed.
    */

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
