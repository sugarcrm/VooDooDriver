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
import org.openqa.selenium.interactions.Mouse;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.remote.DesiredCapabilities;


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
      DesiredCapabilities dc = DesiredCapabilities.chrome();

      if (this.webDriverLogDirectory != null) {
         File wdl = makeLogfileName(super.webDriverLogDirectory, "webdriver");
         File cl = makeLogfileName(super.webDriverLogDirectory, "chrome");

         /*
          * XXX: neither System.setProperty nor
          * DesiredCapabilities.setCapability seem to work for this
          * property.  Fortunately, chromedriver logs more information
          * in its one log file than firefox does in both.
          */
         System.out.println("(*) Creating WebDriver log " + wdl);
         System.setProperty("webdriver.log.file",  wdl.toString());

         System.out.println("(*) Creating Chrome log " + cl);
         System.setProperty("webdriver.chrome.logfile", cl.toString());
      }

      this.setDriver(new ChromeDriver(dc));
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
      return ("var ele = arguments[0];\n" +
              "var evObj = document.createEvent('MouseEvents');\n" +
              "evObj.initMouseEvent('" + type.toString().toLowerCase() + "'," +
              "                     true, true, window, 1, 12, 345, 7, 220, " +
              "                     false, false, true, false, 0, null);\n" +
              "ele.dispatchEvent(evObj);\n");
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
      OSInfo.killProcess("Google Chrome");
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
