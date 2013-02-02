/*
Copyright 2011-2012 SugarCRM Inc.

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

package org.sugarcrm.voodoodriver;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;

public class Test {

   private Browser Browser = null;
   private String testFile = "";
   private EventLoop eventDriver = null;
   private Events events = null;
   private Reporter reporter = null;
   private VDDHash GVars = null;
   private VDDHash OldVars = null;
   private VDDHash HiJacks = null;
   private BlockList blocked = null;
   private boolean WatchDog = false;
   private ArrayList<Plugin> plugins = null;
   private static final int ThreadTimeout = 60 * 5; // 5 minute timeout //
   private String assertPage = null;
   private int attachTimeout = 0;
   private boolean isRestartTest = false;
   private int eventTimeout = 0;


   /**
    * Initialize a Test object.
    *
    * @param config     VDD configuration
    * @param testFile   name of this test file
    */

   public Test(VDDHash config, String testFile) {
      this(config, testFile, null, null);
   }


   /**
    * Initialize a Test object.
    *
    * @param config     VDD configuration
    * @param testFile   name of this test file
    * @param suitename  name of the current Soda suite or null
    * @param oldvars    gvars from last test
    */

   public Test(VDDHash config, String testFile, String suitename,
               VDDHash oldvars) {

      this.testFile = testFile;
      this.OldVars = oldvars;
      this.Browser = (Browser)config.get("browser");
      this.HiJacks = (VDDHash)config.get("hijack");
      this.GVars = (VDDHash)config.get("gvar");
      this.blocked = (BlockList)config.get("blocklist");
      @SuppressWarnings("unchecked")
         ArrayList<Plugin> plugin = (ArrayList<Plugin>)config.get("plugin");
      this.eventTimeout = (Integer)config.get("eventtimeout");

      String saveHtml = (String)config.get("savehtml");
      String screenshot = (String)config.get("screenshot");
      String resultsdir = (String)config.get("resultdir");
      String report_name = "";
      File tmp_file = new File(testFile);

      report_name = tmp_file.getName();
      report_name = report_name.replaceAll(".xml$", "");

      if (suitename != null) {
         resultsdir = resultsdir + "/" + suitename;
      }

      this.reporter = new Reporter(report_name, resultsdir);
      this.reporter.setTestName(testFile);
      this.reporter.setBrowser((Browser)config.get("browser"));

      if (saveHtml != null && saveHtml.length() > 0) {
         this.reporter.setSaveHTML(saveHtml);
      }

      if (screenshot != null && screenshot.length() > 0) {
         this.reporter.setScreenshot(screenshot);
      }

      this.Browser.setReporter(this.reporter);

      if (config.get("assertpage") != null) {
         this.setAssertPage((String)config.get("assertpage"));
      }
      this.setPlugins(plugin);
      if (config.get("attachtimeout") != null) {
         this.setAttachTimeout((Integer)config.get("attachtimeout"));
      }
   }


   public void setIsRestartTest(boolean isRestart) {
      this.isRestartTest = isRestart;
   }

   public void setAttachTimeout(int timeout) {
      this.attachTimeout = timeout;
   }

   public void setAssertPage(String assertPage) {
      this.assertPage = assertPage;
      this.Browser.setAssertPageFile(this.assertPage, this.reporter);
   }

   public void setPlugins(ArrayList<Plugin> plugins) {
      this.plugins = plugins;
   }

   public EventLoop getEventLoop() {
      return this.eventDriver;
   }

   public Reporter getReporter() {
      return this.reporter;
   }

   private boolean loadTestFile() {
      boolean result = false;
      TestLoader loader = null;

      try {
         System.out.printf("Loading Soda Test: '%s'.\n", testFile);
         loader = new TestLoader(testFile, this.reporter);
         this.events = loader.getEvents();
         System.out.printf("Finished.\n");
      } catch (Exception exp) {
         this.reporter.ReportException(exp);
         result = false;
      }

      if (this.events == null) {
         result = false;
      } else {
         result = true;
      }

      return result;
   }

   public boolean runTest(boolean isSuitetest) {
      boolean result = false;
      this.WatchDog = false;

      if (this.isRestartTest) {
         this.reporter.setIsRestTest(true);
      }

      result = this.loadTestFile();
      if (!result) {
         this.reporter.ReportError("Failed to parse test file!");
         this.logResults();
         this.reporter.closeLog();
         return result;
      }

      result = CheckTestBlocked();
      if (!result) {
         long current = 0;
         eventDriver = new EventLoop(this.Browser, events, this.reporter,
                                     this.GVars, this.HiJacks, this.OldVars,
                                     this.plugins, this.testFile,
                                     this.eventTimeout);

         if (this.attachTimeout > 0) {
            eventDriver.setAttachTimeout(this.attachTimeout);
         }

         while (eventDriver.isAlive() && this.WatchDog != true) {
            Date current_time = new Date();
            Date thread_time = eventDriver.getThreadTime();
            current = current_time.getTime();
            long thread = thread_time.getTime();

            current = current / 1000;
            thread = thread / 1000;
            long seconds = (current - thread);

            if (seconds > ThreadTimeout) {
               this.WatchDog = true;
               eventDriver.stop();
               String msg = String.format("Test watchdogged out after: '%d' seconds!\n", seconds);
               this.reporter.ReportError(msg);
               this.reporter.ReportWatchDog();
               break;
            }

            try {
               Thread.sleep(9000);
            } catch (Exception exp) {
               exp.printStackTrace();
               System.exit(-1);
            }
         }
      }

      if (this.WatchDog) {
         System.out.printf("Trying to close browser after watchdog!\n");
         this.Browser.forceClose();
         System.out.printf("Closed???!\n");
      }

      this.logResults();
      this.reporter.closeLog();
      return result;
   }

   private void logResults() {
      TestResults tmp = this.reporter.getResults();
      int len = tmp.keySet().size() -1;
      String res = "Soda Test Report:";

      for (int i = 0; i<= len; i++) {
         String key = tmp.keySet().toArray()[i].toString();
         String value = tmp.get(key).toString();
         res = res.concat(String.format("--%s:%s", key, value));
      }

      this.reporter.Log(res);
   }

   private boolean CheckTestBlocked() {
      boolean result = false;
      File fd = null;
      String test_file = this.testFile;

      fd = new File(test_file);
      test_file = fd.getName();
      test_file = test_file.substring(0, test_file.length() -4);

      if (this.blocked == null) {
         return false;
      }

      for (int i = 0; i <= this.blocked.size() -1; i++) {
         String blocked_file = this.blocked.get(i).get("testfile").toString();
         if (test_file.equals(blocked_file)) {
            result = true;
            String module_name = this.blocked.get(i).get("modulename").toString();
            String bug_number = this.blocked.get(i).get("bugnumber").toString();
            String reason = this.blocked.get(i).get("reason").toString();
            String msg = String.format("Test is currently blocked, Bug Number: '%s', Module Name: '%s'"+
                  ", Reason: '%s'", bug_number, module_name, reason);
            this.reporter.Log(msg);
            this.reporter.ReportBlocked();
            break;
         }
      }

      return result;
   }

}
