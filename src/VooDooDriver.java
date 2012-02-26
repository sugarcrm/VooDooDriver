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
import voodoodriver.SodaBlockList;
import voodoodriver.SodaBlockListParser;
import voodoodriver.SodaBrowser;
import voodoodriver.SodaChrome;
import voodoodriver.SodaCmdLineOpts;
import voodoodriver.SodaConfigParser;
import voodoodriver.SodaEvents;
import voodoodriver.SodaFirefox;
import voodoodriver.SodaHash;
import voodoodriver.SodaIE;
import voodoodriver.SodaSuiteParser;
import voodoodriver.SodaSupportedBrowser;
import voodoodriver.SodaTest;
import voodoodriver.SodaTestList;
import voodoodriver.SodaTestResults;
import voodoodriver.SodaUtils;
import voodoodriver.VDDVersionInfo;


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
      javainfo = SodaUtils.getJavaInfo();

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
    * @return SodaHash with four entries:
    *            <dl>
    *              <dt>gvar</dt>
    *              <dd>SodaHash of global variables from the
    *                  configuration file</dd>
    *              <dt>hijack</dt>
    *              <dd>SodaHash of SodaVar substitutions from the
    *                  configuration file</dd>
    *              <dt>suite</dt>
    *              <dd>ArrayList of suites listed in the configuration
    *                  file</dd>
    *              <dt>test</dt>
    *              <dd>ArrayList of tests listed in the configuration
    *                  file</dd>
    *            </dl>
    */

   @SuppressWarnings("unchecked")
   private static SodaHash readConfigFile(SodaHash cmdOpts) {
      String configFile = defaultSodaConfigFile;
      File sodaConfigFD = null;
      SodaHash gvars = new SodaHash();
      SodaHash hijacks = new SodaHash();
      ArrayList<String> SodaSuitesList = null;
      ArrayList<String> SodaTestsList = null;
      SodaHash returnVal = new SodaHash();
      SodaConfigParser configParser = null;
      SodaEvents configFileOpts = null;

      returnVal.put("gvar", gvars);
      returnVal.put("hijack", hijacks);
      returnVal.put("suite", null);
      returnVal.put("test", null);

      if (cmdOpts.containsKey("config") && cmdOpts.get("config") != null) {
         configFile = cmdOpts.get("config").toString();
      }
      sodaConfigFD = new File(configFile);

      if (!sodaConfigFD.exists()) {
         if (configFile != defaultSodaConfigFile) {
            System.out.printf("(!)Error: Specified config file '%s' does not exist!\n",
                              configFile);
            System.exit(5);
         }
         return returnVal;
      }

      System.out.printf("(*)Reading VooDooDriver config file '%s'.\n",
                        configFile);

      configParser = new SodaConfigParser(sodaConfigFD);
      configFileOpts = configParser.parse();
      int argsLen = configFileOpts.size() - 1;

      for (int i = 0; i <= argsLen; i++) {
         SodaHash tmp = configFileOpts.get(i);
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
                  cmdOpts.put(s, value);
                  System.out.printf("(*)Adding Config-File cmdopts: '%s' => '%s'.\n",
                                    name, value);
               }
            }
         } else if (type.contains("hijacks")) {
            ArrayList<String> jacks = (ArrayList<String>)tmp.get("hijacks");

            for (int x = 0; x <= jacks.size() -1; x++) {
               String[] jdata = jacks.get(x).split("::");
               hijacks.put(jdata[0], jdata[1]);
               System.out.printf("(*)Adding Config-File hijack: '%s' => '%s'\n",
                                 jdata[0], jdata[1]);
            }

         } else if (type.contains("suites")) {
            SodaSuitesList = (ArrayList<String>)tmp.get("suites");
            for (int x = 0; x <= SodaSuitesList.size() -1; x++) {
               String sname = SodaSuitesList.get(x);
               System.out.printf("(*)Adding Config-File suite file: '%s'\n",
                                 sname);
            }
            returnVal.put("suite", SodaSuitesList);
         } else if (type.contains("tests")) {
            SodaTestsList = (ArrayList<String>)tmp.get("tests");
            for (int x = 0; x <= SodaTestsList.size() -1; x++) {
               String tname = SodaTestsList.get(x);
               System.out.printf("(*)Adding Config-File test file: '%s'\n",
                                 tname);
            }
            returnVal.put("test", SodaTestsList);
         }
      }

      return returnVal;
   }


   /**
    * VooDooDriver entry point
    *
    * @param args  array of command line arguments
    */

   @SuppressWarnings("unchecked")
   public static void main(String[] args) {
      String blockListFile = null;
      SodaBlockList blockList = null;
      SodaCmdLineOpts opts = null;
      SodaHash cmdOpts = null;
      SodaSupportedBrowser browserType = null;
      ArrayList<String> SodaSuitesList = null;
      ArrayList<String> SodaTestsList = null;
      SodaPluginParser plugParser = null;
      SodaEvents plugins = null;
      String savehtml = "";
      String downloadDir = null;
      String assertpage = null;
      SodaHash gvars = null;
      int restartCount = 0;
      String restartTest = null;
      int attachTimeout = 0;
      SodaHash hijacks = null;
      String resultdir = null;
      Boolean haltOnFailure = false;

      opts = new SodaCmdLineOpts();
      opts.parse(args);
      cmdOpts = opts.getOptions();

      System.out.println("(*)Starting VooDooDriver...");

      SodaHash config = readConfigFile(cmdOpts);
      gvars = (SodaHash)config.get("gvar");
      hijacks = (SodaHash)config.get("hijack");
      SodaSuitesList = (ArrayList<String>)config.get("suite");
      SodaTestsList = (ArrayList<String>)config.get("test");

      if (cmdOpts.get("gvar") != null) {
         gvars.putAll((SodaHash)cmdOpts.get("gvar"));
      }
      if (cmdOpts.containsKey("hijack")) {
         hijacks.putAll((SodaHash)cmdOpts.get("hijack"));
      }

      if (!cmdOpts.containsKey("browser")) {
         System.out.println("(!)Error: Missing --browser argument!");
         System.exit(1);
      }
      try {
         String b = (String)cmdOpts.get("browser");
         browserType = SodaSupportedBrowser.valueOf(b.toUpperCase());
      } catch (Exception e) {
         System.out.printf("(!)Unsupported browser: '%s'!\n",
                           cmdOpts.get("browser"));
         System.exit(2);
      }

      if (cmdOpts.containsKey("attachtimeout")) {
         attachTimeout = new Integer((String)cmdOpts.get("attachtimeout"));
         System.out.printf("(*)Setting default Attach Timeout to %ss.\n",
                           attachTimeout);
      }
      if (cmdOpts.containsKey("restarttest")) {
         restartTest = (String)cmdOpts.get("restarttest");
      }
      if (cmdOpts.containsKey("restartcount")) {
         restartCount = new Integer((String)cmdOpts.get("restartcount"));
      }
      if (restartCount > 0) {
         System.out.printf("(*)Restart Count => '%d'\n", restartCount);

         if (restartTest != null) {
            System.out.printf("(*)Restart Test => '%s'.\n", restartTest);
            File retmp = new File(restartTest);

            if (!retmp.exists()) {
               System.out.printf("(!)Error: failed to find Restart Test: => '%s'!\n\n",
                                 restartTest);
               System.exit(5);
            }
         }
      }
      if (cmdOpts.containsKey("assertpagefile")) {
         assertpage = cmdOpts.get("assertpagefile").toString();
      }
      if (cmdOpts.containsKey("downloaddir")) {
         downloadDir = cmdOpts.get("downloaddir").toString();
      }
      if (cmdOpts.containsKey("haltonfailure")) {
         haltOnFailure = (Boolean)cmdOpts.get("haltonfailure");
      }
      if (cmdOpts.containsKey("savehtml")) {
         savehtml = (String)cmdOpts.get("savehtml");
         System.out.printf("(*)SaveHTML: %s\n", savehtml);
      }
      if (cmdOpts.containsKey("plugin")) {
         String pluginFile = (String)cmdOpts.get("plugin");
         System.out.printf("(*)Loading Plugins from file: '%s'.\n",
                           pluginFile);
         try {
            plugParser = new SodaPluginParser(pluginFile);
            plugins = plugParser.parse();
         } catch (Exception e) {
            /* XXX: Just what Exception does SodaPluginParser actually throw? */
            System.err.println("(!)Failed to load plugin file: " + e);
            System.exit(1);
         }
      }
      if (cmdOpts.containsKey("blocklistfile")) {
         String f = (String)cmdOpts.get("blocklistfile");
         SodaBlockListParser sbp = new SodaBlockListParser(f);
         blockList = sbp.parse();
      } else {
         blockList = new SodaBlockList();
      }
      if (cmdOpts.containsKey("resultdir")) {
         resultdir = (String)cmdOpts.get("resultdir");
      } else {
         Date now = new Date();
         String frac = String.format("%1$tN", now);
         String dstr = String.format("%1$tm-%1$td-%1$tY-%1$tI-%1$tM-%1$tS",
                                     now);
         frac = frac.subSequence(0, 3).toString();
         dstr += String.format(".%s", frac);

         resultdir = System.getProperty("user.dir") + File.separator + dstr;
      }

      dumpJavaInfo();

      try {
         ArrayList<String> cmdSuites = (ArrayList<String>)cmdOpts.get("suite");
         if ((cmdSuites != null) && (!cmdSuites.isEmpty())) {

            if (SodaSuitesList == null) {
               SodaSuitesList = new ArrayList<String>();
            }
            SodaSuitesList.addAll(cmdSuites);
         }

         if ((SodaSuitesList != null) && (!SodaSuitesList.isEmpty())) {
            RunSuites(SodaSuitesList, resultdir, browserType, gvars, hijacks,
                      blockList, plugins, savehtml, downloadDir, assertpage,
                      restartTest, restartCount, attachTimeout, haltOnFailure);
         }

         ArrayList<String> cmdTests = (ArrayList<String>)cmdOpts.get("tests");
         if (cmdTests != null && !cmdTests.isEmpty()) {
            if (SodaTestsList == null) {
               SodaTestsList = new ArrayList<String>();
            }

            SodaTestsList.addAll(cmdTests);
         } else {
            SodaTestsList = new ArrayList<String>();
         }

         if (!SodaTestsList.isEmpty()) {
            RunTests(SodaTestsList, resultdir, browserType, gvars, hijacks,
                     plugins, savehtml, downloadDir, assertpage, attachTimeout,
                     haltOnFailure);
         }
      } catch (Exception exp) {
         exp.printStackTrace();
      }

      System.out.printf("(*)VooDooDriver Finished.\n");
      System.exit(0);
   }


   /**
    * Run the tests specified on the command line with --test
    *
    * @param tests          array of tests to run
    * @param resultdir      output directory
    * @param browserType    which browser to test against
    * @param gvars          test global variables
    * @param hijacks        hijacks
    * @param plugins        plugins to use during test execution
    * @param savehtml       value of --savehtml command line option
    * @param downloaddir    download directory
    * @param assertpage     assert page
    * @param attachTimeout  attach timeout
    * @param haltOnFailure  value of --haltOnFailure command line option
    */

   private static void RunTests(ArrayList<String> tests, String resultdir,
                                SodaSupportedBrowser browserType,
                                SodaHash gvars, SodaHash hijacks,
                                SodaEvents plugins, String savehtml,
                                String downloaddir, String assertpage,
                                int attachTimeout, Boolean haltOnFailure) {
      File resultFD = null;
      SodaBrowser browser = null;
      int len = 0;
      SodaTest testobj = null;

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
         browser = new SodaFirefox();
         break;
      case CHROME:
         browser = new SodaChrome();
         break;
      case IE:
         browser = new SodaIE();
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

         testobj = new SodaTest(test_file, browser, gvars, hijacks, null,
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
    * @param suites
    * @param resultdir      output directory
    * @param browserType    which browser to test against
    * @param gvars          test global variables
    * @param hijacks        hijacks
    * @param blockList      
    * @param plugins        plugins to use during test execution
    * @param savehtml       value of --savehtml command line option
    * @param downloaddir    download directory
    * @param assertpage     assert page
    * @param restartTest    restart the browser when running this test
    * @param restartCount   restart the browser after running this number
    *                       of tests
    * @param attachTimeout  attach timeout
    * @param haltOnFailure  value of --haltOnFailure command line option
    */

   private static void RunSuites(ArrayList<String> suites, String resultdir,
                                 SodaSupportedBrowser browserType,
                                 SodaHash gvars, SodaHash hijacks,
                                 SodaBlockList blockList, SodaEvents plugins,
                                 String savehtml, String downloaddir,
                                 String assertpage, String restartTest,
                                 int restartCount, int attachTimeout,
                                 Boolean haltOnFailure) {
      int len = suites.size() -1;
      File resultFD = null;
      String report_file_name = resultdir;
      String hostname = "";
      FileOutputStream suiteRptFD = null;
      SodaBrowser browser = null;
      Date now = null;
      Date suiteStartTime = null;
      Date suiteStopTime = null;
      Boolean terminateRun = false;

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
         browser = new SodaFirefox();
         break;
      case CHROME:
         browser = new SodaChrome();
         break;
      case IE:
         browser = new SodaIE();
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
         SodaTest testobj = null;
         System.out.printf("(*)Executing Suite: %s\n", suite_base_name);
         System.out.printf("(*)Parsing Suite file...\n");
         SodaSuiteParser suiteP = new SodaSuiteParser(suite_name, gvars);
         SodaTestList suite_test_list = suiteP.getTests();
         SodaHash vars = null;
         SodaTestResults test_results_hash = null;
         ArrayList<SodaTestResults> test_resultsStore =
            new ArrayList<SodaTestResults>();

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

                  testobj = new SodaTest(restartTest, browser, gvars, hijacks,
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
                  String msg = SodaUtils.GetRunTime(test_start_time, now);
                  writeSummary(suiteRptFD,
                               String.format("\t\t\t<totaltesttime>%s</totaltesttime>\n", msg));

                  if (testobj.getSodaEventDriver() != null) {
                     vars = testobj.getSodaEventDriver().getSodaVars();
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

            testobj = new SodaTest(current_test, browser, gvars, hijacks,
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
            String msg = SodaUtils.GetRunTime(test_start_time, now);
            writeSummary(suiteRptFD,
                         String.format("\t\t\t<totaltesttime>%s</totaltesttime>\n", msg));

            if (testobj.getSodaEventDriver() != null) {
               vars = testobj.getSodaEventDriver().getSodaVars();
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
                                    SodaUtils.GetRunTime(suiteStartTime,
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
