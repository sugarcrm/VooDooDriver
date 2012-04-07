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

import java.util.ArrayList;
import org.w3c.dom.Element;


/**
 * VooDooDriver Event base class.
 *
 * @author Jon duSaint
 */

public abstract class Event {

   static {
      // Load Events.xml
      // XXX: Events.xml should contain event name => Event subclass xlat
   }

   /**
    * Factory method to create and return the appropriate Event subclass.
    *
    * @param element  the DOM Element from the test script for this event
    * @return appropriate Event subclass
    * @throws UnknownEventException if <code>eventName</code> does not
    *         correspond to a VDD event
    */

   public static Event createEvent(Element element)
      throws UnknownEventException {
      return new Event();  // XXX
   }


   /**
    * {@link ArrayList} of child events.
    */

   protected ArrayList<Event> children;


   /**
    * True if this Event has child Events.
    */

   public boolean hasChildren() {
      return this.children != null && children.size() > 0;
   }


   /**
    * Retrieve this Event's child Events.
    */

   public ArrayList<Event> getChildren() {
      return this.children;
   }


   /**
    * Execute this Event.
    *
    * @return whether execution was successful
    */

   public boolean execute() {
      return false;
   }


   /*
    * All attributes from Events.xml.  This table will determine how
    * subclasses are constructed.
    *
    * Selectors:
    *
    *    action          string   form
    *    alert           boolean  alert
    *    alt             string   image
    *    class           string   *
    *    css             string   *
    *    for             string   label
    *    href            string   link
    *    id              string   *
    *    index           integer  *
    *    index           string   [?bug]
    *    method          string   *
    *    name            string   *
    *    text            string   *
    *    title           string   attach,filefield
    *    value           string   * [missing from many]
    *    xpath           string   *
    *
    * Actions:
    *
    *    action          string   whitelist
    *    add             string   whitelist
    *    alert           boolean  link,image,button
    *    append          string   textfield,password,textarea
    *    assert          string   *
    *    assertnot       string   *
    *    assertPage      string   button
    *    assertPage      boolean  browser,link,checkbox
    *    assertselected  boolean  select
    *    checked         boolean  radio
    *    classname       string   javaplugin,pluginloader
    *    clear           boolean  textfield,password,select,textarea
    *    click           string   tr,td,select [bug]
    *    click           boolean  *
    *    condition       string   wait
    *    content         string   javasript,whitelist
    *    cssprop         string   *
    *    cssvalue        string   *
    *    csv             string   script
    *    default         boolean  textfield,password,[textarea?]
    *    delete          string   whitelist
    *    disabled        boolean  link,textfield,password,checkbox,radio,
    *                             button,select,select_list,textarea
    *    dst             string   dnd
    *    exist           boolean  browser,alert,div,table,link,form,textfield,
    *                             password,button
    *    exists          boolean  *
    *    file            string   csv,javascript,pluginloader,screenshot,script
    *    fileset         string   script
    *    included        string   select
    *    jscriptevent    string   *
    *    jswait          boolean  * [?]
    *    notincluded     string   select
    *    override        string   csv
    *    required        boolean  *
    *    save            string   *
    *    send_keys       string   browser
    *    set             string   *
    *    setreal         string   select
    *    src             string   dnd,image,frame,button[?]
    *    timeout         integer  *
    *    txt             string   txt
    *    unset           string   var
    *    url             string   attach,browser
    *    var             string   *
    *    vartext         string   span
    */
}
