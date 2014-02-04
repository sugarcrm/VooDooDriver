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

import java.io.File;
import org.sugarcrm.voodoodriver.VDDException;
import org.w3c.dom.Element;

/**
 * The filefield event.
 *
 * @author Jon duSaint
 */

class Filefield extends InteractiveHtmlEvent {

   /**
    * Set action.
    */

   protected class SetAction implements Action {

      /**
       * Run the set action.
       */

      public void action(Object val) {
         File f = new File((String)val);
         boolean isCanonical = false;

         try {
            f = f.getCanonicalFile();
            isCanonical = true;
         } catch (java.io.IOException e) {
         }

         log("Setting value of filefield to '" + f.toString() + "'");

         if (eventLoop.Browser instanceof org.sugarcrm.voodoodriver.IE &&
             (!isCanonical || !f.exists())) {
            rterror("File must exist when running IE.");
         }

         element.sendKeys(f.toString());
      }
   }


   /**
    * Instantiate a Filefield event.
    *
    * @param e  the event as specified in the test script
    * @throws VDDException if event instantiation fails
    */

   public Filefield(Element e) throws VDDException {
      super(e);

      this.actionList.replacePair(new Pair<String,Action>("set",
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
    * Run the filefield event.
    *
    * @throws VDDException if execution is unsuccessful
    */

   public void execute() throws VDDException {
      super.execute();
   }
}
