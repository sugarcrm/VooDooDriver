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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;

/**
 * Loads an external java class file into a java Class.
 *
 * @author trichmond
 *
 */
public class VDDClassLoader extends ClassLoader {

   public VDDClassLoader(ClassLoader parent) {
      super(parent);
   }

   /**
    * Loads a class by name from a java class file.
    *
    * @param className The name of the class to load.
    * @param classFile The java .class file to load into memory.
    * @return Class
    * @throws ClassNotFoundException
    */
   @SuppressWarnings("unchecked")
   public Class<VDDPluginInterface> loadClass(String className, String classFile) throws ClassNotFoundException {
      Class<VDDPluginInterface> result = null;
      byte[] classData;

      try {
         File classfd = new File(classFile);
         FileInputStream fin = new FileInputStream(classfd);
         ByteArrayOutputStream buffer = new ByteArrayOutputStream();
         int data = fin.read();

          while(data != -1){
              buffer.write(data);
              data = fin.read();
          }
          fin.close();

          classData = buffer.toByteArray();
          result = (Class<VDDPluginInterface>)defineClass(className, classData, 0, classData.length);

      } catch (Exception exp) {
         exp.printStackTrace();
         result = null;
      }

      return result;
   }

   @SuppressWarnings("unchecked")
   public Class<VDDPluginInterface> loadClass(String className, byte[] classData) {
      Class<VDDPluginInterface> result = null;

      try {
         result = (Class<VDDPluginInterface>)defineClass(className, classData, 0, classData.length);
      } catch (Exception exp) {
         exp.printStackTrace();
         result = null;
      }

      return result;
   }
}
