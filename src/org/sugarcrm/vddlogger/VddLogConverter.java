/*
Copyright 2011-2012 SugarCRM Inc.

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

      parser = new VddLogCmdParser(args);
      opts = parser.parse();

      if (opts.containsKey("help")) {
         PrintHelp();
         System.exit(0);
      }

      if (opts.containsKey("suitefile")) {
         File suitefile = new File((String)opts.get("suitefile"));

         if (!suitefile.exists()) {
            System.out.println("(!)Suite file '" + suitefile +
                               "' does not exist");
            System.exit(3);
         }

         System.out.printf("(*)Processing suite file: '%s'...\n", suitefile);
         handleSuiteFile(suitefile);
      } else if (opts.containsKey("suitedir")) {
         File suitedir = new File(opts.get("suitedir"));

         if (!suitedir.exists()) {
            System.out.println("(!)Suite directory '" + suitedir +
                               "' does not exist");
            System.exit(4);
         }

         System.out.printf("(*)Opening suitedir: '%s'.\n", suitedir);
         handleSuiteDir(suitedir);

      } else {
         System.out.println("(!)Missing --suitefile or --suitedir!");
         System.exit(2);
      }
   }


   /**
    * Process the directory specified by --suitedir.
    *
    * @param dir  the specified directory
    */

   private static void handleSuiteDir(File dir) {
      ArrayList<File> xmlsuitefiles = new ArrayList<File>();
      String[] files = null;

      files = dir.list();

      for (int i = 0; i <= files.length -1; i++) {
         if (!files[i].toLowerCase().endsWith(".xml")) {
            continue;
         }

         System.out.printf("(*)Found Suite File: '%s'.\n", files[i]);
         String filename = dir.toString() + File.separatorChar + files[i];
         xmlsuitefiles.add(new File(filename));
      }

      System.out.printf("(*)Generating Summary file...\n");
      VDDReporter summary = new VDDReporter(xmlsuitefiles, dir);
      summary.generateReport();
   }


   /**
    * Process the file specified by --suitefile.
    *
    * @param file  the specified file
    */

   public static void handleSuiteFile(File file) {
      ArrayList<File> xmlsuitefiles = new ArrayList<File>();

      xmlsuitefiles.add(file);
      System.out.printf("(*)Generating Summary file...\n");
      VDDReporter summary =
         new VDDReporter(xmlsuitefiles,
                         file.getAbsoluteFile().getParentFile());
      summary.generateReport();
   }


   /**
    *
    */

   public static void PrintHelp() {
      String msg = "This is a help message!";
      System.out.printf("%s\n", msg);
   }

}
