package voodoodriver;
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

import java.io.File;
import java.lang.reflect.Constructor;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Class to parse a VooDooDriver config xml file.
 * 
 * @author trampus
 *
 */
public class SodaConfigParser {

	private File configFile = null;
	
	/**
	 * {@link Constructor}
	 * 
	 * @param configfile VooDooDriver config file.
	 */
	public SodaConfigParser(File configfile) {
		this.configFile = configfile;
	}
	
	/**
	 * Parses the VooDooDriver config file.
	 * 
	 * @return {@link SodaEvents}
	 */
	public SodaEvents parse() {
		SodaEvents options = new SodaEvents();
		DocumentBuilderFactory dbf = null;
		DocumentBuilder db = null;
		Document doc = null;
		NodeList nodes = null;
		int node_count = 0;
		ArrayList<String> hijacks = null;
		ArrayList<String> suites = null;
		ArrayList<String> tests = null;
		
		try {
			dbf = DocumentBuilderFactory.newInstance();
			db = dbf.newDocumentBuilder();
			doc = db.parse(this.configFile);	
		} catch (Exception exp) {
			exp.printStackTrace();
			System.exit(2);
		}
		
		nodes = doc.getDocumentElement().getChildNodes();
		node_count = nodes.getLength() -1;
		
		for (int i = 0; i <= node_count; i++) {
			SodaHash data = new SodaHash();
			Node tmp = nodes.item(i);
			String name = tmp.getNodeName();
			
			if (name.contains("#text")) {
				continue;
			}
			
			if (name.contains("test")) {
				if (tests == null) {
					tests = new ArrayList<String>();
				}

				tests.add(tmp.getTextContent());
				continue;
			}
			
			if (name.contains("suite")) {
				if (suites == null) {
					suites = new ArrayList<String>();
				}
				
				suites.add(tmp.getTextContent());
				continue;
			}
			
			if (name.contains("hijack")) {
				if (hijacks == null) {
					hijacks = new ArrayList<String>();
				}

				hijacks.add(tmp.getTextContent());
				continue;
			}
				
			if (tmp.hasAttributes()) {
				NamedNodeMap attrs = tmp.getAttributes();
				int attrs_count = attrs.getLength() -1;
				
				for (int x = 0; x <= attrs_count; x++) {
					Node tmp_attr = attrs.item(x);
					String attr_name = tmp_attr.getNodeName();
					String attr_value = tmp_attr.getNodeValue();
					data.put(attr_name, attr_value);
				}
			}
			
			String value = tmp.getTextContent();
			data.put("type", name);
			data.put("value", value);
			options.add(data);
		}
		
		if (hijacks != null) {
			SodaHash jackhash = new SodaHash();
			jackhash.put("hijacks", hijacks);
			jackhash.put("type", "hijacks");
			options.add(jackhash);
		}
		
		if (suites != null) {
			SodaHash suitehash = new SodaHash();
			suitehash.put("suites", suites);
			suitehash.put("type", "suites");
			options.add(suitehash);
		}
		
		if (tests != null) {
			SodaHash testhash = new SodaHash();
			testhash.put("tests", tests);
			testhash.put("type", "tests");
			options.add(testhash);
		}
			
		return options;
	}
}
