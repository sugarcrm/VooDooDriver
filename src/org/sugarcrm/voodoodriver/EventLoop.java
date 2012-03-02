/*
Copyright 2011 SugarCRM Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
Please see the License for the specific language governing permissions and
limitations under the License.
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
 * This is the heart of all of VooDooDriver.  This class handles executing all of the SODA
 * language commands in the web browser.
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
   private Events plugIns = null;
   private VDDHash JavaPlugings = null;
   private VDDHash ElementStore = null;
   private VDDPluginsHash loadedPlugins = null;
   private String currentHWnd = null;
   private int attachTimeout = 0;
   private String csvOverrideFile = null;
   private VDDHash whitelist = null;

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
    */
   public EventLoop(Browser browser, Events events,
         Reporter reporter, VDDHash gvars, VDDHash hijacks,
         VDDHash oldvars, Events plugins) {
      testEvents = events;
      this.Browser = browser;
      this.report = reporter;
      this.hijacks = hijacks;

      this.whitelist = new VDDHash();
      this.JavaPlugings = new VDDHash();
      this.loadedPlugins = new VDDPluginsHash();

      if (oldvars != null) {
         sodaVars = oldvars;
      } else {
         sodaVars = new VDDHash();
      }

      this.ElementStore = new VDDHash();
      this.plugIns = plugins;
      if (this.plugIns == null) {
         this.plugIns = new Events();
      }

      if (gvars != null) {
         int len = gvars.keySet().size();

         for (int i = 0; i <= len - 1; i++) {
            String key = gvars.keySet().toArray()[i].toString();
            String value = gvars.get(key).toString();
            System.out.printf("--)'%s' => '%s'\n", key, value);

            this.sodaVars.put(key, value);
         }
      }

      this.loadJavaEventPlugins();
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
      boolean exists = false;
      int i = 0;
      Set<String> windows = this.Browser.getDriver().getWindowHandles();

      for (i = 0; i <= windows.size() - 1; i++) {
         String tmp = windows.toArray()[i].toString();

         if (hwnd.equals(tmp)) {
            exists = true;
            break;
         }
      }

      return exists;
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
    * Loads and stores all of the java Event Plugin classes.
    *
    */
   private void loadJavaEventPlugins() {
      int len = this.plugIns.size() - 1;

      for (int i = 0; i <= len; i++) {
         VDDHash tmp = this.plugIns.get(i);
         if (!tmp.containsKey("classname")) {
            continue;
         }

         String classname = tmp.get("classname").toString();
         String classfile = tmp.get("classfile").toString();
         VDDClassLoader loader = new VDDClassLoader(ClassLoader.getSystemClassLoader());

         try {
            Class<VDDPluginInterface> tmp_class = loader.loadClass(classname,
                  classfile);
            this.loadedPlugins.put(classname, tmp_class);
         } catch (Exception exp) {
            this.report.ReportException(exp);
         }
      }
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

      while ((!this.threadStop) && (i <= event_count)) {
         handleSingleEvent(this.testEvents.get(i), null);
         i += 1;
      }
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
      default:
         System.out.printf("(!)Unknown command: '%s'!\n", type.toString());
         System.exit(1);
      }

      this.firePlugin(null, type, PluginEventType.AFTEREVENT);
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
         } else {
            this.report.Log("Switching back to default frame.");
            this.Browser.getDriver().switchTo().defaultContent();
         }
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
         SodaUtils.takeScreenShot(filename, this.report);
      }

      this.report.Log("Screenshot event finished.");
      return result;
   }

   private boolean alertEvent(VDDHash event) {
      boolean result = false;
      boolean alert_var = false;
      boolean exists = true;
      boolean user_exists_true = false;
      String msg = "";
      String alert_text = "";

      this.report.Log("Alert event starting.");

      if (!event.containsKey("alert") && !event.containsKey("exists")) {
         result = false;
         this.report.ReportError("Alert command is missing alert=\"true\\false\" attribute!");
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

      try {
         Alert alert = this.Browser.getDriver().switchTo().alert();
         alert_text = alert.getText();
         msg = String.format("Found Alert dialog: '%s'.", alert_text);
         this.report.Log(msg);
         if (alert_var) {
            this.report.Log("Alert is being Accepted.");
            alert.accept();
         } else {
            this.report.Log("Alert is being Dismissed.");
            alert.dismiss();
         }

         this.Browser.getDriver().switchTo().defaultContent();
         Thread.currentThread();
         Thread.sleep(1000);

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

         if (user_exists_true) {
            this.report.Assert("Alert dialog does eixts.", true, true);
         }

         handleVars(alert_text, event);

         this.firePlugin(null, Elements.ALERT,
               PluginEventType.AFTERDIALOGCLOSED);

         result = true;
      } catch (NoAlertPresentException exp) {
         if (!exists) {
            msg = String.format("Alert dialog does next exist as expected.",
                  exists);
            this.report.Assert(msg, false, false);
            result = true;
         } else {
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

   private boolean javapluginEvent(VDDHash event, WebElement parent) {
      boolean result = false;
      String[] args = null;
      String classname = "";
      String classfile = "";
      VDDHash classdata;
      String msg = "";
      int err = 0;

      this.report.Log("Javaplugin event started.");

      if (!event.containsKey("classname")) {
         this.report.ReportError("Javaplugin event missing attribute: 'classname'!");
         this.report.Log("Javaplugin event finished.");
         return false;
      }

      classname = event.get("classname").toString();
      if (!this.JavaPlugings.containsKey(classname)) {
         msg = String.format("Faild to find loaded plugin: '%s'!", classname);
         this.report.ReportError(msg);
         this.report.Log("Javaplugin event finished.");
         return false;
      }

      classdata = (VDDHash) this.JavaPlugings.get(classname);
      classfile = classdata.get("file").toString();

      msg = String.format("Loading classname: '%s'.", classname);
      this.report.Log(msg);
      if (!this.JavaPlugings.containsKey(classname)) {
         msg = String.format("Error failed to find a loaded plugin with classname: '%s'!",
               classname);
         this.report.ReportError(msg);
         this.report.Log("Javaplugin event finished.");
         return false;
      }

      if (event.containsKey("args")) {
         args = (String[]) event.get("args");

         if (args != null) {
            int len = args.length - 1;

            for (int i = 0; i <= len; i++) {
               args[i] = this.replaceString(args[i]);
            }
         }
      }

      try {
         VDDClassLoader loader = new VDDClassLoader(
               ClassLoader.getSystemClassLoader());
         Class<VDDPluginInterface> tmp_class = loader.loadClass(classname,
               classfile);
         VDDPluginInterface inner = tmp_class.newInstance();
         this.report.Log("Executing plugin now.");
         err = inner.execute(args, this.Browser, parent);
         if (err != 0) {
            msg = String.format("Javaplugin returned a non-zero value: '%d'!",
                  err);
            this.report.ReportError(msg);
            result = false;
         }
         this.report.Log("Plugin finished executing.");
      } catch (Exception exp) {
         this.report.ReportException(exp);
         result = false;
      }

      this.report.Log("Javaplugin event finished.");
      return result;
   }

   private boolean pluginloaderEvent(VDDHash event) {
      boolean result = false;
      String filename = "";
      String classname = "";
      File tmp = null;
      VDDHash data = null;

      this.report.Log("PluginLoader event started.");

      if (!event.containsKey("file")) {
         this.report
               .ReportError("Error: Missing 'file' attribute for pluginloader command!");
         this.report.Log("PluginLoader event finished.");
         return false;
      }

      if (!event.containsKey("classname")) {
         this.report.ReportError("Error: Missing 'classname' attribute for pluginloader command!");
         this.report.Log("PluginLoader event finished.");
         return false;
      }

      filename = event.get("file").toString();
      filename = this.replaceString(filename);
      classname = event.get("classname").toString();
      classname = this.replaceString(classname);

      tmp = new File(filename);
      if (!tmp.exists()) {
         String msg = String.format("Error: failed to find file: '%s'!", filename);
         this.report.ReportError(msg);
         return false;
      }

      // load & store plugin //
      if (!this.loadedPlugins.containsKey(classname)) {
         try {
            System.out.printf("Loading class into memory: '%s'!\n", classname);
            VDDClassLoader loader = new VDDClassLoader(
                  ClassLoader.getSystemClassLoader());
            Class<VDDPluginInterface> tmp_class = loader.loadClass(classname,
                  filename);
            this.loadedPlugins.put(classname, tmp_class);
         } catch (ClassNotFoundException exp) {
            this.report.ReportException(exp);
            result = false;
         }
      }

      data = new VDDHash();
      data.put("file", filename);
      data.put("classname", classname);
      data.put("enabled", true);
      this.JavaPlugings.put(classname, data);
      this.report.Log("PluginLoader event finished.");

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

         if (event.containsKey("click")) {
            click = this.clickToBool(event.get("click").toString());
         }

         if (click) {
            this.report.Log("UL click started.");
            this.firePlugin(element, Elements.UL,
                  PluginEventType.BEFORECLICK);
            element.click();
            this.firePlugin(element, Elements.UL,
                  PluginEventType.AFTERCLICK);
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
                  PluginEventType.BEFORECLICK);
            element.click();
            this.firePlugin(element, Elements.AREA,
                  PluginEventType.AFTERCLICK);
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
                  PluginEventType.BEFORECLICK);
            element.click();
            this.firePlugin(element, Elements.MAP,
                  PluginEventType.AFTERCLICK);
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
                  PluginEventType.BEFORECLICK);
            element.click();
            this.firePlugin(element, Elements.OL,
                  PluginEventType.AFTERCLICK);
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

         if (click) {
            this.report.Log("Image click started.");
            this.firePlugin(element, Elements.IMAGE,
                  PluginEventType.BEFORECLICK);
            element.click();
            this.firePlugin(element, Elements.IMAGE,
                  PluginEventType.AFTERCLICK);
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
               PluginEventType.AFTERFOUND);

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
                  PluginEventType.BEFORECLICK);
            element.click();
            this.firePlugin(element, Elements.LI,
                  PluginEventType.AFTERCLICK);
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
                  PluginEventType.BEFORECLICK);
            element.click();
            this.firePlugin(element, Elements.TR,
                  PluginEventType.AFTERCLICK);
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
                  PluginEventType.BEFORECLICK);
            element.click();
            this.firePlugin(element, Elements.TD,
                  PluginEventType.AFTERCLICK);
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
               PluginEventType.AFTERFOUND);

         this.checkDisabled(event, element);

         if (event.containsKey("click")) {
            click = this.clickToBool(event.get("click").toString());
         }

         String value = element.getText();
         handleVars(value, event);

         if (click) {
            this.firePlugin(element, Elements.SPAN,
                  PluginEventType.BEFORECLICK);
            element.click();
            this.firePlugin(element, Elements.SPAN,
                  PluginEventType.AFTERCLICK);
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

         this.firePlugin(element, Elements.INPUT, PluginEventType.AFTERFOUND);

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
               PluginEventType.AFTERFOUND);

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
                  PluginEventType.BEFORECLICK);
            this.report.Log("Clicking Element.");
            element.click();
            this.report.Log("Click finished.");
            this.firePlugin(element, Elements.RADIO,
                  PluginEventType.AFTERCLICK);
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

   private WebElement selectEvent(VDDHash event, WebElement parent) {
      boolean required = true;
      WebElement element = null;
      String setvalue = null;
      String msg = "";
      boolean do_assert = false;
      boolean assert_direction = true;
      boolean included = false;
      boolean included_direction = true;
      boolean real = false;
      boolean click = false;
      String assert_value = "";
      String included_value = "";

      this.report.Log("Select event Started.");

      if (event.containsKey("required")) {
         required = this.clickToBool(event.get("required").toString());
      }

      try {
         element = this.findElement(event, parent, required);
         if (element != null) {
            Select sel = new Select(element);
            this.firePlugin(element, Elements.SELECT,
                  PluginEventType.AFTERFOUND);

            this.checkDisabled(event, element);

            if (event.containsKey("set")) {
               setvalue = event.get("set").toString();
               setvalue = this.replaceString(setvalue);
            }

            if (event.containsKey("setreal")) {
               setvalue = event.get("setreal").toString();
               setvalue = this.replaceString(setvalue);
               real = true;
            }

            if (setvalue != null) {
               if (real) {
                  sel.selectByValue(setvalue);
                  this.report.Log("Setting option by value: '" +
                                  setvalue + "'.");

               } else {
                  sel.selectByVisibleText(setvalue);
                  this.report.Log("Setting option by visible text: '" +
                                  setvalue + "'.");
               }

               try {
                  /*
                   * Selecting a value has the potential to refresh
                   * the page.  Check for a stale element and refresh
                   * it if needed (Bug 49533).
                   */
                  element.isDisplayed();
               } catch (StaleElementReferenceException e) {
                  this.report.Log("Page updated. Refreshing stale select element.");
                  element = this.findElement(event, null, required);
                  sel = new Select(element);
               }

               this.firePlugin(element, Elements.SELECT,
                     PluginEventType.AFTERSET);
            }

            if (event.containsKey("assert")) {
               do_assert = true;
               assert_direction = true;
               assert_value = event.get("assert").toString();
               assert_value = this.replaceString(assert_value);
            }

            if (event.containsKey("assertnot")) {
               do_assert = true;
               assert_direction = false;
               assert_value = event.get("assertnot").toString();
               assert_value = this.replaceString(assert_value);
            }

            if (do_assert) {
               int found = -1;
               List<WebElement> options = sel.getOptions();
               int opt_len = options.size() - 1;
               String opt_val = "";

               for (int i = 0; i <= opt_len; i++) {
                  opt_val = options.get(i).getText();
                  if (opt_val.contains(assert_value)) {
                     found = i;
                     break;
                  }
               }

               if (found < 0) {
                  msg = String.format("Failed to find select option: '%s'!",
                        assert_value);
                  this.report.ReportError(msg);
               } else {
                  if (assert_direction) {
                     if (options.get(found).isSelected()) {
                        msg = String.format("Select option: '%s' is selected.",
                              assert_value);
                        this.report.Assert(msg, true, true);
                     } else {
                        msg = String.format(
                              "Select option: '%s' is not selected.",
                              assert_value);
                        this.report.Assert(msg, false, true);
                     }
                  } else {
                     if (options.get(found).isSelected()) {
                        msg = String.format("Select option: '%s' is selected.",
                              assert_value);
                        this.report.Assert(msg, false, true);
                     } else {
                        msg = String.format(
                              "Select option: '%s' is not selected.",
                              assert_value);
                        this.report.Assert(msg, true, true);
                     }
                  }
               }
            }

            if (event.containsKey("click")) {
               click = this.clickToBool(event.get("click").toString());
            }

            if (click) {
               this.firePlugin(element, Elements.FORM,
                               PluginEventType.BEFORECLICK);
               element.click();
               this.firePlugin(element, Elements.FORM,
                               PluginEventType.AFTERCLICK);
            }

            if (event.containsKey("jscriptevent")) {
               this.report.Log("Firing Javascript Event: " +
                               event.get("jscriptevent").toString());
               this.Browser.fire_event(element,
                                       event.get("jscriptevent").toString());
               Thread.sleep(1000);
               this.report.Log("Javascript event finished.");
            }

            if (event.containsKey("included")) {
               included = true;
               included_direction = true;
               included_value = event.get("included").toString();
               included_value = this.replaceString(included_value);
            }

            if (event.containsKey("notincluded")) {
               included = true;
               included_direction = false;
               included_value = event.get("notincluded").toString();
               included_value = this.replaceString(included_value);
            }

            if (included) {
               sel = new Select(element);
               List<WebElement> options = sel.getOptions();
               int sel_len = options.size() - 1;
               boolean found = false;

               for (int i = 0; i <= sel_len; i++) {
                  String opt_value = options.get(i).getText();
                  if (opt_value.contains(included_value)) {
                     found = true;
                     break;
                  }
               }

               if (included_direction) {
                  if (found) {
                     msg = String.format("Found Select list option: '%s'.",
                           included_value);
                     this.report.Assert(msg, true, true);
                  } else {
                     msg = String.format("Failed to find Select list option: '%s'.",
                           included_value);
                     this.report.Assert(msg, false, true);
                  }
               } else {
                  if (found) {
                     msg = String.format("Found Select list option: '%s', when it wasn't expected!",
                           included_value);
                     this.report.Assert(msg, false, false);
                  } else {
                     msg = String.format("Failed to find Select list option: '%s', as expected.",
                           included_value);
                     this.report.Assert(msg, true, true);
                  }
               }

               if (event.containsKey("children") && element != null) {
                  this.processEvents((Events)event.get("children"), element);
               }
            }


            String value = element.getAttribute("value");
            handleVars(value, event);

         }

      } catch (ElementNotVisibleException exp) {
         logElementNotVisible(required, event);
      } catch (Exception exp) {
         this.report.ReportException(exp);
      }

      this.report.Log("Select event finished.");
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
               PluginEventType.AFTERFOUND);

         this.checkDisabled(event, element);

         if (event.containsKey("click")) {
            click = this.clickToBool(event.get("click").toString());
         }

         if (click) {
            this.firePlugin(element, Elements.FORM,
                  PluginEventType.BEFORECLICK);
            element.click();
            this.firePlugin(element, Elements.FORM,
                  PluginEventType.AFTERCLICK);
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
               PluginEventType.AFTERFOUND);

         this.checkDisabled(event, element);

         if (event.containsKey("click")) {
            click = this.clickToBool(event.get("click").toString());
         }

         if (click) {
            this.firePlugin(element, Elements.TABLE,
                  PluginEventType.BEFORECLICK);
            element.click();
            this.firePlugin(element, Elements.TABLE,
                  PluginEventType.AFTERCLICK);
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
      boolean is_REGEX = false;
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
            if (!SodaUtils.isInt(tmp_index)) {
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

         if (this.report.isRegex(finder)) {
            is_REGEX = true;
         }

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

               if (!is_REGEX) {
                  if (!use_URL) {
                     if (tmp_title.equals(finder)) {
                        found_handle = tmp_handle;
                        this.report.Log(String.format(
                              "Found Window Title '%s'", finder));
                        break;
                     }
                  } else {
                     if (tmp_url.equals(finder)) {
                        found_handle = tmp_handle;
                        this.report.Log(String.format("Found Window URL '%s'",
                              finder));
                        break;
                     }
                  }
               } else {
                  if (!use_URL) {
                     Pattern p = Pattern.compile(finder);
                     Matcher m = p.matcher(tmp_title);
                     if (m.find()) {
                        found_handle = tmp_handle;
                        this.report.Log(String.format("Found Window Title '%s'", finder));
                        break;
                     }
                  } else {
                     Pattern p = Pattern.compile(finder);
                     Matcher m = p.matcher(tmp_url);
                     if (m.find()) {
                        found_handle = tmp_handle;
                        this.report.Log(String.format("Found Window URL '%s'",
                              finder));
                        break;
                     }
                  }
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

         this.Browser.setBrowserState(false);
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
         exp.printStackTrace();
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
               PluginEventType.AFTERFOUND);

         this.checkDisabled(event, element);

         if (event.containsKey("click")) {
            click = this.clickToBool(event.get("click").toString());
         }

         if (click) {
            this.firePlugin(element, Elements.DIV,
                  PluginEventType.BEFORECLICK);
            element.click();
            this.firePlugin(element, Elements.DIV,
                  PluginEventType.AFTERCLICK);
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
      SodaXML xml = null;
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

         xml = new SodaXML(testfile, null);
         newEvents = xml.getEvents();
         this.processEvents(newEvents, null);

      } catch (Exception exp) {
         exp.printStackTrace();
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

         this.firePlugin(element, Elements.CHECKBOX, PluginEventType.AFTERFOUND);

         this.checkDisabled(event, element);

         if (event.containsKey("click")) {
            click = this.clickToBool(event.get("click").toString());
            if (click) {
               this.firePlugin(element, Elements.CHECKBOX, PluginEventType.BEFORECLICK);
               element.click();
               this.firePlugin(element, Elements.CHECKBOX, PluginEventType.AFTERCLICK);
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
               this.firePlugin(element, Elements.CHECKBOX, PluginEventType.AFTERCLICK);
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
               PluginEventType.AFTERFOUND);

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
                            PluginEventType.BEFORECLICK);
            element.click();
            this.firePlugin(element, Elements.LINK,
                  PluginEventType.AFTERCLICK);
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
         msg = String.format("Setting CSV file override to file: '%s'.", this.csvOverrideFile);
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
      VDDBrowserActions browser_action = null;

      this.resetThreadTime();

      this.report.Log("Browser event starting...");

      try {
         if (event.containsKey("action")) {
            browser_action = VDDBrowserActions.valueOf(event.get("action").toString().toUpperCase());
            switch (browser_action) {
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
                  this.report.Log(String.format("Borwser assertPage => '%s'.",
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
                         PluginEventType.AFTERFOUND);

         this.checkDisabled(event, element); //??

         handleVars(element.getText(), event);

         if (event.containsKey("click") &&
             this.clickToBool(event.get("click").toString())) {
            this.firePlugin(element, Elements.THEAD,
                            PluginEventType.BEFORECLICK);
            element.click();
            this.firePlugin(element, Elements.THEAD,
                            PluginEventType.AFTERCLICK);
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
                         PluginEventType.AFTERFOUND);

         this.checkDisabled(event, element); //??

         handleVars(element.getText(), event);

         if (event.containsKey("click") &&
             this.clickToBool(event.get("click").toString())) {
            this.firePlugin(element, Elements.TBODY,
                            PluginEventType.BEFORECLICK);
            element.click();
            this.firePlugin(element, Elements.TBODY,
                            PluginEventType.AFTERCLICK);
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

   private WebElement findElement(VDDHash event, WebElement parent,
         boolean required) {
      WebElement element = null;
      By by = null;
      boolean href = false;
      boolean value = false;
      boolean exists = true;
      String how = "";
      String what = "";
      int index = -1;
      int timeout = 5;
      String msg = "";

      if (event.containsKey("exists")) {
         exists = this.clickToBool(event.get("exists").toString());
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

         if (!SodaUtils.isInt(inx)) {
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
            by = By.linkText(what);
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
         default:
            this.report.ReportError(String.format("Error: findElement, unknown how: '%s'!\n", how));
            System.exit(4);
            break;
         }

         if (href) {
            element = this.findElementByHref(event.get("href").toString(),
                  parent);
         } else if (value) {
            element = this.slowFindElement(event.get("do").toString(), what,
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

   @SuppressWarnings("unchecked")
   private WebElement slowFindElement(String ele_type, String how,
         WebElement parent, int index) {
      WebElement element = null;
      ArrayList<WebElement> list = new ArrayList<WebElement>();
      String msg = "";
      String js = "";

      msg = String.format("Looking for elements by value is very very slow!  You should never do this!");
      this.report.Log(msg);
      msg = String.format("Looking for element: '%s' => '%s'.", ele_type, how);
      this.report.Log(msg);

      if (how.contains("OK")) {
         System.out.print("");
      }

      if (ele_type.contains("button")) {
         js = String.format(
               "querySelectorAll('input[type=\"button\"][value=\"%s\"],button[value=\"%s\"],"
               + "input[type=\"submit\"][value=\"%s\"], input[type=\"reset\"][vaue=\"%s\"]', true);",
               how, how, how, how);
      } else {
         js = String.format("querySelectorAll('input[type=\"%s\"][value=\"%s\"],%s[value=\"%s\"]', true)",
               ele_type, how, ele_type, how);
      }

      if (parent == null) {
         js = "return document." + js;
         list = (ArrayList<WebElement>) this.Browser.executeJS(js, null);
      } else {
         js = "return arguments[0]." + js;
         list = (ArrayList<WebElement>) this.Browser.executeJS(js, parent);
      }

      if (index < 0) {
         element = list.get(0);
      } else {
         element = list.get(index);
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

   private boolean firePlugin(WebElement element, Elements type, PluginEventType eventType) {
      boolean result = false;
      int len = 0;
      String js = "var CONTROL = arguments[0];\n\n";
      int index = -1;

      if (this.plugIns == null) {
         return true;
      }

      len = this.plugIns.size() - 1;

      for (int i = 0; i <= len; i++) {
         VDDHash tmp = this.plugIns.get(i);

         if (tmp.get("control").toString()
               .contains((type.toString().toLowerCase()))) {
            if (tmp.get("event").toString()
                  .contains(eventType.toString().toLowerCase())) {

               if (tmp.containsKey("jsfile")) {
                  String jsfile = (String) tmp.get("jsfile");
                  jsfile = FilenameUtils.separatorsToSystem(jsfile);
                  String user_js = SodaUtils.FileToStr(jsfile);
                  if (user_js != null) {
                     js = js.concat(user_js);
                     result = true;
                     break;
                  } else {
                     String msg = String.format("Failed trying to read plugin source file: '%s'!",
                           (String) tmp.get("jsfile"));
                     this.report.ReportError(msg);
                     result = false;
                  }
               } else {
                  index = i;
                  break;
               }
            }
         }
      }

      if (result && index < 0) {
         this.report.Log("Plugin Event Started.");
         Object res = this.Browser.executeJS(js, element);
         int tmp = Integer.valueOf(res.toString());
         if (tmp == 0) {
            result = true;
         } else {
            result = false;
            String msg = String.format("Plugin Event Failed with return value: '%s'!", tmp);
            this.report.ReportError(msg);
         }

         this.report.Log("Plugin Event Finished.");
      }

      if (index > -1) {
         VDDHash data = this.plugIns.get(index);
         String classname = data.get("classname").toString();
         String msg = "";
         Class<VDDPluginInterface> tmp_class = this.loadedPlugins.get(classname);
         msg = String.format("");

         try {
            int err = 0;
            VDDPluginInterface inst = tmp_class.newInstance();

            String tmp_hwnd = this.getCurrentHWND();
            if (this.windowExists(tmp_hwnd)) {
               msg = String.format("Starting plugin: '%s'.", classname);
               this.report.Log(msg);

               err = inst.execute(null, this.Browser, element);
               if (err != 0) {
                  msg = String.format("Plugin Classname: '%s' failed returning error code: '%d'!",
                              classname, err);
                  this.report.ReportError(msg);
               } else {
                  msg = String.format("Plugin: '%s' finished.", classname);
                  this.report.Log(msg);
               }
            } else {
               msg = "Found the browser window has been closed, skipping executing plugin.";
               this.report.Log(msg);
            }

         } catch (Exception exp) {
            this.report.ReportException(exp);
            result = false;
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
               PluginEventType.AFTERFOUND);

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
                  PluginEventType.BEFORECLICK);
            this.report.Log("Clicking button.");
            element.click();
            this.report.Log("Finished clicking button.");
            this.firePlugin(element, Elements.BUTTON,
                  PluginEventType.AFTERCLICK);
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
               PluginEventType.AFTERFOUND);

         this.checkDisabled(event, element);

         if (event.containsKey("clear")) {
            if (this.clickToBool(event.get("clear").toString())) {
               this.report.Log("Clearing textarea.");
               element.clear();
            }
         }

         if (event.containsKey("set")) {
            value = event.get("set").toString();
            value = this.replaceString(value);
            this.report.Log(String.format("Setting Value to: '%s'.", value));
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
                            PluginEventType.BEFORECLICK);
            this.report.Log("Clicking textarea.");
            element.click();
            this.report.Log("Finished clicking textarea.");
            this.firePlugin(element, Elements.TEXTAREA,
                            PluginEventType.AFTERCLICK);
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
               PluginEventType.AFTERFOUND);

         this.checkDisabled(event, element);

         if (event.containsKey("clear")) {
            if (this.clickToBool(event.get("clear").toString())) {
               this.report.Log("Clearing textfield.");
               element.clear();
            }
         }

         if (event.containsKey("set")) {
            String value = event.get("set").toString();
            value = this.replaceString(value);
            this.report.Log(String.format("Setting Value to: '%s'.", value));
            element.clear();
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

         this.firePlugin(element, Elements.PASSWORD, PluginEventType.AFTERFOUND);

         this.checkDisabled(event, element);

         if (event.containsKey("clear")) {
            if (this.clickToBool(event.get("clear").toString())) {
               this.report.Log("Clearing password field.");
               element.clear();
            }
         }

         if (event.containsKey("set")) {
            String value = event.get("set").toString();
            value = this.replaceString(value);
            this.report.Log(String.format("Setting Value to: '%s'.", value));
            element.clear();
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
         SodaUtils.isEnabled(element, this.report, Boolean.valueOf(value));
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
