/*
 * Copyright 2011-2012 SugarCRM Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you
 * may not use this file except in compliance with the License.  You
 * may may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
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
import java.util.HashMap;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


/**
 * <p>PageAsserter is a class that scans the current page in the
 * browser for text or patterns.  Any text found is marked as an
 * assertion failure.  Prior to this search, entries can be added to
 * the whitelist.  They will be subsequently ignored by
 * PageAsserter.</p>
 *
 * <p>The entries for the page assertions come from a file specified
 * on the command line with the <code>--assertpagefile</code> option.
 * This file is an XML file with the root element
 * <code>&lt;voodoo&gt;</code> (<code>&lt;soda&gt;</code> continues to
 * be accepted, though it is deprecated.  Within this file, there can
 * be page assertions and whitelist entries.  The page assertions are
 * specified within a <code>&lt;checks&gt;</code> block, and the
 * whitelist entries are specified within an
 * <code>&lt;ignores&gt;</code> block.  Individual entries are
 * specified within <code>&lt;regex&gt;</code> tags.</p>
 *
 * <p>An example file is:
 * <pre><code>&lt;voodoo&gt;
 *    &lt;checks&gt;
 *       &lt;regex&gt;error&lt;/regex&gt;
 *       &lt;regex&gt;STOP \d+&lt;/regex&gt;
 *    &lt;/checks&gt;
 *    &lt;ignores&gt;
 *       &lt;regex&gt;no errors were found&lt;/regex&gt;
 *    &lt;/ignores&gt;
 * &lt;/voodoo&gt;</code></pre></p>
 *
 * @author trampus
 * @author Jon duSaint
 */

public class PageAsserter {

   /**
    * Pattern to search for.  Any occurrence of one of these is an error.
    */

   private ArrayList<TextFinder> assertions;

   /**
    * The whitelist is for text on a page that should be ignored.
    */

   private ArrayList<TextFinder> whitelist;

   /**
    * Reporter object.
    */

   private Reporter reporter;


   /**
    * Instantiate a PageAsserter object.
    *
    * @param assertPageFile  file containing page assertions
    * @param reporter        Reporter object
    * @throws VDDException if the page assertion file fails to load
    */

   public PageAsserter(File assertPageFile, Reporter reporter)
      throws VDDException {
      this.reporter = reporter;
      this.assertions = new ArrayList<TextFinder>();
      this.whitelist = new ArrayList<TextFinder>();

      loadAssertPageFile(assertPageFile);
   }


   /**
    * Load regex nodes into the provided list.
    *
    * @param lst    list into which to read the regexes
    * @param nodes  list of XML nodes to read
    * @throws VDDException if an unrecognized XML tag is found
    */

   private void readEntries(ArrayList<TextFinder> lst, NodeList nodes)
      throws VDDException {
      for (int k = 0; k < nodes.getLength(); k++) {
         Node node = nodes.item(k);
         if (node.getNodeType() != Node.ELEMENT_NODE) {
            continue;
         }
         if (!node.getNodeName().toLowerCase().equals("regex")) {
            throw new VDDException("Unknown assert page entry type '" +
                                   node.getNodeName() + "'");
         }

         lst.add(new TextFinder(node.getTextContent()));
      }
   }


   /**
    * Load the entries from the specified file.
    *
    * @param f  page assertion file
    * @throws VDDException if the page assertion file fails to load
    */

   private void loadAssertPageFile(File f) throws VDDException {
      NodeList nodes;

      try {
         DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
         DocumentBuilder db = dbf.newDocumentBuilder();
         Document doc = db.parse(f);

         nodes = doc.getDocumentElement().getChildNodes();
      } catch (javax.xml.parsers.ParserConfigurationException e) {
         throw new VDDException("XML parser failed to load", e);
      } catch (org.xml.sax.SAXException e) {
         throw new VDDException("Error during XML parsing", e);
      } catch (java.io.IOException e) {
         throw new VDDException("Failed to read page assertion file", e);
      }


      for (int k = 0; k < nodes.getLength(); k++) {
         Node node = nodes.item(k);
         String name;

         if (node.getNodeType() != Node.ELEMENT_NODE) {
            continue;
         }

         name = node.getNodeName().toLowerCase();

         if (name.equals("checks")) {
            readEntries(this.assertions, node.getChildNodes());
         } else if (name.equals("ignores")) {
            readEntries(this.whitelist, node.getChildNodes());
         } else {
            throw new VDDException("Unknown assert page entry type '" +
                                   name + "'");
         }
      }
   }


   /**
    * Add the values from the list of whitelist entries to the whitelist.
    *
    * @param whitelist  HashMap whose values are whitelist entries
    */

   private void addWhitelistEntries(HashMap<String,String> whitelist) {
      for (String entry: whitelist.values()) {
         this.whitelist.add(new TextFinder(entry));
      }
   }


   /**
    * Search the provided web page for text matching the page assertions.
    *
    * <p>Prior to searching the page, text matched by entries in the
    * whitelist are removed.  The provided whitelist entries are added
    * to the whitelist first.  Should a match be made, it is logged as
    * a failed assertion.</p>
    *
    * @param page       the web page text to search
    * @param whitelist  new whitelist entries
    */

   public void assertPage(String page, HashMap<String,String> whitelist) {
      this.addWhitelistEntries(whitelist);
      assertPage(page);
   }


   /**
    * Search the provided web page for text matching the page assertions.
    *
    * <p>Prior to searching the page, text matched by entries in the
    * whitelist are removed.  Should a match be made, it is logged as
    * a failed assertion.</p>
    *
    * @param page  the web page text to search
    */

   public void assertPage(String page) {
      for (TextFinder m: this.whitelist) {
         page = m.replaceAll(page, "");
      }

      for (TextFinder m: this.assertions) {
         if (m.find(page)) {
            this.reporter.Assert("Page Assert found match for '" +
                                 m.toString() + "'",
                                 true, false);
         }
      }
   }
}
