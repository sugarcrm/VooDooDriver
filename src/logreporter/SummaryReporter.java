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
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;


/**
 * takes in a formatted .xml file containing suite test results and generates a nicely formatted HTML report
 *
 *@param file - an ArrayList of xml files containing suite test data
 *
 */
public class SummaryReporter {
	
	private String HTML_HEADER_RESOURCE = "summaryreporter-head.txt";
	private int count;
	private ArrayList<File> xmlFiles;
	private int passedTests = 0;
	private int failedTests = 0;
	private int blockedTests = 0;
	private int failedAsserts = 0;
	private int passedAsserts = 0;
	private int exceptions = 0;
	private int errors = 0;
	private int watchdog = 0;
	private int failedTotal =0;
	private int hours = 0;
	private int minutes = 0;
	private int seconds = 0;
	private FileOutputStream output;
	private PrintStream repFile;
	private Document dom;

	public SummaryReporter() {
		
	}
	
	/**
	 * Constructor for class SummaryReporter
	 *@param file - an ArrayList of xml files containing suite test data
	 */
	public SummaryReporter(ArrayList<File> xmlFiles, String path){
		count = 0;
		this.xmlFiles = xmlFiles;
		passedTests = 0; 
		failedTests = 0; 
		blockedTests = 0; 
		failedAsserts = 0; 
		passedAsserts = 0; 
		exceptions = 0; 
		errors = 0; 
		watchdog = 0; 
		failedTotal = 0;
		hours = 0;
		minutes = 0;
		seconds = 0;
		
		try {
			output = new FileOutputStream(path+	"summary.html");
			repFile = new PrintStream(output);
		} catch (Exception e) {
			System.err.println("Error writing to summary.html");
			e.printStackTrace();
		}
	}
	
	public void generateReport(){
		repFile.print(generateHTMLHeader());
		
		for (int i = 0; i < xmlFiles.size(); i ++){
			parseXMLFile(xmlFiles.get(i));
			repFile.print(generateTableRow(dom));
		}
		
		repFile.print(generateHTMLFooter());
		repFile.print("\n</body>\n</html>\n");
		repFile.close();
	}
	
	/**
	 * generates a table row for summary.html from a DOM
	 * @return - a nicely formatted html table row for summary.html
	 */
	private String generateTableRow(Document d){
		int passed, failed, blocked, asserts, assertsF, errors, exceptions, wd, total;
		String suiteName = getSuiteName(d);
		String html = "<tr id=\""+count+"\" class=\"unhighlight\" onmouseover=\"this.className='highlight'\" onmouseout=\"this.className='unhighlight'\"> \n";
		html += "<td class=\"td_file_data\">\n" +
				"<a href=\""+suiteName+"/"+suiteName+".html\">"+suiteName+".xml</a> \n" +
				"</td>";
		
		passed = getAmtPassed(d);
		blocked = getAmtBlocked(d);
		failed = getAmtFailed(d) - blocked;  //blocked tests count as failed too
		wd = getAmtwatchdog(d);
		asserts = getAmtAsserts(d);
		assertsF = getAmtAssertsF(d);
		exceptions = getAmtExceptions(d);
		errors = getAmtErrors(d);
		total = assertsF + exceptions + errors;
		
		//"Tests" column
		if (blocked == 0) {
			html += "\t <td class=\"td_run_data\">"+(passed+failed)+"/"+(passed+failed)+"</td>\n";
			html += "\t <td class=\"td_passed_data\">"+passed+"</td> \n";
			html += "\t <td class=\"td_failed_data\">"+failed+"</td> \n";
			html += "\t <td class=\"td_blocked_data_zero\">0</td> \n";
		} else {
			html += "\t <td class=\"td_run_data_error\">"+(passed+failed)+"/"+(passed+failed + blocked)+"</td>\n";
			html += "\t <td class=\"td_passed_data\">"+passed+"</td> \n";
			html += "\t <td class=\"td_failed_data\">"+failed+"</td> \n";
			html += "\t <td class=\"td_blocked_data\">"+blocked+"</td> \n";
		}
		//"Results" column
		if (wd == 0) {
			html += "\t <td class=\"td_watchdog_data\">0</td> \n";
		} else {
			html += "\t <td class=\"td_watchdog_error_data\">"+wd+"</td> \n";
		}
		
		html += "\t <td class=\"td_assert_data\">"+asserts+"</td> \n";
		if (assertsF == 0) {
			html += "\t <td class=\"td_assert_data\">0</td> \n";
		} else {
			html += "\t <td class=\"td_assert_error_data\">"+assertsF+"</td> \n";
		}
		
		if (exceptions == 0) {
			html += "\t <td class=\"td_exceptions_data\">0</td> \n";
		} else {
			html += "\t <td class=\"td_exceptions_error_data\">"+exceptions+"</td> \n";
		}
		
		if (errors == 0) {
			html += "\t <td class=\"td_exceptions_data\">0</td> \n";
		} else {
			html += "\t <td class=\"td_exceptions_error_data\">"+errors+"</td> \n";
		}
		
		if (total == 0) {
			html += "\t <td class=\"td_total_data\">0</td> \n";
		} else {
			html += "\t <td class=\"td_total_error_data\">"+total+"</td> \n";
		}
		
		html += "\t <td class=\"td_time_data\">"+getRunTime(d)+"</td> \n";
		html += "</tr>";
		
		return html;
	}
	
	/**
	 * generates the HTML table header for summary report, then returns it
	 * @return - String of html table header
	 */
	private String generateHTMLHeader() {
		String header = "";
		File headerFD = null;
		String line = "";
		
		try {
			headerFD = new File(getClass().getResource(this.HTML_HEADER_RESOURCE).getFile());
			BufferedReader br = new BufferedReader(new FileReader(headerFD));
			
			while ((line = br.readLine()) != null) {
				header += line;
				header += "\n";
			}
			
		} catch (Exception exp ) {
			exp.printStackTrace();
		}
		
		return header;
	}
	
	/**
	 * generates the HTML table footer for summary report, then returns it
	 * @return - String of html table footer
	 */
	private String generateHTMLFooter(){
		String footer = "";
		footer += "<tr id=\"totals\"> \n" +
				"\t <td class=\"td_header_master\">Totals:</td>" +
				"\t <td class=\"td_footer_run\">"+(passedTests + failedTests - blockedTests)+"/"+(passedTests + failedTests)+"</td>" +
				"\t <td class=\"td_footer_passed\">"+passedTests+"</td>" +
				"\t <td class=\"td_footer_failed\">"+(failedTests - blockedTests)+"</td>" +
				"\t <td class=\"td_footer_skipped\">"+blockedTests+"</td>" +
				"\t <td class=\"td_footer_watchdog\">"+watchdog+"</td>" +
				"\t <td class=\"td_footer_passed\">"+passedAsserts+"</td>" +
				"\t <td class=\"td_footer_assert\">"+failedAsserts+"</td>" +
				"\t <td class=\"td_footer_exceptions\">"+exceptions+"</td>" +
				"\t <td class=\"td_footer_watchdog\">"+errors+"</td>" +
				"\t <td class=\"td_footer_total\">"+(failedAsserts + exceptions + errors)+"</td>" +
				"\t <td class=\"td_footer_times\">"+printTotalTime(hours, minutes, seconds)+"</td>" +
				"</tr>" +
				"</tbody>" +
				"</table>";
				
		return footer;
	}

	private void parseXMLFile(File xml){
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		
		try {
			DocumentBuilder db = dbf.newDocumentBuilder();
			dom = db.parse(xml);
		} catch(Exception e){
			e.printStackTrace();
		}
	}
	
	/**
	 * get the number of tests that passed within this suite document
	 * @ param d - the Document containing suite run data
	 * @ return the number of tests that passed
	 * 
	 */
	private int getAmtPassed(Document d) {
		int n = 0;
		Element el;
		NodeList nl = d.getElementsByTagName("result");
		
		for (int i = 0; i < nl.getLength(); i ++) {
			el = (Element)nl.item(i);
			if (el.getFirstChild().getNodeValue().compareToIgnoreCase("Passed") == 0) {
				n ++;
			}
		}
		
		//global passedTests variable
		passedTests += n;
		return n;
	}
	
	/**
	 * get the number of tests that failed within this suite document
	 * @ param d - the Document containing suite run data
	 * @ return the number of tests that failed
	 * 
	 */
	private int getAmtFailed(Document d) {
		int n = 0;
		Element el;
		NodeList nl = d.getElementsByTagName("result");
		for (int i = 0; i < nl.getLength(); i ++){
			el = (Element)nl.item(i);
			if (el.getFirstChild().getNodeValue().compareToIgnoreCase("Failed") == 0) {
				n ++;
			}
		}
		
		//global failedTests variable
		failedTests += n;
		return n;
	}
	
	/**
	 * get the number of tests that was blocked within this suite document
	 * @ param d - the Document containing suite run data
	 * @ return the number of tests that was blocked
	 * 
	 */
	private int getAmtBlocked(Document d) {
		int n = 0;
		Element el;
		NodeList nl = d.getElementsByTagName("blocked");
		
		for (int i = 0; i < nl.getLength(); i ++) {
			el = (Element)nl.item(i);
			if (el.getFirstChild().getNodeValue().compareToIgnoreCase("1") == 0) {
				n ++;
			}
		}
		
		//global blockedTests variable
		blockedTests += n;
		return n;
	}
	
	/**
	 * get the number of assertions that passed within this suite document
	 * @ param d - the Document containing suite run data
	 * @ return the number of passed assertions
	 * 
	 */
	private int getAmtAsserts(Document d) {
		int n = 0;
		Element el;
		NodeList nl = d.getElementsByTagName("passedasserts");
		
		for (int i = 0; i < nl.getLength(); i ++) {
			el = (Element)nl.item(i);
			if (Integer.parseInt(el.getFirstChild().getNodeValue()) > 0){
				n += Integer.parseInt(el.getFirstChild().getNodeValue());
			}
		}
		//global passedAsserts
		passedAsserts += n;
		return n;
	}
	
	/**
	 * get the number of assertions that failed within this suite document
	 * @ param d - the Document containing suite run data
	 * @ return the number of failed assertions
	 * 
	 */
	private int getAmtAssertsF(Document d) {
		int n = 0;
		Element el;
		NodeList nl = d.getElementsByTagName("failedasserts");
		
		for (int i = 0; i < nl.getLength(); i ++) {
			el = (Element)nl.item(i);
			if (Integer.parseInt(el.getFirstChild().getNodeValue()) > 0) {
				n += Integer.parseInt(el.getFirstChild().getNodeValue());
			}
		}
		//global failedAsserts
		failedAsserts += n;
		return n;
	}
	
	/**
	 * get the number of watchdogs within this suite document
	 * @ param d - the Document containing suite run data
	 * @ return the number of watchdogs
	 * 
	 */
	private int getAmtwatchdog(Document d) {
		int n = 0;
		Element el;
		NodeList nl = d.getElementsByTagName("watchdog");
		
		for (int i = 0; i < nl.getLength(); i ++) {
			el = (Element)nl.item(i);
			if (Integer.parseInt(el.getFirstChild().getNodeValue()) > 0) {
				n += Integer.parseInt(el.getFirstChild().getNodeValue());
			}
		}
		
		//global watchdog variable
		watchdog += n;
		return n;
	}
	
	/**
	 * get the number of exceptions within this suite document
	 * @ param d - the Document containing suite run data
	 * @ return the number of exceptions
	 * 
	 */
	private int getAmtExceptions(Document d) {
		int n = 0;
		Element el;
		NodeList nl = d.getElementsByTagName("exceptions");
		
		for (int i = 0; i < nl.getLength(); i ++) {
			el = (Element)nl.item(i);
			if (Integer.parseInt(el.getFirstChild().getNodeValue()) > 0) {
				n += Integer.parseInt(el.getFirstChild().getNodeValue());
			}
		}
		//global exceptions 
		exceptions += n;
		return n;
	}
	
	private int getAmtErrors(Document d) {
		int n = 0;
		Element el;
		NodeList nl = d.getElementsByTagName("errors");
		for (int i = 0; i < nl.getLength(); i ++) {
			el = (Element)nl.item(i);
			if (Integer.parseInt(el.getFirstChild().getNodeValue()) > 0) {
				n += Integer.parseInt(el.getFirstChild().getNodeValue());
			}
		}
		//global errors
		errors += n;
		return n;
	}
	
	/**
	 * calculates the running time fromt a suite xml file, and return it in a html-friendly format
	 * @param d - document to get time data from
	 * @return - total run time for this suite test in String
	 */
	private String getRunTime(Document d) {
		String  temp;
		int h = 0, m = 0, s = 0;
		Element el;
		NodeList nl = d.getElementsByTagName("totaltesttime");
		
		for (int i = 0; i < nl.getLength(); i ++) {
			el = (Element)nl.item(i);
			temp = el.getFirstChild().getNodeValue();
			h += Integer.parseInt(temp.substring(0, temp.indexOf(":")));
			m += Integer.parseInt(temp.substring(2, temp.lastIndexOf(":")));
			s += Integer.parseInt(temp.substring(temp.lastIndexOf(":")+1, temp.indexOf(".")));
		}
		
		this.hours += h;
		this.minutes += m;
		this.seconds += s;
		return printTotalTime(h, m , s);
	}
	
	/**
	 * formats and returns a correct String representation from inputs of hours, minutes and seconds
	 * @param hours
	 * @param minutes
	 * @param seconds
	 * @return correctly formatted time in String
	 */
	private String printTotalTime(int h, int m, int s) {
		String time = "";
		
		//carry over seconds
		while (s >= 60) {
			m ++;
			s -= 60;
		}
		//carry over minutes
		while(m >= 60) {
			h ++;
			m -= 60;
		}
		
		String ms = ""+ m, ss = ""+ s;
		if (m < 10) {
			ms = "0"+m;
		}
		
		if (s < 10) {
			ss = "0"+s;
		}
		time = "0"+h+":"+ms+":"+ss;
		return time;
	}
	
	/**
	 * get the name of the suite, without extension
	 * @param d
	 * @return
	 */
	private String getSuiteName(Document d) {
		String name = "jfdjfajdlfea";
		NodeList nl = d.getElementsByTagName("suitefile");
		
		if (nl != null && nl.getLength() > 0) {
			Element el = (Element)nl.item(0);
			name = el.getFirstChild().getNodeValue();
		}
		
		name = name.substring(0, name.indexOf("."));
		return name;
	}
	
}
