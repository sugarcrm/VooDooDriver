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


/**
 * VooDooDriver plugin specific exception.
 *
 * This class is used for all exceptions thrown by a plugin.
 * Unwrapping the underlying exception is left to the caller.
 *
 * @author Jon duSaint
 */

public class PluginException extends VDDException {


   /**
    * Construct a new exception with <code>null</code> as its detail message.
    */

   public PluginException() {
      super();
   }


   /**
    * Construct a new exception with the specified detail message.
    */

   public PluginException(String message) {
      super(message);
   }


   /**
    * Construct a new exception with the specified detail message and cause.
    */

   public PluginException(String message, Throwable cause) {
      super(message, cause);
   }
}
