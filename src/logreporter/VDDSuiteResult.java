package logreporter;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class VDDSuiteResult {

	private String suiteFile = null;
	
	public VDDSuiteResult(String suitefile) {
		this.suiteFile = suitefile;
	}
	
	private HashMap<String, String> parseSuite(Node node) {
		HashMap<String, String> data = new HashMap<String, String>();
		NodeList nodes = node.getChildNodes();
		
		for (int i = 0; i <= nodes.getLength() -1; i++) {
			Node tmpNode = nodes.item(i);
			String name = tmpNode.getNodeName();
			
			if (name.contains("#text")) {
				continue;
			} else if (name.contains("suitefile")) {
				String value = tmpNode.getNodeValue();
				data.put(name, value);
			} else if (name.contains("test")) {
				NodeList testNodes = tmpNode.getChildNodes();
				
				for (int x = 0; x <= testNodes.getLength() -1; x++) {
					Node tnode = testNodes.item(x);
					String tname = tnode.getNodeName();
					if (tname.contains("#text")) {
						continue;
					}
					
					String tvalue = tnode.getTextContent();
					data.put(tname, tvalue);
				}
			}
		}
		
		return data;
	}
	
	public ArrayList<HashMap<String, String>> parse() {
		DocumentBuilderFactory dbf = null;
		DocumentBuilder db = null;
		Document doc = null;
		ArrayList<HashMap<String, String>> result = new ArrayList<HashMap<String,String>>();
		File fd = new File(this.suiteFile);
		NodeList nodes = null;
		
		try {
			dbf = DocumentBuilderFactory.newInstance();
			db = dbf.newDocumentBuilder();
			doc = db.parse(fd);
			nodes = doc.getDocumentElement().getChildNodes();
			
			for (int i = 0; i <= nodes.getLength() -1; i++) {
				String name = nodes.item(i).getNodeName();
				if (!name.contains("suite")) {
					continue;
				}
				
				HashMap<String, String> data = this.parseSuite(nodes.item(i));
				result.add(data);
				data = null;
			}
			
		} catch (Exception exp) {
			exp.printStackTrace();
		}
		
		return result;
	}
	
}
