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
 * Internal representation of a VooDooDriver plugin
 *
 * @author Jon duSaint
 */

public interface Plugin {
   public void setElements(String[] elements);
   public void setEvents(String[] events);
   public void setArgs(String[] args);
   public boolean matches(Elements element, PluginEventType event);
   public boolean matches(String className);
   public boolean execute(WebElement element, Browser browser, Reporter report);
}


/**
 * VooDooDriver Plugin superclass
 *
 * @author Jon duSaint
 */

class PluginSuper implements Plugin {

   /**
    * Array of HTML elements that this plugin executes in response to.
    */

   private Elements[] elements = null;

   /**
    * Array of plugin events that this plugin executes in response to.
    */

   private PluginEventType[] events = null;

   /**
    * Array of arguments to pass to the plugin.
    */

   private String[] args = null;


   /**
    * Assign this plugin's list of HTML elements.
    *
    * Each element in the array is converted from a string to the
    * corresponding {@link Element}.
    *
    * @param elements  array of HTML elements
    */

   public void setElements(String[] elements) {
      this.elements = new Elements[elements.length];
      for (int k = 0; k < elements.length; k++) {
         this.elements[k] = Elements.valueOf(elements[k].toUpperCase());
      }
   }


   /**
    * Assign this plugin's list of plugin events.
    *
    * Each event in the array is converted from a string to the
    * corresponding {@link PluginEventType}.
    *
    * @param events  array of plugin events
    */

   public void setEvents(String[] events) {
      this.events = new PluginEventType[events.length];
      for (int k = 0; k < events.length; k++) {
         this.events[k] = PluginEventType.valueOf(events[k].toUpperCase());
      }
   }


   /**
    * Assign this plugin's argument list.
    *
    * @param args  array of arguments
    */

   public void setArgs(String[] args) {
      this.args = args;
   }


   /**
    * Determine whether element and event match this plugin's specification.
    *
    * @param element  current HTML element
    * @param event    current plugin event
    * @return  whether this plugin's spec matches
    */

   public boolean matches(Elements element, PluginEventType event) {
      boolean elementMatches = false;
      boolean eventMatches = false;

      if (this.elements == null || this.events == null) {
         return false;
      }

      for (Elements c: this.elements) {
         if (c.equals(element)) {
            elementMatches = true;
            break;
         }
      }

      for (PluginEventType e: this.events) {
         if (e.equals(event)) {
            eventMatches = true;
            break;
         }
      }

      return elementMatches && eventMatches;
   }


   /**
    * Determine whether this plugin matches the specified class name.
    *
    * @param className  plugin class name
    * @return false
    */

   public boolean matches(String className) {
      return false;
   }


   /**
    * Execute the current plugin.
    *
    * @param element  the HTML element that the plugin is running against
    * @param browser  current {@link Browser} object
    * @param report   {@link Reporter} object
    * @return false -- this class must be subclassed
    */

   public boolean execute(WebElement element, Browser browser,
                          Reporter report) {
      return false;
   }
}
