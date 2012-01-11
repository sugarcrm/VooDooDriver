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

package voodoodriver;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

public class VDDVersionInfo {

   private final String propFile = "vdd.properties";
   private final String propName = "version";

   public VDDVersionInfo() {

   }

   public String getVDDVersion() {
      String result = "";
      InputStream stream = null;
      Properties prop = null;

      String className = this.getClass().getName().replace('.', '/');
      String classJar =  this.getClass().getResource("/" + className + ".class").toString();
      try {
         prop = new Properties();

         if (classJar.startsWith("jar:")) {
            stream = getClass().getResourceAsStream(this.propFile);
         } else {
            File fd = new File(getClass().getResource(this.propFile).getFile());
            stream = new FileInputStream(fd);
         }

         prop.load(stream);
         result = prop.getProperty(this.propName);
      } catch (Exception exp) {
         exp.printStackTrace();
         result = "undefined version(bad build)";
      }

      return result;
   }
}
