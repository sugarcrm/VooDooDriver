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
import org.openqa.selenium.WebElement;
import org.sugarcrm.voodoodriver.EventLoop;
import org.sugarcrm.voodoodriver.PluginEvent;
import org.sugarcrm.voodoodriver.VDDException;
import org.sugarcrm.voodoodriver.VDDHash;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


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
    * Parent HTML element of the element used by this event.
    */

   protected WebElement parent;


   /**
    * The WebElement corresponding to this event.
    *
    * VDD-type events have no element, and not all HTML events will,
    * either.  In those cases, element will be null.
    */

   protected WebElement element;


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
         event = new Arg(element);
      } else if (tagName.equals("attach")) {
         event = new Attach(element);
      } else if (tagName.equals("browser")) {
         event = new Browser(element);
      } else if (tagName.equals("csv")) {
         event = new CSV(element);
      } else if (tagName.equals("delete")) {
         event = new Delete(element);
      } else if (tagName.equals("dnd")) {
         event = new DnD(element);
      } else if (tagName.equals("execute")) {
         event = new Execute(element);
      } else if (tagName.equals("javaplugin")) {
         event = new Javaplugin(element);
      } else if (tagName.equals("javascript")) {
         event = new Javascript(element);
      } else if (tagName.equals("pluginloader")) {
         event = new Pluginloader(element);
      } else if (tagName.equals("puts")) {
         event = new Puts(element);
      } else if (tagName.equals("screenshot")) {
         event = new Screenshot(element);
      } else if (tagName.equals("script")) {
         event = new Script(element);
      } else if (tagName.equals("timestamp")) {
         event = new Timestamp(element);
      } else if (tagName.equals("var")) {
         event = new Var(element);
      } else if (tagName.equals("wait")) {
         event = new Wait(element);
      } else if (tagName.equals("whitelist")) {
         event = new Whitelist(element);
      } else if (tagName.equals("alert")) {
         event = new Alert(element);
      } else if (tagName.equals("div")) {
         event = new SimpleHtmlEvent(element);
      } else if (tagName.equals("span")) {
         event = new SimpleHtmlEvent(element);
      } else if (tagName.equals("h1")) {
         event = new SimpleHtmlEvent(element);
      } else if (tagName.equals("h2")) {
         event = new SimpleHtmlEvent(element);
      } else if (tagName.equals("h3")) {
         event = new SimpleHtmlEvent(element);
      } else if (tagName.equals("h4")) {
         event = new SimpleHtmlEvent(element);
      } else if (tagName.equals("h5")) {
         event = new SimpleHtmlEvent(element);
      } else if (tagName.equals("h6")) {
         event = new SimpleHtmlEvent(element);
      } else if (tagName.equals("address")) {
         event = new SimpleHtmlEvent(element);
      } else if (tagName.equals("em")) {
         event = new SimpleHtmlEvent(element);
      } else if (tagName.equals("strong")) {
         event = new SimpleHtmlEvent(element);
      } else if (tagName.equals("dfn")) {
         event = new SimpleHtmlEvent(element);
      } else if (tagName.equals("code")) {
         event = new SimpleHtmlEvent(element);
      } else if (tagName.equals("samp")) {
         event = new SimpleHtmlEvent(element);
      } else if (tagName.equals("kbd")) {
         event = new SimpleHtmlEvent(element);
      } else if (tagName.equals("cite")) {
         event = new SimpleHtmlEvent(element);
      } else if (tagName.equals("abbr")) {
         event = new SimpleHtmlEvent(element);
      } else if (tagName.equals("acronym")) {
         event = new SimpleHtmlEvent(element);
      } else if (tagName.equals("blockquote")) {
         event = new SimpleHtmlEvent(element);
      } else if (tagName.equals("q")) {
         event = new SimpleHtmlEvent(element);
      } else if (tagName.equals("sub")) {
         event = new SimpleHtmlEvent(element);
      } else if (tagName.equals("sup")) {
         event = new SimpleHtmlEvent(element);
      } else if (tagName.equals("p")) {
         event = new SimpleHtmlEvent(element);
      } else if (tagName.equals("br")) {
         event = new SimpleHtmlEvent(element);
      } else if (tagName.equals("pre")) {
         event = new SimpleHtmlEvent(element);
      } else if (tagName.equals("ins")) {
         event = new SimpleHtmlEvent(element);
      } else if (tagName.equals("del")) {
         event = new SimpleHtmlEvent(element);
      } else if (tagName.equals("ul")) {
         event = new SimpleHtmlEvent(element);
      } else if (tagName.equals("ol")) {
         event = new SimpleHtmlEvent(element);
      } else if (tagName.equals("li")) {
         event = new SimpleHtmlEvent(element);
      } else if (tagName.equals("dl")) {
         event = new SimpleHtmlEvent(element);
      } else if (tagName.equals("dt")) {
         event = new SimpleHtmlEvent(element);
      } else if (tagName.equals("dd")) {
         event = new SimpleHtmlEvent(element);
      } else if (tagName.equals("table")) {
         event = new SimpleHtmlEvent(element);
      } else if (tagName.equals("caption")) {
         event = new SimpleHtmlEvent(element);
      } else if (tagName.equals("thead")) {
         event = new SimpleHtmlEvent(element);
      } else if (tagName.equals("tfoot")) {
         event = new SimpleHtmlEvent(element);
      } else if (tagName.equals("tbody")) {
         event = new SimpleHtmlEvent(element);
      } else if (tagName.equals("colgroup")) {
         event = new SimpleHtmlEvent(element);
      } else if (tagName.equals("col")) {
         event = new SimpleHtmlEvent(element);
      } else if (tagName.equals("tr")) {
         event = new SimpleHtmlEvent(element);
      } else if (tagName.equals("th")) {
         event = new SimpleHtmlEvent(element);
      } else if (tagName.equals("td")) {
         event = new SimpleHtmlEvent(element);
      } else if (tagName.equals("link")) {
         event = new Link(element);
      } else if (tagName.equals("image")) {
         event = new SimpleHtmlEvent(element);
      } else if (tagName.equals("object")) {
         event = new SimpleHtmlEvent(element);
      } else if (tagName.equals("map")) {
         event = new SimpleHtmlEvent(element);
      } else if (tagName.equals("area")) {
         event = new SimpleHtmlEvent(element);
      } else if (tagName.equals("tt")) {
         event = new SimpleHtmlEvent(element);
      } else if (tagName.equals("i")) {
         event = new SimpleHtmlEvent(element);
      } else if (tagName.equals("b")) {
         event = new SimpleHtmlEvent(element);
      } else if (tagName.equals("big")) {
         event = new SimpleHtmlEvent(element);
      } else if (tagName.equals("small")) {
         event = new SimpleHtmlEvent(element);
      } else if (tagName.equals("strike")) {
         event = new SimpleHtmlEvent(element);
      } else if (tagName.equals("s")) {
         event = new SimpleHtmlEvent(element);
      } else if (tagName.equals("u")) {
         event = new SimpleHtmlEvent(element);
      } else if (tagName.equals("hr")) {
         event = new SimpleHtmlEvent(element);
      } else if (tagName.equals("frame")) {
         event = new Iframe(element);
      } else if (tagName.equals("form")) {
         event = new SimpleHtmlEvent(element);
      } else if (tagName.equals("input")) {
         event = new InteractiveHtmlEvent(element);
      } else if (tagName.equals("email")) {
         event = new InteractiveHtmlEvent(element);
      } else if (tagName.equals("textfield")) {
      } else if (tagName.equals("password")) {
      } else if (tagName.equals("checkbox")) {
      } else if (tagName.equals("radio")) {
      } else if (tagName.equals("button")) {
      } else if (tagName.equals("filefield")) {
      } else if (tagName.equals("hidden")) {
      } else if (tagName.equals("select")) {
      } else if (tagName.equals("select_list")) {
      } else if (tagName.equals("option")) {
      } else if (tagName.equals("textarea")) {
      } else if (tagName.equals("label")) {
      } else {
         throw new UnknownEventException("Unknown Event name '" +
                                         element.getTagName() + "'");
      }

      return event;
   }


   /**
    * Log a message.
    *
    * <p>This is a convenience wrapper around <code>Reporter.log</code>.</p>
    *
    * @param msg  message to log
    */

   protected void log(String msg) {
      this.eventLoop.report.log(msg);
   }


   /**
    * Log a warning.
    *
    * <p>This is a convenience wrapper around
    * <code>Reporter.warning</code>.</p>
    *
    * @param msg  warning to log
    */

   protected void warning(String msg) {
      this.eventLoop.report.warning(msg);
   }


   /**
    * Log an error.
    *
    * <p>This is a convenience wrapper around <code>Reporter.error</code>.</p>
    *
    * @param msg  error to log
    */

   protected void error(String msg) {
      this.eventLoop.report.error(msg);
   }


   /**
    * Log an exception.
    *
    * <p>This is a convenience wrapper around
    * <code>Reporter.exception</code>.</p>
    *
    * @param e  exception to log
    */

   protected void exception(Throwable e) {
      this.eventLoop.report.exception(e);
   }


   /**
    * Log an exception with an error message.
    *
    * <p>This is a convenience wrapper around
    * <code>Reporter.exception</code>.</p>
    *
    * @param msg  error to log with the exception
    * @param e  exception to log
    */

   protected void exception(String msg, Throwable e) {
      this.eventLoop.report.exception(msg, e);
   }


   /**
    * Return whether an element is required.
    *
    * <p>Only useful for HTML events</p>
    *
    * @return true
    */

   public boolean required() {
      return true;
   }


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
    * Determine whether a selector or action is a boolean value.
    *
    * @param event  name of this event
    * @param acc    name of the accessor
    */

   private boolean isBoolean(String event, String acc) {
      VDDHash thisEvent = (VDDHash)allowedEvents.get(event);
      String[] attrs = {"selectors", "actions"};
      for (String attr: attrs) {
         VDDHash a = (VDDHash)thisEvent.get(attr);
         if (a != null && a.containsKey(acc)) {
            Object t = a.get(acc);
            if (t instanceof String) {
               String type = (String)a.get(acc);
               return type.equals("boolean");
            }
         }
      }

      return false;
   }


   /**
    * Convert the event from the test file into event actions.
    *
    * @param tev  the event as specified in the test script
    * @throws VDDException if event processing fails
    */

   private void compileEvent(Element tev) throws VDDException {
      String nodeName = tev.getNodeName().toLowerCase();

      if (!this.allowedEvents.containsKey(nodeName)) {
         throw new VDDException("Unknown event name '" +
                                tev.getNodeName() + "'");
      }

      /* Process attributes */
      for (int k = 0; k < tev.getAttributes().getLength(); k++) {
         Node attr = tev.getAttributes().item(k);
         String name = attr.getNodeName().toLowerCase();
         Object value = null;

         if (isBoolean(nodeName, name)) {
            value = toBoolean(attr.getNodeValue());
         } else {
            value = attr.getNodeValue();
         }

         if (isselector(nodeName, name)) {
            selectors.put(name, value);
         } else if (isaction(nodeName, name)) {
            actions.put(name, value);
         } else {
            throw new VDDException("Invalid attribute '" + name + "' " +
                                   "for '" + tev.getNodeName() + "' event");
         }
      }

      /* HTML tag/type for HTML events */
      VDDHash e = (VDDHash)this.allowedEvents.get(nodeName);
      String htmlAttrs[] = {"html_tag", "html_type"};
      for (String attr: htmlAttrs) {
         if (e.containsKey(attr)) {
            selectors.put(attr, e.get(attr));
         }
      }

      /* Process any "arg" children. */
      if (nodeName.equals("execute") || nodeName.equals("javaplugin")) {
         NodeList children = tev.getChildNodes();
         if (children.getLength() > 0) {
            actions.put("args", processArgs(children));
         }
      }

      /* Process text content of certain events. */
      if (nodeName.equals("javascript") || nodeName.equals("whitelist")) {
         String txt = tev.getTextContent();
         if (!txt.isEmpty()) {
            actions.put("content", txt);
         }
      }
   }


   /**
    * Create an array of arg children.
    *
    * @param children  child nodes, possibly arg, of the current event
    * @return array of arg children
    */

   private String[] processArgs(NodeList children) {
      ArrayList<String> args = new ArrayList<String>();

      for (int n = 0; n < children.getLength(); n++) {
         if (children.item(n).getNodeName().toLowerCase().equals("arg")) {
            args.add(children.item(n).getTextContent());
         }
      }

      return args.toArray(new String[0]);
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
      this.element = null;

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
    * Assign this Event's parent WebElement.
    *
    * @param parent  the parent {@link WebElement}
    */

   public void setParent(WebElement parent) {
      this.parent = parent;
   }


   /**
    * Retreive this Event's event name.
    *
    * @return the event name
    */

   public String getName() {
      return this.testEvent.getNodeName();
   }


   /**
    * Retreive this Event's WebElement.
    *
    * @return this Event's WebElement or null
    */

   public WebElement getElement() {
      return this.element;
   }


   /**
    * Execute this Event.
    *
    * @throws StopEventException if child event execution is to be skipped
    * @throws VDDException if event execution is unsuccessful
    */

   public abstract void execute() throws StopEventException, VDDException;


   /**
    * Method to execute after all children have finished.
    *
    * This is used by EventLoop for those events with children.
    *
    * @throws VDDException if an error occurs
    */

   public void afterChildren() throws VDDException {}


   /**
    * Execute a VDD plugin at the appropriate time.
    *
    * @param ev  the current plugin event
    */

   protected void firePlugin(PluginEvent ev) {
      this.eventLoop.firePlugin(this, ev);
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
         } else {
            try {
               String value = this.eventLoop.vars.get(tmp);
               result = result.replace(m, value);
            } catch (NoSuchFieldException e) {
            }
         }
      }

      result = result.replaceAll("\\\\n", "\n");

      return result;
   }


   /**
    * Convert a string into a boolean.
    *
    * If the string is 'true' or 'false', it is converted to the
    * corresponding boolean.  If the string is '0', it is converted to
    * false; if it is a non-zero integer, it is converted to true.  If
    * it doesn't fall into any of those categories, it is converted to
    * false.
    *
    * @param s  the string to convert
    * @return boolean representation of that string
    */

   protected boolean toBoolean(String s) {
      if (s == null) {
         return false;
      }

      try {
         return Integer.valueOf(s) != 0;
      } catch (NumberFormatException e) {}

      return Boolean.valueOf(s);
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
    *    method          string   *
    *    name            string   *
    *    text            string   link
    *    title           string   attach,filefield
    *    value           string   button,input
    *    xpath           string   *
    *
    * Actions used during findElement:
    *    required        boolean  *
    *    exists          boolean  *
    *    timeout         integer  *
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
    *    file            string   csv,javascript,pluginloader,screenshot,script
    *    fileset         string   script
    *    included        string   select
    *    jscriptevent    string   *
    *    notincluded     string   select
    *    override        string   csv
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
