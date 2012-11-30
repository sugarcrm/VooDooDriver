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

import au.com.bytecode.opencsv.CSVReader;
import java.io.File;
import java.io.FileReader;
import java.lang.UnsupportedOperationException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;
import org.sugarcrm.voodoodriver.VDDException;
import org.w3c.dom.Element;

/**
 * The CSV event.
 *
 * XXX: EventLoop.sodaVars is directly updated by this class and the
 * resulting variables are not cleaned up.  The correct way to
 * implement this is to create a new variable context (which implies
 * that EventLoop.sodaVars should be a stack of variable contexts) for
 * use during child execution.  This context could then be popped at
 * the end of CSV execution.  This would avoid polluting parent
 * variable contexts and would make nested CSV events more feasible.
 *
 * @author Jon duSaint
 */

class CSV extends Event {

   /**
    * Variable prefix.  Specified by the "var" attribute.
    */

   private String prefix = null;

   /**
    * CSV file specified by either the "override" or the "file" attribute.
    */

   private File csv = null;

   /**
    * CSV reader and parser.
    */

   private CSVReader reader = null;

   /**
    * Keys from the CSV file.  These come from the first row of data
    * and are used in combination with prefix as the name of the Soda
    * vars.
    */

   private String[] keys;

   /**
    * CSV file line number.  Only used when an error occurs.
    */

   private int line;

   /**
    * Child events.  This variable shadows Event.children.
    */

   CSVChildList children;


   /**
    * Instantiate a CSV event.
    *
    * @param event  the event as specified in the test script
    * @throws VDDException if event instantiation fails
    */

   public CSV(Element event) throws VDDException {
      super(event);
   }


   /**
    * Container class for children of this event.
    * 
    * CSV is the only looping structure in VDD.  To keep the plumbing
    * in EventLoop simple, CSV uses an extended ArrayList,
    * CSVChildList, to store its children.  When the list of children
    * is exhausted, the row in the csv file is advanced and the list
    * of children is refreshed.  Execution continues like this until
    * both the child list and the csv file are exhausted.
    */

   class CSVChildList extends ArrayList<Event> {

      /**
       * Iterator class for CSVChildList.
       *
       * This class handles the logic of making multiple passes over
       * the list of child events.
       */

      class CSVChildIterator implements Iterator<Event> {

         /**
          * Reference to the list iterated over.
          */

         private CSVChildList lst;

         /**
          * Index into the list of children.  Used modulo the child count.
          */

         private int index;


         /**
          * Instantiate this iterator.
          *
          * @param lst  list of child event to iterate over
          */

         public CSVChildIterator(CSVChildList lst) {
            this.lst = lst;
            this.index = 0;
         }


         /**
          * Return whether there are more child events.
          *
          * If the end of the list has been reached, another line is
          * read from the CSV file.
          *
          * @return true if the iterator has more elements
          */

         public boolean hasNext() {
            if (index % lst.size() != 0) {
               return true;
            }

            return readNextCSVLine();
         }


         /**
          * Return the next element in the iteration.
          *
          * @return the next element in the iteration
          * @throws NoSuchElementException iteration has no more elements
          */

         public Event next() throws NoSuchElementException {
            return lst.get(index++ % lst.size());
         }


         /**
          * Remove the last element returned by the iterator.
          *
          * Not supported.
          */

         public void remove() throws UnsupportedOperationException {
            throw new UnsupportedOperationException();
         }
      }


      /**
       * Instantiate a CSVChildList.
       *
       * This constructor is not used.
       */

      public CSVChildList() {
         super();
      }


      /**
       * Instantiate a CSVChildList.
       *
       * @param a  {@link ArrayList} of child events of this event
       */

      public CSVChildList(ArrayList<Event> a) {
         super(a);
      }


      /**
       * Return an iterator over this event's child events.
       *
       * @return an iterator over the child events
       */

      public CSVChildIterator iterator() {
         return new CSVChildIterator(this);
      }

   }


   /**
    * Load the first row of data, the keys, from the CSV file.
    */

   private void loadCSVKeys() throws VDDException {
      try {
         this.reader = new CSVReader(new FileReader(this.csv), ',', '"');
      } catch (java.io.FileNotFoundException e) {
         throw new VDDException(e.getMessage());
      }

      /* Load the keys */
      try {
         this.keys = this.reader.readNext();
      } catch (java.io.IOException e) {
         throw new VDDException(e.getMessage());
      }
      if (this.keys == null) {
         throw new VDDException("CSV file empty");
      }

      line = 0;
   }


   /**
    * Read a line of data from the CSV file.
    *
    * Each field is stored into the corresponding Soda var, the name
    * of which is specified by prefix and the keys row.
    *
    * @return true if another line was read, false otherwise
    */

   private boolean readNextCSVLine() {
      String[] data = null;

      try {
         data = this.reader.readNext();
         line += 1;
      } catch (java.io.IOException e) {}

      if (data == null) {
         return false;
      }

      if (data.length != keys.length) {
         error("Number of elements on line " + line +
               " differs from key count. Skipping.");
         return false;
      }

      for (int k = 0; k < keys.length; k++) {
         String key = (prefix != null ? prefix + "." : "") + keys[k];
         String val = data[k];

         if (eventLoop.hijacks.containsKey(key)) {
            val = String.valueOf(eventLoop.hijacks.get(key));
            log("Hijacking SodaVar: '" + key + "' => '" + val + "'.");
         }

         eventLoop.vars.put(key, val);
      }

      return true;
   }


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
      this.children = new CSVChildList(children);
   }


   /**
    * Run the CSV event.
    *
    * @throws StopEventException if child event execution is to be skipped
    * @throws VDDException if execution is unsuccessful
    */

   public void execute() throws VDDException {
      if (this.actions.containsKey("override")) {
         String of = (String)this.actions.get("override");
         this.eventLoop.csvOverrideFile = new File(this.replaceString(of));
         log("Setting CSV override to " + this.eventLoop.csvOverrideFile);
         throw new StopEventException();
      }

      if (this.eventLoop.csvOverrideFile != null) {
         this.csv = this.eventLoop.csvOverrideFile;
         log("Using CSV override file " + this.csv);
         this.eventLoop.csvOverrideFile = null;
      } else if (this.actions.containsKey("file")) {
         String f = (String)this.actions.get("file");
         this.csv = new File(this.replaceString(f));
      } else {
         throw new VDDException("Missing file attribute (and no override specified)");
      }

      if (this.actions.containsKey("var")) {
         this.prefix = (String)this.actions.get("var");
      }

      try {
         this.eventLoop.vars.pushContext();
         loadCSVKeys();
      } catch (VDDException e) {
         /* Don't leave dangling contexts around. */
         this.eventLoop.vars.popContext();
         throw e;
      }
   }
}
