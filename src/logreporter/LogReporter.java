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
	
	/**
	 * from the directory path, return an ArrayList of either xml files or folders containing .log files
	 * @param path - path of the directory to get folders
	 * @param getFolder - returns ArrayList of folders if true, ArrayList of files if false
	 * @return ArrayList of folders/files
	 */
	private static ArrayList<File> getFolderContent(String path, boolean getFolder) {
		ArrayList<File> list = new ArrayList<File>();
		File folder = new File(path);
		File[] filesList = folder.listFiles();
		
		/**
		 * look through the given directory
		 */
		for (int i = 0; i < filesList.length; i++) {
			if (getFolder) {
				if (filesList[i].isDirectory() && !filesList[i].isHidden()) {
					list.add(filesList[i]);
				}
			} else {
				if (filesList[i].isFile() && !filesList[i].isHidden() && filesList[i].getName().endsWith("xml")) {
					list.add(filesList[i]);
				}
			}
		}	
		
		return list;
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
	
	public static void main(String[] args){
		int count = 0;
		
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
				ArrayList<File> foldersList = getFolderContent(path, true);
				LogConverter htmlLogs = new LogConverter();
				File[] filesList;
				
				if (foldersList.size() == 0) {
					System.out.printf("(*)There are no directories to generate log reports from.\n");
				}
				
				for (int i = 0; i < foldersList.size(); i++) {
					System.out.printf("(*)Checking directory: '%s'.\n", foldersList.get(i).getName());
					filesList = foldersList.get(i).listFiles();
					
					for (int j = 0; j < filesList.length; j ++) {
						//check is file, not hidden, and is a log file
						if (!filesList[j].isHidden() && filesList[j].isFile() && filesList[j].getName().endsWith("log")) {
							htmlLogs = new LogConverter(filesList[j].getAbsolutePath());
							htmlLogs.generateReport();
							count ++;
						}
					}
					
					if (count == 0) {
						System.out.printf("(*)No log reports found.\n");
					} else {
						System.out.printf("(*)Generated '%d' reports.\n", count);
					}
					
					if (count != 0) {
						System.out.printf("(*)Creating test summary for suite: '%s'.\n", foldersList.get(i).getAbsolutePath());
						SuiteReporter suiteSummary = new SuiteReporter(foldersList.get(i));
						suiteSummary.generateReport();
						System.out.printf("(*)Summary creation finished.\n");
					}
					count = 0;
				}
				
				//generate summary of suites
				if (getFolderContent(path, false).size() == 0) {
					System.out.printf("(*)There are no files containing suite test information.\n");
				} else {
					SummaryReporter summaryReport = new SummaryReporter(getFolderContent(path, false), path);
					summaryReport.generateReport();
					System.out.printf("(*)Generated summary.html.\n");
				}	
			} else if (options.containsKey("suite")) {
				String path = (String)options.get("suite");
				File folder = new File(path);
				File[] filesList = folder.listFiles();
				//generate log reports
				LogConverter htmlLogs = new LogConverter();
				//look though folder
				System.out.printf("(*)'%d' file found in directory '%s'.\n", filesList.length, path);
				
				for (int i = 0; i < filesList.length; i ++) {
					if (!filesList[i].isHidden() && filesList[i].isFile() && filesList[i].getName().endsWith("log")) {
						htmlLogs = new LogConverter(filesList[i].getAbsolutePath());
						htmlLogs.generateReport();
						count ++;
					}
				}
				
				if (count == 0) {
					System.out.printf("(*)No log reports found.\n");
				} else {
					System.out.printf("(*)Generated '%d' report(s).\n", count);
				}

				if (count != 0) {
					System.out.printf("(*)Writing test summary for suite: '%s'.\n", path);
					SuiteReporter suiteSummary = new SuiteReporter(folder);
					suiteSummary.generateReport();
					System.out.printf("(*)Finished writing summary.\n");	
				}
				count = 0;
			}
			
		} catch(NullPointerException e) {
			System.out.printf("(!)Error: Invalid path!\n");
		} catch(Exception e) {
			e.printStackTrace();
		}	
	}
}
