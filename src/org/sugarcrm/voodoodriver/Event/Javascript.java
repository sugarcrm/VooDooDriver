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
import java.io.FileInputStream;
import java.io.InputStream;
import org.sugarcrm.voodoodriver.VDDException;
import org.w3c.dom.Element;

/**
 * The javascript event.
 *
 * <p>This event executes javascript code in the context of the
 * current browser window.</p>
 *
 * @author Jon duSaint
 */

class Javascript extends Event {


   /**
    * Instantiate a javascript event.
    *
    * @param e  the event as specified in the test script
    * @throws VDDException if event instantiation fails
    */

   public Javascript(Element e) throws VDDException {
      super(e);
   }


   /**
    * Run the javascript event.
    *
    * @throws VDDException if execution is unsuccessful
    */

   public void execute() throws VDDException {
      String js = "";

      if (this.actions.containsKey("content")) {
         js = (String)this.actions.get("content");
      } else if (this.actions.containsKey("file")) {
         /* No way in Java to read an entire file at once */
         File f = new File(this.replaceString((String)this.actions.get("file")));
         InputStream is;
         try {
            is = new FileInputStream(f);
         } catch (java.io.FileNotFoundException e) {
            throw new VDDException("Javascript file '" + f + "' does not exist");
         }

         byte[] data = new byte[(int)f.length()];
         int used = 0;

         try {
            while (true) {
               int n = is.read(data, used, data.length - used);
               if (n <= 0) {
                  break;
               }
               used += n;
            }
         } catch (java.io.IOException e) {
            throw new VDDException("Error reading javascript file '" + f + "'",
                                   e);
         } finally {
            try {
               is.close();
            } catch (java.io.IOException e) {
               // ignore
            }
         }

         js = new String(data, 0, used);
      } else {
         throw new VDDException("Missing either content or file attributes");
      }

      this.eventLoop.Browser.executeJS(this.replaceString(js), null);
   }
}
