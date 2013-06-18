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

import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;
import org.sugarcrm.voodoodriver.VDDException;
import org.w3c.dom.Element;


/**
 * The select event.
 *
 * @author Jon duSaint
 */

class Select extends HtmlEvent {

   /**
    * Name for the injected action to instantiate the Select object.
    */

   private static String STORE_SELECT_STRING = "store select";


   /**
    * The select control.
    */

   private org.openqa.selenium.support.ui.Select select;


   /**
    * Clicking a multi-select control is additive by default.  If this
    * is true, clicking replaces the previous selection.
    */

   private boolean multiselect = true;


   /**
    * An action to instantiate the Select object.  This action is
    * injected in the constructor to the start of the action list and
    * is always executed.  It's needed so that other actions don't
    * have to individually check whether the Select object has been
    * instantiated and then do it themselves.
    */

   private class StoreSelect implements Action {

      /**
       * Create our Select object.
       */

      public void action(Object val) {
         select = new org.openqa.selenium.support.ui.Select(element);
      }
   }


   /**
    * Update the multiselect attribute of this event.
    */

   protected class MultiselectAction implements Action {

      /**
       * Run the multiselect action.
       */

      public void action(Object val) {
         boolean value = (Boolean)val;

         if (!select.isMultiple() && !value) {
            rterror("multiselect=\"false\" is not valid for single select controls");
         }

         multiselect = value;
      }
   }


   /**
    * The assert and assertnot actions, depending on the polarity setting.
    */

   protected class AssertAction implements Action {

      /**
       * Assertion polarity.
       */

      private boolean wantSelected;


      /**
       * Instantiate an AssertAction object.
       *
       * @param polarity  true means assert, false means assertnot
       */

      public AssertAction(boolean polarity) {
         this.wantSelected = polarity;
      }

      /**
       * Run the assert or assertnot action.
       */

      public void action(Object val) {
         String option = (String)val;

         for (WebElement optionElement: select.getOptions()) {
            if (optionElement.getText().contains(option)) {
               boolean isSelected = optionElement.isSelected();
               eventLoop.report.Assert("Select option '" + option + "' is " +
                                       (isSelected ? "" : "not ") + "selected",
                                       wantSelected ^ isSelected,
                                       false);
               return;
            }
         }

         error("Failed to find select option '" + option + "'");
      }
   }


   /**
    * AssertSelected action.
    *
    * @param event
    */

   protected class AssertSelectedAction implements Action {

      /**
       * Run the assertSelected action.
       */

      public void action(Object val) {
         boolean shouldBeSelected = (Boolean)val;
         boolean anySelected = select.getAllSelectedOptions().size() > 0;

         eventLoop.report.Assert("Option " + (anySelected ? "" : "not ") +
                                 "selected",
                                 anySelected, shouldBeSelected);
      }
   }


   /**
    * Included and NotIncluded actions.
    *
    * @param event
    */

   protected class IncludedAction implements Action {

      /**
       * True for included, false for notincluded.
       */

      private boolean wantOption;


      /**
       * Instantiate an IncludedAction object.
       *
       * @param polarity  true means included, false means notincluded
       */

      public IncludedAction(boolean polarity) {
         this.wantOption = polarity;
      }


      /**
       * Run the included action.
       */

      public void action(Object val) {
         String searchOption = (String)val;
         boolean foundOption = false;

         for (WebElement option: select.getOptions()) {
            if (option.getText().contains(searchOption)) {
               foundOption = true;
               break;
            }
         }

         eventLoop.report.Assert("Select option '" + searchOption + "'" +
                                 (foundOption ? "" : " not") + " found and" +
                                 (wantOption ? "" : " not") + " expected.",
                                 wantOption ^ foundOption,
                                 false);
      }
   }


   /**
    * The clear action
    */

   protected class ClearAction implements Action {

      /**
       * Run the clear action.
       */

      public void action(Object val) {
         if ((Boolean)val && select.isMultiple()) {
            select.deselectAll();
         }
      }
   }


   /**
    * The set action
    */

   protected class SetAction implements Action {

      /**
       * Type of set action.
       */

      private boolean byValue;


      /**
       * Instantiate a SetAction object.
       *
       * @param polarity  true means set by value (setreal), false
       *                  means set by visible text (set).
       */

      public SetAction(boolean polarity) {
         this.byValue = polarity;
      }


      /**
       * Run the set action.
       */

      public void action(Object val) {
         String value = (String)val;

         try {
            log("Setting option by " + (byValue ? "value" : "visible text") +
                ": '" + value + "'.");

            try {
               if (!multiselect) {
                  /*
                   * isMultiple() must be true in order for
                   * multiselect to be false.
                   */
                  select.deselectAll();
               }

               if (byValue) {
                  select.selectByValue(value);
               } else {
                  select.selectByVisibleText(value);
               }

               firePlugin(org.sugarcrm.voodoodriver.PluginEvent.AFTERSET);
            } catch (NoSuchElementException e) {
               rterror("Option with " + (byValue ? "value" : "visible text") +
                       " '" + value + "' does not exist");
            }
         } catch (StaleElementReferenceException e) {
            /*
             * Selecting a value has the potential to refresh the page
             * (Bug 49533).
             */
            log("Page refreshed; select element no longer exists.");
            throw new StopEventException();
         }
      }
   }


   /**
    * Instantiate a select event.
    *
    * @param e  the event as specified in the test script
    * @throws VDDException if event instantiation fails
    */

   public Select(Element e) throws VDDException {
      super(e);

      actionList.addFirst(new Pair<String, Action>(STORE_SELECT_STRING,
                                                   new StoreSelect()));
      actionList.replacePair(new Pair<String,Action>("assert",
                                                     new AssertAction(true)));
      actionList.replacePair(new Pair<String,Action>("assertnot",
                                                     new AssertAction(false)));
      actionList.addLast(new Pair<String,Action>("assertselected",
                                                 new AssertSelectedAction()));
      actionList.addLast(new Pair<String,Action>("included",
                                                 new IncludedAction(true)));
      actionList.addLast(new Pair<String,Action>("notincluded",
                                                 new IncludedAction(false)));
      actionList.addLast(new Pair<String,Action>("multiselect",
                                                 new MultiselectAction()));
      actionList.addLast(new Pair<String,Action>("clear", new ClearAction()));
      actionList.addLast(new Pair<String,Action>("set", new SetAction(false)));
      actionList.addLast(new Pair<String,Action>("setreal",
                                                 new SetAction(true)));

      this.actions.put(STORE_SELECT_STRING, true);
   }


   /**
    * Get the value to be used when storing to a var.
    *
    * In the case of link, the value is the text of the A.
    */

   protected String getVarValue() {
      return this.element.getText();
   }


   /**
    * Run the event.
    *
    * @throws VDDException if execution is unsuccessful
    */

   public void execute() throws VDDException {
      super.execute();
   }

}
