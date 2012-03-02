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
import java.util.Arrays;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;

public class SuiteParser {

   private SodaTestList tests = null;
   private VDDHash gvars = null;

   public SuiteParser(String suitefile, VDDHash gvars) {
      Document doc = null;
      File suiteFD = null;
      DocumentBuilderFactory dbf = null;
      DocumentBuilder db = null;

      this.gvars = gvars;

      if (this.gvars == null) {
         this.gvars = new VDDHash();
      }

      try {
         this.tests = new SodaTestList();
         suiteFD = new File(suitefile);
         dbf = DocumentBuilderFactory.newInstance();
         db = dbf.newDocumentBuilder();
         doc = db.parse(suiteFD);
         this.parse(doc.getDocumentElement().getChildNodes());
      } catch (Exception exp) {
         exp.printStackTrace();
      }
   }

   public SodaTestList getTests() {
      return this.tests;
   }

   private void parse(NodeList nodes) {
      int len = nodes.getLength() -1;

      for (int i = 0; i <= len; i++) {
         String name = nodes.item(i).getNodeName();
         if (name.contains("#text")) {
            continue;
         }

         if (!name.contains("script")) {
            continue;
         }

         NamedNodeMap attrs = nodes.item(i).getAttributes();
         int atts_len = attrs.getLength() -1;
         for (int x = 0; x <= atts_len; x++) {
            String attr_name = attrs.item(x).getNodeName();
            String attr_value = attrs.item(x).getNodeValue();
            attr_value = SodaUtils.replaceString(attr_value, this.gvars);
            System.out.printf("'%s' => '%s'\n", attr_name, attr_value);
            File fd_tmp = null;

            if (attr_name.contains("fileset")) {
               fd_tmp = new File(attr_value);
               String base_path = fd_tmp.getAbsolutePath();
               String[] files = fd_tmp.list();

               if (files != null) {
                  Arrays.sort(files);
               } else {
                  continue;
               }

               for (int findex = 0; findex <= files.length -1; findex++) {
                  if (files[findex].toLowerCase().matches(".*\\.xml")) {
                     this.tests.add(base_path+"/"+files[findex]);
                     System.out.printf("(*)Adding file to Soda Suite list: '%s'.\n", base_path+"/"+files[findex]);
                  } else {
                     System.out.printf("(!)Not adding file to Soda Suite list: '%s'.\n", base_path+"/"+files[findex]);
                  }
               }
            } else {
               this.tests.add(attr_value);
            }
         }
      }
   }

}
