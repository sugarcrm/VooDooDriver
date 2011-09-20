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

package voodoodriver;

import java.util.ArrayList;

import org.apache.commons.io.FilenameUtils;

/**
 * A class for parsing all the of needed command line options for voodoodriver.
 * 
 * @author trampus
 *
 */
public class SodaCmdLineOpts {

	private SodaHash options = null;
	private SodaHash gvars = null;
	private ArrayList<String> tests = null;
	private ArrayList<String> suites = null;
	private String flavor = null;
	private Boolean saveHtml = false;
	private SodaHash hijacks = null;
	private String resultDir = null;
	private String blocklistFile = null;
	private Boolean version = false;
	private String browser = null;
	private Boolean testdelay = false;
	private Boolean skipcssErrors = false;
	private Boolean help = false;
	private String profile = null;
	private String plugin = null;
	private String configfile = null;
	private String downloaddir = null;
	private String assertPageFile = null;
	private int restartcount = 0;
	private String restarttest = null;
	
	public SodaCmdLineOpts(String[] args) {
		
		try {
			this.gvars = new SodaHash();
			this.hijacks = new SodaHash();
			this.tests = new ArrayList<String>();
			this.suites = new ArrayList<String>();
			
			for (int i = 0; i <= args.length -1; i++) {
				if (args[i].contains("--hijack")) {
					this.handleHijackValue(args[i]);
				} else if(args[i].contains("--gvar")) {
					this.handleGvarsValue(args[i]);
				} else if (args[i].contains("--suite")) {
					handleSuites(args[i]);
				} else if (args[i].contains("--test")) {
					handleTests(args[i]);
				} else if (args[i].contains("--browser")) {
					args[i] = args[i].replaceAll("--browser=", "");
					this.browser = args[i];
					System.out.printf("(*)Browser: %s\n", this.browser);
				} else if (args[i].contains("--flavor")) {
					args[i] = args[i].replaceAll("--flavor=", "");
					this.flavor = args[i];
					System.out.printf("(*)Flavor: %s\n", this.flavor);
				} else if (args[i].contains("--savehtml")) {
					this.saveHtml = true;
					System.out.printf("(*)SaveHTML: %s\n", this.saveHtml);
				} else if (args[i].contains("--version")) {
					this.version = true;
					System.out.printf("(*)Version: %s\n", this.version);
				} else if (args[i].contains("--resultdir")) {
					args[i] = args[i].replaceAll("--resultdir=", "");
					this.resultDir = args[i];
					this.resultDir = FilenameUtils.separatorsToSystem(this.resultDir);
					System.out.printf("(*)Result Dir: %s\n", this.resultDir);
				} else if (args[i].contains("--blocklistfile")) {
					args[i] = args[i].replaceAll("--blocklistfile=", "");
					this.blocklistFile = args[i];
					this.blocklistFile = FilenameUtils.separatorsToSystem(this.blocklistFile);
					System.out.printf("(*)Blocklistfile: %s\n", this.blocklistFile);
				} else if (args[i].contains("--testdelay")) {
					this.testdelay = true;
					System.out.printf("(*)Testdelay: %s\n", this.testdelay);
				} else if (args[i].contains("--skipcsserrors")) {
					this.skipcssErrors = true;
					System.out.printf("(*)Skip CSS Errors: %s\n", this.skipcssErrors);
				} else if (args[i].contains("--help")) {
					this.help = true;
				} else if (args[i].contains("--help")) {
					this.profile = args[i];
					System.out.printf("(*)Browser Profile: %s\n", this.profile);
				} else if (args[i].contains("--plugin")) {
					this.plugin = args[i];
					this.plugin = this.plugin.replace("--plugin=", "");
					System.out.printf("(*)VooDooDriver Plugin File: %s\n", this.plugin);
				} else if (args[i].contains("--config")) {
					this.configfile = args[i];
					this.configfile = this.configfile.replace("--config=", "");
					this.configfile = FilenameUtils.separatorsToSystem(this.configfile);
					System.out.printf("(*)VooDooDriver Config File: %s\n", this.configfile);
				} else if (args[i].contains("--downloaddir")) {
						this.downloaddir = args[i];
						this.downloaddir = this.downloaddir.replace("--downloaddir=", "");
						this.downloaddir = FilenameUtils.separatorsToSystem(this.downloaddir);
						System.out.printf("(*)Download Directory: %s\n", this.downloaddir);
				 } else if (args[i].contains("--assertpagefile")) {
						this.assertPageFile = args[i];
						this.assertPageFile = this.assertPageFile.replace("--assertpagefile=", "");
						this.assertPageFile = FilenameUtils.separatorsToSystem(this.assertPageFile);
						System.out.printf("(*)Assertpagefile: %s\n", this.assertPageFile);
				 } else if (args[i].contains("--restartcount")) {
					 this.restartcount = Integer.valueOf(args[i]);
				 } else if (args[i].contains("--restarttest")) {
					 this.restarttest = args[i];
				 }
			}
			
			this.options = new SodaHash();
			this.options.put("skipcsserrors", this.skipcssErrors);
			this.options.put("testdelay", this.testdelay);
			this.options.put("blocklistfile", this.blocklistFile);
			this.options.put("resultdir", this.resultDir);
			this.options.put("version", this.version);
			this.options.put("savehtml", this.saveHtml);
			this.options.put("flavor", this.flavor);
			this.options.put("browser", this.browser);
			this.options.put("tests", this.tests);
			this.options.put("suites", this.suites);
			this.options.put("gvars", this.gvars);
			this.options.put("hijacks", this.hijacks);
			this.options.put("help", this.help);
			this.options.put("profile", this.profile);
			this.options.put("plugin", this.plugin);
			this.options.put("config", this.configfile);
			this.options.put("downloaddir", this.downloaddir);
			this.options.put("assertpagefile", this.assertPageFile);
		} catch (Exception exp) {
			exp.printStackTrace();
			this.options = null;
		}
	}
	
	/**
	 * Parses the --gvar command line option.
	 * 
	 * @param str The gvar command line.
	 */
	private void handleGvarsValue(String str) {
		str = str.replace("--gvar=", "");
		String[] data = str.split("::");
		this.gvars.put("global."+data[0], data[1]);
		System.out.printf("(*)GVar: %s => %s\n", "global."+data[0], data[1]);
	}
	
	/**
	 * Parses the --hijack command line option.
	 * 
	 * @param str The hijack command line.
	 */
	private void handleHijackValue(String str) {
		str = str.replace("--hijack=", "");
		String[] data = str.split("::");
		this.hijacks.put(data[0], data[1]);
		System.out.printf("(*)HiJack: %s => %s\n", data[0], data[1]);
	}

	/**
	 * Parses the --test command line option.
	 * 
	 * @param str The test command line.
	 */
	private void handleTests(String str) {
		str = str.replace("--test=", "");
		str = FilenameUtils.separatorsToSystem(str);
		this.tests.add(str);
		System.out.printf("(*)Test Added: %s\n", str);
	}
	
	/**
	 * Parses the --suite command line option.
	 * 
	 * @param str The suite command line.
	 */
	private void handleSuites(String str) {
		str = str.replace("--suite=", "");
		str = FilenameUtils.separatorsToSystem(str);
		this.suites.add(str);
		System.out.printf("(*)Suite Added: %s\n", str);
	}
	
	/**
	 * Returns the parsed options.
	 * 
	 * @return {@link SodaHash}
	 */
	public SodaHash getOptions() {
		return this.options;
	}
}
