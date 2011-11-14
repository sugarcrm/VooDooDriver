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
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * This is a simple class for reading a soda xml test file into a SodaEvents class.
 * @author trampus
 *
 */

public class SodaXML {
	
	private Document doc = null;
	private SodaElementsList types = null;
	private SodaTypes sodaTypes = null;
	private SodaEvents events = null;
	private SodaReporter reporter = null;
	
	/*
	 * SodaXML: Constructor
	 * 
	 * Input:
	 * 	sodaTest: A full path to a soda test file.
	 * 
	 * Output:
	 * 	None.
	 */
	public SodaXML(String sodaTest, SodaReporter reporter) {
		File testFD = null;
		DocumentBuilderFactory dbf = null;
		DocumentBuilder db = null;

		this.reporter = reporter;
		
		try {
			testFD = new File(sodaTest);
			dbf = DocumentBuilderFactory.newInstance();
			db = dbf.newDocumentBuilder();
			doc = db.parse(testFD);
			sodaTypes = new SodaTypes();
			types = sodaTypes.getTypes();
			events = this.parse(doc.getDocumentElement().getChildNodes());
		} catch (Exception exp) {
			this.events = null;
			if (this.reporter == null) {
				exp.printStackTrace();
			} else {
				this.reporter.ReportException(exp);
			}
		}
	}
	
	/*
	 * getEvents:
	 * 	This method returns the events that were created from the soda test file.
	 * 
	 * Input:
	 * 	None.
	 * 
	 * Output:
	 * 	returns a SodaEvents object.
	 * 
	 */
	public SodaEvents getEvents() {
		return this.events;
	}
	
	/*
	 * findElementAccessor:
	 * 	This method finds an accessor for a given soda element.
	 * 
	 * Intput:
	 * 	sodaelement: This is the element to find an accessor for.
	 * accessor: This is the accessor to find for the given element.
	 * 
	 * Output:
	 * 	returns a string with the given accessor if one exists, else null.
	 * 
	 */
	private String findElementAccessor(SodaElements sodaelement, String accessor) {
		String result = null;
		int len = types.size() -1;
		SodaHash foundType = null;
		SodaHash accessors = null;
		
		for (int i = 0; i <= len; i++) {
			if (types.get(i).get("type") == sodaelement) {
				foundType = types.get(i);
				break;
			}
		}
		
		if (foundType == null) {
			System.out.printf("foundType == null!\n");
			return null;
		}
		
		if (!foundType.containsKey("accessor_attributes")) {
			return null;
		}
		
		accessors = (SodaHash)foundType.get("accessor_attributes");
		if (accessors.containsKey(accessor)) {
			result = accessor;
		}
		
		return result;
	}
	
	/*
	 * processAttributes:
	 * 	This method gets all of the attributes for a given soda element.
	 * 
	 * Intput:
	 * 	map: This is the soda element's map.
	 *  node: This is the xml node for the given element.
	 * 
	 * Output:
	 * 	returns a SodaHash object filled with the node's attributes if it has any.  If there are
	 * 	no attributes then an empty SodaHash is returned.
	 * 
	 */
	private SodaHash processAttributes(SodaHash map, Node node) {
		int len = node.getAttributes().getLength();
		String found_index = null;
		String accessor = null;
		
		if (node.hasAttributes()) {
			for (int i = 0; i <= len -1; i++) {
				Node tmp = node.getAttributes().item(i);
				String name = tmp.getNodeName();
				String value = tmp.getNodeValue();
				
				if (name == "index") {
					found_index = name;
				} else {
					accessor = findElementAccessor((SodaElements)map.get("type"), name);
				}

				map.put(name, value);
			}
			
			if (accessor != null) {
				map.put("how", accessor);
			} else if (accessor == null && found_index != null) {
				map.put("how", found_index);
			}
		}
		
		return map;
	}
	
	/*
	 * parse:
	 * 	This method parses all the xml nodes into a SodaEvents object.
	 * 
	 * Input:
	 * 	node:  This is a node list from the soda xml test.
	 * 
	 * Output:
	 * 	returns a SodaEvents object.
	 */
	private SodaEvents parse(NodeList node) throws Exception{
		SodaHash data = null;
		SodaEvents dataList = null;
		boolean err = false;
		int len = 0;
		
		dataList = new SodaEvents();

		len = node.getLength();
		for (int i = 0; i <= len -1; i++) {
			Node child = node.item(i);
			String name = child.getNodeName();
			
			if (name.startsWith("#")) {
				continue;
			}
			
			if (!sodaTypes.isValid(name)) {
				if (this.reporter == null) {
					System.err.printf("Error: Invalid Soda Element: '%s'!\n", name);
				} else {
					this.reporter.ReportError(String.format("Error: Invalid Soda Element: '%s'!", name));
				}
				
				err = true;
				break;
			}
			
			data = new SodaHash();
			data.put("do", name);
			data.put("type", SodaElements.valueOf(name.toUpperCase()));
		
			if (child.hasAttributes()) {
				data = processAttributes(data, child);
			}
			
			if (name.contains("javascript")) {
				String tmp = child.getTextContent();
				if (!tmp.isEmpty()) {
					data.put("content", tmp);
				}
			}
			
			if (name.contains("whitelist")) {
				String tmp = child.getTextContent();
				if (!tmp.isEmpty()) {
					data.put("content", tmp);
				}
			}
			
			if (child.hasChildNodes()) {
				if (name.contains("execute") || name.contains("javaplugin")) {
					String[] list = processArgs(child.getChildNodes());
					data.put("args", list);
				} else {
					SodaEvents tmp = parse(child.getChildNodes());
					if (tmp != null) {
						data.put("children", tmp);
					} else {
						err = true;
						break;
					}
				}
			}
			
			if (!data.isEmpty()) {
				dataList.add(data);
			} else {
				System.out.printf("Note: No data found.\n");
			}
		}
		
		if (err) {
			dataList = null;
		}
		
		return dataList;
	}
	
	private String[] processArgs(NodeList nodes) {
		int len = nodes.getLength() -1;
		String[] list;
		int arg_count = 0;
		int current = 0;
		
		for (int i = 0; i <= len; i++) {
			String name = nodes.item(i).getNodeName();
			if (name.contains("arg")) {
				arg_count += 1;
			}
		}
		
		list = new String[arg_count];
		
		for (int i = 0; i <= len; i++) {
			String name = nodes.item(i).getNodeName();
			if (name.contains("arg")) {
				String value = nodes.item(i).getTextContent();
				list[current] = value;
				current += 1;
			}
		}
		
		return list;
	}
	
}
