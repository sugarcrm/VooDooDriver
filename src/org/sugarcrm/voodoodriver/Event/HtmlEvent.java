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

import java.util.LinkedList;
import java.util.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.sugarcrm.voodoodriver.PluginEvent;
import org.sugarcrm.voodoodriver.VDDException;
import org.w3c.dom.Element;

/**
 * The HTML event.
 *
 * This event is the parent of all HTML events used by VDD (e.g. link,
 * div, etc.).  Most event functionality is generic enough that it can
 * be handled here.
 *
 * @author Jon duSaint
 */

class HtmlEvent extends Event {


   /**
    * Actions that can be done to elements during event execution.
    */

   interface Action {

      /**
       * Run the associated action.
       */

      public void action();
   }


   /**
    * The assert and assertnot actions, depending on the polarity setting.
    */

   protected class AssertAction implements Action {

      /**
       * Assertion polarity.
       */

      private boolean polarity;


      /**
       * Instantiate an AssertAction object.
       *
       * @param polarity  true means assert, false means assertnot
       */

      public AssertAction(boolean polarity) {
         this.polarity = polarity;
      }

      /**
       * Run the assert or assertnot action.
       */

      public void action() {
      }
   }


   /**
    * The click action.
    */

   protected class ClickAction implements Action {

      /**
       * Run the click action.
       */

      public void action() {
      }
   }


   /**
    * The cssprop and cssvalue action.  These actions are slightly
    * unusual in that they come in pairs.  The cssprop action
    * specifies a css property name, and the cssvalue action then
    * specifies the value that property should be set to.
    */

   protected class CssAction implements Action {

      /**
       * Run the cssprop/cssvalue action pair.
       */

      public void action() {
      }
   }


   /**
    * The jscriptevent action.
    */

   protected class JsAction implements Action {

      /**
       * Run the jscriptevent action.
       */

      public void action() {
      }
   }


   /**
    * The var action.  This assigns the current element to an internal
    * variable for later use.
    */

   protected class VarAction implements Action {

      /**
       * Run the var action.
       */

      public void action() {
      }
   }


   /**
    * An action-name,action pair.  In other languages this would be a
    * typed tuple.
    */

   protected class Pair<String,Action> {

      /**
       * The action name half of the pair.
       */

      private String s;

      /**
       * The action half of the pair.
       */

      private Action a;


      /**
       * Instantiate an action-name,action pair.
       *
       * @param s  the action name
       * @param a  the action
       */

      Pair(String s, Action a) {
         this.s = s;
         this.a = a;
      }


      /**
       * Retrieve the action name.
       */

      public String string() {
         return this.s;
      }


      /**
       * Retreive the action.
       */

      public Action action() {
         return this.a;
      }
   }


   /**
    * A list of actions.  This is a thin wrapper around {@link
    * LinkedList} that adds functionality to insert items before or
    * after named entries.
    */

   protected class ActionList extends LinkedList<Pair> {

      /**
       * The Collections-mandated no args constructor.
       */

      public ActionList() {
         super();
      }


      /**
       * The Collections-mandated Collection arg constructor.
       */

      public ActionList(ActionList list) {
         super(list);
      }


      /**
       * Perform the by-name insertion.
       *
       * The insertion will be done either before or after the
       * specified element, depending on the value of incr.
       *
       * @param element  the action pair to insert
       * @param which    the element before or after which to insert
       * @param incr     0 means insert before, 1 means insert after
       * @throws NoSuchElementException if the specified element doesn't exist
       */

      private void doInsert(Pair element, String which, int incr)
         throws NoSuchElementException {
         assert incr == 0 || incr == 1;
         for (int k = 0; k < this.size(); k++) {
            if (this.get(k).string().equals(which)) {
               this.add(k + incr, element);
               return;
            }
         }
         throw new NoSuchElementException("'" + which + "' action not in list");
      }


      /**
       * Insert a new action pair after the specified element.
       *
       * @param element  the action pair to insert
       * @param which    the element before or after which to insert
       * @throws NoSuchElementException if the specified element doesn't exist
       */

      public void insertAfter(Pair element, String which)
         throws NoSuchElementException {
         doInsert(element, which, 1);
      }


      /**
       * Insert a new action pair before the specified element.
       *
       * @param element  the action pair to insert
       * @param which    the element before or after which to insert
       * @throws NoSuchElementException if the specified element doesn't exist
       */

      public void insertBefore(Pair element, String which)
         throws NoSuchElementException {
         doInsert(element, which, 0);
      }
   }


   /**
    * Ordered list of actions 
    *
    * Actions need to be performed by all events in a well-defined
    * sequence.  This list is a mapping of action_name => action
    * pairs.
    *
    * The parent class, HtmlEvent creates this list and populates it
    * with the few universal actions.  Child classes then insert their
    * own action_name => action pairs in the appropriate place.
    *
    * During event execution, actions specified by the event in the
    * test script are used to iterate through the list and run
    * action_code for each specified action.
    */

   protected ActionList actions;


   /**
    * Instantiate an HTML event.
    *
    * @param event  the event as specified in the test script
    * @throws VDDException if event instantiation fails
    */

   public HtmlEvent(Element e) throws VDDException {
      super(e);

      /*
       * Add action pairs into action list in order.  Only the actions
       * that all events use are added here, although action classes
       * for most will be nested classes of this class to facilitate
       * sharing.
       */

      this.actions = new ActionList();
      this.actions.addLast(new Pair<String,Action>("var", new VarAction()));
      this.actions.addLast(new Pair<String,Action>("assert",
                                                   new AssertAction(true)));
      this.actions.addLast(new Pair<String,Action>("assertnot",
                                                   new AssertAction(false)));
      this.actions.addLast(new Pair<String,Action>("cssprop", new CssAction()));
      this.actions.addLast(new Pair<String,Action>("jscriptevent",
                                                   new JsAction()));
      this.actions.addLast(new Pair<String,Action>("click", new ClickAction()));
   }


   /**
    * Find the specified HTML element.
    *
    * The selector attributes specified in the event in the test
    * script are used here to search the current HTML document for a
    * matching element.
    *
    * @return the first element matching all criteria
    */

   protected WebElement findElement() {
      ElementFinder ef = new ElementFinder();
      ef.setBrowser(this.eventLoop.Browser);
      ef.setSelectors(this.selectors);
      ef.setReporter(this.eventLoop.report);
      if (this.parent != null) {
         ef.setParentElement(this.parent);
      }
      ef.setEventName(this.testEvent.getNodeName().toLowerCase());

      return ef.findElement();
   }


   /**
    * Execute a VDD plugin at the appropriate time.
    *
    * @param ev  the current plugin event
    */

   protected void firePlugin(PluginEvent ev) {
   }


   /**
    * Run the HTML event.
    *
    * @throws VDDException if execution is unsuccessful
    */

   public void execute() throws VDDException {
      /*
       * Replace all var/hijack strings in the selectors.
       */
      for (String key: this.selectors.keySet()) {
         Object val = this.selectors.get(key);
         if (val instanceof String) {
            this.selectors.put(key, replaceString((String)val));
         }
      }

      this.element = findElement();
      if (this.element == null) {
         return;
      }

      firePlugin(PluginEvent.AFTERFOUND);

      /*
       * Go through the actions for this event.  Use the list of all
       * actions (it's correctly ordered), and check this event to see
       * whether that action was specified.
       */

      // for (Pair<String,Action> action: this.actions) {
         
      // }

   }
}
