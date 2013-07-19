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


public enum Elements {
   /* VooDooDriver Events */
   ARG,
   ATTACH,
   ASSERT,
   BROWSER,
   CSV,
   DELETE,
   DND,
   EXECUTE,
   JAVAPLUGIN,
   JAVASCRIPT,
   PLUGINLOADER,
   PUTS,
   SAVEHTML,
   SCREENSHOT,
   SCRIPT,
   TIMESTAMP,
   VAR,
   WAIT,
   WHITELIST,

   /* HTML Events */
   ALERT,
   DIV,
   SPAN,
   H1,
   H2,
   H3,
   H4,
   H5,
   H6,
   P,
   PRE,
   UL,
   OL,
   LI,
   TABLE,
   THEAD,
   TBODY,
   TR,
   TH,
   TD,
   LINK,
   IMAGE,
   MAP,
   AREA,
   FRAME,
   FORM,
   INPUT,
   EMAIL,
   TEXTFIELD,
   PASSWORD,
   CHECKBOX,
   RADIO,
   BUTTON,
   FILEFIELD,
   HIDDEN,
   SELECT,
   SELECT_LIST,
   OPTION,
   TEXTAREA,
   LABEL,
   I,
   B,
   STRIKE,
   S,
   U,

   /* Events not found in Events.xml -- shouldn't these be deleted? */
   ACTION,
   DIALOG,
   EXCEPTION,
   FILE_FIELD,
   GLOBAL,
   NAME,
   PLUGIN,
   PLUGINCONTROL,
   RUBY,
   STAMP,
   TEXT_FIELD;

   static public boolean isMember(String aName) {
      boolean result = false;
      Elements[] values = Elements.values();

      for (Elements amethod: values) {
         if (amethod.name().equals(aName)) {
            result = true;
            break;
         }
      }

      return result;
   }
}
