/*
 * Copyright 2011-2013 SugarCRM Inc.
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

package org.sugarcrm.vddlogger;

import java.util.HashMap;


/**
 * Collection of issues encountered during VDD run
 *
 * <p>There are three types of issues tracked here: errors,
 * exceptions, and warnings.</p>
 */

public class Issues {

   /**
    * Key value pairs of errors and number of appearances.
    */

   private HashMap<String,Integer> errors;

   /**
    * Key value pairs of exceptions and number of appearances.
    */

   private HashMap<String,Integer> exceptions;

   /**
    * Key value pairs of warnings and number of appearances.
    */

   private HashMap<String,Integer> warnings;


   /**
    * Instantiate an Issues object.
    */

   public Issues () {
      this.errors = new HashMap<String,Integer>();
      this.warnings = new HashMap<String,Integer>();
      this.exceptions = new HashMap<String,Integer>();
   }


   /**
    * Get the mapping of issues of the specified type
    *
    * <p>The type can be &quot;errors&quot;, &quot;exceptions&quot;,
    * or &quot;warnings&quot;.  <code>null</code> is returned if the
    * mapping is an unknown type.</p>
    *
    * @param type  issue type
    * @return the issue mapping
    */

   public HashMap<String,Integer> get(String type) {
      if (type.equals("errors")) {
         return this.errors;
      } else if (type.equals("exceptions")) {
         return this.exceptions;
      } else if (type.equals("warnings")) {
         return this.warnings;
      }

      return null;
   }


   /**
    * Add an issue to the appropriate mapping
    *
    * <p>This method adds the issue with a count of 1.</p>
    *
    * @param m  issue mapping
    * @param s  the issue
    */

   private void add(HashMap<String,Integer> m, String s) {
      add(m, s, 1);
   }


   /**
    * Add an issue to the appropriate mapping
    *
    * <p>This method allows the count to be specified.</p>
    *
    * @param m  issue mapping
    * @param s  the issue
    * @param c  issue count
    */

   private void add(HashMap<String,Integer> m, String s, int c) {
      if (m.containsKey(s)) {
         m.put(s, m.get(s) + c);
      } else {
         m.put(s, c);
      }
   }


   /**
    * Add an error to the issues mapping
    *
    * @param e  the error string
    */

   public void error(String e) {
      add(this.errors, e);
   }


   /**
    * Add an exception to the issues mapping
    *
    * @param e  the exception string
    */

   public void exception(String e) {
      add(this.exceptions, e);
   }


   /**
    * Add a warning to the issues mapping
    *
    * @param w  the warning string
    */

   public void warning(String w) {
      add(this.warnings, w);
   }


   /**
    * Append the mappings from an Issues object to this one
    *
    * @param issues  the issues object
    */

   public void append(Issues issues) {
      append(this.errors, issues.errors);
      append(this.exceptions, issues.exceptions);
      append(this.warnings, issues.warnings);
   }


   /**
    * Append the issues in the specified mapping
    *
    * @param dst  destination issues mapping
    * @param src  source issues mapping
    */

   private void append(HashMap<String,Integer> dst,
                       HashMap<String,Integer> src) {
      for (String key: src.keySet()) {
         add(dst, key, src.get(key));
      }
   }
}
