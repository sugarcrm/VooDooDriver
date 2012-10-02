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

/**
 * Enumeration of all the reasons to execute a plugin.
 */

public enum PluginEvent {
   /** Execute plugin prior to a click. */
   BEFORECLICK,
   /** Execute plugin after a click. */
   AFTERCLICK,
   /** Execute plugin after setting a var. */
   AFTERSET,
   /** Execute plugin after an element is found. */
   AFTERFOUND,
   /** Execute plugin after an alert is closed. */
   AFTERDIALOGCLOSED,
   /** Execute plugin at every available opportunity. */
   ALWAYSFIRE,
   /** Execute plugin after each event finished. */
   AFTEREVENT,
   /** Execute plugin before started each test. */
   BEFORETEST,
   /** Execute plugin at the completion of each test. */
   AFTERTEST;
}
