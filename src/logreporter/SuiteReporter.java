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

/**
 * Takes in a File type input (should be the directory of .log files), reads all the log files and 
 * generate a nice html summary table of the tests. The generated html will be in the same directory 
 * 
 * @param folder - path of the folder containing SODA report logs
 *
 */
public class SuiteReporter {
	
	private File folder = null;
	private File[] filesList = null;
	private int count = 0;
	private String suiteName = null;
	private FileReader input = null;
	private BufferedReader br = null;
	private String strLine, tmp = null;
	private FileOutputStream output = null;
	private PrintStream repFile = null;
	
	/**
	 * default constructor
	 */
	public SuiteReporter(){
		folder = new File("");
		filesList = new File[0];
		count = 0;
	}
	
	/**
	 * Constructor for class SuiteReporter
	 * @param folder - the File folder in which the suite log files reside.
	 */
	public SuiteReporter(File folder){
		folder = folder;
		filesList = folder.listFiles();
		count = 0;
		suiteName = folder.getName();
		
		/**
		 * set up file output
		 */
		try {
			output = new FileOutputStream(folder.getAbsolutePath()+"/"+suiteName+".html");
			repFile = new PrintStream(output);
		} catch(Exception e) {
			System.err.println("Error writing to file "+suiteName+".html");
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
		for (int i=0; i < filesList.length; i++) {
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
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					//find log status, generate table row
					if (strLine.contains("blocked:1")) {
						generateTableRow(temp, 2);
					} else if (strLine.contains("result:-1")) {
						generateTableRow(temp, 0);
					} else {
						generateTableRow(temp, 1);
					}
				}
			}		
		}
		
		repFile.print("\n</table>\n</body>\n</html>\n");
		repFile.close();
	}
	
	/**
	 * generates a html table row based on data from .log report file
	 * 
	 * @param fileName - name of the .log report file this table row is representing
	 * @param failed - status of the test. 0 = failed, 1 = passed, 2 = blocked
	 */
	public void generateTableRow(String fileName, int status){
		count ++;
		repFile.println("<tr id=\""+count+"\" onMouseOver=\"this.className='highlight'\" "+
				"onMouseOut=\"this.className='tr_normal'\" class=\"tr_normal\" >");
		repFile.println("\t<td class=\"td_file_data\">"+count+"</td>");
		repFile.println("\t<td class=\"td_file_data\">"+fileName+".xml</td>");
		
		if (status == 0) {
			repFile.println("\t<td class=\"td_failed_data\">Failed</td>");
		} else if (status == 1) {
			repFile.println("\t<td class=\"td_passed_data\">Passed</td>");
		} else {
			repFile.println("\t<td class=\"_data\">Blocked</td>");
		}
		
		repFile.println("\t<td class=\"td_report_data\"><a href='Report-"+fileName+".html'>Report Log</a></td>");
		repFile.println("</tr>");
	}
	
	/**
	 * generates the html header for the report file
	 */
	private void generateHTMLHeader() {
		final String title = "suite "+suiteName+".xml test results";
		String header = "<html> \n" +
				"<style type=\"text/css\"> \n" +
				"table { \n" +
				"\twidth: 100%; \n" +
				"\tborder: 2px solid black; \n" +
				"\tborder-collapse: collapse; \n" +
				"\tpadding: 0px; \n" +
				"\tbackground: #FFFFFF; \n" +
				"} \n" +
				".td_header_master { \n" +
				"\twhite-space: nowrap; \n" +
				"\tbackground: #b6dde8; \n" +
				"\ttext-align: center; \n" +
				"\tfont-family: Arial; \n" +
				"\tfont-weight: bold; \n" +
				"\tfont-size: 12px; \n" +
				"\tborder-left: 0px solid black; \n" +
				"\tborder-right: 2px solid black; \n" +
				"\tborder-bottom: 2px solid black; \n" +
				"} \n" +
				".td_file_data { \n" +
				"\twhite-space: nowrap; \n" +
				"\ttext-align: left; \n" +
				"\tfont-family: Arial; \n" +
				"\tfont-weight: bold; \n" +
				"\tfont-size: 12px; \n" +
				"\tborder-left: 0px solid black; \n" +
				"\tborder-right: 2px solid black; \n" +
				"\tborder-bottom: 2px solid black; \n" +
				"} \n" +
				".td_passed_data { \n" +
				"\twhite-space: nowrap; \n" +
				"\ttext-align: center; \n" +
				"\tfont-family: Arial; \n" +
				"\tfont-weight: bold; \n" +
				"\tcolor: #00cc00; \n" +
				"\tfont-size: 12px; \n" +
				"\tborder-left: 0px solid black; \n" +
				"\tborder-right: 2px solid black; \n" +
				"\tborder-bottom: 2px solid black; \n" +
				"} \n" +
				"._data { \n" +
				"\twhite-space: nowrap; \n" +
				"\ttext-align: center; \n" +
				"\tfont-family: Arial; \n" +
				"\tfont-weight: bold; \n" +
				"\tcolor: #FFCF10; \n" +
				"\tfont-size: 12px; \n" +
				"\tborder-left: 0px solid black; \n" +
				"\tborder-right: 2px solid black; \n" +
				"\tborder-bottom: 2px solid black; \n" +
				"} \n" +
				".td_failed_data { \n" +
				"\twhite-space: nowrap; \n" +
				"\ttext-align: center; \n" +
				"\tfont-family: Arial; \n" +
				"\tfont-weight: bold; \n" +
				"\tcolor: #FF0000; \n" +
				"\tfont-size: 12px; \n" +
				"\tborder-left: 0px solid black; \n" +
				"\tborder-right: 2px solid black; \n" +
				"\tborder-bottom: 2px solid black; \n" +
				"} \n" +
				".td_failed_data_zero { \n" +
				"\twhite-space: nowrap; \n" +
				"\ttext-align: center; \n" +
				"\tfont-family: Arial; \n" +
				"\tfont-weight: normal; \n" +
				"\tcolor: #FFFFFF; \n" +
				"\tfont-size: 12px; \n" +
				"\tborder-left: 0px solid black; \n" +
				"\tborder-right: 2px solid black; \n" +
				"\tborder-bottom: 2px solid black; \n" +
				"} \n" +		
				".td_report_data { \n" +
				"\twhite-space: nowrap; \n" +
				"\ttext-align: center; \n" +
				"\tfont-family: Arial; \n" +
				"\tfont-weight: normal; \n" +
				"\tfont-size: 12px; \n" +
				"\tborder-left: 0px solid black; \n"+
				"\tborder-right: 2px solid black; \n" +
				"\tborder-bottom: 2px solid black; \n" +
				"} \n" +
				".highlight { \n" +
				"\tbackground-color: #8888FF; \n" +
				"} \n" +
				".tr_normal { \n" +
				"\tbackground-color: #e5eef3; \n" +
				"} \n" +
				"</style> \n" +
				"<title>"+title+"</title> \n" +
				"<body> \n" +
				"<table id=\"tests\"> \n" +
				"<tr id=\"header\"> \n" +
				"\t<td class=\"td_header_master\" colspan=\"4\"> \n" +
				"\tSuite: "+suiteName+".xml Test Results \n" +
				"</td> \n" +
				"<tr id=\"header_key\"> \n" +
				"\t<td class=\"td_header_master\"></td> \n" +
				"\t<td class=\"td_header_master\">Test File</td> \n" +
				"\t<td class=\"td_header_master\">Status</td> \n" +
				"\t<td class=\"td_header_master\">Report Log</td> \n" +
				"</tr> \n";
				
				repFile.print(header);
	}
	
	/**
	 * sets up FileReader and BufferedReader for the next report file
	 * @param inputFile - a properly formatted .log SODA report file
	 */
	private void readNextLog(File inputFile) {
		try{
			/*sets up file reader to read input one character at a time*/
			input = new FileReader(inputFile);
			/*sets up buffered reader to read input one line at a time*/
			br = new BufferedReader(input);
		} catch (FileNotFoundException e) {
			System.err.println("file not found: "+inputFile);
		} catch (Exception e) {
			System.err.println("error reading file" + inputFile);
		}
	}
}
