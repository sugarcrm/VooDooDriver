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

import org.sugarcrm.voodoodriver.PluginEvent;
import org.sugarcrm.voodoodriver.VDDException;
import org.w3c.dom.Element;

/**
 * The checkbox event.
 *
 * @author Jon duSaint
 */

class Checkbox extends HtmlEvent {

   /**
    * Set action.
    */

   protected class SetAction implements Action {

      /**
       * Run the set action.
       */

      public void action(Object val) {
         Boolean desired = (Boolean)val;
         Boolean actual = element.isSelected();

         if (desired ^ actual) {
            log("Setting checkbox state to " +
                (desired ? "" : "un") + "checked.");
            firePlugin(PluginEvent.BEFORECLICK);
            element.click();
            firePlugin(PluginEvent.AFTERCLICK);
         } else {
            log("Checkbox is already " + 
                (actual ? "" : "un") + "checked. Skipping.");
         }
      }
   }


   /**
    * Instantiate a Checkbox event.
    *
    * @param e  the event as specified in the test script
    * @throws VDDException if event instantiation fails
    */

   public Checkbox(Element e) throws VDDException {
      super(e);

      this.actionList.addLast(new Pair<String,Action>("disabled",
                                                      new DisabledAction()));
      this.actionList.addLast(new Pair<String,Action>("set",
                                                      new SetAction()));
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
