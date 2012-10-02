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

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.sugarcrm.voodoodriver.VDDHash;
import org.sugarcrm.voodoodriver.PluginData;
import org.sugarcrm.voodoodriver.PluginInterface;
import org.sugarcrm.voodoodriver.Vars;


/**
 * An example Java plugin for use with VooDooDriver.
 *
 * <p>This plugin demonstrates how to use a plugin to interact with
 * Selenium and its WebElement interface, extending VooDooDriver's
 * functionality without needing to modify the base code.</p>
 *
 * @author Lehan Huang
 */

public class JavaPluginDemo implements PluginInterface {

   @Override
   public int execute(PluginData data) {
      System.out.println("(*)Plugin: Starting VDD JavaPluginTest.");

      String []args = data.getArgs();
      WebElement element = data.getElement();
      Vars vars = data.getVars();
      VDDHash hijacks = data.getHijacks();
      String testName = data.getTestName();

      System.out.println("(*)VDD is currently running the test " + testName);

      if (args == null && element == null) {
         return 1;
      } else if ((args == null || args.length < 1) && element != null) {
         element.findElement(By.id("text1")).clear();
         element.findElement(By.id("text1")).sendKeys("voodoo");
      } else if (element != null) {
         System.out.printf("(*)Plugin: arg size: %d\n", args.length);
         element.findElement(By.id("text1")).clear();
         element.findElement(By.id("text1")).sendKeys(args[0]);
      } else {
         System.out.printf("(*)Plugin: arg size: %d\n", args.length);
         for (String arg: args) {
            System.out.println("  => arg: " + arg);
         }
      }

      if (hijacks != null) {
         System.out.println("(*)All VDD Hijacks");
         for (String k: hijacks.keySet()) {
            System.out.printf("--)'%s' => '%s'\n", k, hijacks.get(k));
         }
      }

      System.out.println("(*)Plugin: JavaPluginTest finished.");

      return 0;
   }

}
