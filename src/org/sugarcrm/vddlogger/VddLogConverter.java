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

package org.sugarcrm.vddlogger;

import java.io.File;
import java.util.ArrayList;

public class VddLogConverter {

   /**
    * @param args
    */
   public static void main(String[] args) {
      VddLogCmdOpts opts = null;
      VddLogCmdParser parser = null;
      String suitedir = null;
      String suitefile = null;

      parser = new VddLogCmdParser(args);
      opts = parser.parse();

      if (opts.get("help") != null) {
         PrintHelp();
         System.exit(0);
      }

      suitefile = opts.get("suitefile");
      suitedir = opts.get("suitedir");
      if (suitefile == null && suitedir == null) {
         System.out.printf("(!)Error: Missing needed command line options: --suitefile or --suitedir!\n\n\n");
         PrintHelp();
         System.exit(2);
      }

      if (suitefile != null) {
         File suiteFD = new File(suitefile);

         if (!suiteFD.exists()) {
            System.out.printf("(!)Error: Failed to find suite file: '%s'!\n\n");
            System.exit(3);
         }

         System.out.printf("(*)Processing suite file: '%s'...\n", suitefile);
         handleSuiteFile(suitefile);
      }

      if (suitedir != null) {
         File suiteDir = new File(suitedir);

         if (!suiteDir.exists()) {
            System.out.printf("(!)Error: Failed to find suite directory: '%s'!\n\n", suitedir);
            System.exit(4);
         }

         System.out.printf("(*)Opening suitedir: '%s'.\n", suitedir);
         handleSuiteDir(suitedir);

      }
   }

   public static void handleSuiteDir(String dir) {
      File dirFD = new File(dir);
      ArrayList<File> xmlsuitefiles = new ArrayList<File>();
      ArrayList<String> suiteFiles = new ArrayList<String>();
      String[] files = null;

      files = dirFD.list();
      for (int i = 0; i <= files.length -1; i++) {
         if (!files[i].toLowerCase().endsWith("xml")) {
            continue;
         }

         System.out.printf("(*)Found Suite File: '%s'.\n", files[i]);
         String filename = String.format("%s%s%s", dir, File.separatorChar, files[i]);
         suiteFiles.add(filename);
         xmlsuitefiles.add(new File(filename));
      }

      System.out.printf("(*)Generating Summary file...\n");
      VddSummaryReporter summary = new VddSummaryReporter(xmlsuitefiles, dir);
      summary.generateReport();

   }

   public static void processSuiteData(ArrayList<VddSuiteFileList> data) {

      for (int i = 0; i <= data.size() -1; i++) {
         VddSuiteFileList list = data.get(i);
         for (int suiteIndex = 0; suiteIndex <= list.size() -1; suiteIndex++) {
            VddSuiteFile sfile = list.get(suiteIndex);
            String[] keys = sfile.keySet().toArray(new String[0]);
            for (int x = 0; x <= keys.length -1; x++) {
               System.out.printf("(KEY)%s => %s\n", keys[x], sfile.get(keys[x]));
            }
         }
      }

   }

   public static void handleSuiteFile(String filename) {
      ArrayList<File> xmlsuitefiles = new ArrayList<File>();
      String dir = "";

      File tmp = new File(filename);
      dir = tmp.getParent();
      if (dir == null) {
         dir = "";
      }

      xmlsuitefiles.add(new File(filename));
      System.out.printf("(*)Generating Summary file...\n");
      VddSummaryReporter summary = new VddSummaryReporter(xmlsuitefiles, dir);
      summary.generateReport();
   }

   public static void PrintHelp() {
      String msg = "This is a help message!";
      System.out.printf("%s\n", msg);
   }

}
