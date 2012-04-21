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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.sugarcrm.voodoodriver.EventLoop;
import org.sugarcrm.voodoodriver.VDDException;
import org.sugarcrm.voodoodriver.VDDHash;
import org.w3c.dom.Element;
import org.w3c.dom.Node;


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
    * The event from the test file.
    */

   protected Element testEvent;


   /**
    * Event selector attributes from the test script.
    */

   protected VDDHash selectors;

   /**
    * Event action attributes from the test script.
    */

   protected VDDHash actions;

   /**
    * The {@link EventLoop} running this Event.
    */

   protected EventLoop eventLoop;


   /**
    * Factory method to create and return the appropriate Event subclass.
    *
    * @param element  the DOM Element from the test script for this event
    * @return appropriate Event subclass
    * @throws UnknownEventException if <code>eventName</code> does not
    *         correspond to a VDD event
    * @throws VDDException if event instantiation fails
    */

   public static Event createEvent(Element element)
      throws UnknownEventException, VDDException {
      Event event = new TestEvent(element); // XXX temporary
      String tagName = element.getTagName().toLowerCase();

      if (tagName.equals("arg")) {
      } else if (tagName.equals("attach")) {
      } else if (tagName.equals("assert")) {
      } else if (tagName.equals("browser")) {
      } else if (tagName.equals("csv")) {
      } else if (tagName.equals("delete")) {
      } else if (tagName.equals("dnd")) {
      } else if (tagName.equals("execute")) {
      } else if (tagName.equals("javaplugin")) {
      } else if (tagName.equals("javascript")) {
      } else if (tagName.equals("pluginloader")) {
      } else if (tagName.equals("puts")) {
         event = new Puts(element);
      } else if (tagName.equals("screenshot")) {
      } else if (tagName.equals("script")) {
      } else if (tagName.equals("timestamp")) {
      } else if (tagName.equals("var")) {
      } else if (tagName.equals("wait")) {
      } else if (tagName.equals("whitelist")) {
      } else if (tagName.equals("alert")) {
      } else if (tagName.equals("div")) {
      } else if (tagName.equals("span")) {
      } else if (tagName.equals("h1")) {
      } else if (tagName.equals("h2")) {
      } else if (tagName.equals("h3")) {
      } else if (tagName.equals("h4")) {
      } else if (tagName.equals("h5")) {
      } else if (tagName.equals("h6")) {
      } else if (tagName.equals("p")) {
      } else if (tagName.equals("pre")) {
      } else if (tagName.equals("ul")) {
      } else if (tagName.equals("ol")) {
      } else if (tagName.equals("li")) {
      } else if (tagName.equals("table")) {
      } else if (tagName.equals("thead")) {
      } else if (tagName.equals("tbody")) {
      } else if (tagName.equals("tr")) {
      } else if (tagName.equals("td")) {
      } else if (tagName.equals("link")) {
      } else if (tagName.equals("image")) {
      } else if (tagName.equals("map")) {
      } else if (tagName.equals("area")) {
      } else if (tagName.equals("frame")) {
      } else if (tagName.equals("form")) {
      } else if (tagName.equals("input")) {
      } else if (tagName.equals("textfield")) {
      } else if (tagName.equals("password")) {
      } else if (tagName.equals("checkbox")) {
      } else if (tagName.equals("radio")) {
      } else if (tagName.equals("button")) {
      } else if (tagName.equals("filefield")) {
      } else if (tagName.equals("hidden")) {
      } else if (tagName.equals("select")) {
      } else if (tagName.equals("select_list")) {
      } else if (tagName.equals("textarea")) {
      } else if (tagName.equals("label")) {
      } else {
         throw new UnknownEventException("Unknown Event name '" +
                                         element.getTagName() + "'");
      }

      return event;
   }

   /////////////////////////////////////////////////////
   // XXX: This code will be used during element search:
   // /* Fill out HTML tag/type information, if applicable. */
   // VDDHash type = getSodaElement((Elements)map.get("type"));
   // String htmlAttrs[] = {"html_tag", "html_type"};
   // for (String attr: htmlAttrs) {
   //    if (type.get(attr) != null) {
   //       map.put(attr, type.get(attr));
   //    }
   // }
   /////////////////////////////////////////////////////

   /**
    * Determine whether this attribute is valid for this event.
    *
    * @param type   type of attribute: "selectors" or "actions"
    * @param event  current event name
    * @param attr   attribute to check
    * @return true if the attribute is valid
    */

   private boolean checkForAttribute(String type, String event, String attr) {
      VDDHash thisEvent = (VDDHash)allowedEvents.get(event);
      VDDHash thisSelector = (VDDHash)thisEvent.get(type);
      return thisSelector != null && thisSelector.containsKey(attr);
   }


   /**
    * Determine whether this attribute is a valid selector.
    *
    * Selectors are attributes used to find elements on an HTML page.
    *
    * @param event  name of this event
    * @param attr  the attribute
    * @return true if the attribute is a selector
    */

   private boolean isselector(String event, String attr) {
      return checkForAttribute("selectors", event, attr);
   }


   /**
    * Determine whether this attribute is a valid action.
    *
    * Action attributes are those that specify what to do with an HTML
    * element once found, or, for VooDoo events, just specify what to
    * do.
    *
    * @param event  name of this event
    * @param attr  the attribute
    * @return true if the attribute is an action
    */

   private boolean isaction(String event, String attr) {
      return checkForAttribute("actions", event, attr);
   }


   /**
    * Convert the event from the test file into event actions.
    *
    * @param tev  the event as specified in the test script
    * @throws VDDException if event processing fails
    */

   private void compileEvent(Element tev) throws VDDException {
      String nodeName = tev.getNodeName();

      /* Process attributes */
      for (int k = 0; k < tev.getAttributes().getLength(); k++) {
         Node attr = tev.getAttributes().item(k);
         String name = attr.getNodeName();
         String value = attr.getNodeValue();

         if (isselector(nodeName, name)) {
            selectors.put(name, value);
         } else if (isaction(nodeName, name)) {
            actions.put(name, value);
         } else {
            throw new VDDException("Invalid attribute '" + name + "' " +
                                   "for '" + tev.getNodeName() + "' event");
         }
      }

      /////////////////////////////////////////////////////////////////
      // ***** Events that need special handling  *****
      //
      // This code (originally from TestLoader.java) will go into
      // individual subclasses.
      //
      // if (name.contains("javascript") || name.contains("whitelist")) {
      //    String tmp = child.getTextContent();
      //    if (!tmp.isEmpty()) {
      //       data.put("content", tmp);
      //    }
      // }
      //
      // Check for "arg" children -- execute and javaplugin can have them
      //
      //String[] list = processArgs(child.getChildNodes());
      //data.put("args", list);
      //
      // /**
      //  * Process the argument list for &lt;execute&gt;
      //  *  and &lt;javaplugin&gt; event Nodes.
      //  *
      //  * @param nodes  the argument list as an XML NodeList
      //  * @return String array of those arguments
      //  */
      //
      // private String[] processArgs(NodeList nodes) {
      //    int len = nodes.getLength() -1;
      //    String[] list;
      //    int arg_count = 0;
      //    int current = 0;
      //
      //    for (int i = 0; i <= len; i++) {
      //       String name = nodes.item(i).getNodeName();
      //       if (name.contains("arg")) {
      //          arg_count += 1;
      //       }
      //    }
      //
      //    list = new String[arg_count];
      //
      //    for (int i = 0; i <= len; i++) {
      //       String name = nodes.item(i).getNodeName();
      //       if (name.contains("arg")) {
      //          String value = nodes.item(i).getTextContent();
      //          list[current] = value;
      //          current += 1;
      //       }
      //    }
      //
      //    return list;
      // }
      //
      /////////////////////////////////////////////////////////////////
   }

   /**
    * This constructor, which will never be called, is needed to extend Event.
    */

   protected Event() {}


   /**
    * Constructor for (almost) all Event subclasses.
    *
    * @param testEvent  the event as specified in the test script
    * @throws VDDException if event instantiation fails
    */

   protected Event(Element testEvent) throws VDDException {
      this.testEvent = testEvent;
      this.selectors = new VDDHash();
      this.actions = new VDDHash();

      compileEvent(testEvent);
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
    * Assign this Event's parent EventLoop.
    *
    * @param el  the {@link EventLoop} running this event
    */

   public void setEventLoop(EventLoop el) {
      this.eventLoop = el;
   }


   /**
    * Execute this Event.
    *
    * @return whether execution was successful
    */

   public boolean execute() {
      return false;
   }


   /**
    * Perform VDD string substitution.
    *
    * @param str  the raw string
    * @return the input string with all sequences replaces appropriately
    */

   protected String replaceString(String str) {
      String result = str;

      Pattern patt = Pattern.compile("\\{@[\\w\\.]+\\}",
                                     Pattern.CASE_INSENSITIVE);
      Matcher matcher = patt.matcher(str);

      while (matcher.find()) {
         String m = matcher.group();
         String tmp = m;
         tmp = tmp.replace("{@", "");
         tmp = tmp.replace("}", "");

         if (this.eventLoop.hijacks.containsKey(tmp)) {
            String value = this.eventLoop.hijacks.get(tmp).toString();
            result = result.replace(m, value);
         } else if (this.eventLoop.sodaVars.containsKey(tmp)) {
            String value = this.eventLoop.sodaVars.get(tmp).toString();
            result = result.replace(m, value);
         }
      }

      result = result.replaceAll("\\\\n", "\n");

      return result;
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
