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

import org.sugarcrm.voodoodriver.Utils;
import org.sugarcrm.voodoodriver.VDDException;
import org.w3c.dom.Element;

/**
 * The screenshot event.
 *
 * @author Jon duSaint
 */

class Screenshot extends Event {


   /**
    * Instantiate a screenshot event.
    *
    * @param event  the event as specified in the test script
    * @throws VDDException if event instantiation fails
    */

   public Screenshot(Element e) throws VDDException {
      super(e);
   }


   /**
    * Run the screenshot event.
    *
    * @throws VDDException if execution is unsuccessful
    */

   public void execute() throws VDDException {
      if (!this.actions.containsKey("file")) {
         throw new VDDException("Screenshot event missing 'file' attribute");
      }

      Utils.takeScreenShot((String)this.actions.get("file"),
                           this.eventLoop.report);
   }
}
