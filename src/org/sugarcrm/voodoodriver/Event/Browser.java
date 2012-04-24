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
 * The browser event.
 *
 * @author Jon duSaint
 */

public class Browser extends Event {

   /**
    * Instantiate a browser event.
    * @param event  the event as specified in the test script
    * @throws VDDException if event instantiation fails
    */

   public Browser(Element event) throws VDDException {
      super(event);
   }


   /**
    * Execute the browser action attribute.
    *
    * @param b      the current {@link org.sugarcrm.voodoodriver.Browser} object
    * @param action the action attribute
    * @throws VDDException  if an unknown browser action was specified
    */

   void browserAction(org.sugarcrm.voodoodriver.Browser b, String action)
      throws VDDException {
      this.eventLoop.report.Log("Calling browser action " + action + ".");

      String actionl = action.toLowerCase();
      if (actionl.equals("back")) {
         b.back();
      } else if (actionl.equals("close")) {
         b.close();
      } else if (actionl.equals("forward")) {
         b.forward();
      } else if (actionl.equals("refresh")) {
         b.refresh();
      } else {
         throw new VDDException("Unknown browser action '" + action + "'");
      }
   }


   /**
    * Run the browser event.
    *
    * @throws VDDException if execution is unsuccessful
    */

   public void execute() throws VDDException {
      org.sugarcrm.voodoodriver.Browser b = this.eventLoop.Browser;

      for (String attr: this.actions.keySet()) {
         attr = attr.toLowerCase();
         if (attr.equals("action")) {
            browserAction(b, (String)this.actions.get(attr));
         } else if (attr.equals("assert")) {
            String assertion = (String)this.actions.get(attr);
            assertion = replaceString(assertion);

            if (this.parent != null) {
               b.Assert(assertion, this.parent);
            } else {
               b.Assert(assertion);
            }
         } else if (attr.equals("assertnot")) {
            String assertion = (String)this.actions.get(attr);
            assertion = replaceString(assertion);

            if (this.parent != null) {
               b.AssertNot(assertion, this.parent);
            } else {
               b.AssertNot(assertion);
            }
         } else if (attr.equals("assertpage")) {
            // assertPage = this.clickToBool(event.get("assertPage").toString());
            // this.report.Log(String.format("Borwser assertPage => '%s'.",
            //                               assertPage));
            this.eventLoop.report.Warn("browser attribute assertpage unimplemented");
         } else if (attr.equals("cssprop")) {
            this.eventLoop.report.Warn("browser attribute cssprop unimplemented");
         } else if (attr.equals("cssvalue")) {
            this.eventLoop.report.Warn("browser attribute cssvalue unimplemented");
         } else if (attr.equals("exist")) {
            this.eventLoop.report.Warn("browser attribute exist unimplemented");
         } else if (attr.equals("jscriptevent")) {
            this.eventLoop.report.Warn("browser attribute jscriptevent unimplemented");
         } else if (attr.equals("send_keys")) {
            this.eventLoop.report.Warn("browser attribute send_keys unimplemented");
         } else if (attr.equals("url")) {
            String url = (String)this.actions.get(attr);
            url = replaceString(url);
            this.eventLoop.report.Log("URL: " + url);
            b.url(url);
         } else {
            throw new VDDException("Unknown browser attribute '" + attr + "'");
         }
      }
   }
}
