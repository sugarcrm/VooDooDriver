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
 * The link event.
 *
 * @author Jon duSaint
 */

class Link extends HtmlEvent {


   /**
    * Instantiate a link event.
    *
    * @param event  the event as specified in the test script
    * @throws VDDException if event instantiation fails
    */

   public Link(Element e) throws VDDException {
      super(e);

      this.actionList.addFirst(new Pair<String,Action>("disabled",
                                                       new DisabledAction()));
      this.actionList.insertBefore(new Pair<String,Action>("alert",
                                                           new AlertAction()),
                                   "click");
   }


   /**
    * Get the value to be used when storing to a var.
    *
    * In the case of link, the value is the text of the A.
    */

   protected String getVarValue() {
      return this.element.getText();
   }


   /**
    * Run the link event.
    *
    * @throws VDDException if execution is unsuccessful
    */

   public void execute() throws VDDException {
      /*
       * The click action runs in the link event by default.  Inject
       * it here if it wasn't already specified.
       */
      if (!this.actions.containsKey("click")) {
         this.actions.put("click", true);
      }

      super.execute();
   }
}
