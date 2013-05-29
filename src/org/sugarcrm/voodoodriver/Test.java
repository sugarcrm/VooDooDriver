/*
 * Copyright 2011-2012 SugarCRM Inc.
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

package org.sugarcrm.voodoodriver;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import org.sugarcrm.voodoodriver.Event.Event;


/**
 * Representation of a test script in VDD.
 *
 * @author Trampus
 * @author Jon duSaint
 */

public class Test {

   /**
    * VDD configuration information.
    */

   private VDDHash config;

   /**
    * The test file for this Test.
    */

   private File testFile;

   /**
    * The Vars object for this test.
    */

   private Vars vars;

   /**
    * This Test's {@link EventLoop}.
    */

   private EventLoop eventLoop;

   /**
    * List of events from the test script.
    */

   private ArrayList<Event> events;

   /**
    * This Test's {@link Reporter}.
    */

   private Reporter reporter;

   /**
    * Whether this Test is a restart test.
    */

   private boolean isRestartTest = false;

   /**
    * Amount of time allowed before this test times out.  Default is 5 minutes.
    */

   private static final long watchdogTimeout = 60 * 5 * 1000; // 5 minutes

   /**
    * Milliseconds to sleep while waiting for test completion.
    */

   private static final long SLEEPYTIME = 9000;


   /**
    * Initialize a Test object.
    *
    * @param config     VDD configuration
    * @param testFile   name of this test file
    * @param suitename  name of the current suite or null
    * @param vars       the Vars object for this test
    */

   public Test(VDDHash config, File testFile, String suitename, Vars vars) {
      this.config = config;
      this.testFile = testFile;
      this.vars = vars;

      initializeReporter(suitename);
   }


   /**
    * Initialize this Test's {@link Reporter} object.
    *
    * @param suitename  name of the current suite, if any
    */

   private void initializeReporter(String suitename) {
      String resultsDir = (String)this.config.get("resultdir");
      if (suitename != null) {
         resultsDir = resultsDir + "/" + suitename;
      }

      String reportName = this.testFile.getName();
      reportName = reportName.replaceAll(".xml$", "");

      this.reporter = new Reporter(reportName, resultsDir, this.config);
      this.reporter.setTestName(this.testFile.getName());
      this.reporter.setBrowser((Browser)config.get("browser"));

      if (this.config.containsKey("savehtml")) {
         String saveHtml = (String)this.config.get("savehtml");
         if (saveHtml.length() > 0) {
            this.reporter.setSaveHTML(saveHtml);
         }
      }
      if (this.config.containsKey("screenshot")) {
         String screenshot = (String)config.get("screenshot");
         if (screenshot.length() > 0) {
            this.reporter.setScreenshot(screenshot);
         }
      }
      Browser b = ((Browser)config.get("browser"));
      b.setReporter(this.reporter);
      String apf = (String)config.get("assertpagefile");
      if (apf != null) {
         b.setAssertPageFile(new File(apf));
      }
   }


   /**
    * Indicate whether this is a restart test.
    *
    * @param isRestart  true if this is a restart test
    */

   public void setIsRestartTest(boolean isRestart) {
      this.isRestartTest = isRestart;
   }


   /**
    * Retrieve this Test's EventLoop.
    *
    * @return this Test's {@link EventLoop}
    */

   public EventLoop getEventLoop() {
      return this.eventLoop;
   }


   /**
    * Retrieve this Test's Reporter object.
    *
    * @return this Test's {@link Reporter} object
    */

   public Reporter getReporter() {
      return this.reporter;
   }


   /**
    * Load and compile this Test's test file.
    *
    * @return true if the file loaded successfully
    */

   private boolean loadTestFile() throws VDDException {
      this.reporter.log("Loading Test: " + this.testFile);

      try {
         TestLoader loader = new TestLoader(this.testFile, this.reporter);
         this.events = loader.getEvents();
      } catch (Exception e) {
         // XXX -- this will need to be revisited
         throw new VDDException("Exception loading " + this.testFile, e);
      }

      this.reporter.log("Finished.");

      return this.events != null; // ? Should be void with exception for error
   }


   /**
    * Log the results of running this Test.
    */

   private void logResults() {
      TestResults tmp = this.reporter.getResults();
      int len = tmp.keySet().size() - 1;
      String res = "Soda Test Report:";

      for (int i = 0; i<= len; i++) {
         String key = tmp.keySet().toArray()[i].toString();
         String value = tmp.get(key).toString();
         res = res.concat(String.format("--%s:%s", key, value));
      }

      this.reporter.log(res);
   }


   /**
    * Determine whether this Test is on the blocked list.
    *
    * @return true if the test is on the blocked list
    */

   private boolean testBlocked() {
      if (!this.config.containsKey("blocklist")) {
         return false;
      }

      /* Chop off ".xml" */
      String baseName = this.testFile.getName();
      baseName = baseName.substring(0, baseName.length() - 4);

      for (VDDHash item: (BlockList)this.config.get("blocklist")) {
         if (!baseName.equals(item.get("testfile").toString())) {
            continue;
         }

         String moduleName = (item.containsKey("modulename") ?
                              item.get("modulename").toString() :
                              "<<No Module>>");
         String bugNumber = (item.containsKey("bugnumber") ?
                             item.get("bugnumber").toString() :
                             "00000");
         String reason = (item.containsKey("reason") ?
                          item.get("reason").toString() :
                          "");
         this.reporter.log("Test is currently blocked, " +
                           "Bug Number: '" + bugNumber + "', " +
                           "Module Name: '" + moduleName + "', " +
                           "Reason: '" + reason + "'");
         this.reporter.setBlocked();
         return true;
      }

      return false;
   }


   /**
    * Run this Test.
    *
    * @return true if the test ran, false otherwise
    */

   public boolean runTest() {
      boolean watchdog = false;

      if (this.isRestartTest) {
         this.reporter.setIsRestTest(true);
      }

      try {
         if (!this.loadTestFile()) {
            this.reporter.error("Failed to parse test file!");
            this.logResults();
            this.reporter.closeLog();
            return false;
         }
      } catch (VDDException e) {
         this.reporter.exception("Failed to parse test file!", e.getCause());
         this.logResults();
         this.reporter.closeLog();
         return false;
      }

      if (testBlocked()) {
         this.logResults();
         this.reporter.closeLog();
         return false;
      }

      this.eventLoop = new EventLoop(this.events, this.config, this.reporter,
                                     this.vars, this.testFile.toString());

      if (this.config.containsKey("attachtimeout")) {
         int attachTimeout = (Integer)this.config.get("attachtimeout");
         if (attachTimeout > 0) {
            this.eventLoop.setAttachTimeout(attachTimeout);
         }
      }

      while (eventLoop.isAlive()) {
         long elstamp = eventLoop.getThreadTime().getTime();
         long now = (new Date()).getTime();

         if (now - elstamp >
             watchdogTimeout + eventLoop.getWaitDuration()) {
            watchdog = true;
            eventLoop.stop();
            this.reporter.watchdog((now - elstamp) / 1000);
            break;
         }

         try {
            Thread.sleep(SLEEPYTIME);
         } catch (InterruptedException e) {
            this.reporter.warning("Thread interrupted.  Ignoring.");
         }
      }

      if (watchdog) {
         this.reporter.log("Trying to close browser after watchdog.");
         ((Browser)this.config.get("browser")).forceClose();
      }

      this.logResults();
      this.reporter.closeLog();
      return true;
   }
}
