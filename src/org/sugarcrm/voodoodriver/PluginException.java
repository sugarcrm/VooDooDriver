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

public class PluginException extends Exception {

   /**
    * Exception detail message.
    */

   private String message;

   /**
    * Exception cause.
    */

   private Throwable cause;

   /**
    * Instantiate this exception with only a message.
    *
    * @param message  the detail message
    */

   public PluginException(String message) {
      this.message = message;
   }


   /**
    * Instantiate this exception a message and an underlying exception.
    *
    * @param message  the detail message
    * @param cause    the original cause of this exception
    */

   public PluginException(String message, Throwable cause) {
      this.message = message;
      this.cause = cause;
   }
}
