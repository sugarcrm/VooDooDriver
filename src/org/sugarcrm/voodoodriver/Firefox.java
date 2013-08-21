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

import java.io.File;
import org.openqa.selenium.interactions.Mouse;
import org.openqa.selenium.firefox.FirefoxBinary;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.remote.DesiredCapabilities;


/**
 * Class representing the Firefox web browser.
 *
 * @author Trampus
 * @author Jon duSaint
 */

public class Firefox extends Browser {

   /**
    * The download directory for this browser instance.
    */

   private String downloadDirectory = null;

   /**
    * Set this object's download directory.
    *
    * @param dir  path to the download directory
    */

   public void setDownloadDirectory(String dir) {
      this.downloadDirectory = dir;
   }


   /**
    * Create a new firefox browser instance.
    */

   public void newBrowser() {
      FirefoxBinary b = new FirefoxBinary();
      FirefoxProfile p = null;
      if (this.profile == null) {
         p = new FirefoxProfile();
      } else {
         p = new FirefoxProfile(new java.io.File(this.profile));
      }

      if (this.downloadDirectory != null) {
         try {
            p.setPreference("browser.download.dir", this.downloadDirectory);
         } catch (java.lang.IllegalArgumentException e) {
            System.err.println("Ill-formed downloaddir '" +
                               this.downloadDirectory + "'");
            System.exit(1);
         }
         p.setPreference("browser.download.manager.closeWhenDone", true);
         p.setPreference("browser.download.manager.retention", 0);
         p.setPreference("browser.download.manager.showAlertOnComplete", false);
         p.setPreference("browser.download.manager.scanWhenDone", false);
         p.setPreference("browser.download.manager.skipWinSecurityPolicyChecks",
                         true);
         p.setPreference("browser.startup.page", 0);
         p.setPreference("browser.download.manager.alertOnEXEOpen", false);
         p.setPreference("browser.download.manager.focusWhenStarting", false);
         p.setPreference("browser.download.useDownloadDir", true);
      }

      if (super.webDriverLogDirectory != null) {
         File wdl = makeLogfileName(super.webDriverLogDirectory, "webdriver");
         File fl = makeLogfileName(super.webDriverLogDirectory, "firefox");

         System.out.println("(*) Creating WebDriver log " + wdl);
         p.setPreference("webdriver.log.file", wdl.toString());

         System.out.println("(*) Creating Firefox log " + fl);
         System.setProperty("webdriver.firefox.logfile", fl.toString());
      }

      /* Allow scripts to run until finished. */
      p.setPreference("dom.max_script_run_time", 0);
      p.setPreference("dom.max_chrome_script_run_time", 0);

      DesiredCapabilities c = DesiredCapabilities.firefox();
      c.setCapability("unexpectedAlertBehaviour", "ignore");

      this.setDriver(new FirefoxDriver(b, p, c));
      this.setBrowserOpened();
   }


   /**
    * Prevent javascript alert() windows from appearing.
    *
    * This method stomps on the existing alert confirm dialog code to
    * keep the dialog from popping up.  This is a total hack, but I
    * have yet to see any better way to handle this on all platforms.
    * Hackie hack!
    *
    * @param alert whether to allow alert() windows
    */

   public void alertHack(boolean alert) {
      String alert_js = "var old_alert = window.alert;\n" +
         "var old_confirm = window.confirm;\n" +
         "window.alert = function() {return " + alert + ";};\n" +
         "window.confirm = function() {return " + alert + ";};\n" +
         "window.onbeforeunload = null;\n" +
         "var result = 0;\n" +
         "result;\n";

      this.executeJS(alert_js, null);

   }


   /**
    * Force the browser window to close via the native operating system.
    */

   public void forceClose() {
      OSInfo.killProcesses(OSInfo.getProcessIDs("firefox"));
      this.setBrowserClosed();
   }


   /**
    * Get the {@link Mouse} object for access to the raw input device.
    *
    * @return the {@link Mouse} device for this machine
    */

   public Mouse getMouse() {
      return ((FirefoxDriver)this.getDriver()).getMouse();
   }
}
