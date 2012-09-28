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
 * The whitelist event.
 *
 * <p>This event adds the named text or regular expression to the page
 * asserter whitelist.  When an event uses the assertPage attribute,
 * the current page is checked against the assertions in the assert
 * page.  Anything matched in the whitelist is exempted from this
 * check.</p>
 *
 * @author Jon duSaint
 */

class Whitelist extends Event {


   /**
    * Instantiate a whitelist event.
    *
    * @param e  the event as specified in the test script
    * @throws VDDException if event instantiation fails
    */

   public Whitelist(Element e) throws VDDException {
      super(e);
   }


   /**
    * Run the whitelist event.
    *
    * @throws VDDException if execution is unsuccessful
    */

   public void execute() throws VDDException {
      String keys[] = {"action", "name"};
      for (String k: keys) {
         if (!this.actions.containsKey(k)) {
            throw new VDDException("Missing attribute '" + k + "'");
         }
      }
      String action = (String)this.actions.get("action");
      String name = (String)this.actions.get("name");

      if (action.equals("add")) {
         if (!this.actions.containsKey("content")) {
            throw new VDDException("Missing attribute 'content'");
         }
         String content = (String)this.actions.get("content");
         this.eventLoop.report.log("Adding whitelist item '" + name +
                                   "' => '" + content + "'");
         this.eventLoop.whitelist.put(name, content);
      } else if (action.equals("delete")) {
         this.eventLoop.report.log("Removing whitelist item '" + name + "'");
         this.eventLoop.whitelist.remove(name);
      } else {
         throw new VDDException("Invalid action '" + action +
                                "'. Valid actions are 'add' and 'delete'.");
      }
   }
}
