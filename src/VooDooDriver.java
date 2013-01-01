/*
 * Copyright 2011-2012 SugarCRM Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * Please see the License for the specific language governing permissions and
 * limitations under the License.
 */


import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import org.sugarcrm.voodoodriver.BlockList;
import org.sugarcrm.voodoodriver.BlockListParser;
import org.sugarcrm.voodoodriver.Browser;
import org.sugarcrm.voodoodriver.Chrome;
import org.sugarcrm.voodoodriver.Config;
import org.sugarcrm.voodoodriver.ConfigFileParser;
import org.sugarcrm.voodoodriver.Events;
import org.sugarcrm.voodoodriver.Firefox;
import org.sugarcrm.voodoodriver.IE;
import org.sugarcrm.voodoodriver.Plugin;
import org.sugarcrm.voodoodriver.PluginLoader;
import org.sugarcrm.voodoodriver.SuiteParser;
import org.sugarcrm.voodoodriver.SupportedBrowser;
import org.sugarcrm.voodoodriver.Test;
import org.sugarcrm.voodoodriver.TestResults;
import org.sugarcrm.voodoodriver.Utils;
import org.sugarcrm.voodoodriver.Vars;
import org.sugarcrm.voodoodriver.VDDException;
import org.sugarcrm.voodoodriver.VDDHash;


/**
 * Primary class of VooDooDriver
 *
 * @author trampus
 */

public class VooDooDriver {

   /**
    * Default name of the VooDooDriver configuration file.
    */

   final static String defaultSodaConfigFile = "soda-config.xml";

   /**
    * Name of VooDooDriver's primary log file.
    */

   final static String vddLogFilename = "voodoo.log";

   /**
    * VDD summary file.
    */

   private static VDDSummary summary;


   /**
    * Dump key-value pairs from a {@link VDDHash}
    *
    * @param kvps  {@link VDDHash} to be dumped
    */

   private static void dumpKeys(VDDHash kvps) {
      String[] keys = kvps.keySet().toArray(new String[0]);
      int col = 0;
      java.util.Arrays.sort(keys);
      for (String key: keys) {
         if (key.length() > col) {
            col = key.length();
         }
      }
      for (String key: keys) {
         System.out.printf("--)%" + String.valueOf(col + 2) + "s: %s\n",
                           key, kvps.get(key));
      }
   }


   /**
    * Dump JVM information to the console.
    */

   private static void dumpJavaInfo() {
      VDDHash javaInfo = Utils.getJavaInfo();

      System.out.printf("(*)Java RunTime Info:\n");
      dumpKeys(javaInfo);

      if (javaInfo.containsKey("java.vendor") &&
          !javaInfo.get("java.vendor").toString().contains("Sun Microsystems")) {
         System.out.println("(!)Warning: This is not a 'Sun Microsystems' " +
                            "JRE/JDK and is not supported.");
      }
   }


   /**
    * Dump VooDooDriver configuration.
    *
    * @param config  VooDooDriver configuration
    */

   private static void dumpConfig(VDDHash config) {
      System.out.println("(*)VooDooDriver Configuration:");
      dumpKeys(config);
   }


   /**
    * Read the VoodDooDriver configuration file.
    *
    * The default configuration file is soda-config.xml, but that can
    * be changed with the --config command line option.
    *
    * @param cmdOpts  parsed command line options
    * @return VDDHash with four entries:
    *            <dl>
    *              <dt>gvar</dt>
    *              <dd>VDDHash of global variables from the
    *                  configuration file</dd>
    *              <dt>hijack</dt>
    *              <dd>VDDHash of SodaVar substitutions from the
    *                  configuration file</dd>
    *              <dt>suite</dt>
    *              <dd>ArrayList of suites listed in the configuration
    *                  file</dd>
    *              <dt>test</dt>
    *              <dd>ArrayList of tests listed in the configuration
    *                  file</dd>
    *            </dl>
    */

   private static VDDHash readConfigFile(File configFile) {
      VDDHash gvars = new VDDHash();
      VDDHash hijacks = new VDDHash();
      VDDHash configOpts = new VDDHash();
      ConfigFileParser configParser = null;
      Events configFileOpts = null;

      configOpts.put("gvar", gvars);
      configOpts.put("hijack", hijacks);
      configOpts.put("suite", null);
      configOpts.put("test", null);

      if (configFile == null) {
         configFile = new File(defaultSodaConfigFile);
         if (!configFile.exists()) {
            return configOpts;
         }
      }

      System.out.printf("(*)Reading VooDooDriver config file '%s'.\n",
                        configFile.getName());

      configParser = new ConfigFileParser(configFile);
      configFileOpts = configParser.parse();
      int argsLen = configFileOpts.size() - 1;

      for (int i = 0; i <= argsLen; i++) {
         VDDHash tmp = configFileOpts.get(i);
         String type = tmp.get("type").toString();
         String name = null;
         String value = null;

         if (type.contains("gvar")) {
            name = tmp.get("name").toString();
            value = tmp.get("value").toString();

            if (!gvars.containsKey(name)) {
               name = String.format("global.%s", name);
               gvars.put(name, value);
               System.out.printf("(*)Adding Config-File gvar: '%s' => '%s'.\n",
                                 name, value);
            }
         } else if (type.contains("cmdopt")) {
            String validCmdopts[] = {"attachtimeout", "blocklistfile",
                                     "browser", "haltOnFailure", "plugin",
                                     "restartcount", "restarttest",
                                     "resultdir", "savehtml", "screenshot"};
            name = tmp.get("name").toString();
            value = tmp.get("value").toString();

            for (String s: validCmdopts) {
               if (name.contains(s)) {
                  if (name.equals("attachtimeout") ||
                      name.equals("restartcount")) {
                     /* Integer cmdopts */
                     try {
                        configOpts.put(s, Integer.valueOf(value));
                     } catch (java.lang.NumberFormatException e) {
                        System.err.printf("(!)Invalid cmdopt for %s '%s'\n",
                                          name, value);
                        System.exit(1);
                     }
                  } else if (name.equals("haltOnFailure")) {
                     configOpts.put(s, Boolean.valueOf(value));
                  } else if (name.equals("plugin")) {
                     /*
                      * This is a hack.  This config file reading
                      * really needs to be done in Config.java, and
                      * that already has the means to handle array
                      * options.
                      */
                     @SuppressWarnings("unchecked")
                        ArrayList<String> plugins =
                        (ArrayList<String>)configOpts.get(name);
                     if (plugins == null) {
                        plugins = new ArrayList<String>();
                     }
                     plugins.add(value);
                     configOpts.put(name, plugins);
                  } else {
                     configOpts.put(s, value);
                  }
                  System.out.printf("(*)Adding Config-File cmdopts: '%s' => '%s'.\n",
                                    name, value);
               }
            }
         } else if (type.contains("hijacks")) {
            @SuppressWarnings("unchecked")
               ArrayList<String> jacks = (ArrayList<String>)tmp.get("hijacks");

            for (int x = 0; x <= jacks.size() -1; x++) {
               String[] jdata = jacks.get(x).split("::");
               hijacks.put(jdata[0], jdata[1]);
               System.out.printf("(*)Adding Config-File hijack: '%s' => '%s'\n",
                                 jdata[0], jdata[1]);
            }

         } else if (type.contains("suites")) {
            @SuppressWarnings("unchecked") ArrayList<String>
               SodaSuitesList = (ArrayList<String>)tmp.get("suites");
            for (int x = 0; x <= SodaSuitesList.size() -1; x++) {
               String sname = SodaSuitesList.get(x);
               System.out.printf("(*)Adding Config-File suite file: '%s'\n",
                                 sname);
            }
            configOpts.put("suite", SodaSuitesList);
         } else if (type.contains("tests")) {
            @SuppressWarnings("unchecked") ArrayList<String>
               SodaTestsList = (ArrayList<String>)tmp.get("tests");
            for (int x = 0; x <= SodaTestsList.size() -1; x++) {
               String tname = SodaTestsList.get(x);
               System.out.printf("(*)Adding Config-File test file: '%s'\n",
                                 tname);
            }
            configOpts.put("test", SodaTestsList);
         }
      }

      return configOpts;
   }


   /**
    * Create a default result directory name.
    *
    * @return default result directory name
    */

   private static String defaultResultDir() {
      Date now = new Date();
      String frac = String.format("%1$tN", now);
      String dstr = String.format("%1$tm-%1$td-%1$tY-%1$tH-%1$tM-%1$tS", now);
      frac = frac.subSequence(0, 3).toString();
      dstr += String.format(".%s", frac);

      return System.getProperty("user.dir") + File.separator + dstr;
   }


   /**
    * Merge config file and command line options, applying defaults as needed.
    *
    * @param fileOpts  options from the config file
    * @param cmdOpts   options from the command line
    * @return {@link VDDHash} with all options
    */

   @SuppressWarnings("unchecked")
   private static VDDHash mergeConfigs(VDDHash fileOpts, VDDHash cmdOpts) {
      VDDHash opts = new VDDHash();

      /* Defaults */
      opts.put("attachtimeout", 0);
      opts.put("haltOnFailure", false);
      opts.put("restartcount", 0);
      opts.put("resultdir", defaultResultDir());

      /* Merge gvar */
      VDDHash gvar = new VDDHash();
      try {
         gvar.putAll((VDDHash)fileOpts.remove("gvar"));
      } catch (NullPointerException e) {}
      try {
         gvar.putAll((VDDHash)cmdOpts.remove("gvar"));
      } catch (NullPointerException e) {}

      /* Merge hijack */
      VDDHash hijack = new VDDHash();
      try {
         hijack.putAll((VDDHash)fileOpts.remove("hijack"));
      } catch (NullPointerException e) {}
      try {
         hijack.putAll((VDDHash)cmdOpts.remove("hijack"));
      } catch (NullPointerException e) {}

      /* Merge suite */
      ArrayList<String> suite = new ArrayList<String>();
      try {
         suite.addAll((ArrayList<String>)fileOpts.remove("suite"));
      } catch (NullPointerException e) {}
      try {
         suite.addAll((ArrayList<String>)cmdOpts.remove("suite"));
      } catch (NullPointerException e) {}

      /* Merge test */
      ArrayList<String> test = new ArrayList<String>();
      try {
         test.addAll((ArrayList<String>)fileOpts.remove("test"));
      } catch (NullPointerException e) {}
      try {
         test.addAll((ArrayList<String>)cmdOpts.remove("test"));
      } catch (NullPointerException e) {}

      /* Merge plugin */
      ArrayList<String> plugin = new ArrayList<String>();
      try {
         plugin.addAll((ArrayList<String>)fileOpts.remove("plugin"));
      } catch (NullPointerException e) {}
      try {
         plugin.addAll((ArrayList<String>)cmdOpts.remove("plugin"));
      } catch (NullPointerException e) {}

      /* Merge all from the config file */
      opts.putAll(fileOpts);

      /* Merge all from the command line */
      opts.putAll(cmdOpts);

      /* Store gvar, hijack, suite, test, and plugin */
      opts.put("gvar", gvar);
      opts.put("hijack", hijack);
      opts.put("suite", suite);
      opts.put("test", test);
      opts.put("plugin", plugin);

      return opts;
   }


   /**
    * Load VDD's {@link Browser} object.
    *
    * @param config  VDD's config object
    */

   private static void loadBrowser(VDDHash config) {
      SupportedBrowser browserType = null;
      Browser browser = null;

      if (!config.containsKey("browser")) {
         System.out.println("(!)Error: Missing --browser argument!");
         System.exit(1);
      }

      try {
         String b = (String)config.get("browser");
         browserType = SupportedBrowser.valueOf(b.toUpperCase());
      } catch (IllegalArgumentException e) {
         System.out.println("(!)Unsupported browser: " + config.get("browser"));
         System.exit(2);
      }

      switch (browserType) {
      case FIREFOX:
         browser = new Firefox();
         break;
      case CHROME:
         browser = new Chrome();
         break;
      case IE:
         browser = new IE();
         break;
      }

      if (config.get("downloaddir") != null) {
         browser.setDownloadDirectory((String)config.get("downloaddir"));
      }

      config.put("browser", browser);
   }


   /**
    * Load command-line specified VDD plugins.
    *
    * @param config  VDD's config object
    */

   private static void loadPlugins(VDDHash config) {
      if (!config.containsKey("plugin")) {
         return;
      }

      @SuppressWarnings("unchecked")
         ArrayList<String> plugin = (ArrayList<String>)config.get("plugin");
      ArrayList<Plugin> loadedPlugins = new ArrayList<Plugin>();

      for (String p: plugin) {
         System.out.println("(*)Loading plugins from " + p);

         try {
            PluginLoader loader = new PluginLoader(new File(p));
            ArrayList<Plugin> pluginList = loader.load();
            loadedPlugins.addAll(pluginList);
         } catch (org.sugarcrm.voodoodriver.PluginException e) {
            System.err.println("(!)Failed to load plugin file:");
            e.printStackTrace(System.err);
            System.exit(1);
         }
      }
      
      config.put("plugin", loadedPlugins);
   }


   /**
    * Load VDD's block list.
    *
    * @param config  VDD's config object
    */

   private static void loadBlocklist(VDDHash config) {
      BlockList blockList;

      if (!config.containsKey("blocklistfile")) {
         blockList = new BlockList();
      } else {
         String f = (String)config.get("blocklistfile");
         BlockListParser sbp = new BlockListParser(f);
         blockList = sbp.parse();
      }

      config.put("blocklist", blockList);
   }


   /**
    * Create VDD's output directory.
    *
    * @param config  VDD's config object
    */

   private static void createResultDir(VDDHash config) {
      File rd = new File(config.get("resultdir").toString());

      if (!rd.exists()) {
         System.out.println("(*)Creating result directory: " +
                            config.get("resultdir"));
         if (rd.mkdirs() == false) {
            System.err.println("(!)Failed to create result directory: " +
                               config.get("resultdir"));
            System.exit(3);
         }
      }
   }


   /**
    * Start VDD logging.
    *
    * This method hijacks System.out and System.err with VDDLog objects.
    */

   private static void earlyLog() {
      VDDLog out = new VDDLog(System.out);
      System.setOut(out);
      VDDLog err = new VDDLog(System.err);
      System.setErr(err);
   }


   /**
    * Create VDD log file and flush output queue to it.
    *
    * @param config  VDD configuration object
    */

   private static void lateLog(VDDHash config) {
      String resultDir = (String)config.get("resultdir");
      File vddLog = new File(resultDir + File.separator + vddLogFilename);
      System.out.println("(*)Creating log file: " + vddLog);
      FileOutputStream logStream = null;
      try {
         logStream = new FileOutputStream(vddLog);
         ((VDDLog)System.out).openLog(logStream);
         ((VDDLog)System.err).openLog(logStream);
      } catch (java.io.FileNotFoundException e) {
         System.err.println("(!)Failed to open VDD log. Continuing anyways.");
      }
   }


   /**
    * Close the VDD log file.
    */

   private static void closeLog() {
      ((VDDLog)System.out).closeLog();
   }


   /**
    * VooDooDriver entry point
    *
    * @param args  array of command line arguments
    */

   public static void main(String[] args) {
      Thread shutdown = new Thread() {
            public void run() {
               System.out.println("(!)VDD terminated prematurely");
            }
         };

      earlyLog();

      Config opts = new Config();
      opts.parse(args);
      VDDHash cmdOpts = opts.getOptions();

      System.out.println("(*)Starting VooDooDriver...");
      VDDHash cfg = readConfigFile(cmdOpts.containsKey("config") ?
                                    new File((String)cmdOpts.get("config")) :
                                    null);
      VDDHash config = mergeConfigs(cfg, cmdOpts);

      createResultDir(config);
      lateLog(config);
      dumpJavaInfo();
      dumpConfig(config);
      loadBrowser(config);
      loadPlugins(config);
      loadBlocklist(config);

      try {
         summary = new VDDSummary(config);
      } catch (java.io.IOException e) {
         System.err.println("(!)Failed to create summary file: " + e);
         System.exit(1);
      }
      System.out.println("(*)Report: " + summary.getFilename());

      Runtime.getRuntime().addShutdownHook(shutdown);

      if (config.containsKey("suite")) {
         RunSuites(config);
      }

      if (config.containsKey("test")) {
         RunTests(config);
      }

      Runtime.getRuntime().removeShutdownHook(shutdown);

      summary.close();

      System.out.println("(*)VooDooDriver Finished.");
      closeLog();
      System.exit(0);
   }


   /**
    * Create a vars object and populate its base context.
    *
    * @param config  VDD config object
    * @return populated Vars object
    */

   private static Vars createVars(VDDHash config) {
      Vars v = new Vars();

      VDDHash gvars = (VDDHash)config.get("gvar");
      if (gvars == null) {
         return v;
      }

      for (String key: gvars.keySet()) {
         v.put(key, gvars.get(key).toString());
      }

      return v;
   }


   /**
    * Format a date into a string.
    *
    * @param d  Date to be formatted
    */

   private static String formatDate(Date d) {
      return String.format("%1$tm-%1$td-%1$tY-%1$tH-%1$tM-%1$tS.%1$tL", d);
   }


   /**
    * Run a single test and record its results.
    *
    * @param test       the test to run
    * @param isRestart  true if this is a restart test
    * @return true if the test passed, false otherwise
    */

   private static boolean runOneTest(File test, String suite, VDDHash config,
                                     Vars vars, boolean isRestart) {
      Date startTime, stopTime;
      boolean result = true;

      System.out.println("(*)Executing " + (isRestart ? "restart " : "") +
                         "test: " + test);

      summary.openTag("test");
      summary.writeData("testfile", test.toString());

      startTime = new Date();

      Test t = new Test(config, test, suite, vars);
      t.setIsRestartTest(isRestart);
      t.runTest();

      stopTime = new Date();

      summary.writeData("starttime", formatDate(startTime));
      summary.writeData("stoptime", formatDate(stopTime));
      summary.writeData("totaltesttime", Utils.GetRunTime(startTime, stopTime));

      TestResults r = t.getReporter().getResults();

      for (String key: r.keySet()) {
         String value = "";

         if (key.equals("result")) {
            if ((Integer)r.get(key) == 0) {
               value =  "Passed";
               result = true;
            } else {
               value = "Failed";
               result = false;
            }
         } else {
            value = String.valueOf(r.get(key));
         }

         summary.writeData(key, value);
      }

      summary.closeTag();

      return result;
   }


   /**
    * Run the tests specified on the command line with --test.
    *
    * @param config  VooDooDriver configuration
    */

   private static void RunTests(VDDHash config) {
      @SuppressWarnings("unchecked")
         ArrayList<String> tests = (ArrayList<String>)config.get("test");
      Boolean haltOnFailure = (Boolean)config.get("haltOnFailure");;
      Browser browser = (Browser)config.get("browser");
      Date startTime, stopTime;

      if (tests.size() == 0) {
         return;
      }

      System.out.println("(*)Running Soda Tests...");

      summary.openTag("suite");
      summary.writeData("suitefile", "Command Line Tests");
      startTime = new Date();

      for (String test: tests) {
         Vars vars = createVars(config);

         if (browser.isClosed()) {
            browser.newBrowser();
         }

         boolean testPassed = runOneTest(new File(test), "Command line", config,
                                         vars, false);

         if (haltOnFailure && !testPassed) {
            System.out.println("(*)Test failed and --haltOnFailure is set. " +
                               "Terminating run...");
            break;
         }
      }

      stopTime = new Date();

      summary.writeData("starttime", formatDate(startTime));
      summary.writeData("stoptime", formatDate(stopTime));
      summary.writeData("runtime", Utils.GetRunTime(startTime, stopTime));

      summary.closeTag();
   }


   /**
    * Run test suites specified with --suite.
    *
    * @param config  VooDooDriver configuration
    */

   private static void RunSuites(VDDHash config) {
      @SuppressWarnings("unchecked")
         ArrayList<String> suites = (ArrayList<String>)config.get("suite");
      File restartTest = null;
      int restartCount = (Integer)config.get("restartcount");
      Boolean haltOnFailure = (Boolean)config.get("haltOnFailure");;
      Browser browser = (Browser)config.get("browser");
      boolean testPassed = true;

      if (suites.size() == 0) {
         return;
      }

      if (config.containsKey("restarttest")) {
         restartTest = new File((String)config.get("restarttest"));
      }

      System.out.println("(*)Running Suite files...");

      browser.newBrowser();

      /* Loop over suites */
      for (String suiteStr: suites) {
         File suite = new File(suiteStr);
         String baseName = suite.getName().replaceAll("\\.xml$", "");
         int testsRan = 0;
         Date suiteStartTime, suiteStopTime;
         SuiteParser sp = null;
         Vars vars = createVars(config);

         System.out.println("(*)Executing Suite: " + baseName);

         suiteStartTime = new Date();

         summary.openTag("suite");
         summary.writeData("suitefile", suite.getName());

         try {
            sp = new SuiteParser(suite, (VDDHash)config.get("gvar"));
         } catch (VDDException e) {
            System.err.println("Failed to load " + suite + ": " + e);
            continue;
         }

         /* Loop over tests within each suite */
         for (File test: sp.getTests()) {
            /* Restart browser and run restart test if required */
            if ((restartCount > 0) && (testsRan >= restartCount)) {
               System.out.println("(*))Auto restarting browser.");
               if (!browser.isClosed()) {
                  browser.close();
               }
               browser.newBrowser();

               if (restartTest != null) {
                  testPassed = runOneTest(restartTest, baseName, config,
                                          vars, true);

                  if (haltOnFailure && !testPassed) {
                     break;
                  }
               }

               testsRan = 0;
            }

            /* Run test */

            if (browser.isClosed()) {
               System.out.println("(*)Browser was closed by another suite, creating new browser...");
               browser.newBrowser();
            }

            testPassed = runOneTest(test, baseName, config, vars, false);

            if (restartCount > 0) {
               File pf = test.getParentFile();

               if (pf == null ||
                   !pf.getAbsolutePath().toLowerCase().contains("lib")) {
                  testsRan += 1;
                  System.out.println("(*)Tests ran since last restart: " +
                                     testsRan);
               }
            }

            if (haltOnFailure && !testPassed) {
               break;
            }
         }

         suiteStopTime = new Date();

         summary.writeData("starttime", formatDate(suiteStartTime));
         summary.writeData("stoptime", formatDate(suiteStopTime));
         summary.writeData("runtime", Utils.GetRunTime(suiteStartTime,
                                                       suiteStopTime));
         summary.closeTag();

         if (!testPassed) {
            System.out.printf("(*)Test failed and --haltOnFailure is set. Terminating run...\n");
            break;
         }
      }
   }
}
