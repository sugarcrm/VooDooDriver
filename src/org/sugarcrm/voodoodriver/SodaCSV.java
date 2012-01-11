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

public class SodaCSV {

   private SodaReporter report = null;
   private ArrayList<String> keys = null;
   private SodaCSVData data = null;
   private CSVParser parser = null;

   public SodaCSV(String csvfile, SodaReporter reporter) {
      FileInputStream fs = null;
      BufferedReader br = null;

      this.report = reporter;
      this.parser = new CSVParser(',','"');

      try {
         this.keys = new ArrayList<String>();
         data = new SodaCSVData();

         fs = new FileInputStream(csvfile);
         br = new BufferedReader(new InputStreamReader(fs));
         this.findKeys(br);
         this.createData(br);
      } catch (Exception e) {
         this.report.ReportException(e);
      }
   }

   /*
    * getData -- method
    *    This method returns the data generated from a soda CSV file.
    *
    * Input:
    *    None.
    *
    *
    * Output:
    *    returns a SodaCSVData object.
    *
    */
   public SodaCSVData getData() {
      return this.data;
   }

   /*
    * createData -- method
    *    This method reads the csv file and process the file into an array of hashes.
    *
    * Input:
    *    br: the BufferedReader for the open csv file, after the key line has been read.
    *
    * Output:
    *    None.
    *
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
            SodaHash tmphash = new SodaHash();
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

   /*
    * findKeys -- method
    *    This method finds the csv files key line and processes the line into an array.
    *
    * Input:
    *    br: The BufferedReader for the open file starting at the beginning of the file.
    *
    * Output:
    *    None.
    *
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
