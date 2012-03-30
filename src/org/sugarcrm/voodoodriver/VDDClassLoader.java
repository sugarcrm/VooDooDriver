/*
 * Copyright 2011-2012 SugarCRM Inc.
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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;

/**
 * Load a VDD plugin class file.
 *
 * @author trichmond
 * @author Jon duSaint
 */

public class VDDClassLoader extends ClassLoader {

   /**
    * Load a class by name from the specified java class file.
    *
    * @param className  name of the class to load
    * @param classFile  Java class file that contains the class to load
    * @return the plugin {@link Class}
    * @throws java.io.FileNotFoundException
    * @throws java.io.IOException
    */

   public Class<PluginInterface> loadClass(String className, String classFile)
      throws java.io.FileNotFoundException, java.io.IOException {
      File classfd = new File(classFile);
      FileInputStream fin = new FileInputStream(classfd);
      ByteArrayOutputStream buffer = new ByteArrayOutputStream();

      int data = fin.read();
      while (data != -1) {
         buffer.write(data);
         data = fin.read();
      }
      fin.close();

      return this.loadClass(className, buffer.toByteArray());
   }


   /**
    * Load the named class from the provided byte array.
    *
    * @param className  name of the class to load
    * @param classData  byte array containing the compiled class
    * @return the plugin {@link Class}
    */

   public Class<PluginInterface> loadClass(String className, byte[] classData) {
      @SuppressWarnings("unchecked")
         Class<PluginInterface> cls =
         (Class<PluginInterface>)defineClass(className, classData,
                                             0, classData.length);

      return cls;
   }
}
