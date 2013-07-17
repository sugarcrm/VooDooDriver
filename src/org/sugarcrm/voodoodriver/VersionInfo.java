/*
 * Copyright 2011-2013 SugarCRM Inc.
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
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

public class VersionInfo {

   private static final String PROPERTIES = "vdd.properties";
   private static final String COMMIT = "commit";
   private static final String VERSION = "version";

   private static String commit = "unknown";
   private static String version = "undefined";

   static {
      Class c = VersionInfo.class;
      Properties p = new Properties();

      try {
         if (c.getResource("/" + c.getName().replace('.', '/') + ".class").getProtocol().equals("jar")) {
            p.load(c.getResourceAsStream(PROPERTIES));
         } else {
            p.load(new FileInputStream(new File(c.getResource(PROPERTIES).getFile())));
         }

         commit = p.getProperty(COMMIT);
         version = p.getProperty(VERSION);
      } catch (java.io.IOException e) {
         System.err.println("(!)IOException loading " + PROPERTIES + ":");
         e.printStackTrace();
      }
   }


   public String getVDDVersion() {
      return version;
   }

   public String getVDDCommit() {
      return commit;
   }
}
