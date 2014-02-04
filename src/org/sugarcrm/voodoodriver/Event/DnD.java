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

import java.util.HashMap;
import org.openqa.selenium.WebElement;
import org.sugarcrm.voodoodriver.VDDException;
import org.sugarcrm.voodoodriver.VDDMouse;
import org.w3c.dom.Element;

/**
 * The DnD event.
 *
 * <p>This event implements drag and drop.  This functionality is
 * currently blocked by Selenium bug 2558.</p>
 *
 * @author Jon duSaint
 */

class DnD extends Event {


   /**
    * Instantiate a dnd event.
    *
    * @param e  the event as specified in the test script
    * @throws VDDException if event instantiation fails
    */

   public DnD(Element e) throws VDDException {
      super(e);
   }


   /**
    * Run the dnd event.
    *
    * @throws VDDException if execution is unsuccessful
    */

   public void execute() throws VDDException {
      String[] attrs = {"src", "dst"};
      HashMap<String,WebElement> elements = new HashMap<String,WebElement>();

      for (String attr: attrs) {
         String eln;
         WebElement e;

         if (!this.actions.containsKey(attr)) {
            throw new VDDException("Missing attribute '" + attr + "'");
         }

         eln = (String)this.actions.get(attr);

         if (!this.eventLoop.elementStore.containsKey(eln)) {
            throw new VDDException("No element named '" + eln +
                                   "' exists in the element store");
         }

         elements.put(attr, this.eventLoop.elementStore.get(eln));
      }

      VDDMouse mouse = new VDDMouse(this.eventLoop.report);
      mouse.DnD(elements.get("src"), elements.get("dst"));
   }
}
