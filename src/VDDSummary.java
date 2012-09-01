/*
 * Copyright 2012 SugarCRM Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * Please see the License for the specific language governing permissions and
 * limitations under the License.
 */


import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.Stack;
import org.sugarcrm.voodoodriver.VDDHash;


/**
 * Class representing a VDD summary log file.
 *
 * @author Jonathan duSaint
 */

public class VDDSummary {

   /**
    * Summary file file name.
    */

   private File filename;

   /**
    * Summary file file handle.
    */

   private FileWriter fs;

   /**
    * Stack of the XML tags currently in use in the summary file.
    * This is used to determine indentation level and closing tag tag
    * name.
    */

   private Stack<String> tags;


   /**
    * Create a VDDSummary object.
    *
    * @param config  VDD config object
    */

   public VDDSummary(VDDHash config) throws IOException {
      this.generateFilename(config);
      this.fs = new FileWriter(this.filename);
      this.tags = new Stack<String>();
      this.openTag("data");
   }


   /**
    * Create the summary file filename.
    *
    * @param config  VDD config object
    * @return fully resolved summary file filename
    */

   private void generateFilename(VDDHash config) {
      String host = "unknown";

      try {
         host = java.net.InetAddress.getLocalHost().getHostName();
      } catch (java.net.UnknownHostException e) {
         // Unlikely and harmless.
      }

      String date = String.format("%1$tm-%1$td-%1$tY-%1$tI-%1$tM-%1$tS.%1$tL",
                                  new Date());

      this.filename = new File((String)config.get("resultdir"),
                               host + "-" + date + ".xml");
   }


   /**
    * Get the generate summary file file name.
    *
    * @return summary file name
    */

   public File getFilename() {
      return this.filename;
   }


   /**
    * Return the number of spaces to indent in the summary file.
    *
    * @return a String with the correct number of spaces
    */

   private String indent() {
      char[] c = new char[this.tags.size() * 2];
      for (int k = 0; k < c.length; k++) {
         c[k] = ' ';
      }
      return new String(c);
   }


   /**
    * Write a line of data to the summary file.
    */

   private void write(String line) {
      try {
         this.fs.write(line + "\n");
      } catch (IOException e) {
         System.err.println("(!)Failed to write line to summary file: " + e);
      }
   }


   /**
    * Open an XML block tag.
    *
    * @param tag  the XML tag name
    */

   public void openTag(String tag) {
      this.write(indent() + "<" + tag + ">");
      this.tags.push(tag);
   }


   /**
    * Close an XML block tag.
    */

   public void closeTag() {
      assert !this.tags.empty();
      String tag = this.tags.pop();
      this.write(indent() + "</" + tag + ">");
   }


   /**
    * Write a data tag and its data to the summary file.
    *
    * @param tag   the tag name
    * @param data  the tag data
    */

   public void writeData(String tag, String data) {
      this.write(String.format("%s<%s>%s</%s>", indent(), tag, data, tag));
   }


   /**
    * Close the VDD summary file.
    */

   public void close() {
      this.closeTag();
      try {
         this.fs.close();
      } catch (IOException e) {
         // Ignore
      }
   }

}
