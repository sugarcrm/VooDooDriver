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
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.io.FilenameUtils;
import org.sugarcrm.voodoodriver.BlockList;
import org.sugarcrm.voodoodriver.BlockListParser;
import org.sugarcrm.voodoodriver.Browser;
import org.sugarcrm.voodoodriver.Chrome;
import org.sugarcrm.voodoodriver.Config;
import org.sugarcrm.voodoodriver.ConfigFileParser;
import org.sugarcrm.voodoodriver.Events;
import org.sugarcrm.voodoodriver.Firefox;
import org.sugarcrm.voodoodriver.IE;
import org.sugarcrm.voodoodriver.PluginLoader;
import org.sugarcrm.voodoodriver.SuiteParser;
import org.sugarcrm.voodoodriver.SupportedBrowser;
import org.sugarcrm.voodoodriver.Test;
import org.sugarcrm.voodoodriver.TestList;
import org.sugarcrm.voodoodriver.TestResults;
import org.sugarcrm.voodoodriver.Utils;
import org.sugarcrm.voodoodriver.VDDHash;


/**
 * Primary class of VooDooDriver
 *
 * @author trampus
 */

public class VooDooDriver {
   final static String defaultSodaConfigFile = "soda-config.xml";

   /**
    * Dump JVM information to the console and warn if a non-Sun jvm is
    * being used.
    */

   private static void dumpJavaInfo() {
      HashMap<String, String> javainfo = null;
      javainfo = Utils.getJavaInfo();

      String[] jinfoKeys = javainfo.keySet().toArray(new String[0]);
      Arrays.sort(jinfoKeys);

      System.out.printf("(*)Java RunTime Info:\n");

      for (int i = 0; i <= jinfoKeys.length -1; i++) {
         String value = javainfo.get(jinfoKeys[i]);
         System.out.printf("--)'%s' => '%s'\n", jinfoKeys[i], value);
      }

      if (javainfo.containsKey("java.vendor") &&
          !javainfo.get("java.vendor").contains("Sun Microsystems Inc")) {
         System.out.printf("\n(!)Warning: This is not a 'Sun Microsystems Inc.' JRE/JDK and is not supported!\n");
      }
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
            String validCmdopts[] = {"browser", "attachtimeout", "resultdir",
                                     "savehtml", "plugin"};
            name = tmp.get("name").toString();
            value = tmp.get("value").toString();

            for (String s: validCmdopts) {
               if (name.contains(s)) {
                  if (name.equals("attachtimeout")) {
                     /* The only integer cmdopt */
                     try {
                        configOpts.put(s, Integer.valueOf(value));
                     } catch (java.lang.NumberFormatException e) {
                        System.err.printf("(!)Invalid cmdopt for %s '%s'\n",
                                          name, value);
                        System.exit(1);
                     }
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
      String dstr = String.format("%1$tm-%1$td-%1$tY-%1$tI-%1$tM-%1$tS", now);
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

      /* Merge all from the config file */
      opts.putAll(fileOpts);

      /* Merge all from the command line */
      opts.putAll(cmdOpts);

      /* Store gvar, hijack, suite, and test */
      opts.put("gvar", gvar);
      opts.put("hijack", hijack);
      opts.put("suite", suite);
      opts.put("test", test);

      return opts;
   }


   /**
    * VooDooDriver entry point
    *
    * @param args  array of command line arguments
    */

   public static void main(String[] args) {
      Events plugins = null;
      BlockList blockList = null;

      Config opts = new Config();
      opts.parse(args);
      VDDHash cmdOpts = opts.getOptions();

      System.out.println("(*)Starting VooDooDriver...");
      VDDHash cfg = readConfigFile(cmdOpts.containsKey("config") ?
                                    new File((String)cmdOpts.get("config")) :
                                    null);

      VDDHash config = mergeConfigs(cfg, cmdOpts);

      // XXX: Implement dumpConfig(config);
      dumpJavaInfo();

      if (!config.containsKey("browser")) {
         System.out.println("(!)Error: Missing --browser argument!");
         System.exit(1);
      }
      try {
         String b = (String)config.get("browser");
         config.put("browser", SupportedBrowser.valueOf(b.toUpperCase()));
      } catch (IllegalArgumentException e) {
         System.out.println("(!)Unsupported browser: " + config.get("browser"));
         System.exit(2);
      }

      if (config.containsKey("attachtimeout")) {
         System.out.printf("(*)Setting attach timeout to %ss.\n",
                           (Integer)config.get("attachtimeout"));
      }

      if (config.containsKey("restartcount")) {
         System.out.printf("(*)Restart count => '%d'\n",
                           (Integer)config.get("restartcount"));
      }

      if (config.containsKey("savehtml")) {
         System.out.printf("(*)SaveHTML: %s\n", config.get("savehtml"));
      }

      if (config.containsKey("plugin")) {
         String p = (String)config.get("plugin");
         System.out.println("(*)Loading plugins from " + p);
         try {
            PluginLoader loader = new PluginLoader(p);
            config.put("plugin", loader.parse());
         } catch (Exception e) {
            /* XXX: Just what Exception does SodaPluginParser actually throw? */
            /* A: It literally throws exception. That needs to be fixed. */
            System.err.println("(!)Failed to load plugin file: " + e);
            System.exit(1);
         }
      }

      if (config.containsKey("blocklistfile")) {
         String f = (String)config.get("blocklistfile");
         BlockListParser sbp = new BlockListParser(f);
         blockList = sbp.parse();
      } else {
         blockList = new BlockList();
      }
      config.put("blocklist", blockList);

      if (config.containsKey("suite")) {
         RunSuites(config);
      }

      if (config.containsKey("test")) {
         RunTests(config);
      }

      System.out.printf("(*)VooDooDriver Finished.\n");
      System.exit(0);
   }


   /**
    * Run the tests specified on the command line with --test
    *
    * @param config  VooDooDriver configuration
    */

   private static void RunTests(VDDHash config) {
      /* XXX Start block to be refactored. */
      @SuppressWarnings("unchecked")
         ArrayList<String> tests = (ArrayList<String>)config.get("test");
      String resultdir = (String)config.get("resultdir");;
      SupportedBrowser browserType = (SupportedBrowser)config.get("browser");
      VDDHash gvars = (VDDHash)config.get("gvar");
      VDDHash hijacks = (VDDHash)config.get("hijack");
      Events plugins = (Events)config.get("plugin");
      String savehtml = (String)config.get("savehtml");;
      String downloaddir = (String)config.get("downloaddir");;
      String assertpage = (String)config.get("assertpage");
      int attachTimeout = (Integer)config.get("attachtimeout");;
      Boolean haltOnFailure = (Boolean)config.get("haltOnFailure");;
      /* XXX End block to be refactored. */

      File resultFD = null;
      Browser browser = null;
      int len = 0;
      Test testobj = null;

      if (tests.size() == 0) {
         return;
      }

      System.out.printf("(*)Running Soda Tests now...\n");

      resultFD = new File(resultdir);
      if (!resultFD.exists()) {
         System.out.printf("(*)Result directory doesn't exist, trying to create dir: '%s'\n",
                           resultdir);
         try {
            resultFD.mkdirs();
         } catch (Exception exp) {
            System.out.printf("(!)Error: Failed to create reportdir: '%s'!\n",
                              resultdir);
            System.out.printf("(!)Exception: %s\n", exp.getMessage());
            System.exit(3);
         }
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

      if (downloaddir != null) {
         browser.setDownloadDirectory(downloaddir);
      }

      browser.newBrowser();

      len = tests.size() -1;
      for (int i = 0; i <= len; i++) {
         String test_file = tests.get(i);
         test_file = FilenameUtils.separatorsToSystem(test_file);
         System.out.printf("Starting Test: '%s'.\n", test_file);

         testobj = new Test(test_file, browser, gvars, hijacks, null,
                            null, null, resultdir, savehtml);
         if (assertpage != null) {
            testobj.setAssertPage(assertpage);
         }
         testobj.setPlugins(plugins);
         testobj.runTest(false);

         if (haltOnFailure &&
             (Integer)testobj.getReporter().getResults().get("result") != 0) {
            System.out.printf("(*)Test failed and --haltOnFailure is set. Terminating run...\n");
            break;
         }
      }
   }


   /**
    * Helper function to log summary data
    */

   private static void writeSummary(FileOutputStream in, String msg) {
      try {
         in.write(msg.getBytes());
      } catch (Exception exp) {
         exp.printStackTrace();
      }
   }


   /**
    * Run all test suites
    *
    * @param config  VooDooDriver configuration
    */

   private static void RunSuites(VDDHash config) {
      /* XXX Start block to be refactored. */
      @SuppressWarnings("unchecked")
         ArrayList<String> suites = (ArrayList<String>)config.get("suite");
      String resultdir = (String)config.get("resultdir");;
      SupportedBrowser browserType = (SupportedBrowser)config.get("browser");
      VDDHash gvars = (VDDHash)config.get("gvar");
      VDDHash hijacks = (VDDHash)config.get("hijack");
      BlockList blockList = (BlockList)config.get("blocklist");
      Events plugins = (Events)config.get("plugin");
      String savehtml = (String)config.get("savehtml");;
      String downloaddir = (String)config.get("downloaddir");;
      String assertpage = (String)config.get("assertpage");
      String restartTest = (String)config.get("restarttest");
      int restartCount = (Integer)config.get("restartcount");
      int attachTimeout = (Integer)config.get("attachtimeout");;
      Boolean haltOnFailure = (Boolean)config.get("haltOnFailure");;
      /* XXX End block to be refactored. */

      int len = suites.size() -1;
      File resultFD = null;
      String report_file_name = resultdir;
      String hostname = "";
      FileOutputStream suiteRptFD = null;
      Browser browser = null;
      Date now = null;
      Date suiteStartTime = null;
      Date suiteStopTime = null;
      Boolean terminateRun = false;

      if (suites.size() == 0) {
         return;
      }

      System.out.printf("(*)Running Suite files now...\n");
      System.out.printf("(*)Timeout: %s\n", attachTimeout);

      resultFD = new File(resultdir);
      if (!resultFD.exists()) {
         System.out.printf("(*)Result directory doesn't exist, trying to create dir: '%s'\n",
                           resultdir);

         try {
            resultFD.mkdirs();
         } catch (Exception exp) {
            System.out.printf("(!)Error: Failed to create reportdir: '%s'!\n",
                              resultdir);
            System.out.printf("(!)Exception: %s\n", exp.getMessage());
            System.exit(3);
         }
      }

      try {
         InetAddress addr = InetAddress.getLocalHost();
         hostname = addr.getHostName();
         addr.getHostAddress();

         if (hostname.isEmpty()) {
            hostname = addr.getHostAddress();
         }
      } catch (Exception exp) {
         System.out.printf("(!)Error: %s!\n", exp.getMessage());
         System.exit(4);
      }

      now = new Date();
      String frac = String.format("%1$tN", now);
      String date_str = String.format("%1$tm-%1$td-%1$tY-%1$tI-%1$tM-%1$tS",
                                      now);
      frac = frac.subSequence(0, 3).toString();
      date_str += String.format(".%s", frac);

      report_file_name += "/"+ hostname + "-" + date_str + ".xml";
      report_file_name = FilenameUtils.separatorsToSystem(report_file_name);

      try {
         suiteRptFD = new FileOutputStream(report_file_name);
         System.out.printf("(*)Report: %s\n", report_file_name);
      } catch (Exception exp) {
         System.out.printf("(!)Error: %s!\n", exp.getMessage());
         System.exit(5);
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

      browser.newBrowser();

      writeSummary(suiteRptFD, "<data>\n");

      /* Loop over suites */
      for (int i = 0; i <= len; i++) {
         String suite_base_noext = "";
         String suite_name = suites.get(i);
         String suite_base_name = "";
         File suite_fd = new File(suite_name);
         suite_base_name = suite_fd.getName();
         int testRanCount = 0;

         writeSummary(suiteRptFD, "\t<suite>\n\n");
         writeSummary(suiteRptFD,
                      String.format("\t\t<suitefile>%s</suitefile>\n",
                                    suite_base_name));

         Pattern p = Pattern.compile("\\.xml$", Pattern.CASE_INSENSITIVE);
         Matcher m = p.matcher(suite_base_name);
         suite_base_noext = m.replaceAll("");

         suite_fd = null;
         Test testobj = null;
         System.out.printf("(*)Executing Suite: %s\n", suite_base_name);
         System.out.printf("(*)Parsing Suite file...\n");
         SuiteParser suiteP = new SuiteParser(suite_name, gvars);
         TestList suite_test_list = suiteP.getTests();
         VDDHash vars = null;
         TestResults test_results_hash = null;
         ArrayList<TestResults> test_resultsStore =
            new ArrayList<TestResults>();

         /* Loop over tests within each suite. */
         suiteStartTime = new Date();
         for (int test_index = 0;
              test_index <= suite_test_list.size() - 1;
              test_index++) {
            Date test_start_time = null;
            Boolean testPassed = false;

            if ( (restartCount > 0) && (testRanCount >= restartCount)) {
               System.out.printf("(*))Auto restarting browser.\n");
               if (!browser.isClosed()) {
                  browser.close();
               }
               browser.newBrowser();

               if (restartTest != null) {
                  System.out.printf("(*)Executing Restart Test: '%s'\n",
                                    restartTest);
                  writeSummary(suiteRptFD, "\t\t<test>\n");
                  writeSummary(suiteRptFD,
                               String.format("\t\t\t<testfile>%s</testfile>\n",
                                             restartTest));
                  now = new Date();
                  test_start_time = now;
                  frac = String.format("%1$tN", now);
                  date_str = String.format("%1$tm-%1$td-%1$tY-%1$tI-%1$tM-%1$tS",
                                           now);
                  frac = frac.subSequence(0, 3).toString();
                  date_str += String.format(".%s", frac);

                  writeSummary(suiteRptFD,
                               String.format("\t\t\t<starttime>%s</starttime>\n",
                                             date_str));

                  testobj = new Test(restartTest, browser, gvars, hijacks,
                                         blockList, vars, suite_base_noext,
                                         resultdir, savehtml);
                  testobj.setIsRestartTest(true);

                  if (assertpage != null) {
                     testobj.setAssertPage(assertpage);
                  }

                  if (plugins != null) {
                     testobj.setPlugins(plugins);
                  }

                  testobj.setAttachTimeout(attachTimeout);
                  testobj.runTest(false);
                  now = new Date();
                  frac = String.format("%1$tN", now);
                  date_str = String.format("%1$tm-%1$td-%1$tY-%1$tI-%1$tM-%1$tS",
                                           now);
                  frac = frac.subSequence(0, 3).toString();
                  date_str += String.format(".%s", frac);

                  writeSummary(suiteRptFD,
                               String.format("\t\t\t<stoptime>%s</stoptime>\n",
                                             date_str));
                  String msg = Utils.GetRunTime(test_start_time, now);
                  writeSummary(suiteRptFD,
                               String.format("\t\t\t<totaltesttime>%s</totaltesttime>\n", msg));

                  if (testobj.getEventLoop() != null) {
                     vars = testobj.getEventLoop().getSodaVars();
                  }

                  test_results_hash = testobj.getReporter().getResults();
                  test_resultsStore.add(test_results_hash);
                  for (int res_index = 0;
                       res_index <= test_results_hash.keySet().size() - 1;
                       res_index++) {
                     String key = test_results_hash.keySet().toArray()[res_index].toString();
                     String value = test_results_hash.get(key).toString();

                     if (key.contains("result")) {
                        if (Integer.valueOf(value) != 0) {
                           value = "Failed";
                        } else {
                           value = "Passed";
                        }
                     }
                     writeSummary(suiteRptFD,
                                  String.format("\t\t\t<%s>%s</%s>\n",
                                                key, value, key));
                  }
                  writeSummary(suiteRptFD, "\t\t</test>\n\n");
               }

               testRanCount = 0;
            }

            writeSummary(suiteRptFD, "\t\t<test>\n");
            String current_test = suite_test_list.get(test_index);
            writeSummary(suiteRptFD,
                         String.format("\t\t\t<testfile>%s</testfile>\n",
                                       current_test));
            System.out.printf("(*)Executing Test: '%s'\n", current_test);
            now = new Date();
            test_start_time = now;
            frac = String.format("%1$tN", now);
            date_str = String.format("%1$tm-%1$td-%1$tY-%1$tI-%1$tM-%1$tS",
                                     now);
            frac = frac.subSequence(0, 3).toString();
            date_str += String.format(".%s", frac);

            writeSummary(suiteRptFD,
                         String.format("\t\t\t<starttime>%s</starttime>\n",
                                       date_str));

            if (browser.isClosed()) {
               System.out.printf("(*)Browser was closed by another suite, creating new browser...\n");
               browser.newBrowser();
               System.out.printf("(*)New browser created.\n");
            }

            testobj = new Test(current_test, browser, gvars, hijacks,
                               blockList, vars, suite_base_noext,
                               resultdir, savehtml);
            if (assertpage != null) {
               testobj.setAssertPage(assertpage);
            }

            if (plugins != null) {
               testobj.setPlugins(plugins);
            }

            testobj.setAttachTimeout(attachTimeout);
            testobj.runTest(false);

            now = new Date();
            frac = String.format("%1$tN", now);
            date_str = String.format("%1$tm-%1$td-%1$tY-%1$tI-%1$tM-%1$tS",
                                     now);
            frac = frac.subSequence(0, 3).toString();
            date_str += String.format(".%s", frac);
            writeSummary(suiteRptFD,
                         String.format("\t\t\t<stoptime>%s</stoptime>\n",
                                       date_str));
            String msg = Utils.GetRunTime(test_start_time, now);
            writeSummary(suiteRptFD,
                         String.format("\t\t\t<totaltesttime>%s</totaltesttime>\n", msg));

            if (testobj.getEventLoop() != null) {
               vars = testobj.getEventLoop().getSodaVars();
            }

            test_results_hash = testobj.getReporter().getResults();
            test_resultsStore.add(test_results_hash);
            for (int res_index = 0;
                 res_index <= test_results_hash.keySet().size() - 1;
                 res_index++) {
               String key =
                  test_results_hash.keySet().toArray()[res_index].toString();
               String value = test_results_hash.get(key).toString();

               if (key.contains("result")) {
                  if (Integer.valueOf(value) != 0) {
                     value = "Failed";
                  } else {
                     value = "Passed";
                     testPassed = true;
                  }
               }
               writeSummary(suiteRptFD, String.format("\t\t\t<%s>%s</%s>\n",
                                                      key, value, key));
            }
            writeSummary(suiteRptFD, "\t\t</test>\n\n");

            if (restartCount > 0) {
               File tmpF = new File(current_test);
               File pF = tmpF.getParentFile();

               if (pF != null) {
                  String path = pF.getAbsolutePath();
                  path = path.toLowerCase();
                  if (!path.contains("lib")) {
                     testRanCount += 1;
                     System.out.printf("(*)Tests ran since last restart: '%d'\n",
                                       testRanCount);
                  }
               } else {
                  testRanCount += 1;
                  System.out.printf("(*)Tests ran since last restart: '%d'\n",
                                    testRanCount);
               }
            }

            if (haltOnFailure && testPassed == false) {
               System.out.printf("(*)Test failed and --haltOnFailure is set. Terminating run...\n");
               terminateRun = true;
               break;
            }
         }

         suiteStopTime = new Date();
         frac = String.format("%1$tN", suiteStopTime);
         frac = frac.subSequence(0, 3).toString();
         String stopTimeStr =
            String.format("%1$tm-%1$td-%1$tY-%1$tI-%1$tM-%1$tS", suiteStopTime);
         stopTimeStr += String.format(".%s", frac);

         frac = String.format("%1$tN", suiteStartTime);
         frac = frac.subSequence(0, 3).toString();
         String startTimeStr =
            String.format("%1$tm-%1$td-%1$tY-%1$tI-%1$tM-%1$tS",
                          suiteStartTime);
         startTimeStr += String.format(".%s", frac);


         String msg = String.format("\t\t<runtime>%s</runtime>\n",
                                    Utils.GetRunTime(suiteStartTime,
                                                     suiteStopTime));
         writeSummary(suiteRptFD,
                      String.format("\t\t<starttime>%s</starttime>\n",
                                    startTimeStr));
         writeSummary(suiteRptFD,
                      String.format("\t\t<stoptime>%s</stoptime>\n",
                                    stopTimeStr));
         writeSummary(suiteRptFD, msg);
         writeSummary(suiteRptFD, "\t</suite>\n");

         if (terminateRun) {
            break;
         }
      }
      writeSummary(suiteRptFD, "</data>\n\n");
   }
}
