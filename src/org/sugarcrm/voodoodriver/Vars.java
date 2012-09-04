/*
 * Copyright 2012 SugarCRM Inc.
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

import java.util.HashMap;
import java.util.LinkedList;


/**
 * Implement VooDoo user variables.
 *
 * <p>VooDoo user variables are named locations that users can use to
 * save data for later.  Each variable exists within a context, and
 * there can exist more than one variable context.</p>
 *
 * <p>When a variable look-up is performed, the most recent variable
 * context is searched first.  If not found, the search continues
 * through the next most recent context and on until the bottom-most
 * context is reached.</p>
 *
 * <p>Operations on variables and the VooDooDriver events they
 * correspond to are:
 *
 * <dl>
 *   <dt>creation</dt><dd>var, csv, timestamp</dd>
 *   <dt>access</dt>  <dd>any event via text substitution</dd>
 *   <dt>deletion</dt><dd>delete</dd>
 * </dl>
 *
 * <p>New contexts are created for each child event and during the
 * execution of the csv event.  These contexts are automatically
 * destroyed at the end of the event.</p>
 *
 * <p>Variables can be used only to store Strings.</p>
 *
 * @author Jonathan duSaint
 */

public class Vars {

   /**
    * The stack of variable contexts.
    */

   private LinkedList<HashMap<String,String>> context;


   /**
    * Create a Vars object with a single context.
    */

   public Vars() {
      this.context = new LinkedList<HashMap<String,String>>();
      this.pushContext();
   }


   /**
    * Create a Vars object out of an existing Vars object.
    *
    * All the contexts in the source Vars object will be flattened
    * into one context.
    *
    * @param v  Vars object to copy
    */

   public Vars(Vars v) {
      this.context = new LinkedList<HashMap<String,String>>();
      this.pushContext();

      for (HashMap<String,String> h: v.context) {
         for (String key: h.keySet()) {
            this.put(key, h.get(key));
         }
      }
   }


   /**
    * Assign a value to a variable.
    *
    * @param var    the name of the variable
    * @param value  the value to assign to the variable
    */

   public void put(String var, String value) {
      HashMap<String,String> context = this.context.peek();

      for (HashMap<String,String> search: this.context) {
         if (search.containsKey(var)) {
            context = search;
            break;
         }
      }

      context.put(var, value);
   }


   /**
    * Get the value of the specified variable.
    *
    * @param var  the name of the variable
    * @return the value of the variable or an empty string
    * @throws NoSuchFieldException if the variable does not exist
    */

   public String get(String var) throws NoSuchFieldException {
      for (HashMap<String,String> search: this.context) {
         if (search.containsKey(var)) {
            return search.get(var);
         }
      }

      throw new NoSuchFieldException("Var '" + var + "' does not exist");
   }


   /**
    * Remove a variable from the top-most context.
    *
    * @param var  the name of the variable
    */

   public void remove(String var) {
      for (HashMap<String,String> search: this.context) {
         if (search.containsKey(var)) {
            search.remove(var);
            return;
         }
      }
   }


   /**
    * Create a new context.
    */

   public void pushContext() {
      HashMap<String,String> newContext = new HashMap<String,String>();
      this.context.push(newContext);
   }


   /**
    * Remove the top-most context.
    */

   public void popContext() {
      assert this.context.size() >= 1;
      this.context.pop();
   }

}
