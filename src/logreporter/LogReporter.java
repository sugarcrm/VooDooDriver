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

package logreporter;

import java.io.*;
import java.util.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

public class LogReporter {
	private static void printUsage() {
		String msg = "LogReporter - goes through each folder in given directory and generates html version of SODA log files,\n" +
				"report of each suite, and a summary of all suites\n\n"+
		"Usage: LogReporter.jar  --help --suitedir=\"path/to/suite/directory\" "+
				"--suite=\"/path/to/single/suite/diectory\"... \n\n" +
				"flags: \n" +
				"--suite:\t pass in a single suite file\n" +
				"--suitedir:\t pass in a directory of suites\n" +
				"--help:\t\t displays this message \n";
		
		System.out.printf("%s\n", msg);
	}
	
	private static ArrayList<String> getSuiteFiles(String path) {
		ArrayList<String> result = new ArrayList<String>();
		File dir = new File(path);
		File[] files;
		
		if (!dir.exists()) {
			System.out.printf("(!)Error: Directory doesn't exist: '%s'!\n", path);
			result = null;
			return result;
		}
		
		files = dir.listFiles();
		for (int i = 0; i <= files.length -1; i++) {
			String name = files[i].getName();
			name = name.toLowerCase();
			if (name.endsWith(".xml")) {
				result.add(files[i].getAbsolutePath());
			}
		}
		
		return result;
	}
	
	/**
	 * processes the list of arguments, and returns a HashMap of options for main to use
	 * @return 
	 */
	private static HashMap<String, Object> cmdLineOptions(String[] args) {
		HashMap<String, Object> options = new HashMap<String, Object>();
		
		try {
			for (int i = 0; i < args.length; i++) {
				if (args[i].contains("--help")) {
					options.put("help", true);
				} else if (args[i].contains("--suite=")) {
					String str = args[i];
					str = str.replace("--suite=", "");

					if (!str.endsWith("/")) {
						str = str + "/";
					}
					System.out.println("Processing suite: "+str);
					options.put("suite", str);
				} else if (args[i].contains("--suitedir=")) {
					String str = args[i];
					str = str.replace("--suitedir=", "");

					if (!str.endsWith("/")) {
						str = str + "/";
					}
					
					options.put("suitedir", str);
				}
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		return options;
	}
	
	public static ArrayList<HashMap<String, String>> parseSuiteFile(String filename) {
		ArrayList<HashMap<String, String>> result = null;
		result = new ArrayList<HashMap<String,String>>();
		File fd = new File(filename);
		DocumentBuilderFactory dbf = null;
		DocumentBuilder db = null;
		Document doc = null;
		
		if (!fd.exists()) {
			result = null;
			return result;
		}

		try {
			dbf = DocumentBuilderFactory.newInstance();
			db = dbf.newDocumentBuilder();
			doc = db.parse(fd);
			NodeList nodes = doc.getDocumentElement().getChildNodes();
			
			for (int i = 0; i <= nodes.getLength() -1; i++) {
				String name = nodes.item(i).getNodeName();
				if (!name.contains("suite")) {
					continue;
				}
				System.out.printf("(*)Node Name: %s\n", name);
			}
			
		} catch (Exception exp) {
			exp.printStackTrace();
			result = null;
		}
		
		return result;
	}
	
	public static void main(String[] args) {
		
		try {
			HashMap<String, Object> options = cmdLineOptions (args);
			if (options.isEmpty()) {
				printUsage();
				System.exit(0);
			}
			
			if (options.containsKey("help")) {
				printUsage();
				System.exit(0);
			}
			
			if (options.containsKey("suitedir")) {
				String path = (String)options.get("suitedir");
				ArrayList<String> suiteFiles = null;
				suiteFiles = getSuiteFiles(path);
				
				LogConverter htmlLogs = new LogConverter();
				
				if (suiteFiles.size() < 1) {
					System.out.printf("(*)There are no directories to generate log reports from.\n");
				}
				
				for (int i = 0; i <= suiteFiles.size() -1; i++) {
					String sfile = suiteFiles.get(i);
					System.out.printf("(*)Processing suite file: '%s'.\n", sfile);
					VDDSuiteResult suiteResult = new VDDSuiteResult(sfile);
					ArrayList<HashMap<String, String>> data = suiteResult.parse();
					
					for (int suiteIndex = 0; suiteIndex <= data.size() -1; suiteIndex++) {
						HashMap<String, String> testInfo = data.get(suiteIndex);
 						String testlog = testInfo.get("testlog");
 						System.out.printf("TEST LOG: %s\n", testlog);
						htmlLogs = new LogConverter(testlog);
						htmlLogs.generateReport();
					}
					
				}

				SuiteReporter suiteSummary = new SuiteReporter(new File(path));
				suiteSummary.generateReport();
				System.out.printf("(*)Summary creation finished.\n");
			}
			
			} catch (Exception exp) {
				exp.printStackTrace();
			}
	}
}
	