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

package org.sugarcrm.voodoodriver;

import java.io.File;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * This class is for parsing Soda Block List files.
 *
 * @author trampus
 *
 */
public class BlockListParser {

   private String file_name = null;

   /**
    * Constructor.
    *
    * @param file   The blocklist xml file to parse.
    */
   public BlockListParser(String file) {
      this.file_name = file;
   }

   /**
    * Parses the block list file.
    *
    * @return   Returns a BlockList.
    *
    * @see BlockList
    */
   public BlockList parse() {
      BlockList list = null;
      File FD = null;
      DocumentBuilderFactory dbf = null;
      DocumentBuilder db = null;
      Document doc = null;
      NodeList nodes = null;

      try {
         FD = new File(this.file_name);
         dbf = DocumentBuilderFactory.newInstance();
         db = dbf.newDocumentBuilder();
         doc = db.parse(FD);
         list = new BlockList();
      } catch (Exception exp) {
         System.err.printf("(!)Error: %s\n", exp.getMessage());
         list = null;
      }

      nodes = doc.getDocumentElement().getChildNodes();
      for (int i = 0; i <= nodes.getLength() -1; i++) {
         Node n = nodes.item(i);
         String name = n.getNodeName();
         if (!name.contains("block")) {
            continue;
         }

         NodeList kids = n.getChildNodes();
         VDDHash tmp = new VDDHash();
         for (int x = 0; x <= kids.getLength() -1; x++) {
            Node kid = kids.item(x);

            if (kid.getNodeName().contains("#text")) {
               continue;
            }

            String kid_name = kid.getNodeName();
            String value = kid.getTextContent();
            if (value != null) {
               tmp.put(kid_name, value);
            }
         }
         list.add(tmp);
      }

      return list;
   }
}
