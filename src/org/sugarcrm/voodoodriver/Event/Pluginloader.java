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

import org.sugarcrm.voodoodriver.JavaPlugin;
import org.sugarcrm.voodoodriver.PluginException;
import org.sugarcrm.voodoodriver.VDDException;
import org.w3c.dom.Element;

/**
 * The pluginloader event.
 *
 * <p>The pluginloader event dynamically loads a java plugin.  This
 * plugin is then active for the duration of the execution of the
 * current test.</p>
 *
 * @author Jon duSaint
 */

class Pluginloader extends Event {


   /**
    * Instantiate a pluginloader event.
    *
    * @param e  the event as specified in the test script
    * @throws VDDException if event instantiation fails
    */

   public Pluginloader(Element e) throws VDDException {
      super(e);
   }


   /**
    * Run the pluginloader event.
    *
    * @throws VDDException if execution is unsuccessful
    */

   public void execute() throws VDDException {
      String[] attrs = {"classname", "file"};
      for (String attr: attrs) {
         if (!this.actions.containsKey(attr)) {
            throw new VDDException("Missing '" + attr + "'attribute");
         }
      }

      String c = this.replaceString((String)this.actions.get("classname"));
      String f = this.replaceString((String)this.actions.get("file"));

      this.eventLoop.report.Log("Loading plugin with classname '" + c +
                                "' from file '" + f + "'");

      try {
         this.eventLoop.plugins.add(new JavaPlugin(c, f));
      } catch (PluginException e) {
         throw new VDDException("Failed to load plugin", e);
      }
   }
}
