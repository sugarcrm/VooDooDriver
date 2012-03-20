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

import org.openqa.selenium.Mouse;
import org.openqa.selenium.chrome.ChromeDriver;


/**
 * Class representing the Chrome web browser.
 *
 * @author trampus
 */

public class Chrome extends Browser {


   /**
    * Set this object's download directory.
    *
    * Not applicable to Chrome.
    *
    * @param dir  path to the download directory
    */

   public void setDownloadDirectory(String dir) {
   }


   /**
    * Create a new Chrome browser instance.
    */

   public void newBrowser() {
      this.setDriver(new ChromeDriver());
      this.setBrowserOpened();
   }


   /**
    * Generate a browser event of the specified type.
    *
    * XXX: this is causing chrome to hang forever!
    *
    * @param type  the type of browser event
    * @return the resulting browser event code
    */

   public String generateUIEvent(UIEvents type) {
      String result = "var ele = arguments[0];\n";
      result += "var evObj = document.createEvent('MouseEvents');\n";
      result += "evObj.initMouseEvent('" + type.toString().toLowerCase() + "', true, true, window, 1, 12, 345, 7, 220,"+
         "false, false, true, false, 0, null );\n";
      result += "ele.dispatchEvent(evObj);\n";

      return result;
   }


   /**
    * Prevent javascript alert() windows from appearing.
    *
    * @param alert whether to allow alert() windows
    */

   public void alertHack(boolean alert) {
   }


   /**
    * Force the browser window to close via the native operating system.
    */

   public void forceClose() {
      OSInfo.killProcesses(OSInfo.getProcessIDs("Google Chrome"));
      this.setBrowserClosed();
   }


   /**
    * Get the {@link Mouse} object for access to the raw input device.
    *
    * @return the {@link Mouse} device for this machine
    */

   public Mouse getMouse() {
      return ((ChromeDriver)this.getDriver()).getMouse();
   }
}
