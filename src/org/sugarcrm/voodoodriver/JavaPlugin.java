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

package org.sugarcrm.voodoodriver;

import java.util.ArrayList;
import org.openqa.selenium.WebElement;


/**
 * A VooDooDriver Java plugin.
 *
 * @author Jon duSaint
 */

public class JavaPlugin extends Plugin {

   /**
    * The class name of this plugin.
    */

   private String className;

   /**
    * Path to the file containing this plugin's class.
    */

   private String classFile;

   /**
    * The loaded plugin.
    */

   private PluginInterface plugin;


   /**
    * Load the Java plugin from its class file.
    */

   private void loadPlugin() throws PluginException {
      Throwable exc = null;
      String errm = null;

      VDDClassLoader c = new VDDClassLoader();
      try {
         Class<PluginInterface> pluginClass = c.loadClass(className, classFile);
         this.plugin = pluginClass.newInstance();
      } catch (java.io.FileNotFoundException e) {
         errm = "Plugin file '" + classFile + "' not found";
         exc = e;
      } catch (java.io.IOException e) {
         errm = "Error reading plugin file '" + classFile + "'";
         exc = e;
      } catch (java.lang.NoClassDefFoundError e) {
         errm = "No class definition found for plugin class " + className;
         exc = e;
      } catch (java.lang.InstantiationException e) {
         errm = "Failed to instantiate plugin " + className;
         exc = e;
      } catch (java.lang.IllegalAccessException e) {
         errm = "No access to plugin " + className;
         exc = e;
      } catch (Exception e) {
         /*
          * Any exceptions raised during execution of the plugin
          * object constructor will end up here.
          */
         errm = "Unexpected exception during instantiation of " + className;
         exc = e;
      }

      if (exc != null) {
         throw new PluginException(errm, exc);
      }
   }


   /**
    * Instantiate a JavaPlugin object.
    *
    * @param className  the name of the plugin's class
    * @param classFile  file containing this plugin's class
    */

   public JavaPlugin(String className, String classFile)
      throws PluginException {
      this.className = className;
      this.classFile = classFile;

      loadPlugin();
   }


   /**
    * Determine whether the specified class name matches this class name.
    *
    * @param className  name of the Java class
    * @return whether the specified class name matches this class name
    */

   public boolean matches(String className) {
      return className != null && this.className.equals(className);
   }


   /**
    * Merge args from this object and the {@link PluginData} object
    *
    * @param args  args list from the {@link PluginData} object
    * @return combined args list or null if args and this.args is null
    */

   private String[] mergeArgs(String[] args) {
      if (args == null && this.args == null) {
         return null;
      }

      ArrayList<String> merged = new ArrayList<String>();

      if (this.args != null) {
         for (String a: this.args) {
            merged.add(a);
         }
      }
      if (args != null) {
         for (String a: args) {
            merged.add(a);
         }
      }

      return merged.toArray(new String[0]);
   }


   public boolean execute(PluginData data, Reporter report) {
      int rv = 0;
      PluginData localData = new PluginData(data);

      report.Log("Plugin event " + className + " started.");

      localData.setArgs(this.mergeArgs(localData.getArgs()));

      try {
         rv = plugin.execute(localData);
      } catch (Exception e) {
         /*
          * Because all software can't be perfect ;)
          */
         report.ReportError("Exception during plugin (" + className +
                            ") execution.");
         report.ReportException(e);
         return false;
      }

      if (rv != 0) {
         report.ReportError("Plugin " + className +
                            " failed (error code " + rv + ").");
         return false;
      }

      report.Log("Plugin event " + className + " finished.");
   
      return true;
   }
}
