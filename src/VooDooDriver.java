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

public class VooDooDriver {
	
	public static void printUsage() {
		VooDooHelp help = new VooDooHelp();
		help.printHelp();
	}
		
	/**
	 * Dump JVM information to the console and warn if a non-Sun jvm is being used.
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

		if (javainfo.containsKey("java.vendor") && !javainfo.get("java.vendor").contains("Sun Microsystems Inc")) {
			System.out.printf("\n(!)Warning: This is not a 'Sun Microsystems Inc.' JRE/JDK and is not supported!\n");
		}
	}

	@SuppressWarnings("unchecked")
	public static void main(String[] args) {
		String sodaConfigFile = "soda-config.xml";
		File sodaConfigFD = null;
		String blockListFile = null;
		SodaBlockList blockList = null;
		SodaCmdLineOpts opts = null;
		SodaHash cmdOpts = null;
		SodaSupportedBrowser browserType = null;
		ArrayList<String> SodaSuitesList = null;
		ArrayList<String> SodaTestsList = null;
		String pluginFile = null;
		SodaPluginParser plugParser = null;
		SodaEvents plugins = null;
		String savehtml = "";
		SodaConfigParser configParser = null;
		String downloadDir = null;
		String assertpage = null;
		SodaHash gvars = null;
		int restartCount = 0;
		String restartTest = null;
		int attachTimeout = 0;
		SodaHash hijacks = new SodaHash();
		String resultdir = null;
		SodaEvents configFileOpts = null;
		
		System.out.printf("(*)Starting VooDooDriver...\n");
                dumpJavaInfo();
		
		try {
			opts = new SodaCmdLineOpts(args);
			cmdOpts = opts.getOptions();
			gvars = new SodaHash();
			
			if (cmdOpts.containsKey("config")) {
				if (cmdOpts.get("config") != null) {
					sodaConfigFile = cmdOpts.get("config").toString();
					System.out.printf("(*)Overwriting default config file to: '%s'.\n", sodaConfigFile);
				}
			}
			
			sodaConfigFD = new File(sodaConfigFile);
			if (sodaConfigFD.exists()) {
				System.out.printf("(*)Found VooDooDriver config file: %s\n", sodaConfigFile);
				configParser = new SodaConfigParser(sodaConfigFD);
				configFileOpts = configParser.parse();
				int argsLen = configFileOpts.size() -1;
				
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
							System.out.printf("(*)Adding Config-File gvar: '%s' => '%s'.\n", name, value);
						}	
					} else if (type.contains("cmdopt")) {
						name = tmp.get("name").toString();
						value = tmp.get("value").toString();
						if (name.contains("browser")) {
							cmdOpts.put("browser", value);
							System.out.printf("(*)Adding Confile-File cmdopts: '%s' => '%s'.\n", name, value);
						} else if (name.contains("attachtimeout")) {
							cmdOpts.put("attachtimeout", value);
						} else if (name.contains("resultdir")) {
							System.out.printf("(*)Adding Confile-File cmdopts: '%s' => '%s'.\n", name, value);
							resultdir = value;
						} else if (name.contains("savehtml")) {
							System.out.printf("(*)Adding Confile-File cmdopts: '%s' => '%s'.\n", name, value);
							savehtml = value;
						}
					} else if (type.contains("hijacks")) {
						ArrayList<String> jacks = (ArrayList<String>)tmp.get("hijacks");
						
						for (int x = 0; x <= jacks.size() -1; x++) {
							String[] jdata = jacks.get(x).split("::");
							hijacks.put(jdata[0], jdata[1]);
							System.out.printf("(*)Adding Config-File hijack: '%s' => '%s'\n", jdata[0], jdata[1]);
						}
						
					} else if (type.contains("suites")) {
						SodaSuitesList = (ArrayList<String>)tmp.get("suites");
						for (int x = 0; x <= SodaSuitesList.size() -1; x++) {
							String sname = SodaSuitesList.get(x);
							System.out.printf("(*)Adding Config-File suite file: '%s'\n", sname);
						}
					} else if (type.contains("tests")) {
						SodaTestsList = (ArrayList<String>)tmp.get("tests");
						for (int x = 0; x <= SodaTestsList.size() -1; x++) {
							String tname = SodaTestsList.get(x);
							System.out.printf("(*)Adding Config-File test file: '%s'\n", tname);
						}
					}
				}
			}

			gvars.putAll((SodaHash)cmdOpts.get("gvars"));
			hijacks.putAll((SodaHash)cmdOpts.get("hijacks"));
			
			if ((Boolean)cmdOpts.get("help")) {
				printUsage();
				System.exit(0);
			}
			
			if ((Boolean)cmdOpts.get("version")) {
				VDDVersionInfo vinfo = new VDDVersionInfo();
				System.out.printf("(*)VooDooDriver Version: %s\n", vinfo.getVDDVersion());
				System.exit(0);
			}
			
			attachTimeout = (Integer)cmdOpts.get("attachtimeout");
			restartTest = (String)cmdOpts.get("restarttest");
			restartCount = (Integer)cmdOpts.get("restartcount");
			
			if (attachTimeout > 0) {
				System.out.printf("(*)Setting the default Attach Timeout to: '%s' seconds.\n", attachTimeout);
			}
			
			if (restartCount > 0) {
				System.out.printf("(*)Restart Count => '%d'\n", restartCount);
				
				if (restartTest != null) {
					System.out.printf("(*)Restart Test => '%s'.\n", restartTest);
					File retmp = new File(restartTest);
					
					if (!retmp.exists()) {
						System.out.printf("(!)Error: failed to find Restart Test: => '%s'!\n\n", restartTest);
						System.exit(5);
					}
				}
			}
			
			if (cmdOpts.get("browser") == null) {
				System.out.printf("(!)Error: Missing --browser commandline option!\n\n");
				System.exit(-1);
			}
			
			if (cmdOpts.get("assertpagefile") != null) {
				assertpage = cmdOpts.get("assertpagefile").toString(); 	
			}
			
			if (cmdOpts.get("downloaddir") != null) {
				downloadDir = cmdOpts.get("downloaddir").toString();
			}
			
			String cmdSaveHtml = (String)cmdOpts.get("savehtml");
			if (cmdSaveHtml != null && !cmdSaveHtml.isEmpty()) {
				savehtml = cmdSaveHtml; 
			}
			System.out.printf("(*)SaveHTML: %s\n", savehtml);
			
			pluginFile = (String)cmdOpts.get("plugin");
			if (pluginFile != null) {
				pluginFile = FilenameUtils.separatorsToSystem(pluginFile);
				System.out.printf("(*)Loading Plugins from file: '%s'.\n", pluginFile);
				plugParser = new SodaPluginParser(pluginFile);
				plugins = plugParser.parse();
			}
			
			try {
				browserType = SodaSupportedBrowser.valueOf(cmdOpts.get("browser").toString().toUpperCase());
			} catch (Exception expBrowser) {
				System.out.printf("(!)Unsupported browser: '%s'!\n", cmdOpts.get("browser").toString());
				System.out.printf("(!)Exiting!\n\n");
				System.exit(2);
			}
			
			blockListFile = (String)cmdOpts.get("blocklistfile");
			if (blockListFile != null) {
				blockListFile = FilenameUtils.separatorsToSystem(blockListFile);
				SodaBlockListParser sbp = new SodaBlockListParser(blockListFile);
				blockList = sbp.parse();
			} else {
				System.out.printf("(*)No Block list file to parse.\n");
				blockList = new SodaBlockList();
			}
			
			String cmdResultdir = (String)cmdOpts.get("resultdir");
			if (cmdResultdir != null && !cmdResultdir.isEmpty()) {
				resultdir = cmdResultdir; 
			}
			if (resultdir == null) {
				Date now = new Date();
				String cwd = System.getProperty("user.dir");
				String frac = String.format("%1$tN", now);
				String date_str = String.format("%1$tm-%1$td-%1$tY-%1$tI-%1$tM-%1$tS", now);
				frac = frac.subSequence(0, 3).toString();
				date_str += String.format(".%s", frac);

				cwd = cwd.concat("/");
				cwd = cwd.concat(date_str);
				cwd = FilenameUtils.separatorsToSystem(cwd);
				resultdir = cwd;
			}
			
			ArrayList<String> cmdSuites = (ArrayList<String>)cmdOpts.get("suites");
			if ((cmdSuites != null) && (!cmdSuites.isEmpty())) {
				
				if (SodaSuitesList == null) {
					SodaSuitesList = new ArrayList<String>();
				}
				SodaSuitesList.addAll(cmdSuites);
			}

			if ((SodaSuitesList != null) && (!SodaSuitesList.isEmpty())) {
				if (resultdir == null) {
					System.out.printf("(!)Error: Missing command line flag --resultdir!\n");
					System.out.printf("--)--resultdir is needed when running SODA suites.\n\n");
					System.exit(3);
				}
				
				RunSuites(SodaSuitesList, resultdir, browserType, gvars, hijacks, blockList, plugins, savehtml, downloadDir,
						assertpage, restartTest, restartCount, attachTimeout);
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
						plugins, savehtml, downloadDir, assertpage, attachTimeout);
			}
		} catch (Exception exp) {
			exp.printStackTrace();
		}
		
		System.out.printf("(*)VooDooDriver Finished.\n");
		System.exit(0);
	}
	
	private static void RunTests(ArrayList<String> tests, String resultdir, SodaSupportedBrowser browserType,
			SodaHash gvars, SodaHash hijacks, SodaEvents plugins, String savehtml, String downloaddir, 
			String assertpage, int attachTimeout) {
		File resultFD = null;
		SodaBrowser browser = null;
		int len = 0;
		SodaTest testobj = null;
		
		System.out.printf("(*)Running Soda Tests now...\n");
		
		resultFD = new File(resultdir);
		if (!resultFD.exists()) {
			System.out.printf("(*)Result directory doesn't exist, trying to create dir: '%s'\n", resultdir);
			try {
				resultFD.mkdirs();
			} catch (Exception exp) {
				System.out.printf("(!)Error: Failed to create reportdir: '%s'!\n", resultdir);
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
			
			testobj = new SodaTest(test_file, browser, gvars, hijacks, null, null, null, 
					resultdir, savehtml);
			if (assertpage != null) {
				testobj.setAssertPage(assertpage);
			}
			testobj.setPlugins(plugins);
			testobj.runTest(false);
		}
	}
	
	private static void writeSummary(FileOutputStream in, String msg) {
		try {
			in.write(msg.getBytes());
		} catch (Exception exp) {
			exp.printStackTrace();
		}
	}
	
	private static void RunSuites(ArrayList<String> suites, String resultdir, SodaSupportedBrowser browserType,
			SodaHash gvars, SodaHash hijacks, SodaBlockList blockList, SodaEvents plugins, String savehtml,
			String downloaddir, String assertpage, String restartTest, int restartCount, int attachTimeout) {
		int len = suites.size() -1;
		File resultFD = null;
		String report_file_name = resultdir;
		String hostname = "";
		FileOutputStream suiteRptFD = null;
		SodaBrowser browser = null;
		Date now = null;
		Date suiteStartTime = null;
		Date suiteStopTime = null;
		
		System.out.printf("(*)Running Suite files now...\n");
		System.out.printf("(*)Timeout: %s\n", attachTimeout);
		
		resultFD = new File(resultdir);
		if (!resultFD.exists()) {
			System.out.printf("(*)Result directory doesn't exist, trying to create dir: '%s'\n", resultdir);
			
			try {
				resultFD.mkdirs();
			} catch (Exception exp) {
				System.out.printf("(!)Error: Failed to create reportdir: '%s'!\n", resultdir);
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
		String date_str = String.format("%1$tm-%1$td-%1$tY-%1$tI-%1$tM-%1$tS", now);
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
		
		for (int i = 0; i <= len; i++) {
			String suite_base_noext = "";
			String suite_name = suites.get(i);
			String suite_base_name = "";
			File suite_fd = new File(suite_name);
			suite_base_name = suite_fd.getName();
			int testRanCount = 0;
		
			writeSummary(suiteRptFD, "\t<suite>\n\n");
			writeSummary(suiteRptFD, String.format("\t\t<suitefile>%s</suitefile>\n", suite_base_name));
			
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
			ArrayList<SodaTestResults> test_resultsStore = new ArrayList<SodaTestResults>();
			
			suiteStartTime = new Date();
			for (int test_index = 0; test_index <= suite_test_list.size() -1; test_index++) {
				Date test_start_time = null;
				
				if ( (restartCount > 0) && (testRanCount >= restartCount)) {
					System.out.printf("(*))Auto restarting browser.\n");
					if (!browser.isClosed()) {
						browser.close();
					}
					browser.newBrowser();
					
					if (restartTest != null) {
						System.out.printf("(*)Executing Restart Test: '%s'\n", restartTest);
						writeSummary(suiteRptFD, "\t\t<test>\n");
						writeSummary(suiteRptFD, String.format("\t\t\t<testfile>%s</testfile>\n", restartTest));
						now = new Date();
						test_start_time = now;
						frac = String.format("%1$tN", now);
						date_str = String.format("%1$tm-%1$td-%1$tY-%1$tI-%1$tM-%1$tS", now);
						frac = frac.subSequence(0, 3).toString();
						date_str += String.format(".%s", frac);
						
						writeSummary(suiteRptFD, String.format("\t\t\t<starttime>%s</starttime>\n", date_str));
						
						testobj = new SodaTest(restartTest, browser, gvars, hijacks, blockList, vars, 
								suite_base_noext, resultdir, savehtml);
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
						date_str = String.format("%1$tm-%1$td-%1$tY-%1$tI-%1$tM-%1$tS", now);
						frac = frac.subSequence(0, 3).toString();
						date_str += String.format(".%s", frac);

						writeSummary(suiteRptFD, String.format("\t\t\t<stoptime>%s</stoptime>\n", date_str));
						String msg = SodaUtils.GetRunTime(test_start_time, now);
						writeSummary(suiteRptFD, String.format("\t\t\t<totaltesttime>%s</totaltesttime>\n", msg));
						
						if (testobj.getSodaEventDriver() != null) {
							vars = testobj.getSodaEventDriver().getSodaVars();
						}
						
						test_results_hash = testobj.getReporter().getResults();
						test_resultsStore.add(test_results_hash);
						for (int res_index = 0; res_index <= test_results_hash.keySet().size() -1; res_index++) {
							String key = test_results_hash.keySet().toArray()[res_index].toString();
							String value = test_results_hash.get(key).toString();
							
							if (key.contains("result")) {
								if (Integer.valueOf(value) != 0) {
									value = "Failed";	
								} else {
									value = "Passed";	
								}
							}
							writeSummary(suiteRptFD, String.format("\t\t\t<%s>%s</%s>\n", key, value, key));
						}
						writeSummary(suiteRptFD, "\t\t</test>\n\n");
					}
					
					testRanCount = 0;
				}
				
				writeSummary(suiteRptFD, "\t\t<test>\n");
				String current_test = suite_test_list.get(test_index);
				writeSummary(suiteRptFD, String.format("\t\t\t<testfile>%s</testfile>\n", current_test));
				System.out.printf("(*)Executing Test: '%s'\n", current_test);
				now = new Date();
				test_start_time = now;
				frac = String.format("%1$tN", now);
				date_str = String.format("%1$tm-%1$td-%1$tY-%1$tI-%1$tM-%1$tS", now);
				frac = frac.subSequence(0, 3).toString();
				date_str += String.format(".%s", frac);
				
				writeSummary(suiteRptFD, String.format("\t\t\t<starttime>%s</starttime>\n", date_str));
				
				if (browser.isClosed()) {
					System.out.printf("(*)Browser was closed by another suite, creating new browser...\n");
					browser.newBrowser();
					System.out.printf("(*)New browser created.\n");
				}
				
				testobj = new SodaTest(current_test, browser, gvars, hijacks, blockList, vars, 
						suite_base_noext, resultdir, savehtml);
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
				date_str = String.format("%1$tm-%1$td-%1$tY-%1$tI-%1$tM-%1$tS", now);
				frac = frac.subSequence(0, 3).toString();
				date_str += String.format(".%s", frac);
				writeSummary(suiteRptFD, String.format("\t\t\t<stoptime>%s</stoptime>\n", date_str));
				String msg = SodaUtils.GetRunTime(test_start_time, now);
				writeSummary(suiteRptFD, String.format("\t\t\t<totaltesttime>%s</totaltesttime>\n", msg));
				
				if (testobj.getSodaEventDriver() != null) {
					vars = testobj.getSodaEventDriver().getSodaVars();
				}
				
				test_results_hash = testobj.getReporter().getResults();
				test_resultsStore.add(test_results_hash);
				for (int res_index = 0; res_index <= test_results_hash.keySet().size() -1; res_index++) {
					String key = test_results_hash.keySet().toArray()[res_index].toString();
					String value = test_results_hash.get(key).toString();
					
					if (key.contains("result")) {
						if (Integer.valueOf(value) != 0) {
							value = "Failed";	
						} else {
							value = "Passed";	
						}
					}
					writeSummary(suiteRptFD, String.format("\t\t\t<%s>%s</%s>\n", key, value, key));
				}
				writeSummary(suiteRptFD, "\t\t</test>\n\n");
				
				//Integer watchdog = Integer.valueOf(test_results_hash.get("watchdog").toString());

				
				if (restartCount > 0) {
					File tmpF = new File(current_test);
					File pF = tmpF.getParentFile();
					
					if (pF != null) {
						String path = pF.getAbsolutePath();
						path = path.toLowerCase();
						if (!path.contains("lib")) {
							testRanCount += 1;
							System.out.printf("(*)Tests ran since last restart: '%d'\n", testRanCount);
						}
					} else {
						testRanCount += 1;
						System.out.printf("(*)Tests ran since last restart: '%d'\n", testRanCount);
					}
				}
			}
			
			suiteStopTime = new Date();
			frac = String.format("%1$tN", suiteStopTime);
			frac = frac.subSequence(0, 3).toString();
			String stopTimeStr = String.format("%1$tm-%1$td-%1$tY-%1$tI-%1$tM-%1$tS", suiteStopTime);
			stopTimeStr += String.format(".%s", frac);
			
			frac = String.format("%1$tN", suiteStartTime);
			frac = frac.subSequence(0, 3).toString();
			String startTimeStr = String.format("%1$tm-%1$td-%1$tY-%1$tI-%1$tM-%1$tS", suiteStartTime);
			startTimeStr += String.format(".%s", frac);
			
			
			String msg = String.format("\t\t<runtime>%s</runtime>\n", SodaUtils.GetRunTime(suiteStartTime, suiteStopTime));
			writeSummary(suiteRptFD, String.format("\t\t<starttime>%s</starttime>\n", startTimeStr));
			writeSummary(suiteRptFD, String.format("\t\t<stoptime>%s</stoptime>\n", stopTimeStr));
			writeSummary(suiteRptFD, msg);
			writeSummary(suiteRptFD, "\t</suite>\n");
		}
		writeSummary(suiteRptFD, "</data>\n\n");
	}
}
