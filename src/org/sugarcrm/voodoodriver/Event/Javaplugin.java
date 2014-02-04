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

import org.sugarcrm.voodoodriver.Plugin;
import org.sugarcrm.voodoodriver.PluginData;
import org.sugarcrm.voodoodriver.VDDException;
import org.w3c.dom.Element;

/**
 * The javaplugin event.
 *
 * <p>This event executes a previously loaded java plugin.</p>
 *
 * @author Jon duSaint
 */

class Javaplugin extends Event {


   /**
    * Instantiate a javaplugin event.
    *
    * @param e  the event as specified in the test script
    * @throws VDDException if event instantiation fails
    */

   public Javaplugin(Element e) throws VDDException {
      super(e);
   }


   /**
    * Run the javaplugin event.
    *
    * @throws VDDException if execution is unsuccessful
    */

   public void execute() throws VDDException {
      String classname;
      Plugin plugin = null;
      PluginData data = new PluginData();

      if (!this.actions.containsKey("classname")) {
         throw new VDDException("Missing attribute 'classname'");
      }

      classname = (String)this.actions.get("classname");
      
      for (Plugin p: this.eventLoop.plugins) {
         if (p.matches(classname)) {
            plugin = p;
            break;
         }
      }

      if (plugin == null) {
         throw new VDDException("No plugin with class name '" +
                                classname + "' has been loaded");
      }

      if (this.actions.containsKey("args") &&
          this.actions.get("args") != null) {
         String[] args = (String[])this.actions.get("args");

         for (int k = 0; k < args.length; k++) {
            args[k] = this.replaceString(args[k]);
         }

         data.setArgs(args);
      }

      data.setElement(this.parent);
      data.setBrowser(this.eventLoop.Browser);
      data.setVars(this.eventLoop.vars);
      data.setHijacks(this.eventLoop.hijacks);
      data.setTestName(this.eventLoop.testName);

      if (!plugin.execute(data, this.eventLoop.report)) {
         error("Java plugin failed");
      }
   }
}
