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

package org.sugarcrm.voodoodriver.Event;

import org.sugarcrm.voodoodriver.VDDException;

/**
 * Class for miscellaneous VDD-specific exceptions.
 *
 * @author Jon duSaint
 */

public class UnknownEventException extends VDDException {

   /**
    * Construct a new exception with <code>null</code> as its detail message.
    */

   public UnknownEventException() {
      super();
   }


   /**
    * Construct a new exception with the specified detail message.
    *
    * @param message  the detail message
    */

   public UnknownEventException(String message) {
      super(message);
   }


   /**
    * Construct a new exception with the specified detail message and cause.
    *
    * @param message  the detail message
    * @param cause    the low-level cause of this exception or <code>null</code>
    */

   public UnknownEventException(String message, Throwable cause) {
      super(message, cause);
   }
}
