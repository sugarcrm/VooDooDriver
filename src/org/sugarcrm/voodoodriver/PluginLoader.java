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
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


/**
 * VooDooDriver plugin loader
 *
 * @author trampus
 * @author Jon duSaint
 */

public class PluginLoader {

   /**
    * The filename of the plugin config file.
    */

   private String pluginFilename;

   /**
    * The <plugin> node from the parsed plugin config file.
    */

   private Node pluginNode;


   /**
    * Read and parse the XML plugin configuration file
    *
    * @param plugin  path to the XML plugin configuration file
    */

   public PluginLoader(String plugin) throws PluginException {
      Element data;

      this.pluginFilename = plugin;

      try {
         DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
         DocumentBuilder db = dbf.newDocumentBuilder();
         Document doc = db.parse(new File(plugin));
         data = doc.getDocumentElement();
      } catch (javax.xml.parsers.ParserConfigurationException e) {
         throw new PluginException("??? Failed configuring plugin XML parser");
      } catch (java.io.IOException e) {
         throw new PluginException("Unable to read " + plugin, e);
      } catch (org.xml.sax.SAXException e) {
         throw new PluginException("XML error in " + plugin, e);
      }

      if (!data.getNodeName().equals("data")) {
         throw new PluginException("Plugin " + plugin +
                                   " missing <data> root element");
      }

      NodeList nl = data.getChildNodes();
      for (int k = 0; k < nl.getLength(); k++) {
         Node node = nl.item(k);
         if (node.getNodeName().equals("plugin")) {
            if (!node.hasChildNodes()) {
               throw new PluginException("Plugin " + plugin + " <plugin> " +
                                         "node contains no information");
            }
            this.pluginNode = node;
            return;
         }
      }

      throw new PluginException("Plugin " + plugin +
                                " missing <plugin> element");
   }


   /**
    * Load the VooDooDriver plugin
    *
    * @return  either a {@link JavaPlugin} or a {@link JsPlugin}
    */

   public Plugin load() throws PluginException {
      Plugin plugin;
      String classname = null;
      String classfile = null;
      String jsfile = null;
      String elements[] = null;
      String events[] = null;
      String args[] = null;

      NodeList nl = pluginNode.getChildNodes();
      for (int k = 0; k < nl.getLength(); k++) {
         Node node = nl.item(k);
         String name = node.getNodeName().toLowerCase();

         if (name.equals("classname")) {
            classname = node.getTextContent();
         } else if (name.equals("classfile")) {
            classfile = node.getTextContent();
         } else if (name.equals("jsfile")) {
            jsfile = node.getTextContent();
         } else if (name.equals("control")) {
            elements = node.getTextContent().split(",");
         } else if (name.equals("event")) {
            events = node.getTextContent().split(",");
         } else if (name.equals("event")) {
            args = node.getTextContent().split(",");
         } else if (name.contains("#text")) {
            continue;
         } else {
            System.out.println("(W)Unknown plugin tag '" + name + "' in " +
                               pluginFilename);
            continue;
         }
      }

      if (classname != null && classfile != null) {
         plugin = new JavaPlugin(classname, classfile);
      } else if (jsfile != null) {
         plugin = new JsPlugin(jsfile);
      } else {
         throw new PluginException("Plugin " + pluginFilename +
                                   " is neither a javascript plugin nor a" +
                                   " java plugin.");
      }

      plugin.setElements(elements);
      plugin.setEvents(events);
      plugin.setArgs(args);

      return plugin;
   }
}
