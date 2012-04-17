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
import org.sugarcrm.voodoodriver.VDDHash;
import org.w3c.dom.Element;


/**
 * VooDooDriver Event base class.
 *
 * @author Jon duSaint
 */

public abstract class Event {

   /**
    * List of all allowed events and their attributes from Events.xml.
    */

   protected static VDDHash allowedEvents;


   /**
    * Load allowed events list from Events.xml.
    */

   static {
      EventLoader el = new EventLoader();

      try {
         el.loadEvents();
      } catch (org.sugarcrm.voodoodriver.VDDException e) {
         System.err.println("(!)Exception loading Events.xml: " + e);
         e.printStackTrace(System.err);
      }

      allowedEvents = el.getEvents();
   }


   /**
    * Enumeration of events
    *
    * Getting rid of big hardcoded lists like this is one of the aims
    * of the current (2012-04-16) VDD code re-orgs.  Once more code
    * elsewhere is in place, this will be revisited and this list will
    * be replaced by annotations in Events.xml.
    */

   private enum EventToClass {
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
      SCREENSHOT,
      SCRIPT,
      TIMESTAMP,
      VAR,
      WAIT,
      WHITELIST,
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
      TD,
      LINK,
      IMAGE,
      MAP,
      AREA,
      FRAME,
      FORM,
      INPUT,
      TEXTFIELD,
      PASSWORD,
      CHECKBOX,
      RADIO,
      BUTTON,
      FILEFIELD,
      HIDDEN,
      SELECT,
      SELECT_LIST,
      TEXTAREA,
      LABEL
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
      EventToClass eventClassName = null;
      Event event = new TestEvent(element); // XXX temporary

      try {
         eventClassName = Enum.valueOf(EventToClass.class,
                                       element.getTagName().toUpperCase());
      } catch (IllegalArgumentException e) {
         throw new UnknownEventException("Unknown Event name '" +
                                         element.getTagName() + "'");
      }

      /* Yuck. See above comment. */
      switch (eventClassName) {
      case ARG:
         break;
      case ATTACH:
         break;
      case ASSERT:
         break;
      case BROWSER:
         break;
      case CSV:
         break;
      case DELETE:
         break;
      case DND:
         break;
      case EXECUTE:
         break;
      case JAVAPLUGIN:
         break;
      case JAVASCRIPT:
         break;
      case PLUGINLOADER:
         break;
      case PUTS:
         break;
      case SCREENSHOT:
         break;
      case SCRIPT:
         break;
      case TIMESTAMP:
         break;
      case VAR:
         break;
      case WAIT:
         break;
      case WHITELIST:
         break;
      case ALERT:
         break;
      case DIV:
         break;
      case SPAN:
         break;
      case H1:
         break;
      case H2:
         break;
      case H3:
         break;
      case H4:
         break;
      case H5:
         break;
      case H6:
         break;
      case P:
         break;
      case PRE:
         break;
      case UL:
         break;
      case OL:
         break;
      case LI:
         break;
      case TABLE:
         break;
      case THEAD:
         break;
      case TBODY:
         break;
      case TR:
         break;
      case TD:
         break;
      case LINK:
         break;
      case IMAGE:
         break;
      case MAP:
         break;
      case AREA:
         break;
      case FRAME:
         break;
      case FORM:
         break;
      case INPUT:
         break;
      case TEXTFIELD:
         break;
      case PASSWORD:
         break;
      case CHECKBOX:
         break;
      case RADIO:
         break;
      case BUTTON:
         break;
      case FILEFIELD:
         break;
      case HIDDEN:
         break;
      case SELECT:
         break;
      case SELECT_LIST:
         break;
      case TEXTAREA:
         break;
      case LABEL:
         break;
      }

      return event;
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
    * Assign this Event's child Events.
    */

   public void setChildren(ArrayList<Event> children) {
      this.children = children;
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
    *
    */


}
