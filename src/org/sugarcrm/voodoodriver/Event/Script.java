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
import java.io.FilenameFilter;
import org.sugarcrm.voodoodriver.TestLoader;
import org.sugarcrm.voodoodriver.VDDException;
import org.w3c.dom.Element;

/**
 * The script event.
 *
 * @author Jon duSaint
 */

class Script extends Event {


   /**
    * Instantiate a script event.
    *
    * <p>Script loads and runs another test script.  This is generally
    * used with library functionality.</p>
    *
    * @param e  the event as specified in the test script
    * @throws VDDException if event instantiation fails
    */

   public Script(Element e) throws VDDException {
      super(e);
   }

   /**
    * Run a single test script.
    *
    * @param script  the test script to run
    */

   private void runOneScript(File script) throws VDDException {
      if (!script.exists()) {
         throw new VDDException("Script to run '" + script +
                                "' does not exist");
      }

      try {
         this.eventLoop.vars.pushContext();
         TestLoader tl = new TestLoader(script, this.eventLoop.report);
         this.eventLoop.processEvents(tl.getEvents(), this.parent);
      } finally {
         this.eventLoop.vars.popContext();
      }
   }


   /**
    * Run the script event.
    *
    * @throws VDDException if execution is unsuccessful
    */

   public void execute() throws VDDException {
      if (this.actions.containsKey("file")) {
         runOneScript(new File(this.replaceString((String)
                                                  this.actions.get("file"))));
      } else if (this.actions.containsKey("fileset")) {
         File dir = new File(this.replaceString((String)
                                                this.actions.get("fileset")));
         if (!dir.isDirectory()) {
            throw new VDDException("Specified fileset must be directory");
         }
         File[] scripts = dir.listFiles(new FilenameFilter() {
               public boolean accept(File d, String fn) {
                  return fn.toLowerCase().endsWith(".xml");
               }
            });

         for (File script: scripts) {
            runOneScript(script);
         }
      } else {
         throw new VDDException("Missing file or fileset attributes");
      }
   }
}
