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
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import voodoodriver.SodaEvents;
import voodoodriver.SodaHash;

public class SodaPluginParser {

	private NodeList Nodedata = null;
	
	public SodaPluginParser(String filename) throws Exception {
		File fd = null;
		DocumentBuilderFactory dbf = null;
		DocumentBuilder db = null;
		Document doc = null;
		
		fd = new File(filename);
		if (!fd.exists()) {
			throw new Exception("Failed to find file: " + filename);
		}
		
		dbf = DocumentBuilderFactory.newInstance();
		db = dbf.newDocumentBuilder();
		doc = db.parse(fd);
		this.Nodedata = doc.getDocumentElement().getChildNodes();
	}
	
	public SodaEvents parse() throws Exception {
		SodaEvents data = null;
		int len = this.Nodedata.getLength() -1;
		
		data = new SodaEvents();
		
		for (int i = 0; i <= len; i++) {
			Node child = this.Nodedata.item(i);
			String name = child.getNodeName();
			
			if (!name.contains("plugin")) {
				continue;
			}
			
			if (!child.hasChildNodes()) {
				System.out.printf("(!)Error: Failed to find all needed data for plugin node!\n");
				continue;
			}
			
			SodaHash tmp = new SodaHash();
			
			int clen = child.getChildNodes().getLength() -1;
			String controls = "";
			for (int cindex = 0; cindex <= clen; cindex++) {
				Node info = child.getChildNodes().item(cindex);
				String cname = info.getNodeName();
				cname = cname.toLowerCase();
				
				if (cname.contains("#text")) {
					continue;
				}
				
				if (cname.contains("control")) {
					controls = info.getTextContent();
					continue;
				}
				
				String value = info.getTextContent();
				tmp.put(cname, value);
			}
			
			if (controls.contains(",")) {
				String[] control_data = controls.split(",");
				int cdata_len = control_data.length -1;
				
				for (int p = 0; p <= cdata_len; p++) {
					SodaHash newdata = new SodaHash();
					newdata.putAll(tmp);
					newdata.put("control", control_data[p]);
					data.add(newdata);
					System.out.printf("Adding control permute: %s\n", control_data[p]);
				}
			} else {
				tmp.put("control", controls);
				data.add(tmp);
			}
		}
		
		return data;
	}
}
