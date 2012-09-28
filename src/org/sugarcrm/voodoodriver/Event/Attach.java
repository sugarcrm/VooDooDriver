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

import java.util.regex.Pattern;
import java.util.Set;
import org.sugarcrm.voodoodriver.VDDException;
import org.w3c.dom.Element;

/**
 * The attach event.
 *
 * @author Jon duSaint
 */

class Attach extends Event {

   /**
    * Window handle of the parent window.
    */

   private String parentWindow;


   /**
    * Instantiate an attach event.
    * @param event  the event as specified in the test script
    * @throws VDDException if event instantiation fails
    */

   public Attach(Element e) throws VDDException {
      super(e);
   }


   /**
    * Get the window title of the window with the specified handle.
    *
    * @param b       the current Browser object
    * @param handle  window handle
    * @return window title
    */

   private String getTitle(org.sugarcrm.voodoodriver.Browser b, String handle) {
      return b.getDriver().switchTo().window(handle).getTitle();
   }


   /**
    * Get the window URL of the window with the specified handle.
    *
    * @param b       the current Browser object
    * @param handle  window handle
    * @return window URL
    */

   private String getUrl(org.sugarcrm.voodoodriver.Browser b, String handle) {
      return b.getDriver().switchTo().window(handle).getCurrentUrl();
   }


   /**
    * List all open windows.
    *
    * @param r  the current Reporter object
    * @param b  the current Browser object
    */

   private void listWindows(org.sugarcrm.voodoodriver.Reporter r,
                            org.sugarcrm.voodoodriver.Browser b) {
      int n = 0;
      for (String h: b.getDriver().getWindowHandles()) {
         r.log(String.format("[%d] Handle: %s", n, h));
         r.log(String.format("[%d] Title:  %s", n, getTitle(b, h)));
         r.log(String.format("[%d] URL:    %s", n, getUrl(b, h)));
         n++;
      }
   }


   /**
    * Determine whether a search string matches a target string.
    *
    * @param search  the search string
    * @param target  the target string
    * @return true if search matches target
    */

   private boolean matches(String search, String target) {
      return Pattern.matches(search, target);
   }


   /**
    * Find a browser window with the matching title.
    *
    * @param b      the current Browser object
    * @param title  window title to search for
    * @param index  index into the list of matching windows
    * @return window handle of the matching window
    * @throws VDDException if no matching window can be found
    */

   private String getWindowByTitle(org.sugarcrm.voodoodriver.Browser b,
                                   String title, int index)
   throws VDDException {
      int nthMatch = 0;

      for (String h: b.getDriver().getWindowHandles()) {
         if (matches(title, this.getTitle(b, h))) {
            if (nthMatch == index) {
               return h;
            }
            nthMatch++;
         }
      }

      throw new VDDException("Unable to find window by title '" + title + "'");
   }


   /**
    * Find a browser window with the matching URL.
    *
    * @param b     the current Browser object
    * @param url   window URL to search for
    * @param index index into the list of matching windows
    * @return window handle of the matching window
    * @throws VDDException if no matching window can be found
    *
    */

   private String getWindowByUrl(org.sugarcrm.voodoodriver.Browser b,
                                 String url, int index)
   throws VDDException {
      int nthMatch = 0;

      for (String h: b.getDriver().getWindowHandles()) {
         if (matches(url, getUrl(b, h))) {
            if (nthMatch == index) {
               return h;
            }
            nthMatch++;
         }
      }

      throw new VDDException("Unable to find window by url '" + url + "'");
   }


   /**
    * Find the Nth browser window
    *
    * @param b      the current Browser object
    * @param index  index into the list of matching windows
    * @return window handle of the matching window
    * @throws VDDException if no matching window can be found
    */

   private String getWindowByIndex(org.sugarcrm.voodoodriver.Browser b,
                                   int index)
   throws VDDException {
      for (String h: b.getDriver().getWindowHandles()) {
         if (index == 0) {
            return h;
         }
         index--;
      }

      throw new VDDException("Unable to find window by index '" + index + "'");
   }


   /**
    * Run the attach event.
    *
    * @throws StopEventException if child event execution is to be skipped
    * @throws VDDException if execution is unsuccessful
    */

   public void execute() throws StopEventException, VDDException {
      org.sugarcrm.voodoodriver.Browser b = this.eventLoop.Browser;
      org.sugarcrm.voodoodriver.Reporter r = this.eventLoop.report;

      this.parentWindow = b.getDriver().getWindowHandle();
      String attachWindow = null;
      int index = 0;

      listWindows(r, b);

      r.log("Current Window Handle: " + this.parentWindow);

      if (this.actions.containsKey("index")) {
         String si = this.replaceString((String)this.actions.get("index"));
         try {
            index = Integer.valueOf(si);
         } catch (NumberFormatException e) {
            r.error("Specified attach index '" +
                    (String)this.actions.get("index") + "' " +
                    "is not a valid integer. Using 0.");
         }
      }

      int nRetries = this.eventLoop.getAttachTimeout();

      while (true) {
         try {
            if (this.actions.containsKey("title")) {
               String t = this.replaceString((String)this.actions.get("title"));
               r.log("Search for window with title " + t);
               attachWindow = getWindowByTitle(b, t, index);
            } else if (this.actions.containsKey("url")) {
               String u = this.replaceString((String)this.actions.get("url"));
               r.log("Search for window with url " + u);
               attachWindow = getWindowByUrl(b, u, index);
            } else {
               r.log("Search for window with index " + index);
               attachWindow = getWindowByIndex(b, index);
            }

            break;
         } catch (VDDException e) {
            if (nRetries-- > 0) {
               try {
                  Thread.sleep(1000);
               } catch (InterruptedException i) {
                  // Ignore
               }
            } else {
               b.getDriver().switchTo().window(this.parentWindow);
               r.error(e.getMessage());
               throw new StopEventException();
            }
         }
      }

      r.log("Switching to matching window:");
      r.log("Handle: " + attachWindow);
      r.log("Title: " + getTitle(b, attachWindow));
      r.log("URL: " + getUrl(b, attachWindow));

      this.eventLoop.setCurrentHWND(attachWindow);
      b.getDriver().switchTo().window(attachWindow);
   }


   /**
    * Perform the after-attach wait.
    *
    * @throws VDDException if an error occurs
    */

   public void afterChildren() throws VDDException {
      int timeout = this.eventLoop.getAttachTimeout();

      this.eventLoop.report.log("Switching back to window handle: " +
                                this.parentWindow);
      this.eventLoop.Browser.setBrowserOpened();
      this.eventLoop.Browser.getDriver().switchTo().window(this.parentWindow);
      this.eventLoop.setCurrentHWND(this.parentWindow);

      if (timeout > 0) {
            this.eventLoop.report.log("Waiting " + timeout + " seconds " +
                                      "before executing next event.");
            try {
               Thread.sleep(timeout * 1000);
            } catch (InterruptedException i) {
               // Ignore
            }
      }
   }
}
