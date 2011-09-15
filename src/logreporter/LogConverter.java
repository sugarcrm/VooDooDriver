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
import java.util.regex.Pattern;


/**
 * converts a single .log file into a readable HTML file. The output file will be in the same directory as the .log source
 * 
 * @param inputFile - absolute path and name of file. If no path is given, will assume is in same directory. 
 *
 */
public class LogConverter{
	
	private String fileName;
	private FileReader input;
	private BufferedReader br;
	private String strLine;
	private FileOutputStream output;
	private PrintStream repFile;
	private int backTraceID;
	private int eventDumpID;
	
	public LogConverter(){
		fileName = "";
		strLine = "";
		backTraceID = 0;
		eventDumpID = 0;
	}

	/**
	 * Constructor for class LogConverter.
	 * @param inputFile - name of the file to be converted. Should be in correct format, with .log extention
	 */
	public LogConverter(String inputFile) {
		strLine = new String();
		backTraceID = 0;
		eventDumpID = 0 ;
		
		/**
		 * set up file reading 
		 */
		try {
			/*sets up file reader to read input one character at a time*/
			input = new FileReader(inputFile);
			/*sets up buffered reader to read input one line at a time*/
			br = new BufferedReader(input);
		} catch (FileNotFoundException e) {
			System.err.println("file not found: "+inputFile);
		} catch (Exception e) {
			System.err.println("error reading file" + inputFile);
		}
		
			
		/**
		 * add the correct extension and "Report-" prefix to the output html
		 */
		fileName = inputFile.substring(inputFile.lastIndexOf("/")+1, inputFile.length()-4);
		String filePath = inputFile.substring(0, inputFile.lastIndexOf('/')+1);
		fileName = "Report-"+fileName+".html";
		System.out.println(fileName);
		
		/**
		 * sets up output file
		 */
		try {
			output = new FileOutputStream(filePath+fileName);
			repFile = new PrintStream(output);
		} catch (Exception e) {
			System.err.println("Error writing to file "+fileName);
		}
	}
	
	/**
	 * Generates a new html table row from a raw .log file line
	 * 
	 * @param line - A line from the raw .log file
	 * @return A string of html that is a table row
	 * 
	 * index 0 = date
	 * index 1 = message type
	 * index 2 = message
	 */
	private String generateTableRow(String line){
		String htmlRow = "";
		String[] rowData = new String[3]; 
		String trStyle = "tr_normal";
		char msgType = line.charAt(line.indexOf("(")+1); 
		String message = line.substring(line.indexOf(")")+1, line.length());
		
		/**
		 * case switches for different message types
		 */
		//assertion passed
		if (message.contains("Assert Passed")) {
			rowData = formatAssertionPassed(line, message);
			trStyle = "tr_assert_passed";
		} else if (message.contains("Assert Failed")) {
			rowData = formatAssertionFailed(line, message);
			trStyle = "tr_error";
		} else if (message.startsWith("Test") || message.startsWith("Lib")|| message.startsWith("Module")) {
			rowData = formatModuleLine(line, message);
			trStyle = "tr_module";
		} else if (message.startsWith("Clicking Link")) {
			rowData = formatClickingElement(line, message);		
		} else if (message.startsWith("Setting Value") || message.startsWith("Setting Select") || message.startsWith("Setting SODA")) {
			rowData = formatClickingElement(line, message);
		} else if (message.startsWith("Looking for element:")) {
			rowData = formatClickingElement(line, message);
		} else if (message.startsWith("Soda Test Report")){
			rowData = formatTestResults(line,message);
		} else if (message.startsWith("--Exception Backtrace")){
			rowData = formatExceptionBT(line,message);
		} else if (message.startsWith("Major Exception")){
			rowData = formatMajorException(line, message);
		} else if (message.startsWith("(?i)html saved")){
			rowData = this.formatHTMLSavedResults(line, message, msgType);
		} else if (msgType == 'E'){
			rowData = formatEventDump(line, message);
		} else if (message.contains("(?i)css error") || message.contains("(?i)javascript error")){
			rowData = formatJSError(line, message);
		} else if (message.contains("(?i)replacing string")){
			rowData = formatReplaceString(line, message);
		} else if (message.startsWith("Tring to find")){
			rowData = formatFindingElement(line, message);
		} else if (message.startsWith("(?i)element")){
			rowData = formatClickingElement(line, message);
			if (message.contains("(?i)screenshot taken")){
				rowData = formatScreenShot(line, message);
			}	
		} else{
			rowData = formatDefaultLine(line, message);
		}
		
		/**
		 * if there are no data for some reason, return empty html row
		 */
		if (rowData[0].isEmpty() && rowData[2].isEmpty()){
	         return "";
		}
		
		/**
		 * special message types
		 */
		if (msgType == '!'){
			rowData[1] = "Failure";
			trStyle = "tr_error";
		}
		else if (msgType == 'W'){
			rowData[1] = "Warning";
			trStyle = "tr_warning";
		}
		else if (msgType == 'M'){
			rowData[1] = "Un/Load";
			trStyle = "tr_module";
		}
		
		htmlRow = "<tr class=\""+ trStyle +"\" "+
		         "onMouseOver=\"this.className='highlight'\" " +
		         "onMouseOut=\"this.className='"+ trStyle +"'\">\n";
		htmlRow += "\t<td>" + rowData[0] + "</td>\n";
		htmlRow += "\t<td class=\"td_msgtype\">" + rowData[1] + "</td>\n";
		htmlRow += "\t<td>" + rowData[2] + "</td>\n</tr>\n";
		
		return htmlRow;
	}

	/**
	 * generates a html report file 
	 */
	public void generateReport(){
		generateHtmlHeader();
		
		try{
			/**
			 * read first line
			 */
			strLine = br.readLine();		
			while (strLine != null){
				repFile.println(generateTableRow(strLine));
				strLine = br.readLine();
			}
			
		}catch (Exception e){
			System.err.println("error reading input file");
			e.printStackTrace();
		}
		
		repFile.print("\n</table>\n</body>\n</html>\n");
		repFile.close();
	}
	////////////////////////////////////////////
	//line cases
	////////////////////////////////////////////
	/**
	 * reads a raw .log file and formats it into a generic, default report line
	 * 
	 * @param line - A line from the raw .log file
	 * @return array with the expected format
	 */
	private String[] formatDefaultLine(String line, String message){
		String[] rowData = new String[3];
		
		rowData[0] = generateDateTime (line);
		rowData[1] = "Log";
		rowData[2] = safeHTMLString(message);
		
		return rowData;
	}
	
	/**
	 * takes the results line from the raw log file and generates a nice html row
	 * 
	 * @param line - the "Soda Test Report:" line from the raw log file
	 * @return array in expected format
	 */
	private String[] formatTestResults (String line, String message){
		String[] rowData = new String[3];
		String tableHTML = "\n<table>\n";
		String[] resData = message.split("--");
		
		for (int i = 1; i < resData.length; i++){
			tableHTML += "<tr class=\"tr_normal\""+
		            " \"onMouseOver=\"this.className='highlight_report'\" "+
		            "onMouseOut=\"this.className='tr_normal'\"> \n";
			String[] lineData = resData[i].split(":");
			//data type
			if (lineData[0].equals("failedasserts")){
				tableHTML += "\t<td><b>failed asserts: </b></td>\n";
			}
			else if (lineData[0].equals("passedasserts")){
				tableHTML += "\t<td><b>passed asserts: </b></td>\n";
			}
			else {
				tableHTML += "\t<td><b>" + lineData[0] + ":</b></td>\n";
			}
			
			//table data
			if (!lineData[1].contentEquals("0") && !lineData[0].equals("passedasserts")){
				tableHTML += "\t<td><font color=\"#FF0000\">\n";
				tableHTML += "<b>" + lineData[1] + "</b>\n\t</td>\n";
			}
			else {
				tableHTML += "\t<td>\n";
				tableHTML += "<b>" + lineData[1] + "</b>\n\t</td>\n";
			}
			//System.out.println(lineData[0] + " "+ lineData[1]);
			
		}
		
		tableHTML += "</table>";
		rowData[0] = generateDateTime (line);
		rowData[1] = "Results";
		rowData[2] = tableHTML;
		
		return rowData;
	}
	
	/** 
	 * takes the "HTML saved" line from the raw .log file and generates a happy html row from it
	 *
	 * @param line - the "HTML saved" line from the raw SODA log file
	 * @return an array in the expected format
	 */
	private String[] formatHTMLSavedResults (String line, String message, char msgType) {
		String[] rowData = new String[3];
		
		rowData[0] = generateDateTime (line);
		rowData[1] = "Log";
		
		String baseName = this.fileName;
		rowData[2] = "<b>"+rowData[0]+"</b>" +
		         "<a href=\""+baseName+"\" target=\"_blank\">"+msgType+"</a>";
		
		return rowData;
	}
	
	/** 
	 * takes an exception bt from the raw .log file and makes a html table row from it
	 * 
	 * @param line - the bt line from the raw SODA log file
	 * @return an array in the expected format
	 */
	private String[] formatExceptionBT (String line, String message) {
		String[] rowData = new String[3];
		String btID = "bt_div_"+backTraceID;
		String hrefID = "href_div_"+backTraceID;
		backTraceID += 1;
		
		rowData[0] = generateDateTime (line);
		rowData[1] = "Backtrace";
		
		String rowHTML =  "\t<a id=\""+hrefID+"\" href=\"javascript:showdiv('"+btID+"',"+
		         " '"+hrefID+"')\">[ Expand Backtrace ]<b>+</b><br>\n" +
		         "</a><br>\t<div id=\""+btID+"\" style=\"display: none\">\n";
		
		String[] eData = message.split("--");
		for (int i = 1; i < eData.length; i++){
			rowHTML += "\t\t"+eData[i]+"<br>\n";
		}
		rowHTML += "\t<a href=\"javascript:hidediv('"+btID+"', '"+hrefID+"')\">" +
		         "[ Collaspe Backtrace ]<b>-</b></a>\t\t</div>\n\n";
		rowData[2] = rowHTML;
		return rowData;
	}
	
	/**
	 * takes a major exception line from the raw .log file and creates a nice html row
	 * 
	 * @param line - the "exception" line from the raw .log
	 * @return an array in the expected format
	 */
	private String[] formatMajorException (String line, String message) {
		String[] rowData = new String[3];
		
		String[] msgData = message.split("--");
		msgData[0] = msgData[0].replaceAll("(?i)major exception", "<b>Major Exception:</b>");
		msgData[1] = msgData[1].replaceAll("(?i)exception message", "<b>Exception Message:</b>");
		
		rowData[0] = generateDateTime (line);
		rowData[1] = "Failure";
		rowData[2] = msgData[0]+"</br>"+ msgData[1];
		
		return rowData;
	}
	
	/** 
	 * takes an "assertion failed" line from the raw .log file and makes a happy html row 
	 * 
	 * @param line - the "assertion failed" line from the .log file
	 * @return an array in the expected format
	 */
	private String[] formatAssertionFailed (String line, String message) {
		String[] rowData = new String[3];
		
		rowData[0] = generateDateTime (line);
		rowData[1] = "Failure";
		
		String[] assertData = message.split(":");
		assertData[1] = assertData[1].replaceFirst("'", "<b>'");
		assertData[1] += "</b>";
		rowData[2] = assertData[0] + assertData[1];
		
		return rowData;
	}
	
	/**
	 * takes an "assertion passed" line from the raw .log file and makes a happy html row 
	 * 
	 * @param line - the "assertion passed" line from the .log file
	 * @return an array in the expected format
	 */
	private String[] formatAssertionPassed(String line, String message) {
		String[] rowData = new String[3];
		
		rowData[0] = generateDateTime (line);
		rowData[1] = "Assertion Passed";
		rowData[2] = safeHTMLString(message);
		
		return rowData;
	}
	
	/** 
	 * formats an "event dump" line from the .log file into html row
	 * 
	 * @param line - event dump line from the raw .log file
	 * @return an array in the expected format
	 */
	private String[] formatEventDump (String line, String message) {
		String[] rowData = new String[3];
		String edID = "ed_div_"+eventDumpID;
		String hrefID = "href_div_ed_"+eventDumpID;
		eventDumpID ++;
		
		rowData[0] = generateDateTime (line);
		rowData[1] = "Event Dump";
		
		String msgText = message.substring(0, message.indexOf(".")+1);
		String msgData = message.substring(message.indexOf("--"), message.length()-1);
		
		String rowHTML = "\t<b>"+msgText+":</b>" +
		         "\t<a id=\""+hrefID+"\" href=\"javascript:showdiv('"+edID+"',"+
		         " '"+hrefID+"')\">[ Expand Event Dump ]<b>+</b><br>\n" +
		         "</a><br>\t<div id=\""+edID+"\" style=\"display: none\">\n";
		
		String[] eData = msgData.split("--");
		for (int i = 0; i < eData.length; i ++) {
			rowHTML += "\t\t"+eData[i]+"<br>\n";
		}
		
		rowHTML += "\t<a href=\"javascript:hidediv('"+edID+"'" +
			       ", '"+hrefID+"')\">" +
			         "[ Collaspe Event Dump ]<b>-</b></a>\t\t</div>\n"; 
		
		rowData[2] = rowHTML;
		return rowData;
	}
	
	/**
	 * formats a "javascript error" line into html row
	 * 
	 * @param line - the js error line from the raw .log file
	 * @return an array in the expected format
	 */
	private String[] formatJSError (String line, String message) {
		String[] rowData = new String[3];
		String rowHTML = "";
		
		rowData[0] = generateDateTime (line);
		rowData[1] = "Log";
		
		String[] msgData = message.split("--");
		for (int i = 0; i < msgData.length; i++) {
			String[] info = msgData[i].split("::");
			if (info.length < 2) {
				rowHTML += "\t<b>"+info[0]+"</b><br>\n";
			} else {
				rowHTML += "\t<b>"+info[0]+":</b> "+info[1]+"<br>\n";
			}
		}
		
		rowData[2] = rowHTML;
		return rowData;
	}
	
	/** 
	 * formats module lines into html row
	 * 
	 * @param line - the module line from the raw .log file
	 * @return an array in the expected format
	 */
	private String[] formatModuleLine (String line, String message) {
		String[] rowData = new String[3];
		
		rowData[0] = generateDateTime (line);
		rowData[1] = "Un/Load";
		message = message.replaceFirst("Test:", "<b>Test:</b>");
		message = message.replaceFirst("Lib:", "<b>Lib:</b>");
		message = message.replaceFirst("Module:", "<b>Module:</b>");
		rowData[2] = message;
		
		return rowData;
	}
	
	/**
	 * converts the screenshot line into a nice html row
	 * 
	 * @param line - the screenshot line from the raw .log file
	 * @return an array in the expected format
	 */
	private String[] formatScreenShot(String line, String message) {
		String[] rowData = new String[3];
		
		rowData[0] = generateDateTime (line);
		rowData[1] = "Log";
		String[] data = message.split(":");
		rowData[2] = "<b>"+data[0]+":</b> <a href=\""+data[1]+"\">#{data[1]}</a>";
		
		return rowData;
	}
	
	/**
	 * formats the replace string message line into html row
	 * 
	 * @param line - the replace string line from the raw .log file
	 * @return an array in the expected format
	 */
	private String[] formatReplaceString (String line, String message) {
		String[] rowData = new String[3];
		
		rowData[0] = generateDateTime (line);
		rowData[1] = "Log";
		message = Pattern.quote(message);
		rowData[2] = message.replaceAll("with", "<b>'"+message+"'</b>");
		
		return rowData;
	}
	
	/** 
	 * converts the clicking element message line into a nice html row
	 * 
	 * @param line - the "clicking element" line from the raw .log file
	 * @return an array in the expected format
	 */
	private String[] formatClickingElement(String line, String message) {
		String[] rowData = new String[3];
		
		rowData[0] = generateDateTime (line);
		rowData[1] = "Log";
		message = safeHTMLString(message);
		message = message.replaceAll(": ", ": <b>");
		message += "</b>";
		rowData[2] = message;
		
		return rowData;
	}
	
	/** 
	 * converts the trying to find element message line into a nice html row
	 * 
	 * @param line - the "trying to find" line from the raw .log file
	 * @return an array in the expected format
	 */
	private String[] formatFindingElement(String line, String message) {
		String[] rowData = new String[3];
		rowData[0] = generateDateTime (line);
		rowData[1] = "Log";
		
		String[] msgData = message.split(":");
		String temp = msgData[0].substring(msgData[0].indexOf("'"), msgData[0].indexOf("by")-1);
		msgData[0] = msgData[0].replaceFirst(temp, "<b>"+temp+"</b> ");
		msgData[1] = msgData[1].replaceFirst("'", "<b>'");
		msgData[1] += "</b>";	
		
		rowData[2] = msgData[0] + msgData[1];
		return rowData;
	}
	
	/** 
	 * Generates the proper html header for the report file
	 * 
	 */
	private void generateHtmlHeader() {
		final String title = "SODA Test report";
		String temp = "<html> \n" +
				"<script language=javascript type='text/javascript'> \n" +
		 		"function hidediv(name, href_id) { \n" +
		 		"\t document.getElementById(name).style.display = 'none'; \n" +
		 		"\t document.getElementById(href_id).innerHTML=\"[ Expand Backtrace ]<b>+</b>\"; \n" +
		 		"\t document.getElementById(href_id).href=\"javascript:showdiv('\" + name +\"', '\" + href_id + \"')\";\n" +
		 		"} \n" +
		 		"function showdiv(name, href_id) { \n" +
		 		"\t document.getElementById(name).style.display = 'inline'; \n" +
				"\t document.getElementById(href_id).innerHTML=\"[ Collapse Backtrace ]<b>-</b>\"; \n" +
				"\t document.getElementById(href_id).href=\"javascript:hidediv('\" + name + \"', '\" + href_id + \"')\"; \n" +
				"} \n" +
				"</script> \n" +
				"<style type=\"text/css\"> \n" +
				"body{ \n" +
				"\t margin: 0px; \n" +
				"\t font-family: Arial, Verdana, Helvetica, sans-serif; \n" +
				"} \n" +
				"fieldset, table, pre{ \n" +
				"\t margin-bottom:0; \n" +
				"} \n" +
				"p{ \n" +
				"\t margin-top: 0px; \n" +
				"\t margin-bottom: 0px; \n" +
				"} \n" +
				"textarea{ \n" +
				"\t fontfamily: Arial, Verdana,Helvetica,sans-serif; \n" +
				"} \n" +
				"td{ \n" +
				"\t text-align:left; \n" +
				"\t vertical-align: top; \n" +
				"} \n" +
				".td_msgtype{ \n" +
				"\t text-align: center; \n" +
				"\t vertical-align: middle; \n" +
				"} \n" +
				".tr_normal{ \n" +
				"\t background: #e5eef3; \n" +
				"} \n" +
				".tr_header{ \n" +
				"\t background: #a4a4a4; \n"+
				"} \n" +
				".tr_module{ \n" +
				"\t background: #3c78c8; \n" +
				"} \n" +
				".tr_error{ \n" +
				"\t background: #ff0000; \n" +
				"} \n" +
				".tr_warning{ \n" +
				"\t background: #eeff30; \n" +
				"} \n" +
				".tr_assert_passed{ \n" +
				"\t background: #7ff98a; \n" +
				"} \n" +
				".highlight { \n" +
				"\t background-color: #8888FF; \n" +
				"} \n" +
				".highlight_report { \n" +
				"\t background-color: #5dec6d; \n" +
				"} \n" +
				"table { \n" +
				"\t background: #ffff; \n" +
				"\t border: 1px solid black; \n" +
				"\t border-bottom: 1px solid #0000; \n" +
				"\t border-right: 1px solid #0000; \n" +
				"\t color: #0000; \n" +
				"\t padding: 4px; \n" +
				"\t font-size: 11px; \n" +
				"} \n" +
				"</style> \n" +
				"<title>"+title+"</title> \n" +
				"<body> \n" +
				"<table> \n" +
				"<tr class=\"tr_header\"> \n" +
				"\t <td nowrap> <b>Date Time: </b></td> \n" +
				"\t <td nowrap> <b>Message Type: </b></td> \n" +
				"\t <td> <b>Message: </b></td> \n" +
				"</tr > \n";
		
		repFile.print(temp);
	}
	
	/**
	 * Generates html code formatted to display date and time of log message from a raw .log file line
	 * 
	 * @param line - A line from the raw .log file
	 * @return A string of html that is the date and time of the log line
	 */
	private String generateDateTime (String line){
		String htmlDateTime = line.substring(1, line.indexOf("]"));
		return htmlDateTime.replaceFirst("-", "- <br />");
	}
	
	/**
	 * Takes a String and makes it HTML-safe by performing proper escapes
	 * 
	 * @param str - String to be properly escaped
	 * @return the properly escaped String 
	 */
	private String safeHTMLString (String str){
		str = str.replaceAll("<", "&lt;");
		str = str.replaceAll(">", "&gt;");
		return str;
	}
	
	/**
	 * returns the .html file name
	 * @return output file name
	 */
	public String getFileName(){
		return fileName;
	}
}
