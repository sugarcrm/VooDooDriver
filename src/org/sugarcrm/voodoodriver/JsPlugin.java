/*
 * Copyright 2012 SugarCRM Inc.
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

import org.openqa.selenium.WebElement;


/**
 * A VooDooDriver Javascript plugin.
 *
 * @author Jon duSaint
 */

public class JsPlugin extends Plugin {

   /**
    * Path to the javascript plugin file.
    */

   private String jsFile;


   /**
    * Instantiate a javascript plugin object.
    */

   public JsPlugin(String jsFile) {
      this.jsFile = jsFile;
   }


   /**
    * Not implemented: javascript files have no class name.
    *
    * @param className  name of the Java class
    * @return false
    */

   public boolean matches(String className) {
      return false;
   }


   public boolean execute(PluginData data, Reporter report) {
      WebElement element = data.getElement();
      Browser browser = data.getBrowser();
      String js = "var CONTROL = arguments[0];\n\n";
      String err = null;
      Object res = null;
      int rv = 0;

      try {
         js += Utils.FileToStr(jsFile);
      } catch (java.io.FileNotFoundException e) {
         err = String.format("Specified plugin not found '%s'", jsFile);
      } catch (java.io.IOException e) {
         err = String.format("Error reading plugin file '%s': %s", jsFile, e);
      }

      if (err != null) {
         report.error(err);
         return false;
      }

      report.log("Plugin event started.");

      try {
         res = browser.executeJS(js, element);
         rv = Integer.valueOf(String.valueOf(res));
      } catch (org.openqa.selenium.WebDriverException e) {
         report.exception("Exception executing JS plugin " + jsFile, e);
         return false;
      } catch (java.lang.NumberFormatException e) {
         report.error("JS Plugin '" + jsFile +
                      "' return value is not an integer (" +
                      String.valueOf(res) + ")");
      }
      report.log("Plugin event finished.");

      if (rv != 0) {
         report.error(String.format("Plugin Event failed (return value = %d)",
                                    rv));
      }

      return rv == 0;
   }
}
