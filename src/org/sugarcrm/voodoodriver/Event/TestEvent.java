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
 * The test event.
 *
 * @author Jon duSaint
 */

class TestEvent extends Event {


   /**
    * Instantiate a test event.
    *
    * @param event  the event as specified in the test script
    * @throws VDDException if event instantiation fails
    */

   public TestEvent(Element e) throws VDDException {
      super(e);
      System.out.println("*** TestEvent:");
      System.out.println("  => event name: " + this.testEvent.getNodeName());
      System.out.println("  => selectors:");
      for (String key: this.selectors.keySet()) {
         System.out.println("    " + key + ": " +
                            String.valueOf(this.selectors.get(key)));
      }
      System.out.println("  => actions:");
      for (String key: this.actions.keySet()) {
         System.out.println("    " + key + ": " +
                            String.valueOf(this.actions.get(key)));
      }
   }


   /**
    * Run the test event.
    *
    * @throws VDDException if execution is unsuccessful
    */

   public void execute() throws VDDException {
      System.out.println("*** Executing " + this.testEvent.getNodeName());
   }
}
