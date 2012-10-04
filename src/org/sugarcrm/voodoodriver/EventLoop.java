/*
 * Copyright 2011-2012 SugarCRM Inc.
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

package org.sugarcrm.voodoodriver;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Set;
import org.apache.commons.io.FilenameUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.ElementNotVisibleException;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.NoSuchFrameException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;
import org.sugarcrm.voodoodriver.Event.Event;


/**
 * This is the heart of all of VooDooDriver.  This class handles
 * executing all of the SODA language commands in the web browser.
 *
 * @author trampus
 * @author Jon duSaint
 */

@SuppressWarnings("unchecked")  //XXX
public class EventLoop implements Runnable {

   /**
    * List of events in this test.
    */

   private ArrayList<Event> testEvents;

   /**
    * Browser object for use by this test.
    */

   public Browser Browser;

   /**
    * Reporter object for use by this test.
    */

   public Reporter report;

   /**
    * VDD variables can have their values overridden by values passed
    * in on the command line or in the configuration file.  These are
    * referred to as &quot;hijacks&quot;.  This is a structure of
    * hijack key value pairs.
    *
    * XXX: Should probably be typed, e.g. HashMap<String,Object> or something.
    */

   public VDDHash hijacks;

   /**
    * Name of the current running test.
    */

   public String testName;

   /**
    * Strings to ignore during page assertion processing.
    */

   public HashMap<String,String> whitelist;

   /**
    * Stored, named HTML elements for use with DnD.
    */

   public HashMap<String,WebElement> elementStore;

   /**
    * List of loaded plugins.
    */

   public ArrayList<Plugin> plugins;

   /**
    * VDD variables.
    */

   public Vars vars;

   /**
    * File specified by a &lt;csv override=&quot;&quot;/&rt; event.
    */

   public File csvOverrideFile;

   /**
    * Handle to the browser window.
    */

   private String currentHWnd = null;

   /**
    * Amount of time, in seconds, to wait after switching windows.
    */

   private int attachTimeout = 0;

   /**
    * Timestamp that indicates to Test.runTest that this thread is
    * still alive.  If this is not updated before the expiry of the
    * watchdog timer, the test is considered a failure and this thread
    * is terminated.
    */

   private Date threadTime = null;

   /**
    * This thread.
    */

   private volatile Thread runner;

   /**
    * If true, this thread is to terminate.
    */

   private volatile Boolean threadStop = false;


   /**
    * Instantiate EventLoop.
    *
    * @param events   list of events from the test file
    * @param config   VDD config info
    * @param reporter {@link Reporter}
    * @param vars  {@link Vars}
    * @param testName the current running test
    */

   public EventLoop(ArrayList<Event> events, VDDHash config, Reporter reporter,
                    Vars vars, String testName) {
      this.testEvents = events;
      this.report = reporter;
      this.vars = vars;
      this.testName = testName;

      this.Browser = (Browser)config.get("browser");
      this.hijacks = (VDDHash)config.get("hijack");;
      this.whitelist = new HashMap<String,String>();
      this.elementStore = new HashMap<String,WebElement>();

      this.plugins = new ArrayList<Plugin>();
      @SuppressWarnings("unchecked")
         ArrayList<Plugin> p = (ArrayList<Plugin>)config.get("plugin");
      if (p != null) {
         this.plugins.addAll(p);
      }

      /* Create an initial timestamp. */
      this.vars.put("stamp",
                    new SimpleDateFormat("yyMMdd_hhmmss").format(new Date()));

      SimpleDateFormat df = new SimpleDateFormat("MM/dd/yyyy");
      this.vars.put("currentdate", df.format(new Date()));

      this.setCurrentHWND(this.Browser.getDriver().getWindowHandle());

      this.threadTime = new Date();
      this.runner = new Thread(this, "EventLoop-Thread");
      runner.start();
   }


   /**
    * Set the attach timeout.
    *
    * The attach timeout is the time in seconds after which the Attach
    * event will give up retrying to find the window to attach to.
    *
    * @param timeout  number of seconds to wait after window switch
    */

   public void setAttachTimeout(int timeout) {
      this.attachTimeout = timeout;
   }


   /**
    * Retrieve the attach timeout.
    *
    * @return the attach timeout
    */

   public int getAttachTimeout() {
      return this.attachTimeout;
   }


   /**
    * Check to see if a browser window exists.
    *
    * @param hwnd  window handle
    * @return whether the window exists
    */

   private boolean windowExists(String hwnd) {
      Set<String> windows = null;

      try {
         windows = this.Browser.getDriver().getWindowHandles();
      } catch (org.openqa.selenium.WebDriverException e) {
         /*
          * When running the IE driver, if the window is closed, an
          * exception is thrown.
          */
         return false;
      }

      for (int i = 0; i < windows.size(); i++) {
         if (hwnd.equals(windows.toArray()[i].toString())) {
            return true;
         }
      }

      return false;
   }


   /**
    * Set the active browser window for VDD to use.
    *
    * @param hwnd {@link String}
    */

   public void setCurrentHWND(String hwnd) {
      this.currentHWnd = hwnd;
   }


   /**
    * Return whether this thread is still active.
    *
    * @return true if the thread is alive
    */

   public boolean isAlive() {
      return this.runner.isAlive();
   }


   /**
    * Indicate that this thread should terminate.
    */

   public void stop() {
      synchronized (this.threadStop) {
         this.threadStop = true;
         this.runner.interrupt();
      }
   }


   /**
    * Return whether the thread has been told to stop.
    *
    * @return whether the thread has been told to stop
    */

   public boolean isStopped() {
      boolean result = false;

      synchronized (this.threadStop) {
         result = this.threadStop;
      }

      return result;
   }


   /**
    * Update this EventLoop's thread time.
    */

   public void updateThreadTime() {
      synchronized (this.threadTime) {
         this.threadTime = new Date();
      }
   }


   /**
    * Return the current thread time.
    *
    * @return current thread time
    */

   public Date getThreadTime() {
      Date tmp = null;

      synchronized (this.threadTime) {
         tmp = this.threadTime;
      }

      return tmp;
   }


   /**
    * Thread entry point.
    *
    * Initialize thread state and run the events in this test file.
    */

   public void run() {
      this.report.log("Thread Running...");
      this.threadTime = new Date();

      this.firePlugin(null, PluginEvent.BEFORETEST);

      processEvents(this.testEvents, null);

      this.firePlugin(null, PluginEvent.AFTERTEST);
   }


   /**
    * Run a sequence of events.
    *
    * @param events  {@link ArrayList} of {@link Event} from the test script
    * @param parent  parent element if process child events
    */

   public void processEvents(ArrayList<Event> events, WebElement parent) {
      for (Event event: events) {
         if (isStopped()) {
            break;
         }

         handleSingleEvent(event, parent);
      }
   }


   /**
    * Execute one {@link Event}.
    *
    * @param event   the {@link Event} to execute
    * @param parent  parent element, if currently executing children
    */

   private void handleSingleEvent(Event event, WebElement parent) {
      String eventName = event.getName();

      this.report.log(eventName + " event started...");
      this.updateThreadTime();
      event.setEventLoop(this);
      event.setParent(parent);

      try {
         event.execute();
         if (event.hasChildren()) {
            processEvents(event.getChildren(), event.getElement());
         }
         event.afterChildren();
      } catch (org.sugarcrm.voodoodriver.Event.StopEventException e) {
         /* Not an error */
      } catch (ElementNotVisibleException e) {
         String s = "Element is not visible";
         if (event.required()) {
            this.report.error(s);
         } else {
            this.report.log(s + ", but required = false");
         }
      } catch (VDDException e) {
         Throwable cause = e.getCause();

         if (cause == null) {
            cause = e;
         }

         this.report.exception("Exception during event execution", cause);
      }

      this.updateThreadTime();
      this.report.log(eventName + " event finished.");

      this.firePlugin(PluginEvent.AFTEREVENT);

      // switch (type) {
      // case MAP:
      //    result = mapEvent(event);
      //    break;
      // case AREA:
      //    result = areaEvent(event);
      //    break;
      // case IMAGE:
      //    element = imageEvent(event, parent);
      //    break;

      // case FRAME:
      //    result = frameEvent(event);
      //    break;

      // case FORM:
      //    element = formEvent(event, parent);
      //    break;
      // case INPUT:
      //    element = inputEvent(event, parent);
      //    break;
      // case TEXTFIELD:
      //    element = textfieldEvent(event, parent);
      //    break;
      // case PASSWORD:
      //    element = passwordEvent(event, parent);
      //    break;
      // case BUTTON:
      //    element = buttonEvent(event, parent);
      //    break;
      // case CHECKBOX:
      //    element = checkboxEvent(event, parent);
      //    break;
      // case SELECT:
      //    element = selectEvent(event, parent);
      //    break;
      // case HIDDEN:
      //    result = hiddenEvent(event, parent);
      //    break;
      // case FILEFIELD:
      //    element = filefieldEvent(event, parent);
      //    break;
      // case TEXTAREA:
      //    element = textareaEvent(event, parent);
      //    break;
      // case RADIO:
      //    element = radioEvent(event, parent);
      //    break;
   }


   /**
    * Execute a plugin if the current event matches.
    *
    * @param type  the current plugin event
    */

   public void firePlugin(PluginEvent type) {
      this.firePlugin(null, type);
   }


   /**
    * Execute a plugin if the current event matches.
    *
    * @param event  the current executing event
    * @param type  the current plugin event
    */

   public void firePlugin(Event event, PluginEvent type) {
      PluginData data;
      Elements element = null;

      if (this.plugins.size() == 0) {
         return;
      }

      if (!this.windowExists(this.currentHWnd)) {
         this.report.log("Browser window closed. Skipping plugin execution.");
         return;
      }

      if (event != null) {
         element = Elements.valueOf(event.getName().toUpperCase());
      }

      data = new PluginData();
      if (event != null) {
         data.setElement(event.getElement());
      }
      data.setBrowser(this.Browser);
      data.setVars(this.vars);
      data.setHijacks(this.hijacks);
      data.setTestName(this.testName);

      for (Plugin plugin: this.plugins) {
         if ((event != null && plugin.matches(element, type)) ||
             (event == null && plugin.matches(type))) {
            plugin.execute(data, this.report);
         }
      }
   }


   ////////////////////////////////////////////////////////////
   // delete me

   private void firePlugin(WebElement we, Elements el, PluginEvent pe) {
      // implemented above //
   }

   private void logElementNotVisible(boolean required, VDDHash event) {
      // implemented above //
   }

   private String replaceString(String str) {
      // Moved to Event.replaceString
      return "";
   }

   private WebElement findElement(VDDHash event, WebElement parent,
                                  boolean required) {
      // Moved to ElementFinder.java
      return null;
   }

   private boolean clickToBool(String clickstr) {
      // Moved to Event.toBoolean
      return false;
   }

   private void checkDisabled(VDDHash event, WebElement element) {
      // Moved to HtmlEvent.DisabledAction
   }

   private void handleVars(String value, VDDHash event) {
      // Moved to HtmlEvent.VarAction
   }

   // stop deleting me
   ////////////////////////////////////////////////////////////


   // All below will be moved into events...

   private boolean frameEvent(VDDHash event) {
      boolean result = false;
      int index = -1;
      String frameid = null;

      this.report.log("Frame event starting.");

      if (event.containsKey("index")) {
         String tmp = event.get("index").toString();
         tmp = this.replaceString(tmp);
         index = Integer.valueOf(tmp);
      }

      if (event.containsKey("id")) {
         frameid = event.get("id").toString();
         frameid = this.replaceString(frameid);
      }

      if (event.containsKey("name")) {
         frameid = event.get("name").toString();
         frameid = this.replaceString(frameid);
      }

      try {
         if (index > -1) {
            this.report.log("Switching to frame by index: '" + index + "'.");
            this.Browser.getDriver().switchTo().frame(index);
         } else {
            this.report.log("Switching to frame by name: '" + frameid + "'.");
            this.Browser.getDriver().switchTo().frame(frameid);
         }

         if (event.containsKey("children")) {
            this.processEvents((ArrayList<Event>) event.get("children"), null);
         }

         this.report.log("Switching back to default frame.");
         this.Browser.getDriver().switchTo().defaultContent();
      } catch (NoSuchFrameException exp) {
         this.report.error("Failed to find frame!");
      } catch (Exception exp) {
         this.report.exception(exp);
      }
      this.report.log("Frame event finished.");

      return result;
   }

   private boolean areaEvent(VDDHash event) {
      boolean required = true;
      boolean click = false;
      boolean result = false;
      WebElement element = null;

      this.report.log("Area event Started.");
      this.updateThreadTime();

      if (event.containsKey("required")) {
         required = this.clickToBool(event.get("required").toString());
      }

      try {
         element = this.findElement(event, null, required);
         if (element == null) {
            this.report.log("Area event finished.");
            result = false;
            return result;
         }

         this.checkDisabled(event, element);

         if (event.containsKey("click")) {
            click = this.clickToBool(event.get("click").toString());
         }

         if (click) {
            this.report.log("Area click started.");
            this.firePlugin(element, Elements.AREA,
                  PluginEvent.BEFORECLICK);
            element.click();
            this.firePlugin(element, Elements.AREA,
                  PluginEvent.AFTERCLICK);
            this.report.log("Area click finished.");
         }
      } catch (ElementNotVisibleException exp) {
         logElementNotVisible(required, event);
      } catch (Exception exp) {
         element = null;
         this.report.exception(exp);
      }

      this.report.log("Area event Finished.");
      return result;
   }

   private boolean mapEvent(VDDHash event) {
      boolean required = true;
      boolean click = false;
      boolean result = false;
      WebElement element = null;

      this.report.log("Map event Started.");
      this.updateThreadTime();

      if (event.containsKey("required")) {
         required = this.clickToBool(event.get("required").toString());
      }

      try {
         element = this.findElement(event, null, required);
         if (element == null) {
            this.report.log("Map event finished.");
            result = false;
            return result;
         }

         if (event.containsKey("click")) {
            click = this.clickToBool(event.get("click").toString());
         }

         this.checkDisabled(event, element);

         if (click) {
            this.report.log("Map click started.");
            this.firePlugin(element, Elements.MAP,
                  PluginEvent.BEFORECLICK);
            element.click();
            this.firePlugin(element, Elements.MAP,
                  PluginEvent.AFTERCLICK);
            this.report.log("Map click finished.");
         }

         if (event.containsKey("children")) {
            this.processEvents((ArrayList<Event>) event.get("children"), element);
         }
      } catch (ElementNotVisibleException exp) {
         logElementNotVisible(required, event);
      } catch (Exception exp) {
         element = null;
         this.report.exception(exp);
      }

      this.report.log("Map event Finished.");

      return result;
   }

   /**
    * Handle an &lt;image&gt; event.
    *
    * @param event  the &image;th&gt; event
    * @param parent this element's parent
    * @return the a {@link WebElement} or null
    */

   private WebElement imageEvent(VDDHash event, WebElement parent) {
      boolean required = true;
      boolean click = false;
      WebElement element = null;

      this.report.log("Image event Started.");
      this.updateThreadTime();

      if (event.containsKey("required")) {
         required = this.clickToBool(event.get("required").toString());
      }

      try {
         element = this.findElement(event, parent, required);
         if (element == null) {
            this.report.log("Image event finished.");
            return element;
         }

         if (event.containsKey("click")) {
            click = this.clickToBool(event.get("click").toString());
         }

         this.checkDisabled(event, element);

         handleVars(element.getAttribute("src"), event);

         if (event.containsKey("jscriptevent")) {
            this.report.log("Firing Javascript Event: " +
                            event.get("jscriptevent").toString());
            this.Browser.fire_event(element,
                                    event.get("jscriptevent").toString());
            Thread.sleep(1000);
            this.report.log("Javascript event finished.");
         }

         if (click) {
            this.report.log("Image click started.");
            this.firePlugin(element, Elements.IMAGE,
                  PluginEvent.BEFORECLICK);
            element.click();
            this.firePlugin(element, Elements.IMAGE,
                  PluginEvent.AFTERCLICK);
            this.report.log("Image click finished.");
         }
      } catch (ElementNotVisibleException exp) {
         logElementNotVisible(required, event);
      } catch (Exception exp) {
         element = null;
         this.report.exception(exp);
      }

      this.report.log("Image event Finished.");
      return element;
   }

   private WebElement filefieldEvent(VDDHash event, WebElement parent) {
      boolean required = true;
      WebElement element = null;

      this.report.log("FileField event Started.");
      this.updateThreadTime();

      if (event.containsKey("required")) {
         required = this.clickToBool(event.get("required").toString());
      }

      try {
         element = this.findElement(event, parent, required);
         if (element == null) {
            this.report.log("FileField event finished.");
            return element;
         }

         this.firePlugin(element, Elements.FILEFIELD,
               PluginEvent.AFTERFOUND);

         this.checkDisabled(event, element);

         if (event.containsKey("set")) {
            String setvalue = event.get("set").toString();
            setvalue = this.replaceString(setvalue);
            setvalue = FilenameUtils.separatorsToSystem(setvalue);
            setvalue = (new File(setvalue)).getAbsolutePath();
            this.report.log(String.format("Setting filefield to: '%s'.",
                                          setvalue));
            element.sendKeys(setvalue);
            this.report.log("Finished set.");
         }

         String value = element.getAttribute("value");
         handleVars(value, event);

      } catch (ElementNotVisibleException exp) {
         logElementNotVisible(required, event);
      } catch (Exception exp) {
         element = null;
         this.report.exception(exp);
      }

      this.report.log("FileField event finished..");
      this.updateThreadTime();
      return element;
   }

   private boolean hiddenEvent(VDDHash event, WebElement parent) {
      boolean result = false;
      boolean required = true;
      WebElement element = null;

      this.report.log("Hidden event Started.");

      if (event.containsKey("required")) {
         required = this.clickToBool(event.get("required").toString());
      }

      try {
         element = this.findElement(event, parent, required);
         if (element == null) {
            result = false;
            this.report.log("Hidden event finished.");
            return result;
         }

         String value = element.getAttribute("value");
         handleVars(value, event);
         result = true;
      } catch (Exception exp) {
         this.report.exception(exp);
         result = false;
      }

      this.report.log("Hidden event finished.");
      return result;
   }

   private WebElement inputEvent(VDDHash event, WebElement parent) {
      boolean required = true;
      WebElement element = null;

      this.report.log("Input event starting.");

      if (event.containsKey("required")) {
         required = this.clickToBool(event.get("required").toString());
      }

      try {
         element = this.findElement(event, parent, required);
         if (element == null) {
            this.report.log("Input event finished.");
            return element;
         }

         this.firePlugin(element, Elements.INPUT, PluginEvent.AFTERFOUND);

         this.checkDisabled(event, element);

         String value = element.getAttribute("value");
         handleVars(value, event);

         if (event.containsKey("assert")) {
            String src = element.getText();
            String val = this.replaceString(event.get("assert").toString());
            this.report.Assert(val, src);
         }

         if (event.containsKey("assertnot")) {
            String src = element.getText();
            String val = this.replaceString(event.get("assertnot").toString());
            this.report.AssertNot(val, src);
         }

         if (event.containsKey("jscriptevent")) {
            this.report.log("Firing Javascript Event: " + event.get("jscriptevent").toString());
            this.Browser.fire_event(element, event.get("jscriptevent").toString());
            Thread.sleep(1000);
            this.report.log("Javascript event finished.");
         }
      } catch (Exception exp) {
         this.report.exception(exp);
         element = null;
      }

      this.report.log("Input event finished.");
      return element;
   }

   private WebElement radioEvent(VDDHash event, WebElement parent) {
      boolean required = true;
      boolean click = false;
      WebElement element = null;

      this.report.log("Radio event starting.");

      if (event.containsKey("required")) {
         required = this.clickToBool(event.get("required").toString());
      }

      try {
         element = this.findElement(event, parent, required);
         if (element == null) {
            this.report.log("Radio event finished.");
            return element;
         }

         this.firePlugin(element, Elements.RADIO,
               PluginEvent.AFTERFOUND);

         this.checkDisabled(event, element);

         if (event.containsKey("click")) {
            click = this.clickToBool(event.get("click").toString());
         }

         if (event.containsKey("set")) {
            this.report.warning("Using the 'set' command for a radio element is not supported anymore!  Use click!");
            click = this.clickToBool(event.get("set").toString());
         }

         String value = element.getAttribute("value");
         handleVars(value, event);

         if (click) {
            this.firePlugin(element, Elements.RADIO,
                  PluginEvent.BEFORECLICK);
            this.report.log("Clicking Element.");
            element.click();
            this.report.log("Click finished.");
            this.firePlugin(element, Elements.RADIO,
                  PluginEvent.AFTERCLICK);
         }

         if (event.containsKey("assert")) {
            String src = element.getText();
            String val = this.replaceString(event.get("assert").toString());
            this.report.Assert(val, src);
         }

         if (event.containsKey("assertnot")) {
            String src = element.getText();
            String val = this.replaceString(event.get("assertnot").toString());
            this.report.AssertNot(val, src);
         }

         if (event.containsKey("jscriptevent")) {
            this.report.log("Firing Javascript Event: "
                  + event.get("jscriptevent").toString());
            this.Browser.fire_event(element, event.get("jscriptevent").toString());
            Thread.sleep(1000);
            this.report.log("Javascript event finished.");
         }

         if (event.containsKey("checked")) {
            boolean ischecked = element.isSelected();
            boolean expected = this
                  .clickToBool(event.get("checked").toString());
            String msg = "";
            msg = String.format("Radio control's current checked state: '%s', was expecting: '%s'!",
                        ischecked, expected);
            this.report.Assert(msg, ischecked, expected);

         }
      } catch (ElementNotVisibleException exp) {
         logElementNotVisible(required, event);
      } catch (Exception exp) {
         this.report.exception(exp);
         element = null;
      }

      this.report.log("Radio event finished.");
      return element;
   }


   /**
    * Handle a &lt;select&gt; event
    *
    * @param event  the &lt;select&gt; event
    * @param parent this element's parent
    * @return the a {@link WebElement} or null
    */

   private WebElement selectEvent(VDDHash event, WebElement parent) {
      boolean required = true;
      WebElement element = null;

      this.report.log("Select event Started.");

      if (event.containsKey("required")) {
         required = this.clickToBool(event.get("required").toString());
      }

      try {
         Select sel = null;
         element = this.findElement(event, parent, required);
         if (element == null) {
            this.report.log("Select event finished.");
            return null;
         }

         sel = new Select(element);
         this.firePlugin(element, Elements.SELECT, PluginEvent.AFTERFOUND);

         this.checkDisabled(event, element);
         handleVars(element.getAttribute("value"), event);

         if (event.containsKey("assert") ||
             event.containsKey("assertnot")) {
            boolean wantSel = event.containsKey("assert");
            String val = this.replaceString(event.get(wantSel ? "assert" :
                                                      "assertnot").toString());
            boolean optionFound = false;

            for (WebElement opt: sel.getOptions()) {
               if (opt.getText().equals(val)) {
                  boolean issel = opt.isSelected();
                  this.report.Assert("Select option '" + val + "' is " +
                                     (issel ? "" : "not ") + "selected",
                                     wantSel ^ issel,
                                     false);
                  optionFound = true;
                  break;
               }
            }

            if (!optionFound) {
               this.report.error("Failed to find select option '" + val + "'!");
            }
         }

         if (event.containsKey("assertselected")) {
            boolean anySelected = sel.getAllSelectedOptions().size() > 0;
            boolean shouldBeSelected =
               clickToBool(event.get("assertselected").toString());

            report.Assert("Option " + (anySelected ? "" : "not ") + "selected",
                          anySelected, shouldBeSelected);
         }

         if (event.containsKey("included") ||
             event.containsKey("notincluded")) {
            boolean wantOpt = event.containsKey("included");
            String val = this.replaceString(event.get(!wantOpt ? "notincluded" :
                                                      "included").toString());
            boolean haveOpt = false;

            for (WebElement opt: sel.getOptions()) {
               if (opt.getText().equals(val)) {
                  haveOpt = true;
                  break;
               }
            }

            String m = String.format("Select option %s%s found and%s expected",
                                     val, haveOpt ? "" : " not",
                                     wantOpt ? "" : " not");
            this.report.Assert(m, wantOpt ^ haveOpt, false);
         }

         if (event.containsKey("clear") &&
             this.clickToBool(event.get("clear").toString()) &&
             sel.isMultiple()) {
            this.report.log("Clearing select element.");
            sel.deselectAll();
         }

         try {
            if (event.containsKey("set") ||
                event.containsKey("setreal")) {
               boolean useVal = event.containsKey("setreal");
               String val = this.replaceString(event.get(useVal ? "setreal" :
                                                         "set").toString());
               this.report.log("Setting option by " +
                               (useVal ? "value" : "visible text") +
                               ": '" + val + "'.");
               try {
                  if (useVal) {
                     sel.selectByValue(val);
                  } else {
                     sel.selectByVisibleText(val);
                  }
                  this.firePlugin(element, Elements.SELECT,
                                  PluginEvent.AFTERSET);
               } catch (NoSuchElementException e) {
                  this.report.error("Option with " +
                                    (useVal ? "value" : "visible text") +
                                    " '" + val + "' does not exist");
               }
            }

            if (event.containsKey("jscriptevent")) {
               this.report.log("Firing Javascript Event: " +
                               event.get("jscriptevent").toString());
               this.Browser.fire_event(element,
                                       event.get("jscriptevent").toString());
               try {
                  Thread.sleep(1000);
               } catch (InterruptedException e) {
               }
               this.report.log("Javascript event finished.");
            }

            if (event.containsKey("click") &&
                this.clickToBool(event.get("click").toString())) {
               this.firePlugin(element, Elements.SELECT,
                               PluginEvent.BEFORECLICK);
               element.click();
               this.firePlugin(element, Elements.SELECT,
                               PluginEvent.AFTERCLICK);
            }

            if (element.isDisplayed() && event.containsKey("children")) {
               this.processEvents((ArrayList<Event>)event.get("children"), element);
            }
         } catch (StaleElementReferenceException e) {
            /*
             * Selecting a value has the potential to refresh the page
             * (Bug 49533).
             */
            this.report.log("Page refreshed; select element no longer exists.");
            element = null;
         }

      } catch (ElementNotVisibleException exp) {
         logElementNotVisible(required, event);
      } catch (Exception exp) {
         this.report.exception(exp);
      }

      this.report.log("Select event finished.");
      return element;
   }


   /**
    * Handle an &lt;option&gt; event
    *
    * @param event  the &lt;option&gt; event
    * @param parent this element's parent
    * @return the a {@link WebElement} or null
    */

   private WebElement optionEvent(VDDHash event, WebElement parent) {
      boolean click = true;
      boolean required = true;
      WebElement element = null;

      this.report.log("Option event starting.");

      if (event.containsKey("required")) {
         required = this.clickToBool(event.get("required").toString());
      }

      try {
         element = this.findElement(event, parent, required);
         if (element == null) {
            this.report.log("Option event finished.");
            return element;
         }

         this.firePlugin(element, Elements.OPTION, PluginEvent.AFTERFOUND);

         this.checkDisabled(event, element);

         if (event.containsKey("assert")) {
            String val = this.replaceString(event.get("assert").toString());
            this.report.Assert(val, element.getText());
         }

         if (event.containsKey("assertnot")) {
            String val = this.replaceString(event.get("assertnot").toString());
            this.report.AssertNot(val, element.getText());
         }

         if (event.containsKey("jscriptevent")) {
            String ev = (String)event.get("jscriptevent");
            this.report.log("Firing Javascript Event: " + ev);
            this.Browser.fire_event(element, ev);
            Thread.sleep(1000);
            this.report.log("Javascript event finished.");
         }

         if (event.containsKey("click")) {
            click = this.clickToBool(event.get("click").toString());
         }

         if (click) {
            this.firePlugin(element, Elements.OPTION, PluginEvent.BEFORECLICK);
            element.click();
            this.firePlugin(element, Elements.OPTION, PluginEvent.AFTERCLICK);
         }

      } catch (ElementNotVisibleException e) {
         logElementNotVisible(required, event);
      } catch (Exception e) {
         this.report.exception(e);
      }

      this.report.log("Option event finished.");
      return element;
   }


   private WebElement formEvent(VDDHash event, WebElement parent) {
      boolean required = true;
      boolean click = false;
      WebElement element = null;

      this.report.log("Form event starting.");

      if (event.containsKey("required")) {
         required = this.clickToBool(event.get("required").toString());
      }

      try {
         element = this.findElement(event, parent, required);
         if (element == null) {
            this.report.log("Form event finished.");
            return element;
         }

         this.firePlugin(element, Elements.FORM,
               PluginEvent.AFTERFOUND);

         this.checkDisabled(event, element);

         if (event.containsKey("click")) {
            click = this.clickToBool(event.get("click").toString());
         }

         if (click) {
            this.firePlugin(element, Elements.FORM,
                  PluginEvent.BEFORECLICK);
            element.click();
            this.firePlugin(element, Elements.FORM,
                  PluginEvent.AFTERCLICK);
         }

         if (event.containsKey("children") && element != null) {
            this.processEvents((ArrayList<Event>) event.get("children"), element);
         }
      } catch (ElementNotVisibleException exp) {
         logElementNotVisible(required, event);
      } catch (Exception exp) {
         this.report.exception(exp);
      }

      this.report.log("Form event finished.");
      return element;
   }

   private WebElement checkboxEvent(VDDHash event, WebElement parent) {
      boolean click = false;
      boolean required = true;
      WebElement element = null;

      this.updateThreadTime();

      if (event.containsKey("required")) {
         required = this.clickToBool(event.get("required").toString());
      }

      try {
         element = this.findElement(event, parent, required);
         if (element == null) {
            return element;
         }

         this.firePlugin(element, Elements.CHECKBOX, PluginEvent.AFTERFOUND);

         this.checkDisabled(event, element);

         if (event.containsKey("click")) {
            click = this.clickToBool(event.get("click").toString());
            if (click) {
               this.firePlugin(element, Elements.CHECKBOX, PluginEvent.BEFORECLICK);
               element.click();
               this.firePlugin(element, Elements.CHECKBOX, PluginEvent.AFTERCLICK);
            }
         }

         if (event.containsKey("set")) {
            String msg = "";
            String set = event.get("set").toString();
            set = this.replaceString(set);

            if (Boolean.valueOf(set) == element.isSelected()) {
               msg = String.format("Checkbox current checked state is already: '%s', skipping click.", set);
            } else {
               msg = String.format("Checkbox's state is '%s', clicking to set state to '%s'.", element.isSelected(), set);
               element.click();
               this.firePlugin(element, Elements.CHECKBOX, PluginEvent.AFTERCLICK);
            }
            this.report.log(msg);
         }

         if (event.containsKey("assert")) {
            String src = String.valueOf(element.isSelected());
            String value = event.get("assert").toString();
            value = this.replaceString(value);
            this.report.Assert(value, src);
         }

         if (event.containsKey("assertnot")) {
            String src = String.valueOf(element.isSelected());
            String value = event.get("assertnot").toString();
            value = this.replaceString(value);
            this.report.AssertNot(value, src);
         }

         String value = element.getAttribute("value");
         handleVars(value, event);

      } catch (ElementNotVisibleException exp) {
         logElementNotVisible(required, event);
      } catch (Exception exp) {
         this.report.exception(exp);
      }

      this.updateThreadTime();
      return element;
   }

   private WebElement buttonEvent(VDDHash event, WebElement parent) {
      boolean click = true;
      boolean required = true;
      WebElement element = null;

      this.updateThreadTime();
      this.report.log("Starting button event.");

      if (event.containsKey("required")) {
         required = this.clickToBool(event.get("required").toString());
      }

      try {
         element = this.findElement(event, parent, required);
         if (element == null) {
            this.report.log("Finished button event.");
            return null;
         }

         String value = element.getAttribute("value");
         handleVars(value, event);

         this.firePlugin(element, Elements.BUTTON,
               PluginEvent.AFTERFOUND);

         this.checkDisabled(event, element);

         if (event.containsKey("click")) {
            click = this.clickToBool(event.get("click").toString());
         }

         if (event.containsKey("alert")) {
            boolean alert = this.clickToBool(event.get("alert").toString());
            this.report.log(String.format("Setting Alert Hack to: '%s'", alert));
            this.Browser.alertHack(alert);
            this.report.warning("You are using a deprecated alert hack, please use the <alert> command!");
         }

         if (click) {
            this.firePlugin(element, Elements.BUTTON,
                  PluginEvent.BEFORECLICK);
            this.report.log("Clicking button.");
            element.click();
            this.report.log("Finished clicking button.");
            this.firePlugin(element, Elements.BUTTON,
                  PluginEvent.AFTERCLICK);
         }

         if (event.containsKey("jscriptevent")) {
            this.report.log("Firing Javascript Event: "
                  + event.get("jscriptevent").toString());
            this.Browser.fire_event(element, event.get("jscriptevent").toString());
            Thread.sleep(1000);
            this.report.log("Javascript event finished.");
         }
      } catch (ElementNotVisibleException exp) {
         logElementNotVisible(required, event);
      } catch (Exception exp) {
         this.report.exception(exp);
         element = null;
      }

      this.updateThreadTime();
      this.report.log("Finished button event.");

      return element;
   }

   private WebElement textareaEvent(VDDHash event, WebElement parent) {
      boolean required = true;
      WebElement element = null;

      this.updateThreadTime();

      this.report.log("Starting textarea event.");

      if (event.containsKey("required")) {
         required = this.clickToBool(event.get("required").toString());
      }

      try {
         element = this.findElement(event, parent, required);
         if (element == null) {
            this.report.log("Finished textarea event.");
            return null;
         }

         String value = element.getAttribute("value");
         handleVars(value, event);

         this.firePlugin(element, Elements.TEXTAREA,
               PluginEvent.AFTERFOUND);

         this.checkDisabled(event, element);

         if (event.containsKey("set")) {
            value = event.get("set").toString();
            value = this.replaceString(value);
            this.report.log(String.format("Setting Value to: '%s'.", value));
            clearText(element);
            element.sendKeys(value);
         } else if (event.containsKey("clear")) {
            if (this.clickToBool(event.get("clear").toString())) {
               this.report.log("Clearing textarea.");
               clearText(element);
            }
         } else if (event.containsKey("append")) {
            value = event.get("append").toString();
            value = this.replaceString(value);
            this.report.log(String.format("Appending Value to: '%s'.", value));
            element.sendKeys(value);
         }

         if (event.containsKey("jscriptevent")) {
            this.report.log("Firing Javascript Event: "
                  + event.get("jscriptevent").toString());
            this.Browser.fire_event(element, event.get("jscriptevent").toString());
            Thread.sleep(1000);
            this.report.log("Javascript event finished.");
         }

         if (event.containsKey("assert")) {
            String assvalue = event.get("assert").toString();
            assvalue = this.replaceString(assvalue);
            this.report.Assert(assvalue, element.getAttribute("value"));
         }

         if (event.containsKey("assertnot")) {
            String assvalue = event.get("assertnot").toString();
            assvalue = this.replaceString(assvalue);
            this.report.AssertNot(assvalue, element.getAttribute("value"));
         }

         if (event.containsKey("click") &&
             this.clickToBool(event.get("click").toString())) {
            this.firePlugin(element, Elements.TEXTAREA,
                            PluginEvent.BEFORECLICK);
            this.report.log("Clicking textarea.");
            element.click();
            this.report.log("Finished clicking textarea.");
            this.firePlugin(element, Elements.TEXTAREA,
                            PluginEvent.AFTERCLICK);
         }

      } catch (ElementNotVisibleException exp) {
         logElementNotVisible(required, event);
      } catch (Exception exp) {
         this.report.exception(exp);
         element = null;
      }

      this.updateThreadTime();
      this.report.log("Finished textarea event.");
      return element;
   }


   /**
    * Clear a text or a password input or a textfield element.
    *
    * Using javascript on IE instead of WebElement.clear() is needed
    * due to Selenium issue 3402.
    *
    * @param e  the text element to clear
    */

   void clearText(WebElement e) {
      if (this.Browser instanceof IE) {
         this.Browser.executeJS("arguments[0].value = '';", e);
      } else {
         e.clear();
      }
   }


   /**
    * &lt;textfield&gt; event
    *
    * @param event   the &lt;textfield&gt; event
    * @param parent  this element's parent
    * @return the &lt;textfield&gt; {@link WebElement} or null
    */

   private WebElement textfieldEvent(VDDHash event, WebElement parent) {
      boolean required = true;
      WebElement element = null;

      this.updateThreadTime();
      this.report.log("Starting textfield event.");

      if (event.containsKey("required")) {
         required = this.clickToBool(event.get("required").toString());
      }

      try {
         element = this.findElement(event, parent, required);
         if (element == null) {
            this.report.log("Finished textfield event.");
            return null;
         }

         this.firePlugin(element, Elements.TEXTFIELD,
               PluginEvent.AFTERFOUND);

         this.checkDisabled(event, element);

         if (event.containsKey("clear")) {
            if (this.clickToBool(event.get("clear").toString())) {
               this.report.log("Clearing textfield.");
               clearText(element);
            }
         }

         if (event.containsKey("set")) {
            String value = event.get("set").toString();
            value = this.replaceString(value);
            this.report.log(String.format("Setting Value to: '%s'.", value));
            clearText(element);
            element.sendKeys(value);
         }

         if (event.containsKey("append")) {
            String value = event.get("append").toString();
            value = this.replaceString(value);
            element.sendKeys(value);
         }

         if (event.containsKey("jscriptevent")) {
            this.report.log("Firing Javascript Event: "
                  + event.get("jscriptevent").toString());
            this.Browser.fire_event(element, event.get("jscriptevent").toString());
            Thread.sleep(1000);
            this.report.log("Javascript event finished.");
         }

         if (event.containsKey("assert")) {
            String assvalue = event.get("assert").toString();
            assvalue = this.replaceString(assvalue);
            this.report.Assert(assvalue, element.getAttribute("value"));
         }

         if (event.containsKey("assertnot")) {
            String assvalue = event.get("assertnot").toString();
            assvalue = this.replaceString(assvalue);
            this.report.AssertNot(assvalue, element.getAttribute("value"));
         }

         String value = element.getAttribute("value");
         handleVars(value, event);
      } catch (ElementNotVisibleException exp) {
         logElementNotVisible(required, event);
      } catch (Exception exp) {
         this.report.exception(exp);
         element = null;
      }

      this.updateThreadTime();
      this.report.log("Finished textfield event.");
      return element;
   }

   /**
    * Handle a &lt;password&gt; event
    *
    * @param event  the &lt;password&gt; event
    * @param parent this element's parent
    * @return the &lt;input type=&quot;password&quot; {@link WebElement} or null
    */

   private WebElement passwordEvent(VDDHash event, WebElement parent) {
      boolean required = true;
      WebElement element = null;

      this.updateThreadTime();
      this.report.log("Starting password event.");

      if (event.containsKey("required")) {
         required = this.clickToBool(event.get("required").toString());
      }

      try {
         element = this.findElement(event, parent, required);
         if (element == null) {
            this.report.log("Finished password event.");
            return null;
         }

         this.firePlugin(element, Elements.PASSWORD, PluginEvent.AFTERFOUND);

         this.checkDisabled(event, element);

         if (event.containsKey("clear")) {
            if (this.clickToBool(event.get("clear").toString())) {
               this.report.log("Clearing password field.");
               clearText(element);
            }
         }

         if (event.containsKey("set")) {
            String value = event.get("set").toString();
            value = this.replaceString(value);
            this.report.log(String.format("Setting Value to: '%s'.", value));
            clearText(element);
            element.sendKeys(value);
         }

         if (event.containsKey("append")) {
            String value = event.get("append").toString();
            value = this.replaceString(value);
            element.sendKeys(value);
         }

         if (event.containsKey("jscriptevent")) {
            this.report.log("Firing Javascript Event: " + event.get("jscriptevent").toString());
            this.Browser.fire_event(element, event.get("jscriptevent").toString());
            Thread.sleep(1000);
            this.report.log("Javascript event finished.");
         }

         if (event.containsKey("assert")) {
            String assvalue = event.get("assert").toString();
            assvalue = this.replaceString(assvalue);
            this.report.Assert(assvalue, element.getAttribute("value"));
         }

         if (event.containsKey("assertnot")) {
            String assvalue = event.get("assertnot").toString();
            assvalue = this.replaceString(assvalue);
            this.report.AssertNot(assvalue, element.getAttribute("value"));
         }

         String value = element.getAttribute("value");
         handleVars(value, event);
      } catch (ElementNotVisibleException exp) {
         logElementNotVisible(required, event);
      } catch (Exception exp) {
         this.report.exception(exp);
         element = null;
      }

      this.updateThreadTime();
      this.report.log("Finished password event.");
      return element;
   }
}
