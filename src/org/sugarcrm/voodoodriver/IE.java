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

import org.openqa.selenium.interactions.Mouse;
import org.openqa.selenium.ie.InternetExplorerDriver;


/**
 * Class representing the Internet Explorer web browser.
 *
 * @author Trampus
 */

public class IE extends Browser {


   /**
    * Set this object's download directory.
    *
    * Not applicable to IE.
    *
    * @param dir  path to the download directory
    */

   public void setDownloadDirectory(String dir) {
   }


   /**
    * Create a new IE browser instance.
    */

   public void newBrowser() {
      this.setDriver(new InternetExplorerDriver());
      this.setBrowserOpened();
   }


   /**
    * Force the browser window to close via the native operating system.
    */

   public void forceClose() {
      OSInfo.killProcess("iexplorer");
      this.setBrowserClosed();
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
      String alert_js =  "var old_alert = window.alert;\n" +
         "var old_confirm = window.confirm;\n"+
         "window.alert = function(){return " + alert + ";};\n"+
         "window.confirm = function(){return " + alert + ";};\n"+
         "window.onbeforeunload = null;\n"+
         "var result = 0;\nresult;\n";

      this.executeJS(alert_js, null);
   }


   /**
    * Get the {@link Mouse} object for access to the raw input device.
    *
    * @return the {@link Mouse} device for this machine
    */

   public Mouse getMouse() {
      return ((InternetExplorerDriver)this.getDriver()).getMouse();
   }
}
