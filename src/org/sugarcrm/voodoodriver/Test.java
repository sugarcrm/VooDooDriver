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
   private File testFile;
   private EventLoop eventDriver = null;
   private Events events = null;
   private Reporter reporter = null;
   private VDDHash GVars = null;
   private VDDHash OldVars = null;
   private VDDHash HiJacks = null;
   private BlockList blocked = null;
   private ArrayList<Plugin> plugins = null;
   private static final long watchdogTimeout = 60 * 5 * 1000; // 5 minutes
   private File assertPage = null;
   private int attachTimeout = 0;
   private boolean isRestartTest = false;
   private int eventTimeout = 0;


   /**
    * Initialize a Test object.
    *
    * @param config     VDD configuration
    * @param testFile   name of this test file
    */

   public Test(VDDHash config, File testFile) {
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

   public Test(VDDHash config, File testFile, String suitename,
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

      report_name = testFile.getName();
      report_name = report_name.replaceAll(".xml$", "");

      if (suitename != null) {
         resultsdir = resultsdir + "/" + suitename;
      }

      this.reporter = new Reporter(report_name, resultsdir, config);
      this.reporter.setTestName(testFile.getName());
      this.reporter.setBrowser((Browser)config.get("browser"));

      if (saveHtml != null && saveHtml.length() > 0) {
         this.reporter.setSaveHTML(saveHtml);
      }

      if (screenshot != null && screenshot.length() > 0) {
         this.reporter.setScreenshot(screenshot);
      }

      this.Browser.setReporter(this.reporter);

      if (config.get("assertpagefile") != null) {
         this.setAssertPage(new File((String)config.get("assertpagefile")));
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

   public void setAssertPage(File assertPage) {
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
         this.reporter.Log("Loading Soda Test: '" + testFile + "'");
         loader = new TestLoader(testFile, this.reporter);
         this.events = loader.getEvents();
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
      boolean watchdog = false;

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
         eventDriver = new EventLoop(this.Browser, events, this.reporter,
                                     this.GVars, this.HiJacks, this.OldVars,
                                     this.plugins, this.testFile.getName(),
                                     this.eventTimeout);

         if (this.attachTimeout > 0) {
            eventDriver.setAttachTimeout(this.attachTimeout);
         }

         while (eventDriver.isAlive()) {
            long elstamp = eventDriver.getThreadTime().getTime();
            long now = (new Date()).getTime();

            if (now - elstamp >
                watchdogTimeout + eventDriver.getWaitDuration()) {
               watchdog = true;
               eventDriver.stop();
               this.reporter.ReportWatchDog((now - elstamp) / 1000);
               break;
            }

            try {
               Thread.sleep(9000);
            } catch (InterruptedException e) {
               // ignore
            }
         }
      }

      if (watchdog) {
         System.out.println("Trying to close browser after watchdog timeout!");
         this.Browser.forceClose();
         System.out.println("Closed???!");
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
      String test_file = this.testFile.getName();

      test_file = test_file.substring(0, test_file.length() - 4);

      if (this.blocked == null) {
         return false;
      }

      for (VDDHash item: this.blocked) {
         if (test_file.equals(item.get("testfile").toString())) {
            String module_name = (item.containsKey("modulename") ?
                                  item.get("modulename").toString() :
                                  "<<No Module>>");
            String bug_number = (item.containsKey("bugnumber") ?
                                 item.get("bugnumber").toString() :
                                 "00000");
            String reason = (item.containsKey("reason") ?
                             item.get("reason").toString() :
                             "");
            this.reporter.Log("Test is currently blocked, " +
                              "Bug Number: '" + bug_number + "', " +
                              "Module Name: '" + module_name + "', " +
                              "Reason: '" + reason + "'");
            this.reporter.ReportBlocked();
            return true;
         }
      }

      return false;
   }

}
