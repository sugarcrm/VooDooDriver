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
 * Data passed from VooDooDriver to plugins.
 *
 * <p>Methods will never be removed from this object, only added as
 * new capabilities are needed.  In this manner, newer VDDs can call
 * older plugins, but newer plugins won't (and shouldn't, anyways)
 * work with older VDDs.  Should a plugin need to explicitly verify
 * the presence of a getter/setter, the {@link #provides} method
 * enumerates the instance's capabilities.</p>
 *
 * <p>If a getter method is called and the corresponding setter method
 * was not called, <code>null</code> is returned.</p>
 *
 * @author Jon duSaint
 */

public class PluginData {

   /**
    * Internal storage of plugin data.
    */

   private VDDHash d;

   /**
    * String enumeration of all implemented getters/setters.
    */

   private final String[] methods = {"args", "browser", "element"};


   /**
    * Instantiate a <code>PluginData</code> object.
    */

   public PluginData() {
      this.d = new VDDHash();
   }


   /**
    * Instantiate a <code>PluginData</code> object with data from another.
    *
    * @param original  the <code>PluginData</code> object to copy
    */

   public PluginData(PluginData original) {
      this.d = new VDDHash();

      for (String method: this.methods) {
         this.d.put(method, original.d.get(method));
      }
   }


   /**
    * Enumerate which getters/setters are provided.
    *
    * @param method  name of methods to check for. Current methods are
    *                <ul><li>args</li><li>browser</li><li>element</li></ul>
    * @return true if the methods exist, false otherwise
    */

   public boolean provides(String method) {
      try {
         method = method.toLowerCase();
      } catch (NullPointerException e) {
         return false;
      }

      for (String m: this.methods) {
         if (m == method) {
            return true;
         }
      }

      return false;
   }

   /*
    * Lots of getters and setters.  In a more-loosely typed language
    * (e.g. Python), all of these could be dispensed with by simply
    * modifying the class' method lookup method, but AFAIK Java has no
    * such, so all must be explicitly written.  Sigh.
    */

   /**
    * Assign plugin "command line" args.
    *
    * @param args  arg list passed unmodified from the test script to the plugin
    */

   public void setArgs(String[] args) {
      this.d.put("args", args);
   }


   /**
    * Fetch the plugin arg list.
    *
    * @return plugin arg list
    */

   public String[] getArgs() {
      return (String[])this.d.get("args");
   }


   /**
    * Store the current {@link Browser} object.
    */

   public void setBrowser(Browser browser) {
      this.d.put("browser", browser);
   }


   /**
    * Fetch the current {@link Browser} object.
    *
    * @return current {@link Browser} object
    */

   public Browser getBrowser() {
      return (Browser)this.d.get("browser");
   }


   /**
    * Set the current {@link WebElement}.
    *
    * @param element  {@link WebElement} of the current event
    */

   public void setElement(WebElement element) {
      this.d.put("element", element);
   }


   /**
    * Fetch the current {@link WebElement}.
    *
    * @return {@link WebElement}
    */

   public WebElement getElement() {
      return (WebElement)this.d.get("element");
   }

}
