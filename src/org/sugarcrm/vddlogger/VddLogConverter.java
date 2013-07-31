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
    * VDDReporter entry point.
    *
    * @param args  command line arguments
    */

   public static void main(String[] args) {
      ArrayList<File> xml = null;
      File dir = null;
      VddLogCmdParser p = new VddLogCmdParser(args);
      VddLogCmdOpts opts = p.parse();

      if (opts.containsKey("help")) {
         System.out.println("Usage:\n" +
                            "   VDDReporter --suitefile=<suite.xml>\n" +
                            "   VDDReporter --suitedir=<suite dir>");
         System.exit(0);
      }

      if (opts.containsKey("suitefile")) {
         File f = new File(opts.get("suitefile"));

         if (!f.exists()) {
            System.out.println("(!)Suite file '" + f + "' does not exist");
            System.exit(3);
         }

         System.out.println("(*)Processing suite file: '" + f + "'...");
         xml = new ArrayList<File>();
         xml.add(f);
         dir = f.getAbsoluteFile().getParentFile();
      } else if (opts.containsKey("suitedir")) {
         dir = new File(opts.get("suitedir"));
         System.out.println("(*)Processing suite directory: '" + dir + "'.");

         File fs[] = dir.listFiles(new java.io.FilenameFilter() {
               public boolean accept(File dir, String name) {
                  boolean ok = name.toLowerCase().endsWith(".xml");
                  if (ok) {
                     System.out.println("(*)Found Suite File: '" + name + "'.");
                  }
                  return ok;
               }
            });

         if (fs == null) {
            System.out.println("(!)Suite directory '" + dir + "' is not valid");
            System.exit(4);
         }

         xml = new ArrayList<File>(java.util.Arrays.asList(fs));

      } else {
         System.out.println("(!)Missing --suitefile or --suitedir!");
         System.exit(2);
      }

      System.out.println("(*)Generating Summary file...");
      VDDReporter r = new VDDReporter(xml, dir);
      r.generateReport();
   }
}
