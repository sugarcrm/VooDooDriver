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
 * The execute event.
 *
 * <p>Execute allows the user to run an external program.  Arguments
 * may be passed to the program using nested &lt;arg&gt; events, but
 * nothing from the program is returned.</p>
 *
 * @author Jon duSaint
 */

class Execute extends Event {


   /**
    * Instantiate an execute event.
    *
    * @param e  the event as specified in the test script
    * @throws VDDException if event instantiation fails
    */

   public Execute(Element e) throws VDDException {
      super(e);
   }


   /**
    * Run the execute event.
    *
    * @throws VDDException if execution is unsuccessful
    */

   public void execute() throws VDDException {
      String[] args;
      Process p = null;
      int rv = 0;

      if (!this.actions.containsKey("args")) {
         throw new VDDException("Missing <arg> children");
      }

      args = (String[])this.actions.get("args");
      log("Executing child process...");
      for (String arg: args) {
         log("  => " + arg);
      }
      
      this.eventLoop.resetThreadTime();
      try {
         p = Runtime.getRuntime().exec(args);
      } catch (java.io.IOException e) {
         throw new VDDException("Failed to exec child process", e);
      }

      while (true) {
         try {
            rv = p.waitFor();
            break;
         } catch (InterruptedException e) {
            // ignore
         }
      }

      this.eventLoop.resetThreadTime();
      log("Child process finished.");

      if (rv != 0) {
         error("Error code from child process: '" +
               (new Integer(rv)).toString() + "'");
      } else {
         log("Child process was successful.");
      }
   }
}
