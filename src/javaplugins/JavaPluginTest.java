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

package javaplugins;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.sugarcrm.voodoodriver.Browser;
import org.sugarcrm.voodoodriver.VDDPluginInterface;

/**
 * a simple example Java plugin for use with VDD. This plugin demonstrates VDD's functionality of interacting with WebElement, and performing
 * actions that are not possible with standard VDD elements.
 *
 * @author Lehan Huang
 */
public class JavaPluginTest implements VDDPluginInterface {

   @Override
   public int execute(String[] args, Browser browser, WebElement element) {
      System.out.printf("(*)Plugin: Starting VDD JavaPluginTest...\n");

      if (args == null && element == null){
         return 1;
      }
      else if (args == null){
         element.findElement(By.id("text1")).clear();
         element.findElement(By.id("text1")).sendKeys("voodoo");
      }
      else{
         System.out.printf("(*)Plugin: arg size: "+args.length+"\n");
         element.findElement(By.id("text1")).clear();
         element.findElement(By.id("text1")).sendKeys(args[0]);
      }

      System.out.printf("(*)Plugin: JavaPluginTest finished.\n");

      return 0;
   }

}
