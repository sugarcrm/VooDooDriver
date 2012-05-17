/*
 * Copyright 2011-2012 SugarCRM Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you
 * may not use this file except in compliance with the License.  You
 * may may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  Please see the License for the specific language
 * governing permissions and limitations under the License.
 */

package org.sugarcrm.voodoodriver.Event;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.sugarcrm.voodoodriver.VDDException;
import org.sugarcrm.voodoodriver.VDDHash;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


/**
 * Class that loads VDD events and metadata from Events.xml.
 *
 * @author Trampus
 * @author Jon duSaint
 */

public class EventLoader {

   /**
    * XML file containing descriptions of all events.
    */

   private static final String EVENTS = "Events.xml";

   /**
    * Processed list of kvps events from Events.xml.
    *
    * The keys are the event names, the values are VDDHashes
    * containing all the event metadata.
    */

   private VDDHash events;


   /**
    * Instantiate EventLoader class.
    */

   public EventLoader() {
      this.events = new VDDHash();
   }


   private Document loadEventsXml() throws VDDException {
      Class c = getClass();
      String className = c.getName().replace('.', '/');
      String classJar =  c.getResource("/" + className + ".class").toString();
      InputStream eis = null;
      DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      DocumentBuilder db = null;
      Document d = null;

      try {
         db = dbf.newDocumentBuilder();
      } catch (javax.xml.parsers.ParserConfigurationException e) {
         throw new VDDException("Failed to load XML parser for Events.xml", e);
      }

      if (classJar.startsWith("jar:")) {
         eis = c.getResourceAsStream(EVENTS);
      } else {
         try {
            eis = new FileInputStream(c.getResource(EVENTS).getFile());
         } catch (java.io.FileNotFoundException e) {
            throw new VDDException("Failed to find Events.xml", e);
         }
      }

      try {
         d = db.parse(eis);
      } catch (java.io.IOException e) {
         throw new VDDException("Failed to read Events.xml", e);
      } catch (org.xml.sax.SAXException e) {
         throw new VDDException("Illegal XML in Events.xml", e);
      }

      return d;
   }


   /**
    * Load all events from the {@link NodeList} created from Events.xml.
    */

   public void loadEvents() throws VDDException {
      Document d = loadEventsXml();
      NodeList nodes = d.getDocumentElement().getChildNodes();

      for (int k = 0; k < nodes.getLength(); k++) {
         Node node = nodes.item(k);
         VDDHash event = new VDDHash();

         if (node.getNodeType() != Node.ELEMENT_NODE) {
            continue;
         }

         if (node.hasAttributes()) {
            NamedNodeMap attrs = node.getAttributes();
            String validAttrs[] = {"html_tag", "html_type"};
            for (String key: validAttrs) {
               Node value = attrs.getNamedItem(key);
               if (value != null) {
                  event.put(key, value.getNodeValue());
               }
            }
         }

         if (node.hasChildNodes()) {
            NodeList children = node.getChildNodes();
            for (int m = 0; m < children.getLength(); m++) {
               Node child = children.item(m);
               String childName = child.getNodeName();

               if (childName.equals("selectors") ||
                   childName.equals("actions")) {
                  event.put(childName, parseAccessors(child.getChildNodes()));
               }
            }
         }

         this.events.put(node.getNodeName(), event);
      }
   }


   /**
    * Process the attributes lists for events.
    *
    * @param {@link NodeList} of attributes
    * @return {@link VDDHash} of processed attributes
    */

   private VDDHash parseAccessors(NodeList nodes) {
      VDDHash hash = new VDDHash();

      for (int i = 0; i < nodes.getLength(); i++) {
         Node node = nodes.item(i);
         String nodeName = node.getNodeName();

         if (node.getNodeType() != Node.ELEMENT_NODE) {
            continue;
         }

         if (nodeName.equals("accessor")) {
            String value = node.getTextContent();
            if (value.isEmpty() || value.startsWith("\n")) {
               continue;
            }
            
            hash.put(value,
                     node.getAttributes().getNamedItem("type").getNodeValue());
         } else if (nodeName.equals("action")) {
            VDDHash act_hash = new VDDHash();
            NodeList actions = node.getChildNodes();
            int actlen = actions.getLength() -1;

            for(int x = 0; x <= actlen; x++) {
               String act = actions.item(x).getTextContent();
               String act_name = actions.item(x).getNodeName();

               if (act_name == "name") {
                  act_hash.put(act_name, act);
               }
            }
            hash.put(nodeName, act_hash);
         }
      }

      return hash;
   }


   /**
    * Get the list of events processed from Events.xml.
    *
    * @return {@link ArrayList} of processed events
    */

   public VDDHash getEvents() {
      return events;
   }
}
