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

public enum SodaElements {
   WHITELIST,
   INPUT,
   BUTTON,
   LABEL,
   H3,
   NAME,
   SCRIPT,
   FILE_FIELD,
   UL,
   OL,
   H4,
   H5,
   TIMESTAMP,
   PRE,
   SELECT_LIST,
   H6,
   WAIT,
   ACTION,
   CHECKBOX,
   P,
   RADIO,
   TABLE,
   TD,
   JAVASCRIPT,
   ASSERT,
   EXCEPTION,
   AREA,
   DIV,
   FORM,
   FRAME,
   MAP,
   TEXTAREA,
   PASSWORD,
   TEXTFIELD,
   PUTS,
   CSV,
   LI,
   VAR,
   RUBY,
   LINK,
   TR,
   SELECT,
   BROWSER,
   IMAGE,
   H1,
   DIALOG,
   HIDDEN,
   SPAN,
   TEXT_FIELD,
   H2,
   FILEFIELD,
   ATTACH,
   STAMP,
   DND,
   EXECUTE,
   ARG,
   PLUGIN,
   PLUGINLOADER,
   JAVAPLUGIN,
   DELETE,
   SCREENSHOT,
   ALERT,
   PLUGINCONTROL,
   GLOBAL;

   static public boolean isMember(String aName) {
      boolean result = false;
      SodaElements[] values = SodaElements.values();

      for (SodaElements amethod : values) {
         if (amethod.name().equals(aName)) {
            result = true;
            break;
         }
      }

      return result;
   }
}



