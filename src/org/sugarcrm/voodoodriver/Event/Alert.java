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

//import org.openqa.selenium.Alert;
import org.openqa.selenium.NoAlertPresentException;
import org.sugarcrm.voodoodriver.VDDException;
import org.w3c.dom.Element;

/**
 * The alert event.
 *
 * <p>This event handles the dialog boxes created by the javascript
 * snippet <code>window.alert()</code>.</p>
 *
 * <p>Attributes are:
 * <dl>
 *   <dt>alert</dt><dd>Whether to click &quot;OK&quot; (<code>true</code>)
 *                     or &quot;Cancel&quot; (<code>false</code>).</dd>
 *   <dt>assert</dt><dd>Assert that the alert contains the specified
 *                      string.</dd>
 *   <dt>assertnot</dt><dd>Assert that the alert does not contain the
 *                         specified string.</dd>
 *   <dt>exists</dt><dd>Only determine whether or not the alert exists.</dd>
 *   <dt>required</dt><dd>If false, this alert need not be present.</dd>
 * </dl>
 *
 * @author Jon duSaint
 */

public class Alert extends Event {


   /**
    * Instantiate an alert event.
    *
    * @param e  the event as specified in the test script
    * @throws VDDException if event instantiation fails
    */

   public Alert(Element e) throws VDDException {
      super(e);
   }


   /**
    * Switch back to the parent window.
    */

   private void switchBack() {
      try {
         this.eventLoop.Browser.getDriver().switchTo().defaultContent();
      } catch (Exception e) {
         /*
          * Bug 53577: if this alert is put up in response to a
          * window.close, switching back to the default content
          * will throw an exception.  Curiously, it's a javascript
          * exception rather than NoSuchFrameException or
          * NoSuchWindowException that would be expected.  Catch
          * generic "Exception" in case the Selenium folks fix the
          * Javascript error.
          */
         log("Unable to switch back to window. Is it closed?");
      }
   }


   /**
    * Run the alert event.
    *
    * @throws VDDException if execution is unsuccessful
    */

   public void execute() throws VDDException {
      Boolean accept = null;
      String assertStr = null;
      String assertNotStr = null;
      Boolean exists = null;
      boolean required = true;
      org.openqa.selenium.Alert alert = null;

      /*
       * These attributes have the same functionality as those used by
       * the children of HtmlEvent, but Alert is different enough to
       * implement it that it can't descend from that and so this
       * functionality has be to reimplemented here.
       */

      if (this.actions.containsKey("alert")) {
         accept = (Boolean)this.actions.get("alert");
      }
      if (this.actions.containsKey("exists")) {
         exists = (Boolean)this.actions.get("exists");
         accept = true;
      }

      if (accept == null && exists == null) {
         throw new VDDException("Missing 'alert' or 'exists' attribute");
      }

      if (this.actions.containsKey("assert")) {
         assertStr = replaceString((String)this.actions.get("assert"));
      }
      if (this.actions.containsKey("assertnot")) {
         assertNotStr = replaceString((String)this.actions.get("assertnot"));
      }

      if (this.actions.containsKey("required")) {
         required = (Boolean)this.actions.get("required");
      }

      /*
       * Attempt to switch to the alert so we can handle it.
       */

      try {
         alert = this.eventLoop.Browser.getDriver().switchTo().alert();
      } catch (NoAlertPresentException e) {
         if (exists != null && exists == false) {
            log("Alert not found and exists is false.");
         } else if (required == false) {
            log("Alert not found and required is false.");
         } else {
            error("Alert not found");
         }
         return;
      }

      if (exists != null && exists == false) {
         error("Alert found but exists is false");
         switchBack();
         return;
      }

      log("Found alert with text '" + alert.getText() + "'");

      if (assertStr != null) {
         this.eventLoop.report.Assert(assertStr, alert.getText());
      }
      if (assertNotStr != null) {
         this.eventLoop.report.AssertNot(assertNotStr, alert.getText());
      }

      if (accept) {
         log("Alert is being accepted.");
         alert.accept();
      } else {
         log("Alert is being dismissed.");
         alert.dismiss();
      }

      switchBack();

      try {
         Thread.sleep(1000);
      } catch (InterruptedException e) {
         /* NBD */
      }

      firePlugin(org.sugarcrm.voodoodriver.PluginEvent.AFTERDIALOGCLOSED);
   }
}
