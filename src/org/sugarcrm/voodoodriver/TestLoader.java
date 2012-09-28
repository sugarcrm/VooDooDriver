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

package org.sugarcrm.voodoodriver;

import java.io.File;
import java.util.ArrayList;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.sugarcrm.voodoodriver.Event.Event;
import org.sugarcrm.voodoodriver.Event.UnknownEventException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


/**
 * Read a VDD/Soda test script and convert it into an array of {@link Event}.
 *
 * @author trampus
 * @author Jon duSaint
 */

public class TestLoader {

   /**
    * This Test's {@link Reporter} object.
    */

   private Reporter reporter;

   /**
    * The {@link Event} objects created from this test script.
    */

   private ArrayList<Event> events;


   /**
    * Initialize a TestLoader object using the provided test script.
    *
    * @param test  test script
    * @param rpt   {@link Reporter} object for logging messages and errors
    * @throws UnknownEventException
    */

   public TestLoader(File test, Reporter rpt)
      throws UnknownEventException, VDDException {
      this.reporter = rpt;

      compileTestFile(test);
   }


   /**
    * Load the Nodes from the test script.
    *
    * @param test  VooDoo test script
    * @return {@link NodeList} of unprocessed events
    * @throws VDDException should there be any problem parsing the test XML
    */

   private NodeList loadNodes(File test) throws VDDException {
      DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      DocumentBuilder db = null;
      Document doc = null;
      try {
         db = dbf.newDocumentBuilder();
      } catch (javax.xml.parsers.ParserConfigurationException e) {
         throw new VDDException("Failed to load " + test, e);
      }
      try {
         doc = db.parse(test);
      } catch (java.io.IOException e) {
         throw new VDDException("Unable to read " + test, e);
      } catch (org.xml.sax.SAXException e) {
         throw new VDDException("Invalid XML in " + test, e);
      }
      Element root = doc.getDocumentElement();

      if (root.getTagName().toLowerCase() != "voodoo") {
         /*
          * The root element should be <voodoo>; <soda> is deprecated.
          * This will eventually become a deprecation warning.
          */
         this.reporter.log("Root elements other than <voodoo> are deprecated");
      }

      return root.getChildNodes();
   }


   /**
    * Process a list of Nodes into Events.
    *
    * @param nodes  {@link NodeList} of nodes at this document level
    * @return {@link ArrayList} of Events
    * @throws UnknownEventException
    */

   private ArrayList<Event> processNodes(NodeList nodes)
      throws UnknownEventException, VDDException {
      ArrayList<Event> events = new ArrayList<Event>();

      for (int n = 0; n < nodes.getLength(); n++) {
         Node node = nodes.item(n);

         if (node.getNodeType() != Node.ELEMENT_NODE) {
            continue;
         }

         Event event = Event.createEvent((Element)node);

         if (node.hasChildNodes()) {
            ArrayList<Event> children = processNodes(node.getChildNodes());
            event.setChildren(children);
         }

         events.add(event);
      }

      return events;
   }


   /**
    * Load and process the test script into a form suitable for execution.
    *
    * @throws UnknownEventException
    */

   private void compileTestFile(File test)
      throws UnknownEventException, VDDException {
      NodeList nodes = loadNodes(test);
      this.events = processNodes(nodes);
   }


   /**
    * Retrieve the {@link ArrayList} of {@link Event} objects.
    *
    * @return list of Event objects
    */

   public ArrayList<Event> getEvents() {
      return this.events;
   }
}
