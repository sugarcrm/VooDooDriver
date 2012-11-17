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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
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


/**
 * This is the heart of all of VooDooDriver.  This class handles
 * executing all of the SODA language commands in the web browser.
 *
 * @author trampus
 *
 */

public class EventLoop implements Runnable {

   private Events testEvents = null;
   private Browser Browser = null;
   private VDDHash sodaVars = null;
   private Reporter report = null;
   private VDDHash hijacks = null;
   private Date threadTime = null;
   private volatile Thread runner;
   private volatile Boolean threadStop = false;
   private VDDHash ElementStore = null;
   private String currentHWnd = null;
   private int attachTimeout = 0;
   private String csvOverrideFile = null;
   private VDDHash whitelist = null;
   private ArrayList<Plugin> plugins = null;
   private String testName = "";


   /**
    * The class Constructor.
    *
    * @param browser  {@link Browser}
    * @param events   {@link Events}
    * @param reporter {@link Reporter}
    * @param gvars    {@link VDDHash}
    * @param hijacks  {@link VDDHash}
    * @param oldvars  {@link VDDHash}
    * @param plugins  {@link Events}
    * @param testName the current running test
    */
   public EventLoop(Browser browser, Events events, Reporter reporter,
                    VDDHash gvars, VDDHash hijacks, VDDHash oldvars,
                    ArrayList<Plugin> plugins, String testName) {
      testEvents = events;
      this.Browser = browser;
      this.report = reporter;
      this.hijacks = hijacks;
      this.testName = testName;
      this.whitelist = new VDDHash();

      if (oldvars != null) {
         sodaVars = oldvars;
      } else {
         sodaVars = new VDDHash();
      }

      this.ElementStore = new VDDHash();

      if (gvars != null) {
         int len = gvars.keySet().size();

         for (int i = 0; i <= len - 1; i++) {
            String key = gvars.keySet().toArray()[i].toString();
            String value = gvars.get(key).toString();
            System.out.printf("--)'%s' => '%s'\n", key, value);

            this.sodaVars.put(key, value);
         }
      }

      this.plugins = new ArrayList<Plugin>();
      if (plugins != null) {
         this.plugins.addAll(plugins);
      }
      this.stampEvent();
      SimpleDateFormat df = new SimpleDateFormat("MM/dd/yyyy");
      this.sodaVars.put("currentdate", df.format(new Date()));
      this.threadTime = new Date();
      String hwnd = this.Browser.getDriver().getWindowHandle();
      this.setCurrentHWND(hwnd);
      this.runner = new Thread(this, "EventLoop-Thread");
      runner.start();
   }

   /**
    * Sets the timeout in seconds for the Attach command to give up on retrying
    * to find the wanted window to attach to.
    *
    * @param timeout int
    */
   public void setAttachTimeout(int timeout) {
      this.attachTimeout = timeout;
   }

   /**
    * Checks to see if a browser window exists.
    *
    * @param hwnd {@link String}
    * @return
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
    * Sets the active browser window for VDD to use.
    *
    * @param hwnd {@link String}
    */
   private void setCurrentHWND(String hwnd) {
      this.currentHWnd = hwnd;
   }

   /**
    * Gets the current browser window handle.
    *
    * @return {@link String}
    */
   private String getCurrentHWND() {
      return this.currentHWnd;
   }

   /**
    *
    * @param event
    */
   private void assertPage(VDDHash event) {
      boolean assertpage = true;

      if (event.containsKey("assertPage")) {
         assertpage = this.clickToBool(event.get("assertPage").toString());
      }

      if (assertpage) {
         this.Browser.assertPage(this.whitelist);
      }
   }

   private void saveElement(VDDHash event, WebElement element) {
      if (!event.containsKey("save")) {
         return;
      }

      String name = event.get("save").toString();
      if (this.ElementStore.containsKey(name)) {
         String msg = String.format("Found existing saved element var: '%s', Overwriting now.",
                     name);
         this.report.Warn(msg);
      }

      if (element == null) {
         String msg = String.format(
               "Element trying to be saved: '%s => 'NULL', not storing NULL element!", name);
         this.report.ReportError(msg);
      } else {
         this.ElementStore.put(name, element);
         String msg = String.format("Stored HTML element to be referenced by: '%s'.", name);
         this.report.Log(msg);
      }
   }

   public VDDHash getSodaVars() {
      return this.sodaVars;
   }

   public void appedSodaVars(VDDHash vars) {
      int len = 0;

      if (vars == null) {
         return;
      }

      len = vars.keySet().size() - 1;
      for (int i = 0; i <= len; i++) {
         String name = vars.keySet().toArray()[i].toString();
         String value = vars.get(name).toString();
         this.sodaVars.put(name, value);
      }

   }

   public boolean isAlive() {
      return this.runner.isAlive();
   }

   public Thread getThread() {
      return this.runner;
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

   public void run() {
      System.out.printf("Thread Running...\n");
      this.threadTime = new Date();
      int i = 0;
      int event_count = this.testEvents.size() - 1;

      this.firePlugin(null, PluginEvent.BEFORETEST);

      while ((!this.threadStop) && (i <= event_count)) {
         handleSingleEvent(this.testEvents.get(i), null);
         i += 1;
      }

      this.firePlugin(null, PluginEvent.AFTERTEST);
   }

   private void resetThreadTime() {
      synchronized (this.threadTime) {
         this.threadTime = new Date();
      }
   }

   public Date getThreadTime() {
      Date tmp = null;

      synchronized (this.threadTime) {
         tmp = this.threadTime;
      }

      return tmp;
   }

   private void processEvents(Events events, WebElement parent) {
      int event_count = events.size() - 1;

      for (int i = 0; i <= event_count; i++) {
         if (isStopped()) {
            break;
         }

         handleSingleEvent(events.get(i), parent);
      }
   }

   public Events getElements() {
      return testEvents;
   }

   private boolean handleSingleEvent(VDDHash event, WebElement parent) {
      boolean result = false;
      WebElement element = null;
      Elements type = (Elements)event.get("type");

      if (isStopped()) {
         return result;
      }

      this.resetThreadTime();

      switch (type) {
      case BROWSER:
         result = browserEvent(event, parent);
         break;
      case THEAD:
         element = theadEvent(event, parent);
         break;
      case TBODY:
         element = tbodyEvent(event, parent);
         break;
      case PUTS:
         result = putsEvent(event);
         break;
      case WAIT:
         result = waitEvent(event);
         break;
      case TEXTFIELD:
         element = textfieldEvent(event, parent);
         break;
      case PASSWORD:
         element = passwordEvent(event, parent);
         break;
      case BUTTON:
         element = buttonEvent(event, parent);
         break;
      case CSV:
         result = csvEvent(event);
         break;
      case LINK:
         element = linkEvent(event, parent);
         break;
      case CHECKBOX:
         element = checkboxEvent(event, parent);
         break;
      case VAR:
         result = varEvent(event);
         break;
      case SCRIPT:
         result = scriptEvent(event);
         break;
      case DIV:
         element = divEvent(event, parent);
         break;
      case ATTACH:
         result = attachEvent(event);
         break;
      case TABLE:
         element = tableEvent(event, parent);
         break;
      case FORM:
         element = formEvent(event, parent);
         break;
      case SELECT:
         element = selectEvent(event, parent);
         break;
      case OPTION:
         element = optionEvent(event, parent);
         break;
      case STAMP:
         result = stampEvent();
         break;
      case TIMESTAMP:
         result = stampEvent();
         break;
      case SPAN:
         element = spanEvent(event, parent);
         break;
      case HIDDEN:
         result = hiddenEvent(event, parent);
         break;
      case TR:
         element = trEvent(event, parent);
         break;
      case TH:
         element = thEvent(event, parent);
         break;
      case TD:
         element = tdEvent(event, parent);
         break;
      case FILEFIELD:
         element = filefieldEvent(event, parent);
         break;
      case IMAGE:
         element = imageEvent(event, parent);
         break;
      case DND:
         result = dndEvent(event);
         break;
      case TEXTAREA:
         element = textareaEvent(event, parent);
         break;
      case LI:
         element = liEvent(event, parent);
         break;
      case RADIO:
         element = radioEvent(event, parent);
         break;
      case EXECUTE:
         result = executeEvent(event);
         break;
      case JAVASCRIPT:
         result = javascriptEvent(event);
         break;
      case UL:
         result = ulEvent(event);
         break;
      case OL:
         result = olEvent(event);
         break;
      case MAP:
         result = mapEvent(event);
         break;
      case AREA:
         result = areaEvent(event);
         break;
      case PLUGINLOADER:
         result = pluginloaderEvent(event);
         break;
      case JAVAPLUGIN:
         result = javapluginEvent(event, parent);
         break;
      case DELETE:
         result = deleteEvent(event);
         break;
      case ALERT:
         result = alertEvent(event);
         break;
      case SCREENSHOT:
         result = screenshotEvent(event);
         break;
      case FRAME:
         result = frameEvent(event);
         break;
      case WHITELIST:
         result = whitelistEvent(event);
         break;
      case INPUT:
         element = inputEvent(event, parent);
         break;
      case EMAIL:
         element = emailEvent(event, parent);
         break;
      case H1:
         element = miscEvent(event, parent);
         break;
      case H2:
         element = miscEvent(event, parent);
         break;
      case H3:
         element = miscEvent(event, parent);
         break;
      case H4:
         element = miscEvent(event, parent);
         break;
      case H5:
         element = miscEvent(event, parent);
         break;
      case H6:
         element = miscEvent(event, parent);
         break;
      case I:
         element = miscEvent(event, parent);
         break;
      case B:
         element = miscEvent(event, parent);
         break;
      case STRIKE:
         element = miscEvent(event, parent);
         break;
      case S:
         element = miscEvent(event, parent);
         break;
      case U:
         element = miscEvent(event, parent);
         break;
      default:
         System.out.printf("(!)Unknown command: '%s'!\n", type.toString());
         System.exit(1);
      }

      this.firePlugin(null, type, PluginEvent.AFTEREVENT);
      this.resetThreadTime();

      if (element != null) {
         this.saveElement(event, element);
      }

      this.assertPage(event);

      return result;
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

   private boolean whitelistEvent(VDDHash event) {
      boolean result = false;
      String action = null;
      String name = null;
      String msg = "";

      this.report.Log("Whitelist event starting...");

      if (event.containsKey("name")) {
         name = event.get("name").toString();
         name = this.replaceString(name);
      }

      if (name == null) {
         this.report.ReportError("Error: Missing 'name' attribute!");
         this.report.Log("Whitelist event finished.");
         return false;
      }

      if (event.containsKey("action")) {
         action = event.get("action").toString();
         action = this.replaceString(action);
         action = action.toLowerCase();
      }

      if ((action == null) || (!action.contains("add") && !action.contains("delete"))) {
         msg = String.format("Error: action is an unknow type: '%s'!", action);
         this.report.ReportError(msg);
         this.report.Log("Whitelist event finished.");
         return false;
      }

      if (action.contains("add")) {
         String tmp = event.get("content").toString();
         tmp = this.replaceString(tmp);
         this.whitelist.put(name, tmp);
         msg = String.format("Added whitelist item: '%s' => '%s'.", name, tmp);
         this.report.Log(msg);
      } else {
         this.whitelist.remove(name);
         msg = String.format("Deleted whitelist item: '%s'.", name);
         this.report.Log(msg);
      }

      this.report.Log("Whitelist event finished.");

      return result;
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
            this.processEvents((Events) event.get("children"), null);
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

   private boolean screenshotEvent(VDDHash event) {
      boolean result = false;
      String filename = "";

      this.report.Log("Screenshot event starting.");

      if (!event.containsKey("file")) {
         result = false;
         this.report.ReportError("Error: screenshot command missing 'file' attribute!");
         this.report.Log("Screenshot event finished.");
         return result;
      } else {
         filename = event.get("file").toString();
         filename = this.replaceString(filename);
         Utils.takeScreenShot(filename, this.report);
      }

      this.report.Log("Screenshot event finished.");
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

   private boolean deleteEvent(VDDHash event) {
      boolean result = false;

      this.report.Log("Delete event starting.");
      if (event.containsKey("name")) {
         String name = event.get("name").toString();
         name = this.replaceString(name);

         if (this.ElementStore.containsKey(name)) {
            this.ElementStore.remove(name);
            this.report.Log("Deleted stored var.");
            result = true;
         } else {
            result = false;
            String msg = String.format("Error: failed to find variable to delete by name: '%s'!",
                  name);
            this.report.ReportError(msg);
         }
      } else {
         this.report.ReportError("Error: delete command missing name attribute!");
         result = false;
      }

      this.report.Log("Finsihed Delete event.");
      return result;
   }


   /**
    * Execute a <javaplugin> event.
    *
    * @param event   the <javaplugin> event
    * @param parent  the parent HTML element
    * @return whether plugin execution succeeded
    */

   private boolean javapluginEvent(VDDHash event, WebElement parent) {
      String classname;
      Plugin plugin = null;
      PluginData data = new PluginData();
      boolean result;

      this.report.Log("Javaplugin event started.");

      if (!event.containsKey("classname")) {
         report.ReportError("Javaplugin event missing attribute 'classname'!");
         report.Log("Javaplugin event finished.");
         return false;
      }

      classname = (String)event.get("classname");
      
      for (Plugin p: this.plugins) {
         if (p.matches(classname)) {
            plugin = p;
            break;
         }
      }
      if (plugin == null) {
         report.ReportError("Failed to find a loaded plugin with classname " +
                            classname);
         report.Log("Javaplugin event finished.");
         return false;
      }

      if (event.containsKey("args")) {
         String[] args = (String[])event.get("args");

         if (args != null) {
            for (int k = 0; k < args.length; k++) {
               args[k] = replaceString(args[k]);
            }
         }

         data.setArgs(args);
      }

      data.setElement(parent);
      data.setBrowser(this.Browser);
      data.setSodaVars(this.sodaVars);
      data.setHijacks(this.hijacks);
      data.setTestName(this.testName);

      result = plugin.execute(data, report);

      if (result == false) {
         report.ReportError("Javaplugin failed");
      }

      report.Log("Javaplugin event finished.");

      return result;
   }


   /**
    * Execute a <pluginloader> event
    *
    * @param event  the <pluginloader> event
    * @return whether loading the new plugin succeeded
    */

   private boolean pluginloaderEvent(VDDHash event) {
      String classname;
      String classfile;
      boolean result = true;

      report.Log("PluginLoader event started.");

      if (!event.containsKey("file")) {
         report.ReportError("Missing 'file' attribute for <pluginloader>");
         report.Log("PluginLoader event finished.");
         return false;
      }

      if (!event.containsKey("classname")) {
         report.ReportError("Missing 'classname' attribute for <pluginloader>");
         report.Log("PluginLoader event finished.");
         return false;
      }

      classfile = (String)event.get("file");
      classname = (String)event.get("classname");

      report.Log("Loading plugin with classname=" + classname);

      try {
         this.plugins.add(new JavaPlugin(classname, classfile));
      } catch (PluginException e) {
         report.ReportError("Failed to load plugin " + classname);
         report.ReportException(e);
         result = false;
      }

      report.Log("PluginLoader event finished.");

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
            this.processEvents((Events) event.get("children"), element);
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
            this.processEvents((Events) event.get("children"), element);
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

   private boolean javascriptEvent(VDDHash event) {
      boolean result = false;
      String scriptdata = "";

      this.report.Log("Javascript event starting.");

      if (event.containsKey("content")) {
         this.report.Warn("Using javascript contents is deprecated, please use the file attribute!");
         scriptdata = event.get("content").toString();
      }

      if (event.containsKey("file")) {
         scriptdata = "";
         String filename = event.get("file").toString();
         filename = this.replaceString(filename);

         File fd = new File(filename);
         try {
            BufferedReader reader = new BufferedReader(new FileReader(fd));
            String tmp;

            while ((tmp = reader.readLine()) != null) {
               scriptdata = scriptdata.concat(tmp);
            }
            result = true;
         } catch (Exception exp) {
            this.report.ReportException(exp);
            result = false;
         }
      }

      this.Browser.executeJS(scriptdata, null);

      this.report.Log("Javascript event finished.");
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
         WebElement Esrc = (WebElement) this.ElementStore.get(src);
         WebElement Edst = (WebElement) this.ElementStore.get(dst);
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
         this.processEvents((Events) event.get("children"), element);
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
         this.processEvents((Events) event.get("children"), element);
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
            this.processEvents((Events) event.get("children"), element);
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
         this.processEvents((Events) event.get("children"), element);
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

   private boolean stampEvent() {
      boolean result = false;
      Date now = new Date();
      DateFormat df = new SimpleDateFormat("yyMMdd_hhmmss");
      String date_str = df.format(now);

      this.report.Log(String.format("Setting STAMP => '%s'.", date_str));
      this.sodaVars.put("stamp", date_str);
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
            this.processEvents((Events) event.get("children"), element);
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


   /**
    * &lt;email&gt; event
    *
    * @param event   the &lt;email&gt; event
    * @param parent  this element's parent
    * @return the &lt;email&gt; {@link WebElement} or null
    */

   private WebElement emailEvent(VDDHash event, WebElement parent) {
      boolean required = true;
      WebElement element = null;

      this.resetThreadTime();
      this.report.Log("Starting email event.");

      if (event.containsKey("required")) {
         required = this.clickToBool(event.get("required").toString());
      }

      try {
         element = this.findElement(event, parent, required);
         if (element == null) {
            this.report.Log("Finished email event.");
            return null;
         }

         this.firePlugin(element, Elements.EMAIL, PluginEvent.AFTERFOUND);
         this.checkDisabled(event, element);

         if (event.containsKey("clear")) {
            if (this.clickToBool(event.get("clear").toString())) {
               this.report.Log("Clearing email element.");
               clearText(element);
            }
         }

         if (event.containsKey("set")) {
            String value = event.get("set").toString();
            value = this.replaceString(value);
            this.report.Log(String.format("Setting value to: '%s'.", value));
            clearText(element);
            element.sendKeys(value);
         }

         if (event.containsKey("append")) {
            String value = event.get("append").toString();
            value = this.replaceString(value);
            element.sendKeys(value);
         }

         if (event.containsKey("jscriptevent")) {
            String jev = (String)event.get("jscriptevent");
            this.report.Log("Firing Javascript Event: " + jev);
            this.Browser.fire_event(element, jev);
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
      this.report.Log("Finished email event.");
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
               this.processEvents((Events)event.get("children"), element);
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
            this.processEvents((Events) event.get("children"), element);
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
            this.processEvents((Events) event.get("children"), element);
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

   private boolean attachEvent(VDDHash event) {
      boolean result = false;
      Set<String> handles = null;
      int len = 0;
      boolean use_URL = false;
      String finder = "";
      String found_handle = null;
      String msg = "";
      int timeout = 10;
      int index = -1;

      try {
         this.report.Log("Attach event starting.");
         String currentWindow = this.Browser.getDriver().getWindowHandle();
         this.report.Log(String.format("Current Window Handle: '%s'.",
               currentWindow));

         if (event.containsKey("index")) {
            use_URL = false;
            String tmp_index = event.get("index").toString();
            tmp_index = this.replaceString(tmp_index);
            if (!Utils.isInt(tmp_index)) {
               msg = String.format("Error: index is not an integer: '%s'!", tmp_index);
               this.report.ReportError(msg);
               this.report.Log("Attach event finished.");
               return false;
            }

            timeout = 0; // causes the for loop to do nothing this is a hack. should make this method better later ... //
            index = Integer.valueOf(tmp_index);
            finder = tmp_index;
         }else if (event.containsKey("url")) {
            use_URL = true;
            finder = event.get("url").toString();
         } else {
            use_URL = false;
            finder = event.get("title").toString();
         }

         finder = this.replaceString(finder);

         for (int timer = 0; timer <= timeout; timer++) {
            handles = this.Browser.getDriver().getWindowHandles();
            len = handles.size() - 1;
            for (int i = 0; i <= len; i++) {
               String tmp_handle = handles.toArray()[i].toString();
               String tmp_url = this.Browser.getDriver().switchTo()
                     .window(tmp_handle).getCurrentUrl();
               String tmp_title = this.Browser.getDriver().switchTo()
                     .window(tmp_handle).getTitle();
               this.report.Log(String.format("[%d]: Window Handle: '%s'", i,
                     tmp_handle));
               this.report.Log(String.format("[%d]: Window Title: '%s'", i,
                     tmp_title));
               this.report.Log(String.format("[%d]: Window URL: '%s'", i,
                     tmp_url));

               TextFinder f = new TextFinder(finder);

               if (use_URL && f.find(tmp_url)) {
                  found_handle = tmp_handle;
                  this.report.Log(String.format("Found Window URL '%s'",
                                                finder));
                  break;
               } else if (!use_URL && f.find(tmp_title)) {
                  found_handle = tmp_handle;
                  this.report.Log(String.format("Found Window Title '%s'",
                                                finder));
                  break;
               }
            } // end for loop //

            if (found_handle != null) {
               break;
            }
            Thread.sleep(1000);
         } // end timer loop //


         if (index != -1) {
            handles = this.Browser.getDriver().getWindowHandles();
            len = handles.size() -1;

            if (index > len) {
               msg = String.format("Error: index: '%d' is greater then the number of windows found: '%d'!",
                     index, len);
               found_handle = null;
            } else {
               found_handle = handles.toArray()[index].toString();
            }
         }

         if (found_handle == null) {
            msg = String.format("Failed to find window matching: '%s!'", finder);
            this.report.ReportError(msg);
            result = false;
            this.Browser.getDriver().switchTo().window(currentWindow);
            Thread.sleep(2000); // helps control when the page is loaded, might
                                 // remove this later... //
            return result;
         }

         this.Browser.getDriver().switchTo().window(found_handle);
         this.setCurrentHWND(found_handle);
         msg = String.format("Switching to window handle: '%s'.", found_handle);
         this.report.Log(msg);
         if (event.containsKey("children")) {
            this.processEvents((Events) event.get("children"), null);
         }

         this.Browser.setBrowserOpened();
         this.Browser.getDriver().switchTo().window(currentWindow);
         this.setCurrentHWND(currentWindow);
         msg = String.format("Switching back to window handle: '%s'.",
               currentWindow);
         this.report.Log(msg);

         if (this.attachTimeout > 0) {
            long tout = this.attachTimeout * 1000;
            Thread.currentThread();
            msg = String.format(
                  "Waiting '%s' seconds before executing next event.",
                  this.attachTimeout);
            this.report.Log(msg);
            Thread.sleep(tout);
         }
      } catch (Exception exp) {
         this.report.ReportException(exp);
         result = false;
      }

      this.report.Log("Attach event finished.");

      return result;
   }

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
         } else if (this.sodaVars.containsKey(tmp)) {
            String value = this.sodaVars.get(tmp).toString();
            result = result.replace(m, value);
         }
      }

      result = result.replaceAll("\\\\n", "\n");
      this.resetThreadTime();

      return result;
   }

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
            this.processEvents((Events) event.get("children"), element);
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
      Events newEvents = null;

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
         if (newEvents == null) {
            this.report.ReportError("Failed to load script '" + testfile + "'");
            return false;
         }
         this.processEvents(newEvents, null);

      } catch (Exception exp) {
         this.report.ReportException(exp);
         result = false;
      }

      return result;
   }

   /*
    * varEvent -- method This method sets a SODA var that can then be used in
    * the follow script.
    *
    * Input: event: A Soda event.
    *
    * Output: returns true on success or false on fail.
    */
   private boolean varEvent(VDDHash event) {
      boolean result = false;
      String var_name = "";
      String var_value = "";

      try {
         if (event.containsKey("set")) {
            var_name = event.get("var").toString();
            var_name = this.replaceString(var_name);
            var_value = event.get("set").toString();
            var_value = this.replaceString(var_value);
            this.sodaVars.put(var_name, var_value);

            String tmp_value = var_value;
            tmp_value = tmp_value.replaceAll("\n", "\\\n");

            this.report.Log("Setting SODA variable: '" + var_name + "' => '"
                  + tmp_value + "'.");
         }

         if (event.containsKey("unset")) {
            var_name = event.get("var").toString();
            var_name = this.replaceString(var_name);
            this.report.Log("Unsetting SODA variable: '" + var_name + "'.");
            if (!this.sodaVars.containsKey(var_name)) {
               this.report.Log("SODA variable: '" + var_name
                     + "' not found, nothing to unset.");
            } else {
               this.sodaVars.remove(var_name);
            }
         }
      } catch (Exception exp) {
         this.report.ReportException(exp);
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
    * Handle a &lt;link&gt; event
    *
    * &quot;Link&quot; is a VDD-specific synonym for the HTML anchor (a) tag.
    *
    * @param event  the &lt;link&gt; event
    * @param parent this element's parent
    * @return the a {@link WebElement} or null
    */

   private WebElement linkEvent(VDDHash event, WebElement parent) {
      boolean click = true;
      boolean required = true;
      boolean exists = true;
      WebElement element = null;

      this.resetThreadTime();

      if (event.containsKey("required")) {
         required = this.clickToBool(event.get("required").toString());
      }

      if (event.containsKey("exists")) {
         String tmp = event.get("exists").toString();
         tmp = this.replaceString(tmp);
         exists = this.clickToBool(tmp);
      }

      try {
         this.report.Log("Link Event Started.");
         String how = event.get("how").toString();
         how = this.replaceString(how);
         String value = event.get(how).toString();
         value = this.replaceString(value);
         element = this.findElement(event, parent, required);

         if (element == null) {
            if (required && exists) {
               String msg = String.format("Failed to find link: '%s' => '%s'!",
                     how, value);
               this.report.Log(msg);
            }

            if (exists != true) {
               this.report.Assert("Link does not exist.", false, false);
            }

            element = null;
            this.report.Log("Link Event Finished.");
            return element;
         }

         value = element.getText();
         handleVars(value, event);

         this.firePlugin(element, Elements.LINK,
               PluginEvent.AFTERFOUND);

         this.checkDisabled(event, element);

         if (event.containsKey("alert")) {
            boolean alert = this.clickToBool(event.get("alert").toString());
            this.report.Log(String.format("Setting Alert Hack to: '%s'", alert));
            this.Browser.alertHack(alert);
            this.report.Warn("You are using a deprecated alert hack, please use the <alert> command!");
         }

         if (event.containsKey("click")) {
            click = this.clickToBool(event.get("click").toString());
         }

         if (click) {
            value = this.replaceString(value);
            this.report.Log(String.format("Clicking Link: '%s' => '%s'", how,
                                          value));
            this.firePlugin(element, Elements.LINK,
                            PluginEvent.BEFORECLICK);
            element.click();
            this.firePlugin(element, Elements.LINK,
                  PluginEvent.AFTERCLICK);
         } else {
            String msg = String.format(
                  "Found Link: '%s' but not clicking as click => '%s'.", value,
                  click);
            this.report.Log(msg);
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
      this.report.Log("Link Event Finished.");
      return element;
   }

   private boolean csvEvent(VDDHash event) {
      boolean result = false;
      CSV csv = null;
      CSVData csv_data = null;
      String var_name = null;
      String csv_filename = "";
      String msg = "";

      if (event.containsKey("var")) {
         var_name = event.get("var").toString();
      }

      this.resetThreadTime();
      this.report.Log("CSV event starting...");

      if (this.csvOverrideFile != null) {
         csv_filename = event.get("file").toString();
         csv_filename = this.replaceString(csv_filename);
         msg = String.format("Found existing csv override file: '%s', replacing expected file: '%s'.",
               this.csvOverrideFile, csv_filename);
         event.put("file", this.csvOverrideFile);
         event.remove("csv"); // just in cause someone wanted to override and override. //
         this.csvOverrideFile = null;
      }

      if (event.containsKey("override")) {
         String csv_txt = event.get("override").toString();
         csv_txt = this.replaceString(csv_txt);
         this.csvOverrideFile = csv_txt;
         msg = String.format("Setting CSV file override to file: '%s'.",
                             this.csvOverrideFile);
         this.report.Log(msg);
         this.report.Log("CSV event finished.");
         return true;
      }

      csv_filename = event.get("file").toString();
      csv_filename = replaceString(csv_filename);
      msg = String.format("Processing CSV file: '%s'...", csv_filename);
      this.report.Log(msg);

      csv = new CSV(csv_filename, this.report);
      csv_data = csv.getData();

      for (int i = 0; i <= csv_data.size() - 1; i++) {
         int keys_len = csv_data.get(i).keySet().size() - 1;

         for (int key_index = 0; key_index <= keys_len; key_index++) {
            String key = csv_data.get(i).keySet().toArray()[key_index].toString();
            String sodavar_name = var_name + "." + key;
            String sodavar_value = csv_data.get(i).get(key).toString();

            if (this.hijacks.containsKey(sodavar_name)) {
               sodavar_value = this.hijacks.get(sodavar_name).toString();
               this.report.Log("Hijacking SodaVar: '" + sodavar_name + "' => '"
                     + sodavar_value + "'.");
            }
            this.sodaVars.put(sodavar_name, sodavar_value);
         }

         if (event.containsKey("children")) {
            this.processEvents((Events) event.get("children"), null);
         }
      }

      this.report.Log("CSV event finished.");

      this.resetThreadTime();
      return result;
   }

   private boolean waitEvent(VDDHash event) {
      boolean result = false;
      int default_timeout = 5;

      this.resetThreadTime();
      this.report.Log("Starting Wait event.");

      if (event.containsKey("timeout")) {
         Integer int_out = new Integer(event.get("timeout").toString());
         default_timeout = int_out.intValue();
         this.report.Log(String.format("Setting timeout to: %d seconds.",
               default_timeout));
      } else {
         this.report.Log(String.format("default timeout: %d seconds.",
               default_timeout));
      }

      default_timeout = default_timeout * 1000;

      try {
         this.report.Log(String.format("waiting: '%d' seconds.",
               (default_timeout / 1000)));
         int wait_seconds = default_timeout / 1000;

         for (int i = 0; i <= wait_seconds - 1; i++) {
            if (isStopped()) {
               break;
            }
            Thread.sleep(1000);
         }

         result = true;
      } catch (InterruptedException exp) {
         result = false;
      }

      this.resetThreadTime();
      this.report.Log("Wait event finished.");
      return result;
   }

   private boolean browserEvent(VDDHash event, WebElement parent) {
      boolean result = false;
      boolean assertPage = true;

      this.resetThreadTime();

      this.report.Log("Browser event starting...");

      try {
         if (event.containsKey("action")) {
            int retry = 2;

            while (retry-- > 0) {
               try {
                  switch (BrowserActions.valueOf(event.get("action").toString().toUpperCase())) {
                  case REFRESH:
                     this.report.Log("Calling Browser event refresh.");
                     this.Browser.refresh();
                     break;
                  case CLOSE:
                     this.report.Log("Calling Browser event close.");
                     this.Browser.close();
                     break;
                  case BACK:
                     this.report.Log("Calling Browser event back.");
                     this.Browser.back();
                     break;
                  case FORWARD:
                     this.report.Log("Calling Browser event forward.");
                     this.Browser.forward();
                     break;
                  }
                  break;
               } catch (org.openqa.selenium.UnhandledAlertException e) {
                  this.report.unhandledAlert(e);
                  if (retry >= 0) {
                     this.report.Log("Retrying browser action...");
                  }
               }
            }
         } else {
            int event_count = event.keySet().size() - 1;
            for (int i = 0; i <= event_count; i++) {
               String key = event.keySet().toArray()[i].toString();
               String key_id = "BROWSER_" + key;
               BrowserMethods method = null;

               if (BrowserMethods.isMember(key_id)) {
                  method = BrowserMethods.valueOf(key_id);
               } else {
                  continue;
               }

               String value = "";
               switch (method) {
               case BROWSER_url:
                  String url = event.get(key).toString();
                  url = this.replaceString(url);
                  this.report.Log(String.format("URL: '%s'", url));
                  this.Browser.url(url);
                  break;
               case BROWSER_assert:
                  value = event.get("assert").toString();
                  value = this.replaceString(value);

                  if (parent != null) {
                     result = this.Browser.Assert(value, parent);
                  } else {
                     result = this.Browser.Assert(value);
                  }

                  if (!result) {
                     String msg = String.format(
                           "Browser Assert Failed to find this in page: '%s'",
                           value);
                     this.report.Log(msg);
                  }
                  break;
               case BROWSER_assertnot:
                  value = event.get("assertnot").toString();
                  value = this.replaceString(value);

                  if (parent != null) {
                     result = this.Browser.AssertNot(value, parent);
                  } else {
                     result = this.Browser.AssertNot(value);
                  }

                  if (!result) {
                     String msg = String.format("Browser AssertNot Found text in page: '%s'", value);
                     this.report.Log(msg);
                  }
                  break;

               case BROWSER_assertPage:
                  assertPage = this.clickToBool(event.get("assertPage").toString());
                  this.report.Log(String.format("Browser assertPage => '%s'.",
                        assertPage));
                  break;
               default:
                  System.out.printf("(!)ERROR: Unknown browser method: '%s'!\n", key_id);
                  System.exit(3);
               }
            }
         }
      } catch (Exception exp) {
         this.report.ReportException(exp);
      }

      this.resetThreadTime();
      this.report.Log("Browser Event finished.");
      return result;
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
            this.processEvents((Events) event.get("children"), element);
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
            this.processEvents((Events) event.get("children"), element);
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


   /**
    * Miscellaneous events.
    *
    * This meta-event is for elements that don't have any features
    * that distinguish themselves from other elements.  Currently,
    * H1..H6, I, B, STRIKE, S, and U elements are handled here.
    *
    * @param event  the event
    * @param parent this element's parent
    * @return the matching {@link WebElement} or null
    */

   private WebElement miscEvent(VDDHash event, WebElement parent) {
      Elements type = (Elements)event.get("type");
      String eventName = type.toString().toLowerCase();
      boolean required = true;
      WebElement element = null;

      this.report.Log(eventName + " event starting.");

      if (event.containsKey("required")) {
         required = this.clickToBool(event.get("required").toString());
      }

      try {
         element = this.findElement(event, parent, required);
         if (element == null) {
            this.report.Log(eventName + " event finished.");
            return null;
         }

         this.firePlugin(element, type, PluginEvent.AFTERFOUND);
         this.checkDisabled(event, element);

         String value = element.getText();
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
            this.report.Log("Firing Javascript Event: " +
                            event.get("jscriptevent").toString());
            this.Browser.fire_event(element,
                                    event.get("jscriptevent").toString());
            Thread.sleep(1000);
            this.report.Log("Javascript event finished.");
         }

         if (event.containsKey("click") &&
             this.clickToBool(event.get("click").toString())) {
            this.firePlugin(element, type, PluginEvent.BEFORECLICK);
            element.click();
            this.firePlugin(element, type, PluginEvent.AFTERCLICK);
         }

         if (event.containsKey("children") && element != null) {
            this.processEvents((Events)event.get("children"), element);
         }
      } catch (ElementNotVisibleException exp) {
         logElementNotVisible(required, event);
      } catch (Exception exp) {
         this.report.ReportException(exp);
         element = null;
      }

      this.report.Log(eventName + " event finished.");
      return element;
   }


   private WebElement findElement(VDDHash event, WebElement parent,
         boolean required) {
      WebElement element = null;
      By by = null;
      boolean href = false;
      boolean alt = false;
      boolean text = false;
      boolean value = false;
      boolean exists = true;
      String how = "";
      String what = "";
      int index = -1;
      int timeout = 5;
      String msg = "";

      if (event.containsKey("exists")) {
         exists = this.clickToBool(replaceString((String)event.get("exists")));
      }

      if (event.containsKey("timeout")) {
         timeout = Integer.valueOf(event.get("timeout").toString());
         msg = String.format("Resetting default element finding timeout to: '%d' seconds.",
               timeout);
         this.report.Log(msg);
      }

      if (event.containsKey("index")) {
         String inx = event.get("index").toString();
         inx = this.replaceString(inx);

         if (!Utils.isInt(inx)) {
            msg = String.format("Error: index value: '%s' is not an integer!",inx);
            this.report.ReportError(msg);
            return null;
         }

         index = Integer.valueOf(inx).intValue();
      }

      this.resetThreadTime();

      try {
         msg = "";
         how = event.get("how").toString();
         what = event.get(how).toString();
         what = this.replaceString(what);
         String dowhat = event.get("do").toString();

         if (index > -1) {
            msg = String.format("Trying to find page element '%s' by: '%s' => '%s' index => '%s'.",
                        dowhat, how, what, index);
         } else {
            msg = String.format(
                  "Trying to find page element '%s' by: '%s' => '%s'.", dowhat,
                  how, what);
         }
         this.report.Log(msg);

         if (how.matches("class") && what.matches(".*\\s+.*")) {
            String elem_type = event.get("do").toString();
            String old_how = how;
            how = "css";
            String css_sel = String.format("%s[%s=\"%s\"]", elem_type, old_how,
                  what);
            what = css_sel;
         }

         if (how.contains("index")) {
            how = "tagname";
            what = event.get("do").toString();

            if (what.contains("link")) {
               what = "a";
            }

            if (what.contains("image")) {
               what = "img";
            }
         }

         switch (ElementHow.valueOf(how.toUpperCase())) {
         case ID:
            by = By.id(what);
            break;
         case CLASS:
            by = By.className(what);
            break;
         case CSS:
            by = By.cssSelector(what);
            break;
         case LINK:
            by = By.linkText(what);
            break;
         case HREF:
            by = By.tagName("a");
            href = true;
            break;
         case TEXT:
            text = true;
            break;
         case NAME:
            by = By.name(what);
            break;
         case PARLINK:
            by = By.partialLinkText(what);
            break;
         case TAGNAME:
            by = By.tagName(what);
            break;
         case XPATH:
            by = By.xpath(what);
            break;
         case VALUE:
            value = true;
            break;
         case ALT:
            alt = true;
            break;
         default:
            /* not reached */
            this.report.ReportError(String.format("Error: findElement, unknown how: '%s'!\n", how));
            System.exit(4);
            break;
         }

         if (href) {
            element = this.findElementByHref(event.get("href").toString(),
                                             parent);
         } else if (alt) {
            element = this.findElementByAlt(event.get("alt").toString(),
                                            parent);
         } else if (value) {
            element = this.slowFindElement((String)event.get("html_tag"),
                                           (String)event.get("html_type"),
                                           what, parent, index);
         } else if (text) {
            element = this.findElementByText((String)event.get("html_tag"),
                                             (String)event.get("text"),
                                             parent, index);
         } else {
            List<WebElement> elements;

            if (parent == null) {
               elements = this.Browser.findElements(by, timeout);
            } else {
               elements = parent.findElements(by);
            }
            elements = filterElements(elements, event);
            index = (index < 0) ? 0 : index;
            if (index >= elements.size()) {
               String m = ((index > 0) ?
                           "No elements found." :
                           String.format("Index (%d) out of bounds.", index));
               throw new NoSuchElementException(m);
            }
            element = elements.get(index);
         }
      } catch (NoSuchElementException exp) {
         if (required && exists) {
            this.report.ReportError("Failed to find element! " + exp);
            element = null;
         }
      } catch (Exception exp) {
         this.report.ReportException(exp);
         element = null;
      }

      this.resetThreadTime();

      if (element == null && exists == true) {
         if (required) {
            msg = String.format("Failed to find element: '%s' => '%s'", how,
                  what);
            this.report.ReportError(msg);
         } else {
            msg = String.format("Failed to find element, but required => 'false' : '%s' => '%s'",
                        how, what);
            this.report.Log(msg);
         }
      } else if (element != null && exists != true) {
         this.report.ReportError("Found element with exist => 'false'!");
      } else if (element == null && exists != true) {
         this.report.Log("Did not find element as exist => 'false'.");
      } else {
         this.report.Log("Found element.");
      }

      return element;
   }

   /**
    * Determine whether tag is found in filter
    *
    * @param tag    HTML tag to search for
    * @param filter list of acceptable HTML tags
    * @return true if tag is in filter, false otherwise
    */

   private boolean checkMatch(String tag, String filter) {
      if (filter == null) {
         return true;
      } else if (tag == null) {
         return false;
      }
      String filters[] = filter.split("\\|");
      for (int k = 0; k < filters.length; k++) {
         if (tag.equals(filters[k])) {
            return true;
         }
      }
      return false;
   }

   /**
    * Filter elements by HTML element type
    *
    * Take a list of {@link WebElement}s found using the event selection
    * criteria and filter through by HTML tag type.
    *
    * @param elements  element list returned based on event
    * @param event     current event
    * @return list of elements filtered by HTML tag
    */

   private List<WebElement> filterElements(List<WebElement> elements,
                                           VDDHash event) {
      ArrayList<WebElement> filtered =
         new ArrayList<WebElement>(elements.size());
      String html_tag = (String)event.get("html_tag");
      String html_type = (String)event.get("html_type");

      for (int k = 0; k < elements.size(); k++) {
         WebElement e = elements.get(k);
         if (checkMatch(e.getTagName(), html_tag) &&
             checkMatch(e.getAttribute("type"), html_type)) {
            filtered.add(e);
         }
      }

      return filtered;
   }


   /**
    * Find an input element using its value attribute.
    *
    * This uses the Selectors API to search through the elements on
    * the page for those with matching values.
    *
    * @param tag    the element's tag name
    * @param type   the element's tag type
    * @param how    the value being searched for
    * @param parent root element of the search or null
    * @param index  index into the list of found elements
    * @return the element found or null
    */

   @SuppressWarnings("unchecked")
      private WebElement slowFindElement(String tag, String type, String how,
                                         WebElement parent, int index) {
      WebElement element = null;
      ArrayList<WebElement> list = new ArrayList<WebElement>();
      String msg = "";
      String js = "";

      msg = String.format("Looking for elements by value is very very slow!  You should never do this!");
      this.report.Log(msg);
      msg = String.format("Looking for element: '%s' => '%s'.", tag, how);
      this.report.Log(msg);

      String root = (parent == null) ? "document" : "arguments[0]";
      String[] tags = tag.split("\\|");
      String[] types = (type == null) ? new String[0] : type.split("\\|");

      if (types.length == 0 || this.Browser instanceof IE) {
         /*
          * IE only supports the Selectors API with versions 8 and
          * greater, and then only when rendering the document in
          * standards mode.  Given the special tags required for IE to
          * enter standards mode, it is safe to assume that any page
          * VDD is testing will probably be in quirks mode, making the
          * Selectors API unavailable.  So in the case of IE, look for
          * matching elements by iterating through all elements.
          * Slow, but it'll at least work.
          */

         String tagFinder = "";
         for (int k = 0; k < tags.length; k++) {
            tagFinder += String.format("finder(%s.getElementsByTagName('%s'))%s",
                                       root, tags[k],
                                       (k == tags.length - 1) ? "" : ",");
         }

         String typeFilter = "";
         for (int k = 0; k < types.length; k++) {
            typeFilter += String.format("args[k].type == '%s'%s", types[k],
                                        (k == types.length - 1) ? "" : " || ");
         }
         if (typeFilter.length() == 0) {
            typeFilter = "true"; // Accept all types if none was specified
         }

         js = String.format("function finder(args) {\n" +
                            "   var found = [];\n" +
                            "   for (var k = 0; k < args.length; k++) {\n" +
                            "      if (args[k].value == '%s' &&\n" +
                            "          (%s)) {\n" +
                            "         found.push(args[k]);\n" +
                            "      }\n" +
                            "   }\n" +
                            "   return found;\n" +
                            "}\n" +
                            "return [].concat(%s);\n",
                            how, typeFilter, tagFinder);
      } else {
         /*
          * Not IE, use the Selectors API.
          */
         String selectors = "";

         for (int i = 0; i < tags.length; i++) {
            for (int j = 0; j < types.length; j++) {
               selectors += String.format("%s[type=\"%s\"][value=\"%s\"]%s",
                                          tags[i], types[j], how,
                                          ((i == tags.length - 1) &&
                                           (j == types.length - 1)) ?
                                          "" : ",");
            }
         }

         js = String.format("return %s.querySelectorAll('%s', true);",
                            root, selectors);
      }

      list = (ArrayList<WebElement>)this.Browser.executeJS(js, parent);

      if (list.size() > 0) {
         element = list.get(index < 0 ? 0 : index);
      }

      return element;
   }

   private WebElement findElementByHref(String href, WebElement parent) {
      WebElement element = null;
      List<WebElement> list = null;

      href = this.replaceString(href);

      if (parent != null) {
         list = parent.findElements(By.tagName("a"));
      } else {
         list = this.Browser.getDriver().findElements(By.tagName("a"));
      }

      int len = list.size() - 1;
      for (int i = 0; i <= len; i++) {
         String value = list.get(i).getAttribute("href");

         if (value != null && href.compareTo(value) == 0) {
            element = list.get(i);
            break;
         }
      }

      return element;
   }


   /**
    * Find an image element based on its alt text.
    *
    * This routine returns the first image encountered with matching
    * alt text.
    *
    * @param alt     alt text to search for
    * @param parent  parent element or null
    * @return matching {@link WebElement} or null
    */

   private WebElement findElementByAlt(String alt, WebElement parent) {
      By by = By.tagName("img");
      List<WebElement> elementList = null;
      alt = this.replaceString(alt);

      if (parent != null) {
         elementList = parent.findElements(by);
      } else {
         elementList = this.Browser.getDriver().findElements(by);
      }

      for (WebElement element: elementList) {
         String elementAlt = element.getAttribute("alt");
         if (elementAlt != null && elementAlt.equals(alt)) {
            return element;
         }
      }

      return null;
   }


   /**
    * Find an element by its innerText attribute.
    *
    * @param tag   the HTML tag of the element
    * @param text  the text to search for
    * @return matching {@link WebElement} or null
    */

   private WebElement findElementByText(String tag, String text,
                                        WebElement parent, int index) {
      By by = null;
      List<WebElement> elements = null;
      text = this.replaceString(text);

      if (tag.equals("a")) {
         by = By.linkText(text);
      } else {
         by = By.tagName(tag);
      }

      if (parent == null) {
         elements = this.Browser.getDriver().findElements(by);
      } else {
         elements = parent.findElements(by);
      }

      if (!tag.equals("a")) {
         ArrayList<WebElement> discard = new ArrayList<WebElement>();

         for (WebElement e: elements) {
            if (!e.getText().contains(text)) {
               discard.add(e); // Can't delete elements while iterating
            }
         }

         elements.removeAll(discard);
      }

      index = (index < 0) ? 0 : index; // -1 is default value
      if (index >= elements.size()) {
         return null;
      }

      return elements.get(index);
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
      data.setSodaVars(this.sodaVars);
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
      data.setSodaVars(this.sodaVars);
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
      data.setSodaVars(this.sodaVars);
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
    * Clear a text, password, or email input or a textfield element.
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

   private boolean putsEvent(VDDHash event) {
      boolean result = false;
      String msg = "";

      if (event.containsKey("txt")) {
         msg = this.replaceString(event.get("txt").toString());
      } else {
         msg = this.replaceString(event.get("text").toString());
      }

      this.resetThreadTime();
      this.report.Log(msg);
      result = true;
      this.resetThreadTime();
      return result;
   }

   private void checkDisabled(VDDHash event, WebElement element) {
      String value = null;

      if (!event.containsKey("disabled")) {
         return;
      }

      value = event.get("disabled").toString();
      value = this.replaceString(value);

      try {
         Utils.isEnabled(element, this.report, Boolean.valueOf(value));
      } catch (Exception exp) {
         this.report.ReportException(exp);
      }
   }

   private void handleVars(String value, VDDHash event) {
      if (event.containsKey("var")) {
         String name = event.get("var").toString();
         VDDHash tmp = new VDDHash();
         tmp.put("set", value);
         tmp.put("var", name);
         this.varEvent(tmp);
      }
   }
}
