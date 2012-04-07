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
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


/**
 * Class that loads VDD events and metadata from Events.xml.
 *
 * @author Trampus
 */

public class EventLoader {

   /**
    * XML file containing descriptions of all events.
    */

   private final String EVENTS = "Events.xml";

   /**
    * Processed list of events from Events.xml.
    */

   private ArrayList<VDDHash> events;

   /**
    * Names of events.  Redundantly stored here to speed lookup in isValid.
    */

   private VDDHash eventNames;


   /**
    * Instantiate EventLoader class and load Events.xml.
    */

   public EventLoader() throws VDDException {
      Class c = getClass();
      String className = c.getName().replace('.', '/');
      String classJar =  c.getResource("/" + className + ".class").toString();

      DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      DocumentBuilder db = null;
      try {
         db = dbf.newDocumentBuilder();
      } catch (javax.xml.parsers.ParserConfigurationException e) {
         throw new VDDException("Failed to load XML parser for Events.xml", e);
      }

      InputStream eis = null;
      if (classJar.startsWith("jar:")) {
         eis = c.getResourceAsStream(EVENTS);
      } else {
         try {
            eis = new FileInputStream(c.getResource(EVENTS).getFile());
         } catch (java.io.FileNotFoundException e) {
            throw new VDDException("Failed to find Events.xml", e);
         }
      }

      Document d = null;
      try {
         d = db.parse(eis);
      } catch (java.io.IOException e) {
         throw new VDDException("Failed to read Events.xml", e);
      } catch (org.xml.sax.SAXException e) {
         throw new VDDException("Illegal XML in Events.xml", e);
      }

      this.loadEvents(d.getDocumentElement().getChildNodes());
   }


   /**
    * Load all events from the {@link NodeList} created from Events.xml.
    *
    * @param nodes  {@link NodeList} of event nodes
    */

   private void loadEvents(NodeList nodes) throws VDDException {
      this.events = new ArrayList<VDDHash>();
      this.eventNames = new VDDHash();

      for (int k = 0; k < nodes.getLength(); k++) {
         Node node = nodes.item(k);
         VDDHash data = new VDDHash();
         String name = node.getNodeName();

         if (name.startsWith("#") || name.contains("comment")) {
            continue;
         }

         if (!Elements.isMember(name.toUpperCase())) {
            throw new VDDException("Unknown type '" + name + "' in Events.xml");
         }

         eventNames.put(name.toUpperCase(), true);
         data.put(name, 0);
         data.put("type", Elements.valueOf(name.toUpperCase()));

         if (node.hasAttributes()) {
            NamedNodeMap attrs = node.getAttributes();
            String validAttrs[] = {"html_tag", "html_type"};
            for (String key: validAttrs) {
               Node value = attrs.getNamedItem(key);
               if (value != null) {
                  data.put(key, value.getNodeValue());
               }
            }
         }

         if (node.hasChildNodes()) {
            NodeList children = node.getChildNodes();
            for (int m = 0; m < children.getLength(); m++) {
               Node child = children.item(m);
               String childName = child.getNodeName();

               if (childName.equals("soda_attributes") ||
                   childName.equals("accessor_attributes")) {
                  data.put(childName, parseAccessors(child.getChildNodes()));
               }
            }
         }

         this.events.add(data);
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
         String node_name = nodes.item(i).getNodeName();
         if (node_name == "#text") {
            continue;
         }

         if (node_name != "action") {
            String value = nodes.item(i).getTextContent();
            if (value.isEmpty() || value.startsWith("\n")) {
               continue;
            }
            hash.put(value, 0);
         } else {
            VDDHash act_hash = new VDDHash();
            NodeList actions = nodes.item(i).getChildNodes();
            int actlen = actions.getLength() -1;

            for(int x = 0; x <= actlen; x++) {
               String act = actions.item(x).getTextContent();
               String act_name = actions.item(x).getNodeName();

               if (act_name == "name") {
                  act_hash.put(act_name, act);
               }
            }
            hash.put(node_name, act_hash);
         }
      }

      return hash;
   }


   /**
    * Get the list of events processed from Events.xml.
    *
    * @return {@link ArrayList} of processed events
    */

   public ArrayList<VDDHash> getEvents() {
      return events;
   }


   /**
    * Verify that an event from a test script is valid.
    *
    * @param event  event from a test script
    * @return whether that event is found in Events.xml
    */

   public boolean isValid(String event) {
      return eventNames.containsKey(event.toUpperCase());
   }
}
