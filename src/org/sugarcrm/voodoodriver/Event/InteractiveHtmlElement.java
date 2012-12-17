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

package org.sugarcrm.voodoodriver.Event;

import org.sugarcrm.voodoodriver.VDDException;
import org.w3c.dom.Element;

/**
 * An event for all &quot;interactive&quot; HTML events.
 *
 * <p>Interactive elements are those that can be modified or have
 * values set, i.e. elements that can be found in forms.</p>
 *
 * @author Jon duSaint
 */

class InteractiveHtmlEvent extends HtmlEvent {

   /**
    * Value of the default attribute.
    */

   private String defaultValue;


   /**
    * Clear the text out of an interactive element.
    */

   private void clearText() {
      if (eventLoop.Browser instanceof org.sugarcrm.voodoodriver.IE) {
         /*
          * Using javascript on IE instead of WebElement.clear() is
          * needed due to Selenium issue 3402.
          */
         eventLoop.Browser.executeJS("arguments[0].value = '';", element);
      } else {
         element.clear();
      }
   }


   /**
    * Append action.
    */

   protected class AppendAction implements Action {

      /**
       * Run the append action.
       */

      public void action(Object val) {
         String v = val.toString();
         log("Appending '" + v + "' to " + element.getTagName());
         element.sendKeys(v);
      }
   }


   /**
    * Clear action.
    */

   protected class ClearAction implements Action {

      /**
       * Run the clear action.
       */

      public void action(Object val) {
         log("Clearing " + element.getTagName());
         clearText();
      }
   }


   /**
    * Default action.
    *
    *
    */

   protected class DefaultAction implements Action {

      /**
       * Run the default action.
       *
       * XXX: This action did nothing in earlier versions of the code,
       * but it is used in the test suite to unclear aim.  Once its
       * use case is identified, this code will be revisited.
       */

      public void action(Object val) {
         defaultValue = (String)val;
      }
   }


   /**
    * Set action.
    */

   protected class SetAction implements Action {

      /**
       * Run the set action.
       */

      public void action(Object val) {
         String v = val.toString();
         log("Setting value of " + element.getTagName() + " to '" + v + "'");
         clearText();
         element.sendKeys(v);
      }
   }


   /**
    * Instantiate an Interactive event.
    *
    * @param e  the event as specified in the test script
    * @throws VDDException if event instantiation fails
    */

   public InteractiveHtmlEvent(Element e) throws VDDException {
      super(e);

      this.actionList.addLast(new Pair<String,Action>("disabled",
                                                      new DisabledAction()));
      this.actionList.addLast(new Pair<String,Action>("clear",
                                                      new ClearAction()));
      this.actionList.addLast(new Pair<String,Action>("set",
                                                      new SetAction()));
      this.actionList.addLast(new Pair<String,Action>("append",
                                                      new AppendAction()));
   }


   /**
    * Get the value to be used when storing to a var.
    *
    * @return the value of this element
    */

   protected String getVarValue() {
      return this.element.getText();
   }


   /**
    * Run the event.
    *
    * @throws VDDException if execution is unsuccessful
    */

   public void execute() throws VDDException {
      super.execute();
   }
}
