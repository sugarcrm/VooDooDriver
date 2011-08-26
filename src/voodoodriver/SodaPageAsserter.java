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

import java.io.File;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class SodaPageAsserter {

	private ArrayList<String> ignores = null;
	private ArrayList<String> checkes = null;
	private File fd = null;
	private SodaReporter reporter = null;
	
	public SodaPageAsserter(String assertFile, SodaReporter reporter) {
		this.reporter = reporter;
		this.ignores = new ArrayList<String>();
		this.checkes = new ArrayList<String>();
		Document doc = null;
		DocumentBuilderFactory dbf = null;
		DocumentBuilder db = null;
		
		fd = new File(assertFile);
		if (!fd.exists()) {
			String msg = String.format("Error failed to find assertpage file: '%s'!", assertFile);
			this.reporter.ReportError(msg);
		}
		
		try {
			dbf = DocumentBuilderFactory.newInstance();
			db = dbf.newDocumentBuilder();
			doc = db.parse(fd);
			this.parse(doc.getDocumentElement().getChildNodes());
		} catch (Exception exp) {
			this.reporter.ReportException(exp);
		}
	}
	
	public void assertPage(String pagesrc) {
		//this.reporter.Log("Page Assert Starting.");
		int ignore_len = this.ignores.size() -1;
		int check_len = this.checkes.size() -1;
		
		for (int i = 0; i <= ignore_len; i++) {
			if (reporter.isRegex(this.ignores.get(i))) {
				String regex = reporter.strToRegex(this.ignores.get(i));
				pagesrc = pagesrc.replaceAll(regex, "");
			} else {
				pagesrc = pagesrc.replace(this.ignores.get(i), "");
			}
		}
		
		for (int i = 0; i <= check_len; i++) {
			if (reporter.isRegex(this.checkes.get(i))) {
				String regex = reporter.strToRegex(this.checkes.get(i));
				Pattern p = Pattern.compile(regex, Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
				Matcher m = p.matcher(pagesrc);
				if (m.find()) {
					String msg = String.format("Page Assert Found Match for: '%s'!", this.checkes.get(i));
					this.reporter.ReportError(msg);
					this.reporter.SavePage();
				}
			} else {
				if (pagesrc.contains(this.checkes.get(i))) {
					String msg = String.format("Page Assert Found Match for: '%s'!", this.checkes.get(i));
					this.reporter.ReportError(msg);
					this.reporter.SavePage();
				}
			}
		}
		
		//this.reporter.Log("Page Assert Finished.");
	}
	
	private void parse(NodeList nodes) {
		int len = nodes.getLength() -1;
		
		for (int i = 0; i <= len; i++) {
			Node node = nodes.item(i);
			String name = node.getNodeName();
			
			if (name.contains("ignore")) {
				NodeList inodes = node.getChildNodes();
				int ilen = inodes.getLength() -1;
				
				for (int ii = 0; ii <= ilen; ii++) {
					Node tmp = inodes.item(ii);
					String tmp_name = tmp.getNodeName();
					if (!tmp_name.contains("regex")) {
						continue;
					}
					
					String value = tmp.getTextContent();
					this.ignores.add(value);
				}
				
				continue;
			}
			
			if (name.contains("checks")) {
				NodeList inodes = node.getChildNodes();
				int ilen = inodes.getLength() -1;
				
				for (int ii = 0; ii <= ilen; ii++) {
					Node tmp = inodes.item(ii);
					String tmp_name = tmp.getNodeName();
					if (!tmp_name.contains("regex")) {
						continue;
					}
					
					String value = tmp.getTextContent();
					this.checkes.add(value);
				}
			
				continue;
			}
		}
	}
	
}
