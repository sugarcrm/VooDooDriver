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
import java.util.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class VddSummaryReporter {

	private String HTML_HEADER_RESOURCE = "summaryreporter-header.txt";
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
	private int hours = 0;
	private int minutes = 0;
	private int seconds = 0;
	private FileOutputStream output;
	private PrintStream repFile;
	private Document dom;
	private String basedir = "";
	
	public VddSummaryReporter(ArrayList<File> xmlFiles, String path) {
		this.count = 0;
		this.xmlFiles = xmlFiles;
		passedTests = 0; 
		failedTests = 0; 
		blockedTests = 0; 
		failedAsserts = 0; 
		passedAsserts = 0; 
		exceptions = 0; 
		errors = 0; 
		watchdog = 0;
		hours = 0;
		minutes = 0;
		seconds = 0;
		String summaryFile = String.format("%s%s%s", path, File.separatorChar, "summary.html");
		this.basedir = path;
		
		try {
			output = new FileOutputStream(summaryFile);
			System.out.printf("(*)SummaryFile: %s\n", summaryFile);
			repFile = new PrintStream(output);
		} catch (Exception e) {
			System.out.printf("(!)Error: Failed trying to write file: '%s'!\n", summaryFile);
			e.printStackTrace();
		}
	}
	
	public void generateReport() {
		HashMap<String, HashMap<String, Object>> list = new HashMap<String, HashMap<String,Object>>();
		repFile.print(generateHTMLHeader());
		String name = "";
		String[] keys = null;
		
		
		for (int i = 0; i < xmlFiles.size(); i ++) {
			HashMap<String, Object> suiteData = null;
			suiteData = parseXMLFile(xmlFiles.get(i));
			name = suiteData.get("suitename").toString();
			list.put(name, suiteData);
		}
		
		keys = list.keySet().toArray(new String[0]);
		java.util.Arrays.sort(keys);
		
		for (int i = 0; i <= keys.length -1; i++) {
			String key = keys[i];
			repFile.print(generateTableRow(key, list.get(key)));
		}
		
		repFile.print(generateHTMLFooter());
		repFile.print("\n</body>\n</html>\n");
		repFile.close();
	}
	
	private boolean isRestart(Node node) {
		boolean result = false;
		NodeList parent = node.getParentNode().getChildNodes();
		
		for (int i = 0; i <= parent.getLength() -1; i++) {
			Node tmp = parent.item(i);
			String name = tmp.getNodeName();
			if (name.contains("isrestart")) {
				result = Boolean.valueOf(tmp.getTextContent());
				break;
			}
		}
		
		return result;
	}
	
	private boolean isBlocked(Node node) {
		boolean result = false;
		NodeList parent = node.getParentNode().getChildNodes();
		
		for (int i = 0; i <= parent.getLength() -1; i++) {
			Node tmp = parent.item(i);
			String name = tmp.getNodeName();
			if (name.contains("blocked")) {
				int blocked = Integer.valueOf(tmp.getTextContent());
				if (blocked != 0) {
					result = true;
				} else {
					result = false;
				}
				break;
			}
		}
		
		return result;
	}
	
	private HashMap<String, Object> getSuiteData(Document doc) {
		HashMap<String, Object> result = new HashMap<String, Object>();
		int passed = 0, failed = 0, blocked = 0, asserts = 0, assertsF = 0, errors = 0, exceptions = 0, wd = 0, total = 0;
		String runtime = "";
		String suiteName = getSuiteName(doc);
		
		passed = getAmtPassed(doc);
		blocked = getAmtBlocked(doc);
		failed = getAmtFailed(doc);
		wd = getAmtwatchdog(doc);
		asserts = getAmtAsserts(doc);
		assertsF = getAmtAssertsF(doc);
		exceptions = getAmtExceptions(doc);
		errors = getAmtErrors(doc);
		total = assertsF + exceptions + errors;
		runtime = getRunTime(doc);
		
		result.put("passed", passed);
		result.put("blocked", blocked);
		result.put("failed", failed);
		result.put("wd", wd);
		result.put("asserts", asserts);
		result.put("assertsF", assertsF);
		result.put("exceptions", exceptions);
		result.put("errors", errors);
		result.put("total", total);
		result.put("runtime", runtime);
		result.put("suitename", suiteName);
		result.put("testlogs", this.getTestLogs(doc));

		return result;
	}
	
	/**
	 * generates a table row for summary.html from a DOM
	 * @return - a nicely formatted html table row for summary.html
	 */
	
	@SuppressWarnings("unchecked")
	private String generateTableRow(String suiteName, HashMap<String, Object> data) {
		int passed, failed, blocked, asserts, assertsF, errors, exceptions, wd, total;
		suiteName = data.get("suitename").toString();
		String runtime = "";
		String html = "<tr id=\""+count+"\" class=\"unhighlight\" onmouseover=\"this.className='highlight'\" onmouseout=\"this.className='unhighlight'\"> \n" +
			"<td class=\"td_file_data\">\n" +
			"<a href=\""+suiteName+"/"+suiteName+".html\">"+suiteName+".xml</a> \n" +
			"</td>";
			
		//restarts = (Integer)data.get("restarts");
		passed = (Integer)data.get("passed");
		blocked = (Integer)data.get("blocked");
		failed = (Integer)data.get("failed");
		wd = (Integer)data.get("wd");
		asserts = (Integer)data.get("asserts");
		assertsF = (Integer)data.get("assertsF");
		exceptions = (Integer)data.get("exceptions");
		errors = (Integer)data.get("errors");
		total = assertsF + exceptions + errors;
		runtime = data.get("runtime").toString();
		
		html += "\t <td class=\"td_run_data_error\">"+(passed+failed)+"/"+(passed+failed+blocked)+"</td>\n";
		//html += "\t <td class=\"td_run_data_error\">"+(passed+failed)+"/"+(passed+failed)+"</td>\n";
		html += "\t <td class=\"td_passed_data\">"+passed+"</td> \n";
		html += "\t <td class=\"td_failed_data\">"+failed+"</td> \n";
		html += "\t <td class=\"td_blocked_data\">"+blocked+"</td> \n";
		
		/*
		if (blocked == 0) {
			html += "\t <td class=\"td_run_data\">"+(passed+failed)+"/"+(passed+failed)+"</td>\n";
			html += "\t <td class=\"td_passed_data\">"+passed+"</td> \n";
			html += "\t <td class=\"td_failed_data\">"+failed+"</td> \n";
			html += "\t <td class=\"td_blocked_data_zero\">0</td> \n";
		} else {
			html += "\t <td class=\"td_run_data_error\">"+(passed+failed)+"/"+(passed+failed+blocked)+"</td>\n";
			//html += "\t <td class=\"td_run_data_error\">"+(passed+failed)+"/"+(passed+failed)+"</td>\n";
			html += "\t <td class=\"td_passed_data\">"+passed+"</td> \n";
			html += "\t <td class=\"td_failed_data\">"+failed+"</td> \n";
			html += "\t <td class=\"td_blocked_data\">"+blocked+"</td> \n";
		}
		*/
		
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
		
		html += "\t <td class=\"td_time_data\">"+runtime+"</td> \n";
		html += "</tr>";
		
		ArrayList<HashMap<String, String>> logs = (ArrayList<HashMap<String, String>>)data.get("testlogs");
		VddSuiteReporter reporter = new VddSuiteReporter(suiteName, this.basedir, logs);
		reporter.generateReport();
		
		return html;
	}
	
	/**
	 * generates the HTML table header for summary report, then returns it
	 * @return - String of html table header
	 */
	private String generateHTMLHeader() {
		String header = "";
		String line = "";
		InputStream stream = null;
		
		try {
			String className = this.getClass().getName().replace('.', '/');
			String classJar =  this.getClass().getResource("/" + className + ".class").toString();
			
			if (classJar.startsWith("jar:")) {
				stream = getClass().getResourceAsStream(this.HTML_HEADER_RESOURCE);
			} else {
				File header_fd = new File(getClass().getResource(this.HTML_HEADER_RESOURCE).getFile());
				stream = new FileInputStream(header_fd);
			}
			
			InputStreamReader in = new InputStreamReader(stream);
			BufferedReader br = new BufferedReader(in);
			
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

	private HashMap<String, Object> parseXMLFile(File xml){
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		HashMap<String, Object> result = new HashMap<String, Object>();
		
		try {
			DocumentBuilder db = dbf.newDocumentBuilder();
			dom = db.parse(xml);
			result = getSuiteData(dom);
		} catch(Exception e){
			e.printStackTrace();
			result = null;
		}
		
		return result;
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
		boolean isrestart = false;
		
		for (int i = 0; i < nl.getLength(); i ++) {
			el = (Element)nl.item(i);
			if (el.getFirstChild().getNodeValue().compareToIgnoreCase("Passed") == 0) {
				isrestart = isRestart(nl.item(i));
				if (isrestart) {
					continue;
				}
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
		boolean isrestart = false;
		boolean isblocked = false;
		NodeList nl = d.getElementsByTagName("result");
		
		for (int i = 0; i < nl.getLength(); i ++){
			el = (Element)nl.item(i);
			if (el.getFirstChild().getNodeValue().compareToIgnoreCase("Failed") == 0) {
				isrestart = isRestart(nl.item(i));
				isblocked = isBlocked(nl.item(i));
				if (isrestart) {
					continue;
				}
				
				if (isblocked) {
					continue;
				}
				
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
		boolean isrestart = false;
		NodeList nl = d.getElementsByTagName("blocked");
		
		for (int i = 0; i < nl.getLength(); i ++) {
			el = (Element)nl.item(i);
			if (el.getFirstChild().getNodeValue().compareToIgnoreCase("1") == 0) {
				isrestart = isRestart(nl.item(i));
				if (isrestart) {
					continue;
				}
				n ++;
			}
		}
		
		//global blockedTests variable
		blockedTests += n;
		return n;
	}
	
	private ArrayList<HashMap<String, String>> getTestLogs(Document d) {
		ArrayList<HashMap<String, String>> result = new ArrayList<HashMap<String,String>>();
		NodeList nodes = d.getElementsByTagName("test");
		
		for (int i = 0; i <= nodes.getLength() -1; i++) {
			Node currNode = nodes.item(i);
			HashMap<String, String> newHash = new HashMap<String, String>();
			NodeList kids = currNode.getChildNodes();
			
			for (int x = 0; x <= kids.getLength() -1; x++) {
				Node kidNode = kids.item(x);
				if (kidNode.getNodeName().contains("testlog")) {
					newHash.put(kidNode.getNodeName(), kidNode.getTextContent());
				} else if (kidNode.getNodeName().contains("isrestart")) {
					newHash.put(kidNode.getNodeName(), kidNode.getTextContent().toLowerCase());
				}
			}
			result.add(newHash);
		}
		
		return result;
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
		boolean isrestart = false;
		NodeList nl = d.getElementsByTagName("passedasserts");
		
		for (int i = 0; i < nl.getLength(); i ++) {
			el = (Element)nl.item(i);
			if (Integer.parseInt(el.getFirstChild().getNodeValue()) > 0){
				isrestart = isRestart(nl.item(i));
				if (isrestart) {
					continue;
				}
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
		boolean isrestart = false;
		NodeList nl = d.getElementsByTagName("failedasserts");
		
		for (int i = 0; i < nl.getLength(); i ++) {
			el = (Element)nl.item(i);
			if (Integer.parseInt(el.getFirstChild().getNodeValue()) > 0) {
				isrestart = isRestart(nl.item(i));
				if (isrestart) {
					continue;
				}
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
		boolean isrestart = false;
		NodeList nl = d.getElementsByTagName("watchdog");
		
		for (int i = 0; i < nl.getLength(); i ++) {
			el = (Element)nl.item(i);
			if (Integer.parseInt(el.getFirstChild().getNodeValue()) > 0) {
				isrestart = isRestart(nl.item(i));
				if (isrestart) {
					continue;
				}
				n += Integer.parseInt(el.getFirstChild().getNodeValue());
			}
		}
		
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
		boolean isrestart = false;
		NodeList nl = d.getElementsByTagName("exceptions");
		
		for (int i = 0; i < nl.getLength(); i ++) {
			el = (Element)nl.item(i);
			if (Integer.parseInt(el.getFirstChild().getNodeValue()) > 0) {
				isrestart = isRestart(nl.item(i));
				if (isrestart) {
					continue;
				}
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
		boolean isrestart = false;
		NodeList nl = d.getElementsByTagName("errors");
		for (int i = 0; i < nl.getLength(); i ++) {
			el = (Element)nl.item(i);
			if (Integer.parseInt(el.getFirstChild().getNodeValue()) > 0) {
				isrestart = isRestart(nl.item(i));
				if (isrestart) {
					continue;
				}
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
		String name = "";
		NodeList nl = d.getElementsByTagName("suitefile");
		
		if (nl != null && nl.getLength() > 0) {
			Element el = (Element)nl.item(0);
			name = el.getFirstChild().getNodeValue();
		}
		
		name = name.substring(0, name.indexOf("."));
		return name;
	}
	
}
