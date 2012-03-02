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

package org.sugarcrm.voodoodriver;

import org.openqa.selenium.Mouse;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;

public class Firefox extends Browser implements BrowserInterface {

   private String downloadDirecotry = null;

   public Firefox() {

   }

   public void setDownloadDirectory(String dir) {
      this.downloadDirecotry = dir;
   }

   public void newBrowser() {
      FirefoxDriver fd = null;
      FirefoxProfile profile = null;

      try {
         profile = new FirefoxProfile();

         if (this.downloadDirecotry != null) {
            profile.setPreference("browser.download.dir", this.downloadDirecotry);
            profile.setPreference("browser.download.manager.closeWhenDone", true);
            profile.setPreference("browser.download.manager.retention", 0);
            profile.setPreference("browser.download.manager.showAlertOnComplete", false);
            profile.setPreference("browser.download.manager.scanWhenDone", false);
            profile.setPreference("browser.download.manager.skipWinSecurityPolicyChecks", true);
            profile.setPreference("browser.startup.page", 0);
            profile.setPreference("browser.download.manager.alertOnEXEOpen", false);
            profile.setPreference("browser.download.manager.focusWhenStarting", false);
            profile.setPreference("browser.download.useDownloadDir", true);
         }

         fd = new FirefoxDriver(profile);
         this.setDriver(fd);
         this.setBrowserState(false);
      } catch (Exception exp) {
         exp.printStackTrace();
         System.exit(-1);
      }
   }

   /*
    * alertHack -- method
    *    This method stomps on the existing alert & confirm dialog code to keep the dialog from
    *    popping up.  This is a total hack, but I have yet to see any better way to handle this
    *    on all platforms.  Hackie hack!
    *
    * Input:
    *    alert: true/false, which ok's or cancels the dialog.
    *
    * Output:
    *    None.
    *
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

   public void forceClose() {
      try {
         OSInfo.killProcesses(OSInfo.getProcessIDs("firefox"));
      } catch (Exception exp) {
         exp.printStackTrace();
      }
      this.setBrowserClosed();
   }

   public Mouse getMouse() {
      return ((FirefoxDriver)this.getDriver()).getMouse();
   }
}
