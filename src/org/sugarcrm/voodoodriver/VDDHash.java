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

import java.util.HashMap;


/**
 * Generic VDD-specific data container.
 *
 * @author Trampus
 * @author Jon duSaint
 */

public class VDDHash extends HashMap<String, Object>{

   private static final long serialVersionUID = 1L;

   /**
    * Create a new, empty <code>VDDHash</code>.
    */

   public VDDHash() {
      super();
   }


   /**
    * Create a copy of an existing <code>VDDHash</code>.
    *
    * @param original  the <code>VDDHash</code> to copy.
    *                  <b>N.b. <u>not</u> a deep copy</b>
    */

   @SuppressWarnings("unchecked")
   public VDDHash(VDDHash original) {
      super((HashMap)original);
   }
}
