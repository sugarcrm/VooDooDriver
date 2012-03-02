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
import org.openqa.selenium.chrome.ChromeDriver;

/**
 * Chome browser support for voodoodriver.
 *
 * @author trampus
 *
 */
public class Chrome extends Browser {

   public Chrome() {

   }

   public void setDownloadDirectory(String dir) {

   }

   public void newBrowser() {
      this.setDriver(new ChromeDriver());
      this.setBrowserState(false);
   }

   // this is causing chrome to hang forever! //
   public String generateUIEvent(UIEvents type) {
      String result = "var ele = arguments[0];\n";
      result += "var evObj = document.createEvent('MouseEvents');\n";
      result += "evObj.initMouseEvent('" + type.toString().toLowerCase() + "', true, true, window, 1, 12, 345, 7, 220,"+
         "false, false, true, false, 0, null );\n";
      result += "ele.dispatchEvent(evObj);\n";

      return result;
   }

   public void alertHack(boolean alert) {

   }

   /**
    * Kills the native chome browser process.
    */
   public void forceClose() {
      OSInfo.killProcesses(OSInfo.getProcessIDs("Google Chrome"));
      this.setBrowserClosed();
   }

   public Mouse getMouse() {
      return ((ChromeDriver)this.getDriver()).getMouse();
   }
}
