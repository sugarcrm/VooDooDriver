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
 * The delete event.
 *
 * <p>This event removes HTML elements from the element store.  This
 * element store is used to store HTML elements for use by the drag
 * and drop (Dnd) event.  Elements are added with the "save" attribute
 * of the event that selects the element.</p>
 *
 * @author Jon duSaint
 */

class Delete extends Event {


   /**
    * Instantiate a delete event.
    *
    * @param event  the event as specified in the test script
    * @throws VDDException if event instantiation fails
    */

   public Delete(Element e) throws VDDException {
      super(e);
   }


   /**
    * Run the delete event.
    *
    * @throws VDDException if execution is unsuccessful
    */

   public void execute() throws VDDException {
      if (!this.actions.containsKey("name")) {
         throw new VDDException("Missing name attribute");
      }

      String nm = (String)this.actions.get("name");

      if (!this.eventLoop.elementStore.containsKey(nm)) {
         throw new VDDException("No HTML element stored as '" + nm + "'");
      }

      log("Deleting HTML element stored as '" + nm + "'");
      this.eventLoop.elementStore.remove(nm);
   }
}
