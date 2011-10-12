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

package vddlogger;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VddSuiteReporter {

	private static String HTML_HEADER_RESOURCE = "suitereporter-header.txt";
	private File[] filesList = null;
	private int count = 0;
	private String suiteName = null;
	private FileReader input = null;
	private BufferedReader br = null;
	private String strLine, tmp = null;
	private FileOutputStream output = null;
	private PrintStream repFile = null;
	private String suiteDir = "";
	
	public VddSuiteReporter(String suitename, String basedir, ArrayList<HashMap<String, String>> logfiles) {
		String filepath = "";
		this.suiteName = suitename;
		String outputDir = String.format("%s%s%s", basedir, File.separatorChar, suitename);
		System.out.printf("(*)SuiteReporter OutputDir: %s\n", outputDir);
		ArrayList<File> tmpFiles = new ArrayList<File>();
		
		for (int i = 0; i <= logfiles.size() -1; i++) {
			HashMap<String, String> log = logfiles.get(i);
			if (!Boolean.valueOf(log.get("isrestart"))) { 
				File tfile = new File(log.get("testlog"));
				String logfilename = String.format("%s%s%s",outputDir, File.separatorChar, tfile.getName());
				tmpFiles.add(new File(logfilename));
			}
		}
		
		filesList = tmpFiles.toArray(new File[0]);
		
		/**
		 * set up file output
		 */
		try {
			this.suiteDir = String.format("%s%s%s", basedir, File.separatorChar, suitename);
			filepath = String.format("%s%s%s.html", outputDir, File.separatorChar,suitename);
			output = new FileOutputStream(filepath);
			repFile = new PrintStream(output);
		} catch(Exception e) {
			System.out.printf("(!)Error: Failed trying to write file: '%s'!\n", filepath);
			e.printStackTrace();
		}
	}
	
	/**
	 * generates a html report file
	 */
	public void generateReport() {
		generateHTMLHeader();
		
		/**
		 * find files in folder that ends with .log, and process them
		 */
		for (int i = 0; i < filesList.length; i++) {
			//ignores nonfiles and hidden files
			if (filesList[i].isFile() && !filesList[i].isHidden()) {
				//read if is .log file
				if (filesList[i].getName().endsWith("log")) {
					readNextLog(filesList[i]);
					//remove the .log extention
					String temp = filesList[i].getName().substring(0, filesList[i].getName().indexOf("."));
					//get last line
					try {
						while ((tmp = br.readLine()) != null) {
							strLine = tmp;
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
					//find log status, generate table row
					
					if (strLine.contains("blocked:1")) {
						generateTableRow(temp, 2, null);
					} else if (strLine.contains("result:-1")) {
						generateTableRow(temp, 0, strLine);
					} else {
						generateTableRow(temp, 1, null);
					}
					
					String logname = String.format("%s%s%s", this.suiteDir, File.separatorChar, filesList[i].getName());
					System.out.printf("(*)Log File: %s\n", logname);
					VddLogToHTML log2html = new VddLogToHTML(logname);
					log2html.generateReport();
				}
			}
		}
		
		repFile.print("\n</table>\n</body>\n</html>\n");
		repFile.close();
	}
	
	private HashMap<String, String>findErrorInfo(String line) {
		HashMap<String, String> result = new HashMap<String, String>();
		String[] items = {
			"failedasserts",
			"exceptions",
			"errors",
			"watchdog"
		};
		Pattern p = null;
		Matcher m = null;
		
		for (int i = 0; i <= items.length -1; i++) {
			String value = "";
			String reg = String.format("%s:(\\d+)", items[i]);
			p = Pattern.compile(reg, Pattern.CASE_INSENSITIVE);
			
			m = p.matcher(line);
			if (!m.find()) {
				System.out.printf("(!)Error: Failed to find needed matches when parsing a failed tests results line!\n");
				System.out.printf("--)Line: '%s'\n\n", line);
				value = "";
			} else {
				value = m.group(1);
			}
			
			result.put(items[i], value);
		}
		
		return result;
	}
	
	private String GenMiniErrorTable(String line) {
		String result = "";
		String exceptions = "";
		String watchdog = "";
		String fasserts = "";
		String errors = "";
		String color = "";
		
		findErrorInfo(line);

		try {
			HashMap<String, String> data = this.findErrorInfo(line);
			fasserts = data.get("failedasserts");
			exceptions = data.get("exceptions");
			watchdog = data.get("watchdog");
			errors = data.get("errors");
			
			result = "\t<td class=\"td_issues_data\">\n\t<table class=\"table_sub\">\n"+
			"\t<tr>\n";
			
			if (Integer.valueOf(watchdog) > 0) {
				color = "#FF0000";
			} else {
				color = "#000000";
			}
         
         result += "\t\t<td class=\"td_sub\">"+
               "WD:&nbsp;<font color=\"" + color + "\">" + watchdog + "</font></td>\n";
         
         if (Integer.valueOf(exceptions) > 0) {
				color = "#FF0000";
			} else {
				color = "#000000";
			}
			
			result += "\t\t<td class=\"td_sub\">Exp:&nbsp;<font color=\"" +
					color + "\">" + exceptions + "</font></td>\n";
			
			if (Integer.valueOf(fasserts) > 0) {
				color = "#FF0000";
			} else {
				color = "#000000";
			}
         
         result += "\t\t<td class=\"td_sub\">"+
               "FA:&nbsp;<font color=\"" + color + "\">" + fasserts + "</font></td>\n";
			
         if (Integer.valueOf(errors) > 0) {
				color = "#FF0000";
			} else {
				color = "#000000";
			}
         
         result += "\t\t<td class=\"td_sub\">"+
               "E:&nbsp;<font color=\"" + color + "\">" + errors + "</font></td>\n"+
               "\t</tr>\n\t</table>\n\t</td>\n";
			
		} catch (Exception exp) {
			exp.printStackTrace();
		}
		
		return result;
	}
	
	/**
	 * generates a html table row based on data from .log report file
	 * 
	 * @param fileName - name of the .log report file this table row is representing
	 * @param failed - status of the test. 0 = failed, 1 = passed, 2 = blocked
	 * 
	 * Note:
	 * I find it odd that the intern needed to change failed = 0, when -1 was already
	 * 0 and 0 always means success in C programming...
	 * 
	 */
	public void generateTableRow(String fileName, int status, String line){
		String html = "\t<td class=\"td_issues_data\"></td>\n";
		String tmp_filename = fileName;
		
		tmp_filename = tmp_filename.replaceAll("-\\d+-\\d+-\\d+-\\d+-\\d+-\\d+-\\d+", "");
		
		count ++;
		repFile.println("<tr id=\""+count+"\" onMouseOver=\"this.className='highlight'\" "+
				"onMouseOut=\"this.className='tr_normal'\" class=\"tr_normal\" >");
		repFile.println("\t<td class=\"td_file_data\">"+count+"</td>");
		repFile.println("\t<td class=\"td_file_data\">"+tmp_filename+".xml</td>");
		
		switch (status) {
			case 0:
				html = GenMiniErrorTable(line);
				html += "\t<td class=\"td_failed_data\">Failed</td>";
				repFile.println(html);
			break;
			
			case 1:
				html += "\t<td class=\"td_passed_data\">Passed</td>";
				repFile.println(html);	
			break;
			
			default:
				html += "\t<td class=\"_data\">Blocked</td>";
				repFile.println(html);
		}
		
		repFile.println("\t<td class=\"td_report_data\"><a href='Report-"+fileName+".html'>Report Log</a></td>");
		repFile.println("</tr>");
	}
	
	/**
	 * generates the html header for the report file
	 */
	private void generateHTMLHeader() {
		String title = "suite "+suiteName+".xml test results";
		String header = "";
		InputStream stream = null;
		String line = null;
		boolean found_title = false;
		boolean found_suitename = false;
		
		try {
			String className = this.getClass().getName().replace('.', '/');
			String classJar =  this.getClass().getResource("/" + className + ".class").toString();
			
			if (classJar.startsWith("jar:")) {
				stream = getClass().getResourceAsStream(HTML_HEADER_RESOURCE);
			} else {
				File header_fd = new File(getClass().getResource(HTML_HEADER_RESOURCE).getFile());
				stream = new FileInputStream(header_fd);
			}
			
			InputStreamReader in = new InputStreamReader(stream);
			BufferedReader br = new BufferedReader(in);
			
			while ((line = br.readLine()) != null) {
				if ( (found_title != true) && (line.contains("__TITLE__"))) {
					line = line.replace("__TITLE__", title);
					found_title = true;
				}
				
				if ((found_suitename != true) && (line.contains("__SUITENAME__")) ) {
					line = line.replace("__SUITENAME__", suiteName);
					found_suitename = true;
				}
				
				header += line;
				header += "\n";
			}
			
		} catch (Exception exp) {
			exp.printStackTrace();
		}
		
		repFile.print(header);
		
	}
	
	/**
	 * sets up FileReader and BufferedReader for the next report file
	 * @param inputFile - a properly formatted .log SODA report file
	 */
	private void readNextLog(File inputFile) {
		
		try {
			input = new FileReader(inputFile);
			br = new BufferedReader(input);
		} catch (FileNotFoundException e) {
			System.out.printf("(!)Error: Failed to find file: '%s'!\n", inputFile);
		} catch (Exception e) {
			System.out.printf("(!)Error: Reading file: '%s'!\n", inputFile);
		}
	}
	
}
