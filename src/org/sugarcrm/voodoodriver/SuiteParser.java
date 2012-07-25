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
import java.io.FilenameFilter;
import java.util.ArrayList;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;


/**
 * Create a list of files specified by a suite file.
 *
 * @author trampus
 * @author Jon duSaint
 */

public class SuiteParser {

   /**
    * List of tests specified in this suite file.
    */

   private ArrayList<File> tests;

   /**
    * VDD global variables.
    */

   private VDDHash gvars;


   /**
    * Create a SuiteParser object.
    *
    * @param suite  name of the suite file
    * @param gvars  hash of global variables
    * @throws VDDException if an error occurs reading the suite file
    */

   public SuiteParser(File suite, VDDHash gvars) throws VDDException {
      File baseDir;
      NodeList nodes;

      this.gvars = (gvars == null) ? new VDDHash() : gvars;
      this.tests = new ArrayList<File>();

      if (this.gvars.containsKey("global.basedir")) {
         baseDir = new File((String)gvars.get("global.basedir"));
      } else {
         baseDir = new File(System.getProperty("user.dir"));
      }

      try {
         DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
         DocumentBuilder db = dbf.newDocumentBuilder();
         Document doc = db.parse(suite);

         nodes = doc.getDocumentElement().getChildNodes();
      } catch (javax.xml.parsers.ParserConfigurationException e) {
         throw new VDDException("XML parser failed to load", e);
      } catch (org.xml.sax.SAXException e) {
         throw new VDDException("Error during XML parsing", e);
      } catch (java.io.IOException e) {
         throw new VDDException("Failed to read XML suite file", e);
      }

      this.parse(nodes, baseDir);
   }


   /**
    * Return the list of tests in this suite.
    *
    * @return {@link ArrayList} of tests
    */

   public ArrayList<File> getTests() {
      return this.tests;
   }


   /**
    * Join a parent path with a path relative to it and return a canoncal form.
    *
    * @param parent  parent directory
    * @param child   child file or directory
    * @return an absolute path with parent and child joined
    * @throws java.io.IOException if an error occurs creating the path
    */

   private File absoluteFile(File parent, String child) throws VDDException {
      File f = new File(child);

      if (!f.isAbsolute()) {
         f = new File(parent, child);
      }

      try {
         return f.getCanonicalFile();
      } catch (java.io.IOException e) {
         throw new VDDException("Failed to get path to test", e);
      }
   }


   /**
    * Iterate through the XML nodes and store the test file names.
    *
    * @param nodes    {@link NodeList} of XML nodes
    * @param baseDir  the parent directory of this suite file
    * @throws VDDException if an error occurs during processing
    */

   private void parse(NodeList nodes, File baseDir) throws VDDException {
      int len = nodes.getLength() -1;

      for (int k = 0; k < nodes.getLength(); k++) {
         Node node = nodes.item(k);

         if (node.getNodeType() != Node.ELEMENT_NODE ||
             !node.getNodeName().toLowerCase().equals("script")) {
            continue;
         }

         NamedNodeMap attrs = node.getAttributes();
         Node attr;

         if ((attr = attrs.getNamedItem("file")) != null) {
            String relFile = Utils.replaceString(attr.getNodeValue(), gvars);
            File file = this.absoluteFile(baseDir, relFile);
            System.out.println("file => " + file);
            this.tests.add(file);
         } else if ((attr = attrs.getNamedItem("fileset")) != null) {
            String relFileset = Utils.replaceString(attr.getNodeValue(), gvars);
            File fileset = this.absoluteFile(baseDir, relFileset);
            System.out.println("fileset => " + fileset);

            String[] files = fileset.list(new FilenameFilter() {
                  public boolean accept(File d, String fn) {
                     return fn.toLowerCase().endsWith(".xml");
                  }
               });

            for (int n = 0; n < files.length; n++) {
               File f = new File(fileset, files[n]);
               System.out.println("(*)Adding file to VDD Suite list: " + f);
               this.tests.add(f);
            }
         }
      }
   }
}
