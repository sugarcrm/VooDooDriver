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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.io.FilenameUtils;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.ElementNotVisibleException;
import org.openqa.selenium.NoAlertPresentException;
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
 *
 */

@SuppressWarnings("unchecked")  //XXX
public class EventLoop implements Runnable {

   private ArrayList<Event> testEvents;
   public Browser Browser = null;
   public Reporter report = null;
   public VDDHash hijacks = null;

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

   public File csvOverrideFile = null;

   private String currentHWnd = null;
   private int attachTimeout = 0;
   private Date threadTime = null;
   private volatile Thread runner;
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
    * @param timeout int
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
    * Get the current browser window handle.
    *
    * @return {@link String}
    */

   private String getCurrentHWND() {
      return this.currentHWnd;
   }


   private void saveElement(VDDHash event, WebElement element) {
      /* Moved to HtmlEvent.SaveAction */
   }


   public boolean isAlive() {
      return this.runner.isAlive();
   }

   public void stop() {
      synchronized (this.threadStop) {
         this.threadStop = true;
         this.runner.interrupt();
      }
   }

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

   public void resetThreadTime() {
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
    * Run a sequence of events.
    *
    * @param events  {@link ArrayList} of {@link Event} from the test script
    * @param parent  parent element if process child events
    */

   private void processEvents(ArrayList<Event> events, WebElement parent) {
      for (Event event: events) {
         if (isStopped()) {
            break;
         }

         handleSingleEvent(event, parent);
      }
   }


   /**
    * Thread entry point.
    *
    * Initialize thread state and run the events of this test file.
    */

   public void run() {
      System.out.printf("Thread Running...\n");
      this.threadTime = new Date();

      this.firePlugin(null, PluginEvent.BEFORETEST);

      processEvents(this.testEvents, null);

      this.firePlugin(null, PluginEvent.AFTERTEST);
   }


   /**
    * Execute one {@link Event}.
    *
    * @param event   the {@link Event} to execute
    * @param parent  parent element, if currently executing children
    * @return whether the event was successful
    */

   private boolean handleSingleEvent(Event event, WebElement parent) {
      boolean result = true;
      String eventName = event.getName();

      this.report.Log(eventName + " event started...");
      this.resetThreadTime();
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
      } catch (VDDException e) {
         Throwable cause = e.getCause();

         if (cause == null) {
            cause = e;
         }

         this.report.ReportError("Exception during event execution");
         this.report.ReportException(cause);

         result = false;
      }

      this.resetThreadTime();
      this.report.Log(eventName + " event finished.");

      return result;





      //private boolean handleSingleEvent(VDDHash event, WebElement parent) {
      //Elements type = Elements.valueOf(event.get("type").toString());


      // switch (type) {
      // case EXECUTE:
      //    result = executeEvent(event);
      //    break;
      // case DND:
      //    result = dndEvent(event);
      //    break;

      // case THEAD:
      //    element = theadEvent(event, parent);
      //    break;
      // case TBODY:
      //    element = tbodyEvent(event, parent);
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
      // case SCRIPT:
      //    result = scriptEvent(event);
      //    break;
      // case DIV:
      //    element = divEvent(event, parent);
      //    break;
      // case TABLE:
      //    element = tableEvent(event, parent);
      //    break;
      // case FORM:
      //    element = formEvent(event, parent);
      //    break;
      // case SELECT:
      //    element = selectEvent(event, parent);
      //    break;
      // case SPAN:
      //    element = spanEvent(event, parent);
      //    break;
      // case HIDDEN:
      //    result = hiddenEvent(event, parent);
      //    break;
      // case TR:
      //    element = trEvent(event, parent);
      //    break;
      // case TD:
      //    element = tdEvent(event, parent);
      //    break;
      // case FILEFIELD:
      //    element = filefieldEvent(event, parent);
      //    break;
      // case IMAGE:
      //    element = imageEvent(event, parent);
      //    break;
      // case TEXTAREA:
      //    element = textareaEvent(event, parent);
      //    break;
      // case LI:
      //    element = liEvent(event, parent);
      //    break;
      // case RADIO:
      //    element = radioEvent(event, parent);
      //    break;
      // case UL:
      //    result = ulEvent(event);
      //    break;
      // case OL:
      //    result = olEvent(event);
      //    break;
      // case MAP:
      //    result = mapEvent(event);
      //    break;
      // case AREA:
      //    result = areaEvent(event);
      //    break;
      // case ALERT:
      //    result = alertEvent(event);
      //    break;
      // case FRAME:
      //    result = frameEvent(event);
      //    break;
      // case INPUT:
      //    element = inputEvent(event, parent);
      //    break;
      // default:
      //    System.out.printf("(!)Unknown command: '%s'!\n", type.toString());
      //    System.exit(1);
      // }

      // this.firePlugin(null, type, PluginEvent.AFTEREVENT);
   }


   /**
    * Log when an {@link ElementNotVisibleException} is caught
    *
    * If this exception is caught for a required element, the
    * message is reported as an error.  Otherwise it is logged
    * normally.
    *
    * @param required  whether the element was required
    */

   private void logElementNotVisible(boolean required, VDDHash event) {
      String how = event.get("how").toString();
      String what = this.replaceString(event.get(how).toString());
      String msg = ("The element you are trying to access (" +
                    how + " = " + what + ") is not visible");
      if (required) {
         this.report.ReportError("Error: " + msg + "!");
      } else {
         this.report.Log(msg + ", but required = false.");
      }
   }

   private boolean frameEvent(VDDHash event) {
      boolean result = false;
      int index = -1;
      String frameid = null;

      this.report.Log("Frame event starting.");

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
            this.report.Log("Switching to frame by index: '" + index + "'.");
            this.Browser.getDriver().switchTo().frame(index);
         } else {
            this.report.Log("Switching to frame by name: '" + frameid + "'.");
            this.Browser.getDriver().switchTo().frame(frameid);
         }

         if (event.containsKey("children")) {
            this.processEvents((ArrayList<Event>) event.get("children"), null);
         }

         this.report.Log("Switching back to default frame.");
         this.Browser.getDriver().switchTo().defaultContent();
      } catch (NoSuchFrameException exp) {
         this.report.ReportError("Failed to find frame!");
      } catch (Exception exp) {
         this.report.ReportException(exp);
      }
      this.report.Log("Frame event finished.");

      return result;
   }

   private boolean alertEvent(VDDHash event) {
      boolean result = false;
      boolean alert_var = false;
      boolean exists = true;
      boolean user_exists_true = false;
      boolean required = true;
      String msg = "";
      String alert_text = "";

      this.report.Log("Alert event starting.");

      if (!event.containsKey("alert") && !event.containsKey("exists")) {
         result = false;
         this.report.ReportError("Alert event missing alert attribute!");
         return result;
      }

      if (event.containsKey("alert")) {
         String tmp = event.get("alert").toString();
         tmp = this.replaceString(tmp);
         alert_var = this.clickToBool(tmp);
      }

      if (event.containsKey("exists")) {
         String tmp = event.get("exists").toString();
         tmp = this.replaceString(tmp);
         exists = this.clickToBool(tmp);

         if (exists) {
            user_exists_true = true;
         }
      }

      if (event.containsKey("required")) {
         String s = (String)event.get("required");
         required = this.clickToBool(this.replaceString(s));
      }

      try {
         Alert alert = this.Browser.getDriver().switchTo().alert();
         alert_text = alert.getText();
         msg = String.format("Found Alert dialog: '%s'.", alert_text);
         this.report.Log(msg);

         handleVars(alert_text, event);

         if (event.containsKey("assert")) {
            String ass = event.get("assert").toString();
            ass = this.replaceString(ass);
            this.report.Assert(ass, alert_text);
         }

         if (event.containsKey("assertnot")) {
            String ass = event.get("assertnot").toString();
            ass = this.replaceString(ass);
            this.report.AssertNot(ass, alert_text);
         }

         if (alert_var) {
            this.report.Log("Alert is being Accepted.");
            alert.accept();
         } else {
            this.report.Log("Alert is being Dismissed.");
            alert.dismiss();
         }

         try {
            this.Browser.getDriver().switchTo().defaultContent();
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
            this.report.Log("Unable to switch back to window. Is it closed?");
         }
         Thread.currentThread();
         Thread.sleep(1000);

         if (user_exists_true) {
            this.report.Assert("Alert dialog does exist.", true, true);
         }

         this.firePlugin(null, Elements.ALERT,
               PluginEvent.AFTERDIALOGCLOSED);

         result = true;
      } catch (NoAlertPresentException exp) {
         if (!exists) {
            msg = String.format("Expected alert dialog does not exist.",
                  exists);
            this.report.Assert(msg, false, false);
            result = true;
         } else if (required) {
            this.report.ReportError("Error: No alert dialog found!");
            result = false;
         }
      } catch (Exception exp) {
         this.report.ReportException(exp);
         result = false;
      }

      this.report.Log("Alert event finished.");

      return result;
   }

   private boolean ulEvent(VDDHash event) {
      boolean required = true;
      boolean click = false;
      boolean result = false;
      WebElement element = null;

      this.report.Log("UL event Started.");
      this.resetThreadTime();

      if (event.containsKey("required")) {
         required = this.clickToBool(event.get("required").toString());
      }

      try {
         element = this.findElement(event, null, required);
         if (element == null) {
            this.report.Log("UL event finished.");
            result = false;
            return result;
         }

         this.checkDisabled(event, element);

         if (event.containsKey("jscriptevent")) {
            this.report.Log("Firing Javascript Event: "
                            + event.get("jscriptevent").toString());
            this.Browser.fire_event(element,
                                    event.get("jscriptevent").toString());
            Thread.sleep(1000);
            this.report.Log("Javascript event finished.");
         }

         if (event.containsKey("click")) {
            click = this.clickToBool(event.get("click").toString());
         }

         if (click) {
            this.report.Log("UL click started.");
            this.firePlugin(element, Elements.UL,
                  PluginEvent.BEFORECLICK);
            element.click();
            this.firePlugin(element, Elements.UL,
                  PluginEvent.AFTERCLICK);
            this.report.Log("UL click finished.");
         }

         if (event.containsKey("children")) {
            this.processEvents((ArrayList<Event>) event.get("children"), element);
         }
      } catch (ElementNotVisibleException exp) {
         logElementNotVisible(required, event);
      } catch (Exception exp) {
         element = null;
         this.report.ReportException(exp);
      }

      this.report.Log("UL event Finished.");

      return result;
   }

   private boolean areaEvent(VDDHash event) {
      boolean required = true;
      boolean click = false;
      boolean result = false;
      WebElement element = null;

      this.report.Log("Area event Started.");
      this.resetThreadTime();

      if (event.containsKey("required")) {
         required = this.clickToBool(event.get("required").toString());
      }

      try {
         element = this.findElement(event, null, required);
         if (element == null) {
            this.report.Log("Area event finished.");
            result = false;
            return result;
         }

         this.checkDisabled(event, element);

         if (event.containsKey("click")) {
            click = this.clickToBool(event.get("click").toString());
         }

         if (click) {
            this.report.Log("Area click started.");
            this.firePlugin(element, Elements.AREA,
                  PluginEvent.BEFORECLICK);
            element.click();
            this.firePlugin(element, Elements.AREA,
                  PluginEvent.AFTERCLICK);
            this.report.Log("Area click finished.");
         }
      } catch (ElementNotVisibleException exp) {
         logElementNotVisible(required, event);
      } catch (Exception exp) {
         element = null;
         this.report.ReportException(exp);
      }

      this.report.Log("Area event Finished.");
      return result;
   }

   private boolean mapEvent(VDDHash event) {
      boolean required = true;
      boolean click = false;
      boolean result = false;
      WebElement element = null;

      this.report.Log("Map event Started.");
      this.resetThreadTime();

      if (event.containsKey("required")) {
         required = this.clickToBool(event.get("required").toString());
      }

      try {
         element = this.findElement(event, null, required);
         if (element == null) {
            this.report.Log("Map event finished.");
            result = false;
            return result;
         }

         if (event.containsKey("click")) {
            click = this.clickToBool(event.get("click").toString());
         }

         this.checkDisabled(event, element);

         if (click) {
            this.report.Log("Map click started.");
            this.firePlugin(element, Elements.MAP,
                  PluginEvent.BEFORECLICK);
            element.click();
            this.firePlugin(element, Elements.MAP,
                  PluginEvent.AFTERCLICK);
            this.report.Log("Map click finished.");
         }

         if (event.containsKey("children")) {
            this.processEvents((ArrayList<Event>) event.get("children"), element);
         }
      } catch (ElementNotVisibleException exp) {
         logElementNotVisible(required, event);
      } catch (Exception exp) {
         element = null;
         this.report.ReportException(exp);
      }

      this.report.Log("Map event Finished.");

      return result;
   }

   private boolean olEvent(VDDHash event) {
      boolean required = true;
      boolean click = false;
      boolean result = false;
      WebElement element = null;

      this.report.Log("OL event Started.");
      this.resetThreadTime();

      if (event.containsKey("required")) {
         required = this.clickToBool(event.get("required").toString());
      }

      try {
         element = this.findElement(event, null, required);
         if (element == null) {
            this.report.Log("OL event finished.");
            result = false;
            return result;
         }

         if (event.containsKey("click")) {
            click = this.clickToBool(event.get("click").toString());
         }

         this.checkDisabled(event, element);

         if (click) {
            this.report.Log("OL click started.");
            this.firePlugin(element, Elements.OL,
                  PluginEvent.BEFORECLICK);
            element.click();
            this.firePlugin(element, Elements.OL,
                  PluginEvent.AFTERCLICK);
            this.report.Log("OL click finished.");
         }
      } catch (ElementNotVisibleException exp) {
         logElementNotVisible(required, event);
      } catch (Exception exp) {
         element = null;
         this.report.ReportException(exp);
      }

      this.report.Log("OL event Finished.");

      return result;
   }

   private boolean executeEvent(VDDHash event) {
      boolean result = false;
      Process proc = null;
      int proc_ret = 0;

      this.report.Log("Execute event starting...\n");
      this.resetThreadTime();

      if (event.containsKey("args")) {
         String[] list = (String[]) event.get("args");
         int len = list.length - 1;

         for (int i = 0; i <= len; i++) {
            System.out.printf("(%s) => '%s'\n", i, list[i]);
         }

         try {
            this.report.Log("Executing process now...");
            proc = Runtime.getRuntime().exec(list);
            this.resetThreadTime();
            proc.waitFor();
            this.resetThreadTime();
            this.report.Log("Process finished executing.");
            proc_ret = proc.exitValue();
            if (proc_ret != 0) {
               String msg = String.format(
                     "Error the command being executed returned a non-zero value: '%s'!",
                     proc_ret);
               this.report.ReportError(msg);
            } else {
               this.report.Log("Execute was successful.");
               result = true;
            }
         } catch (Exception exp) {
            this.report.ReportException(exp);
            result = false;
         }
      } else {
         this.report.ReportError("Error no args for Execute Event!");
         result = false;
         this.report.Log("Execute event finished.");
         return result;
      }

      return result;
   }

   private boolean dndEvent(VDDHash event) {
      boolean result = true;
      String src = null;
      String dst = null;

      if (event.containsKey("src")) {
         src = event.get("src").toString();
      }

      if (event.containsKey("dst")) {
         dst = event.get("dst").toString();
      }

      if (src == null) {
         this.report.ReportError("DnD command is missing 'src' attribute!");
         result = false;
      }

      if (dst == null) {
         this.report.ReportError("DnD command is missing 'dst' attribute!");
         result = false;
      }

      if (result) {
         WebElement Esrc = this.elementStore.get(src);
         WebElement Edst = this.elementStore.get(dst);
         VDDMouse mouse = new VDDMouse(this.report);
         mouse.DnD(Esrc, Edst);
      }

      this.report.Log("DND event finished.");

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

      this.report.Log("Image event Started.");
      this.resetThreadTime();

      if (event.containsKey("required")) {
         required = this.clickToBool(event.get("required").toString());
      }

      try {
         element = this.findElement(event, parent, required);
         if (element == null) {
            this.report.Log("Image event finished.");
            return element;
         }

         if (event.containsKey("click")) {
            click = this.clickToBool(event.get("click").toString());
         }

         this.checkDisabled(event, element);

         handleVars(element.getAttribute("src"), event);

         if (event.containsKey("jscriptevent")) {
            this.report.Log("Firing Javascript Event: " +
                            event.get("jscriptevent").toString());
            this.Browser.fire_event(element,
                                    event.get("jscriptevent").toString());
            Thread.sleep(1000);
            this.report.Log("Javascript event finished.");
         }

         if (click) {
            this.report.Log("Image click started.");
            this.firePlugin(element, Elements.IMAGE,
                  PluginEvent.BEFORECLICK);
            element.click();
            this.firePlugin(element, Elements.IMAGE,
                  PluginEvent.AFTERCLICK);
            this.report.Log("Image click finished.");
         }
      } catch (ElementNotVisibleException exp) {
         logElementNotVisible(required, event);
      } catch (Exception exp) {
         element = null;
         this.report.ReportException(exp);
      }

      this.report.Log("Image event Finished.");
      return element;
   }

   private WebElement filefieldEvent(VDDHash event, WebElement parent) {
      boolean required = true;
      WebElement element = null;

      this.report.Log("FileField event Started.");
      this.resetThreadTime();

      if (event.containsKey("required")) {
         required = this.clickToBool(event.get("required").toString());
      }

      try {
         element = this.findElement(event, parent, required);
         if (element == null) {
            this.report.Log("FileField event finished.");
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
            this.report.Log(String.format("Setting filefield to: '%s'.",
                                          setvalue));
            element.sendKeys(setvalue);
            this.report.Log("Finished set.");
         }

         String value = element.getAttribute("value");
         handleVars(value, event);

      } catch (ElementNotVisibleException exp) {
         logElementNotVisible(required, event);
      } catch (Exception exp) {
         element = null;
         this.report.ReportException(exp);
      }

      this.report.Log("FileField event finished..");
      this.resetThreadTime();
      return element;
   }

   private WebElement liEvent(VDDHash event, WebElement parent) {
      boolean required = true;
      boolean click = false;
      WebElement element = null;

      this.report.Log("LI event Started.");

      if (event.containsKey("required")) {
         required = this.clickToBool(event.get("required").toString());
      }

      try {
         element = this.findElement(event, parent, required);
         if (element == null) {
            this.report.Log("LI event finished.");
            return element;
         }

         if (event.containsKey("click")) {
            click = this.clickToBool(event.get("click").toString());
         }

         this.checkDisabled(event, element);
         String value = element.getText();
         handleVars(value, event);

         if (event.containsKey("jscriptevent")) {
            this.report.Log("Firing Javascript Event: "
                            + event.get("jscriptevent").toString());
            this.Browser.fire_event(element, event.get("jscriptevent").toString());
            Thread.sleep(1000);
            this.report.Log("Javascript event finished.");
         }

         if (click) {
            this.report.Log("Click element.");
            this.firePlugin(element, Elements.LI,
                  PluginEvent.BEFORECLICK);
            element.click();
            this.firePlugin(element, Elements.LI,
                  PluginEvent.AFTERCLICK);
            this.report.Log("Click finished.");
         }
      } catch (ElementNotVisibleException exp) {
         logElementNotVisible(required, event);
      } catch (Exception exp) {
         this.report.ReportException(exp);
      }

      if (event.containsKey("children")) {
         this.processEvents((ArrayList<Event>) event.get("children"), element);
      }

      this.report.Log("LI event finished.");
      return element;
   }

   private WebElement trEvent(VDDHash event, WebElement parent) {
      boolean required = true;
      boolean click = false;
      WebElement element = null;

      this.report.Log("TR event Started.");

      if (event.containsKey("required")) {
         required = this.clickToBool(event.get("required").toString());
      }

      try {
         element = this.findElement(event, parent, required);
         if (element == null) {
            this.report.Log("TR event finished.");
            return element;
         }

         String value = element.getText();
         handleVars(value, event);
         this.checkDisabled(event, element);

         if (event.containsKey("click")) {
            click = this.clickToBool(event.get("click").toString());
         }

         if (click) {
            this.report.Log("Click element.");
            this.firePlugin(element, Elements.TR,
                  PluginEvent.BEFORECLICK);
            element.click();
            this.firePlugin(element, Elements.TR,
                  PluginEvent.AFTERCLICK);
            this.report.Log("Click finished.");
         }
      } catch (ElementNotVisibleException exp) {
         logElementNotVisible(required, event);
      } catch (Exception exp) {
         this.report.ReportException(exp);
      }

      if (event.containsKey("children")) {
         this.processEvents((ArrayList<Event>) event.get("children"), element);
      }

      this.report.Log("TR event finished.");
      return element;
   }


   /**
    * Handle a &lt;th&gt; event
    *
    * @param event  the &lt;th&gt; event
    * @param parent this element's parent
    * @return the a {@link WebElement} or null
    */

   private WebElement thEvent(VDDHash event, WebElement parent) {
      boolean required = true;
      boolean click = false;
      WebElement element = null;

      this.report.Log("TH event starting.");

      if (event.containsKey("required")) {
         required = this.clickToBool(event.get("required").toString());
      }

      try {
         element = this.findElement(event, parent, required);
         if (element == null) {
            this.report.Log("TH event finished.");
            return element;
         }

         this.firePlugin(element, Elements.TH, PluginEvent.AFTERFOUND);

         this.checkDisabled(event, element);

         if (event.containsKey("click")) {
            click = this.clickToBool(event.get("click").toString());
         }

         String value = element.getText();
         handleVars(value, event);

         if (click) {
            this.firePlugin(element, Elements.TH, PluginEvent.BEFORECLICK);
            element.click();
            this.firePlugin(element, Elements.TH, PluginEvent.AFTERCLICK);
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
            this.report.Log("Firing Javascript Event: " +
                            event.get("jscriptevent").toString());
            this.Browser.fire_event(element,
                                    event.get("jscriptevent").toString());
            Thread.sleep(1000);
            this.report.Log("Javascript event finished.");
         }

         if (event.containsKey("children") && element != null) {
            this.processEvents((ArrayList<Event>)event.get("children"), element);
         }
      } catch (ElementNotVisibleException exp) {
         logElementNotVisible(required, event);
      } catch (Exception exp) {
         this.report.ReportException(exp);
         element = null;
      }

      this.report.Log("TH event finished.");
      return element;
   }


   private WebElement tdEvent(VDDHash event, WebElement parent) {
      boolean required = true;
      boolean click = false;
      WebElement element = null;

      this.report.Log("TD event Started.");

      if (event.containsKey("required")) {
         required = this.clickToBool(event.get("required").toString());
      }

      try {
         element = this.findElement(event, parent, required);
         if (element == null) {
            this.report.Log("TD event finished.");
            return element;
         }

         String value = element.getText();
         handleVars(value, event);

         this.checkDisabled(event, element);

         if (event.containsKey("click")) {
            click = this.clickToBool(event.get("click").toString());
         }

         if (click) {
            this.report.Log("Click element.");
            this.firePlugin(element, Elements.TD,
                  PluginEvent.BEFORECLICK);
            element.click();
            this.firePlugin(element, Elements.TD,
                  PluginEvent.AFTERCLICK);
            this.report.Log("Click finished.");
         }
      } catch (ElementNotVisibleException exp) {
         logElementNotVisible(required, event);
      } catch (Exception exp) {
         element = null;
         this.report.ReportException(exp);
      }

      if (event.containsKey("children")) {
         this.processEvents((ArrayList<Event>) event.get("children"), element);
      }

      this.report.Log("TD event finished.");
      return element;
   }

   private boolean hiddenEvent(VDDHash event, WebElement parent) {
      boolean result = false;
      boolean required = true;
      WebElement element = null;

      this.report.Log("Hidden event Started.");

      if (event.containsKey("required")) {
         required = this.clickToBool(event.get("required").toString());
      }

      try {
         element = this.findElement(event, parent, required);
         if (element == null) {
            result = false;
            this.report.Log("Hidden event finished.");
            return result;
         }

         String value = element.getAttribute("value");
         handleVars(value, event);
         result = true;
      } catch (Exception exp) {
         this.report.ReportException(exp);
         result = false;
      }

      this.report.Log("Hidden event finished.");
      return result;
   }

   private WebElement spanEvent(VDDHash event, WebElement parent) {
      boolean required = true;
      boolean click = false;
      WebElement element = null;

      this.report.Log("span event starting.");

      if (event.containsKey("required")) {
         required = this.clickToBool(event.get("required").toString());
      }

      try {
         element = this.findElement(event, parent, required);
         if (element == null) {
            this.report.Log("Span event finished.");
            return element;
         }

         this.firePlugin(element, Elements.SPAN,
               PluginEvent.AFTERFOUND);

         this.checkDisabled(event, element);

         if (event.containsKey("click")) {
            click = this.clickToBool(event.get("click").toString());
         }

         String value = element.getText();
         handleVars(value, event);

         if (click) {
            this.firePlugin(element, Elements.SPAN,
                  PluginEvent.BEFORECLICK);
            element.click();
            this.firePlugin(element, Elements.SPAN,
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
            this.report.Log("Firing Javascript Event: "
                  + event.get("jscriptevent").toString());
            this.Browser.fire_event(element, event.get("jscriptevent").toString());
            Thread.sleep(1000);
            this.report.Log("Javascript event finished.");
         }

         if (event.containsKey("children") && element != null) {
            this.processEvents((ArrayList<Event>) event.get("children"), element);
         }
      } catch (ElementNotVisibleException exp) {
         logElementNotVisible(required, event);
      } catch (Exception exp) {
         this.report.ReportException(exp);
         element = null;
      }

      this.report.Log("Span event finished.");
      return element;
   }

   private WebElement inputEvent(VDDHash event, WebElement parent) {
      boolean required = true;
      WebElement element = null;

      this.report.Log("Input event starting.");

      if (event.containsKey("required")) {
         required = this.clickToBool(event.get("required").toString());
      }

      try {
         element = this.findElement(event, parent, required);
         if (element == null) {
            this.report.Log("Input event finished.");
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
            this.report.Log("Firing Javascript Event: " + event.get("jscriptevent").toString());
            this.Browser.fire_event(element, event.get("jscriptevent").toString());
            Thread.sleep(1000);
            this.report.Log("Javascript event finished.");
         }
      } catch (Exception exp) {
         this.report.ReportException(exp);
         element = null;
      }

      this.report.Log("Input event finished.");
      return element;
   }

   private WebElement radioEvent(VDDHash event, WebElement parent) {
      boolean required = true;
      boolean click = false;
      WebElement element = null;

      this.report.Log("Radio event starting.");

      if (event.containsKey("required")) {
         required = this.clickToBool(event.get("required").toString());
      }

      try {
         element = this.findElement(event, parent, required);
         if (element == null) {
            this.report.Log("Radio event finished.");
            return element;
         }

         this.firePlugin(element, Elements.RADIO,
               PluginEvent.AFTERFOUND);

         this.checkDisabled(event, element);

         if (event.containsKey("click")) {
            click = this.clickToBool(event.get("click").toString());
         }

         if (event.containsKey("set")) {
            this.report.Warn("Using the 'set' command for a radio element is not supported anymore!  Use click!");
            click = this.clickToBool(event.get("set").toString());
         }

         String value = element.getAttribute("value");
         handleVars(value, event);

         if (click) {
            this.firePlugin(element, Elements.RADIO,
                  PluginEvent.BEFORECLICK);
            this.report.Log("Clicking Element.");
            element.click();
            this.report.Log("Click finished.");
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
            this.report.Log("Firing Javascript Event: "
                  + event.get("jscriptevent").toString());
            this.Browser.fire_event(element, event.get("jscriptevent").toString());
            Thread.sleep(1000);
            this.report.Log("Javascript event finished.");
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
         this.report.ReportException(exp);
         element = null;
      }

      this.report.Log("Radio event finished.");
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

      this.report.Log("Select event Started.");

      if (event.containsKey("required")) {
         required = this.clickToBool(event.get("required").toString());
      }

      try {
         Select sel = null;
         element = this.findElement(event, parent, required);
         if (element == null) {
            this.report.Log("Select event finished.");
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
               this.report.ReportError("Failed to find select option '" +
                                       val + "'!");
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
            this.report.Log("Clearing select element.");
            sel.deselectAll();
         }

         try {
            if (event.containsKey("set") ||
                event.containsKey("setreal")) {
               boolean useVal = event.containsKey("setreal");
               String val = this.replaceString(event.get(useVal ? "setreal" :
                                                         "set").toString());
               this.report.Log("Setting option by " +
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
                  this.report.ReportError("Option with " +
                                          (useVal ? "value" : "visible text") +
                                          " '" + val + "' does not exist");
               }
            }

            if (event.containsKey("jscriptevent")) {
               this.report.Log("Firing Javascript Event: " +
                               event.get("jscriptevent").toString());
               this.Browser.fire_event(element,
                                       event.get("jscriptevent").toString());
               try {
                  Thread.sleep(1000);
               } catch (InterruptedException e) {
               }
               this.report.Log("Javascript event finished.");
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
            this.report.Log("Page refreshed; select element no longer exists.");
            element = null;
         }

      } catch (ElementNotVisibleException exp) {
         logElementNotVisible(required, event);
      } catch (Exception exp) {
         this.report.ReportException(exp);
      }

      this.report.Log("Select event finished.");
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

      this.report.Log("Option event starting.");

      if (event.containsKey("required")) {
         required = this.clickToBool(event.get("required").toString());
      }

      try {
         element = this.findElement(event, parent, required);
         if (element == null) {
            this.report.Log("Option event finished.");
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
            this.report.Log("Firing Javascript Event: " + ev);
            this.Browser.fire_event(element, ev);
            Thread.sleep(1000);
            this.report.Log("Javascript event finished.");
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
         this.report.ReportException(e);
      }

      this.report.Log("Option event finished.");
      return element;
   }


   private WebElement formEvent(VDDHash event, WebElement parent) {
      boolean required = true;
      boolean click = false;
      WebElement element = null;

      this.report.Log("Form event starting.");

      if (event.containsKey("required")) {
         required = this.clickToBool(event.get("required").toString());
      }

      try {
         element = this.findElement(event, parent, required);
         if (element == null) {
            this.report.Log("Form event finished.");
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
         this.report.ReportException(exp);
      }

      this.report.Log("Form event finished.");
      return element;
   }

   private WebElement tableEvent(VDDHash event, WebElement parent) {
      boolean required = true;
      boolean click = false;
      WebElement element = null;

      this.report.Log("Table event started.");

      if (event.containsKey("required")) {
         required = this.clickToBool(event.get("required").toString());
      }

      try {
         element = this.findElement(event, parent, required);
         if (element == null) {
            this.report.Log("Table event finished.");
            return element;
         }

         this.firePlugin(element, Elements.TABLE,
               PluginEvent.AFTERFOUND);

         this.checkDisabled(event, element);

         if (event.containsKey("click")) {
            click = this.clickToBool(event.get("click").toString());
         }

         if (click) {
            this.firePlugin(element, Elements.TABLE,
                  PluginEvent.BEFORECLICK);
            element.click();
            this.firePlugin(element, Elements.TABLE,
                  PluginEvent.AFTERCLICK);
         }

         if (event.containsKey("assert")) {
            String src = element.getText();
            String value = event.get("assert").toString();
            value = this.replaceString(value);
            this.report.Assert(value, src);
         }

         if (event.containsKey("assertnot")) {
            String src = element.getText();
            String value = event.get("assertnot").toString();
            value = this.replaceString(value);
            this.report.AssertNot(value, src);
         }

         if (event.containsKey("jscriptevent")) {
            this.report.Log("Firing Javascript Event: "
                  + event.get("jscriptevent").toString());
            this.Browser.fire_event(element, event.get("jscriptevent")
                  .toString());
            Thread.sleep(1000);
            this.report.Log("Javascript event finished.");
         }

         if (event.containsKey("children")) {
            this.processEvents((ArrayList<Event>) event.get("children"), element);
         }
      } catch (ElementNotVisibleException exp) {
         logElementNotVisible(required, event);
      } catch (Exception exp) {
         this.report.ReportException(exp);
         element = null;
      }

      this.report.Log("Table event finished.");
      return element;
   }



   ////////////////////////////////////////////////////////////
   // delete me
   private String replaceString(String str) {
      String result = str;
      Pattern patt = null;
      Matcher matcher = null;

      this.resetThreadTime();

      patt = Pattern.compile("\\{@[\\w\\.]+\\}", Pattern.CASE_INSENSITIVE);
      matcher = patt.matcher(str);

      while (matcher.find()) {
         String m = matcher.group();
         String tmp = m;
         tmp = tmp.replace("{@", "");
         tmp = tmp.replace("}", "");

         if (this.hijacks.containsKey(tmp)) {
            String value = this.hijacks.get(tmp).toString();
            result = result.replace(m, value);
         } else {
            try {
               String value = this.vars.get(tmp).toString();
               result = result.replace(m, value);
            } catch (NoSuchFieldException e) {
            }
         }
      }

      result = result.replaceAll("\\\\n", "\n");
      this.resetThreadTime();

      return result;
   }
   ////////////////////////////////////////////////////////////



   private WebElement divEvent(VDDHash event, WebElement parent) {
      boolean required = true;
      boolean click = false;
      WebElement element = null;

      this.report.Log("Div event starting.");

      if (event.containsKey("required")) {
         required = this.clickToBool(event.get("required").toString());
      }

      try {
         element = this.findElement(event, parent, required);
         if (element == null) {
            this.report.Log("Div event finished.");
            return element;
         }

         this.firePlugin(element, Elements.DIV,
               PluginEvent.AFTERFOUND);

         this.checkDisabled(event, element);

         if (event.containsKey("click")) {
            click = this.clickToBool(event.get("click").toString());
         }

         if (click) {
            this.firePlugin(element, Elements.DIV,
                  PluginEvent.BEFORECLICK);
            element.click();
            this.firePlugin(element, Elements.DIV,
                  PluginEvent.AFTERCLICK);
         }

         if (event.containsKey("assert")) {
            String src = element.getText();
            String value = event.get("assert").toString();
            value = this.replaceString(value);
            this.report.Assert(value, src);
         }

         if (event.containsKey("assertnot")) {
            String src = element.getText();
            String value = event.get("assertnot").toString();
            value = this.replaceString(value);
            this.report.AssertNot(value, src);
         }

         if (event.containsKey("jscriptevent")) {
            this.report.Log("Firing Javascript Event: "
                  + event.get("jscriptevent").toString());
            this.Browser.fire_event(element, event.get("jscriptevent").toString());
            Thread.sleep(1000);
            this.report.Log("Javascript event finished.");
         }

         String value = element.getText();
         handleVars(value, event);

         if (event.containsKey("children")) {
            this.processEvents((ArrayList<Event>) event.get("children"), element);
         }

      } catch (ElementNotVisibleException exp) {
         logElementNotVisible(required, event);
      } catch (Exception exp) {
         this.report.ReportException(exp);
         element = null;
      }

      this.report.Log("Div event finished.");
      return element;
   }

   private boolean scriptEvent(VDDHash event) {
      boolean result = false;
      TestLoader loader = null;
      String testfile = "";
      File fd = null;
      ArrayList<Event> newEvents = null;

      testfile = event.get("file").toString();
      testfile = this.replaceString(testfile);

      try {
         fd = new File(testfile);
         if (!fd.exists()) {
            String msg = String.format("Failed to find file: '%s'!", testfile);
            this.report.ReportError(msg);
            return false;
         }
         fd = null;

         loader = new TestLoader(new File(testfile), null);
         newEvents = loader.getEvents();
         this.processEvents(newEvents, null);

      } catch (Exception exp) {
         exp.printStackTrace();
         result = false;
      }

      return result;
   }

   private WebElement checkboxEvent(VDDHash event, WebElement parent) {
      boolean click = false;
      boolean required = true;
      WebElement element = null;

      this.resetThreadTime();

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
            this.report.Log(msg);
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
         this.report.ReportException(exp);
      }

      this.resetThreadTime();
      return element;
   }

   /**
    * &lt;thead&gt; event
    *
    * @param event  the &lt;thead&gt; event
    * @param parent this element's parent
    * @return the &lt;thead&gt; {@link WebElement} or null
    */

   private WebElement theadEvent(VDDHash event, WebElement parent) {
      boolean required = true;
      boolean click = false;
      WebElement element = null;

      this.report.Log("thead event starting.");

      if (event.containsKey("required")) {
         required = this.clickToBool(event.get("required").toString());
      }

      try {
         element = this.findElement(event, parent, required);
         if (element == null) {
            this.report.Log("thead event finished.");
            return element;
         }

         this.firePlugin(element, Elements.THEAD,
                         PluginEvent.AFTERFOUND);

         this.checkDisabled(event, element); //??

         handleVars(element.getText(), event);

         if (event.containsKey("click") &&
             this.clickToBool(event.get("click").toString())) {
            this.firePlugin(element, Elements.THEAD,
                            PluginEvent.BEFORECLICK);
            element.click();
            this.firePlugin(element, Elements.THEAD,
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
            this.report.Log("Firing Javascript Event: "
                  + event.get("jscriptevent").toString());
            this.Browser.fire_event(element,
                                    event.get("jscriptevent").toString());
            Thread.sleep(1000);
            this.report.Log("Javascript event finished.");
         }

         if (event.containsKey("children") && element != null) {
            this.processEvents((ArrayList<Event>) event.get("children"), element);
         }
      } catch (ElementNotVisibleException exp) {
         logElementNotVisible(required, event);
      } catch (Exception exp) {
         this.report.ReportException(exp);
         element = null;
      }

      this.report.Log("thead event finished.");
      return element;
   }

   /**
    * &lt;tbody&gt; event
    *
    * @param event  the &lt;tbody&gt; event
    * @param parent this element's parent
    * @return the &lt;tbody&gt; {@link WebElement} or null
    */

   private WebElement tbodyEvent(VDDHash event, WebElement parent) {
      boolean required = true;
      boolean click = false;
      WebElement element = null;

      this.report.Log("tbody event starting.");

      if (event.containsKey("required")) {
         required = this.clickToBool(event.get("required").toString());
      }

      try {
         element = this.findElement(event, parent, required);
         if (element == null) {
            this.report.Log("thead event finished.");
            return element;
         }

         this.firePlugin(element, Elements.TBODY,
                         PluginEvent.AFTERFOUND);

         this.checkDisabled(event, element); //??

         handleVars(element.getText(), event);

         if (event.containsKey("click") &&
             this.clickToBool(event.get("click").toString())) {
            this.firePlugin(element, Elements.TBODY,
                            PluginEvent.BEFORECLICK);
            element.click();
            this.firePlugin(element, Elements.TBODY,
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
            this.report.Log("Firing Javascript Event: "
                  + event.get("jscriptevent").toString());
            this.Browser.fire_event(element,
                                    event.get("jscriptevent").toString());
            Thread.sleep(1000);
            this.report.Log("Javascript event finished.");
         }

         if (event.containsKey("children") && element != null) {
            this.processEvents((ArrayList<Event>) event.get("children"), element);
         }
      } catch (ElementNotVisibleException exp) {
         logElementNotVisible(required, event);
      } catch (Exception exp) {
         this.report.ReportException(exp);
         element = null;
      }

      this.report.Log("tbody event finished.");
      return element;
   }


   /////////////////////// delete me /////////////////////////////////////

   private WebElement findElement(VDDHash event, WebElement parent,
         boolean required) {
      // Code moved to ElementFinder.java
      return null;
   }


   /*
    * clickToBool -- method This method converts a string into a boolean type.
    *
    * Input: clickstr: a string containing "true" or "false". Case doesn't
    * matter.
    *
    * Output: returns a boolean type.
    */
   private boolean clickToBool(String clickstr) {
      boolean result = false;

      if (clickstr.toLowerCase().contains("true")
            || clickstr.toLowerCase().contains("false")) {
         result = Boolean.valueOf(clickstr).booleanValue();
      } else {
         if (clickstr.contains("0")) {
            result = false;
         } else {
            result = true;
         }
      }

      return result;
   }


   /////////////////////// stop deleting me /////////////////////////////


   /**
    * Perform pre-fire checks for plugin execution.
    *
    * If this routine returns false, plugin execution will be skipped.
    *
    * @return true if plugins can execute, false otherwise
    */

   private boolean pluginPrefireCheck() {
      if (this.plugins.size() == 0) {
         return false;
      }

      if (!this.windowExists(this.getCurrentHWND())) {
         this.report.Log("Browser window closed. Skipping plugin execution.");
         return false;
      }

      return true;
   }


   /**
    * Execute all plugins.
    *
    * @param element    the element on the current HTML page
    * @return true if all plugins succeeded, false otherwise
    */

   private boolean firePlugin(WebElement element) {
      boolean result = true;
      PluginData data = new PluginData();

      if (!pluginPrefireCheck()) {
         return true;
      }

      data.setElement(element);
      data.setBrowser(this.Browser);
      data.setVars(this.vars);
      data.setHijacks(this.hijacks);
      data.setTestName(this.testName);

      for (Plugin plugin: this.plugins) {
         result &= plugin.execute(data, this.report);
      }

      return result;
   }


   /**
    * Execute all plugins that match a plugin event.
    *
    * @param element    the element on the current HTML page
    * @param eventType  the type of plugin event
    * @return true if all plugins succeeded, false otherwise
    */

   private boolean firePlugin(WebElement element, PluginEvent eventType) {
      boolean result = true;
      PluginData data = new PluginData();

      if (!pluginPrefireCheck()) {
         return true;
      }

      data.setElement(element);
      data.setBrowser(this.Browser);
      data.setVars(this.vars);
      data.setHijacks(this.hijacks);
      data.setTestName(this.testName);

      for (Plugin plugin: this.plugins) {
         if (plugin.matches(eventType)) {
            result &= plugin.execute(data, this.report);
         }
      }

      return result;
   }


   /**
    * Execute a plugin that matches the current element and plugin event.
    *
    * @param element    the element on the current HTML page
    * @param type       the element's type
    * @param eventType  the type of plugin event
    * @return true if all plugins succeeded, false otherwise
    */

   private boolean firePlugin(WebElement element, Elements elementType,
                              PluginEvent eventType) {
      boolean result = true;
      PluginData data = new PluginData();

      if (!pluginPrefireCheck()) {
         return true;
      }

      data.setElement(element);
      data.setBrowser(this.Browser);
      data.setVars(this.vars);
      data.setHijacks(this.hijacks);
      data.setTestName(this.testName);

      for (Plugin plugin: this.plugins) {
         if (plugin.matches(elementType, eventType)) {
            result &= plugin.execute(data, this.report);
         }
      }

      return result;
   }


   private WebElement buttonEvent(VDDHash event, WebElement parent) {
      boolean click = true;
      boolean required = true;
      WebElement element = null;

      this.resetThreadTime();
      this.report.Log("Starting button event.");

      if (event.containsKey("required")) {
         required = this.clickToBool(event.get("required").toString());
      }

      try {
         element = this.findElement(event, parent, required);
         if (element == null) {
            this.report.Log("Finished button event.");
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
            this.report.Log(String.format("Setting Alert Hack to: '%s'", alert));
            this.Browser.alertHack(alert);
            this.report.Warn("You are using a deprecated alert hack, please use the <alert> command!");
         }

         if (click) {
            this.firePlugin(element, Elements.BUTTON,
                  PluginEvent.BEFORECLICK);
            this.report.Log("Clicking button.");
            element.click();
            this.report.Log("Finished clicking button.");
            this.firePlugin(element, Elements.BUTTON,
                  PluginEvent.AFTERCLICK);
         }

         if (event.containsKey("jscriptevent")) {
            this.report.Log("Firing Javascript Event: "
                  + event.get("jscriptevent").toString());
            this.Browser.fire_event(element, event.get("jscriptevent").toString());
            Thread.sleep(1000);
            this.report.Log("Javascript event finished.");
         }
      } catch (ElementNotVisibleException exp) {
         logElementNotVisible(required, event);
      } catch (Exception exp) {
         this.report.ReportException(exp);
         element = null;
      }

      this.resetThreadTime();
      this.report.Log("Finished button event.");

      return element;
   }

   private WebElement textareaEvent(VDDHash event, WebElement parent) {
      boolean required = true;
      WebElement element = null;

      this.resetThreadTime();

      this.report.Log("Starting textarea event.");

      if (event.containsKey("required")) {
         required = this.clickToBool(event.get("required").toString());
      }

      try {
         element = this.findElement(event, parent, required);
         if (element == null) {
            this.report.Log("Finished textarea event.");
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
            this.report.Log(String.format("Setting Value to: '%s'.", value));
            clearText(element);
            element.sendKeys(value);
         } else if (event.containsKey("clear")) {
            if (this.clickToBool(event.get("clear").toString())) {
               this.report.Log("Clearing textarea.");
               clearText(element);
            }
         } else if (event.containsKey("append")) {
            value = event.get("append").toString();
            value = this.replaceString(value);
            this.report.Log(String.format("Appending Value to: '%s'.", value));
            element.sendKeys(value);
         }

         if (event.containsKey("jscriptevent")) {
            this.report.Log("Firing Javascript Event: "
                  + event.get("jscriptevent").toString());
            this.Browser.fire_event(element, event.get("jscriptevent").toString());
            Thread.sleep(1000);
            this.report.Log("Javascript event finished.");
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
            this.report.Log("Clicking textarea.");
            element.click();
            this.report.Log("Finished clicking textarea.");
            this.firePlugin(element, Elements.TEXTAREA,
                            PluginEvent.AFTERCLICK);
         }

      } catch (ElementNotVisibleException exp) {
         logElementNotVisible(required, event);
      } catch (Exception exp) {
         this.report.ReportException(exp);
         element = null;
      }

      this.resetThreadTime();
      this.report.Log("Finished textarea event.");
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

      this.resetThreadTime();
      this.report.Log("Starting textfield event.");

      if (event.containsKey("required")) {
         required = this.clickToBool(event.get("required").toString());
      }

      try {
         element = this.findElement(event, parent, required);
         if (element == null) {
            this.report.Log("Finished textfield event.");
            return null;
         }

         this.firePlugin(element, Elements.TEXTFIELD,
               PluginEvent.AFTERFOUND);

         this.checkDisabled(event, element);

         if (event.containsKey("clear")) {
            if (this.clickToBool(event.get("clear").toString())) {
               this.report.Log("Clearing textfield.");
               clearText(element);
            }
         }

         if (event.containsKey("set")) {
            String value = event.get("set").toString();
            value = this.replaceString(value);
            this.report.Log(String.format("Setting Value to: '%s'.", value));
            clearText(element);
            element.sendKeys(value);
         }

         if (event.containsKey("append")) {
            String value = event.get("append").toString();
            value = this.replaceString(value);
            element.sendKeys(value);
         }

         if (event.containsKey("jscriptevent")) {
            this.report.Log("Firing Javascript Event: "
                  + event.get("jscriptevent").toString());
            this.Browser.fire_event(element, event.get("jscriptevent").toString());
            Thread.sleep(1000);
            this.report.Log("Javascript event finished.");
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
         this.report.ReportException(exp);
         element = null;
      }

      this.resetThreadTime();
      this.report.Log("Finished textfield event.");
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

      this.resetThreadTime();
      this.report.Log("Starting password event.");

      if (event.containsKey("required")) {
         required = this.clickToBool(event.get("required").toString());
      }

      try {
         element = this.findElement(event, parent, required);
         if (element == null) {
            this.report.Log("Finished password event.");
            return null;
         }

         this.firePlugin(element, Elements.PASSWORD, PluginEvent.AFTERFOUND);

         this.checkDisabled(event, element);

         if (event.containsKey("clear")) {
            if (this.clickToBool(event.get("clear").toString())) {
               this.report.Log("Clearing password field.");
               clearText(element);
            }
         }

         if (event.containsKey("set")) {
            String value = event.get("set").toString();
            value = this.replaceString(value);
            this.report.Log(String.format("Setting Value to: '%s'.", value));
            clearText(element);
            element.sendKeys(value);
         }

         if (event.containsKey("append")) {
            String value = event.get("append").toString();
            value = this.replaceString(value);
            element.sendKeys(value);
         }

         if (event.containsKey("jscriptevent")) {
            this.report.Log("Firing Javascript Event: " + event.get("jscriptevent").toString());
            this.Browser.fire_event(element, event.get("jscriptevent").toString());
            Thread.sleep(1000);
            this.report.Log("Javascript event finished.");
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
         this.report.ReportException(exp);
         element = null;
      }

      this.resetThreadTime();
      this.report.Log("Finished password event.");
      return element;
   }

   private void checkDisabled(VDDHash event, WebElement element) {
      // Moved to HtmlEvent.DisabledAction
   }

   private void handleVars(String value, VDDHash event) {
      // Moved to HtmlEvent.VarAction
   }
}
