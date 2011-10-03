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

import java.io.File;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class VddSuiteFileParser {

	private String suiteFile = null;
	
	public VddSuiteFileParser(String suitefile) {
		this.suiteFile = suitefile;
		
	}
	
	private VddSuiteFile parseSuite(Node node) {
		VddSuiteFile result = new VddSuiteFile();
		NodeList nodes = node.getChildNodes();
		String tmp = "";
		
		for (int i = 0; i <= nodes.getLength() -1; i++) {
			Node kid = nodes.item(i);
			String name = kid.getNodeName();
			
			if (name.contains("#text")) {
				continue;
			}
			
			if (name.contains("suitefile")) {
				result.put("suitefile", name);
			}
			
			if (name.contains("test")) {
				NodeList testkids = kid.getChildNodes();
				for (int testi = 0; testi <= testkids.getLength() -1; testi++) {
					Node tkid = testkids.item(testi);
					String tkidname = tkid.getNodeName();
					
					if (tkidname.contains("#text")) {
						continue;
					}
					
					result.put(tkidname, tkid.getTextContent());
				}
			}
			
		}
		
		return result;
	}
	
	public VddSuiteFileList parse() {
		VddSuiteFileList list = null;
		DocumentBuilderFactory dbf = null;
		DocumentBuilder db = null;
		Document doc = null;
		NodeList nodes = null;
		
		try {
			dbf = DocumentBuilderFactory.newInstance();
			db = dbf.newDocumentBuilder();
			doc = db.parse(new File(this.suiteFile));
			list = new VddSuiteFileList();
		} catch (Exception exp) {
			exp.printStackTrace();
			return null;
		}
		
		nodes = doc.getDocumentElement().getChildNodes();
		for (int i = 0; i <= nodes.getLength() -1; i++) {
			Node node = nodes.item(i);
			String name = node.getNodeName();
			if (!name.contains("suite")) {
				continue;
			}
			
			VddSuiteFile suitedata = this.parseSuite(node);
			if (suitedata != null) {
				list.add(suitedata);
			}
		}
		
		return list;
	}
}
