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

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import au.com.bytecode.opencsv.CSVParser;

public class CSV {

   private Reporter report = null;
   private ArrayList<String> keys = null;
   private CSVData data = null;
   private CSVParser parser = null;

   public CSV(String csvfile, Reporter reporter) {
      FileInputStream fs = null;
      BufferedReader br = null;

      this.report = reporter;
      this.parser = new CSVParser(',','"');

      try {
         this.keys = new ArrayList<String>();
         data = new CSVData();

         fs = new FileInputStream(csvfile);
         br = new BufferedReader(new InputStreamReader(fs));
         this.findKeys(br);
         this.createData(br);
      } catch (Exception e) {
         this.report.ReportException(e);
      }
   }

   /**
    * Return the data read from a CSV file.
    *
    * @return CSVData object.
    */

   public CSVData getData() {
      return this.data;
   }

   /**
    * Read the CSV file and process the file into an array of hashes.
    *
    * @param br  BufferedReader for the CSV file with line 1 consumed
    */

   private void createData(BufferedReader br) {
      String line = "";
      String[] linedata;

      try {
         while ((line = br.readLine()) != null) {
            line = line.replaceAll("\\n", "");
            if (line.isEmpty()) {
               continue;
            }

            linedata = this.parser.parseLine(line);

            int linelen = linedata.length -1;
            VDDHash tmphash = new VDDHash();
            for (int i = 0; i <= this.keys.size() -1; i++) {
               if (i <= linelen) {
                  tmphash.put(this.keys.get(i), linedata[i]);
               } else {
                  tmphash.put(this.keys.get(i), "");
               }
            }
            this.data.add(tmphash);
         }
      } catch (Exception exp) {
         this.report.ReportException(exp);
      }
   }

   /**
    * Find the CSV file's key line and process the line into an array.
    *
    * @param br  BufferedReader for the open file
    */
   private void findKeys(BufferedReader br) {
      String line = "";
      String[] lines;

      try {
         keys = new ArrayList<String>();

         while ((line = br.readLine()) != null) {
            line = line.replaceAll("\\n", "");
            if (line.isEmpty()) {
               continue;
            } else {
               break;
            }
         }

         //lines = line.split(",");
         //lines = this.processLine(line);
         lines = this.parser.parseLine(line);
         for (int i = 0; i <= lines.length -1; i++) {
            this.keys.add(lines[i]);
         }

      } catch (Exception exp) {
         this.report.ReportException(exp);
      }
   }
}
