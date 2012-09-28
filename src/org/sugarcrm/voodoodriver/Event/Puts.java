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
 * The puts event.
 *
 * @author Jon duSaint
 */

public class Puts extends Event {

   /**
    * Instantiate a puts event.
    * @param event  the event as specified in the test script
    * @throws VDDException if event instantiation fails
    */

   public Puts(Element event) throws VDDException {
      super(event);
   }


   /**
    * Run the puts event.
    *
    * @throws VDDException if execution is unsuccessful
    */

   public void execute() throws VDDException {
      String txt = (this.actions.containsKey("txt") ?
                    (String)this.actions.get("txt") :
                    (this.actions.containsKey("text") ?
                     (String)this.actions.get("text") :
                     null));

      if (txt == null) {
         throw new VDDException("Puts event missing 'txt' or 'text' attribute");
      }

      txt = this.replaceString(txt);
      log(txt);
   }
}
