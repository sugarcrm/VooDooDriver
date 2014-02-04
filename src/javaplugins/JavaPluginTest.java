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

package javaplugins;

import org.openqa.selenium.WebElement;
import org.sugarcrm.voodoodriver.PluginData;
import org.sugarcrm.voodoodriver.PluginInterface;
import org.sugarcrm.voodoodriver.Reporter;


/**
 * Java plugin to test VDD's functionality.
 *
 * @author Jon duSaint
 */

public class JavaPluginTest implements PluginInterface {

   /**
    * Run this plugin.
    */

   @Override
   public int execute(PluginData data) {
      Reporter r = data.getBrowser().getReporter();
      WebElement element = data.getElement();

      if (element == null) {
         r.log("JavaPluginTest executing");
      } else {
         r.log("JavaPluginTest executing against " + element.getTagName());
      }

      return 0;
   }
}
