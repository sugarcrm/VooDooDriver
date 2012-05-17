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
import java.util.List;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.sugarcrm.voodoodriver.Reporter;
import org.sugarcrm.voodoodriver.VDDHash;


/**
 * Class used to search the current HTML page for HTML elements for
 * use by the current test script.
 *
 * @author Jon duSaint
 */

class ElementFinder {

   /**
    * Browser object.
    */

   private org.sugarcrm.voodoodriver.Browser browser;


   /**
    * Event selectors used to find this element.
    */

   private VDDHash selectors;

   /**
    * VDD's Reporter object.
    */

   private Reporter r;

   /**
    * Parent element of the element to be found or null.
    */

   private WebElement parent;

   /**
    * Name of the current event.
    */

   private String event;


   /**
    * Instantiate an ElementFinder object.
    */

   public ElementFinder() {
      this.parent = null;
   }


   /**
    * Set the current browser.
    *
    * @param browser  the current browser
    */

   public void setBrowser(org.sugarcrm.voodoodriver.Browser browser) {
      this.browser = browser;
   }


   /**
    * Set this event's element selectors.
    *
    * @param selectors  element selectors
    */

   public void setSelectors(VDDHash selectors) {
      this.selectors = selectors;
   }


   /**
    * Set this event's Reporter object.
    *
    * @param r  the {@link Reporter} object
    */

   public void setReporter(Reporter r) {
      this.r = r;
   }


   /**
    * Set the parent element.
    *
    * @param parent  the parent element
    */

   public void setParentElement(WebElement parent) {
      this.parent = parent;
   }


   /**
    * Set the name of the current event.
    *
    * The event name is needed when searching for elements by value.
    *
    * @param event  the name of the event
    */

   public void setEventName(String event) {
      this.event = event;
   }


   /**
    * Search through input elements using their value attribute.
    *
    * This kind of search is done using a snippet of javascript
    * executed in the browser and, as such, can be slow.
    *
    * @param value  the value of the element to be searched for
    * @return {@link List} of {@link WebElement} found by this search
    */

   private List<WebElement> findElementsByValue(String value) {
      String js = null;
      String type = (String)this.selectors.get("html_tag");

      this.r.Log("Searching all " + type + " elements for value='" +
                 value + "'. This could be take a while...");

      if (this.event.equals("button")) {
         js = ("querySelectorAll('" +
                  "input[type=\"button\"][value=\"" + value + "\"]," +
                  "button[value=\"" + value + "\"]," +
                  "input[type=\"submit\"][value=\"" + value + "\"]," +
                  "input[type=\"reset\"][value=\"" + value + "\"]" +
               "', true);");
      } else {
         js = ("querySelectorAll('" +
                  "input[type=\"" + type + "\"][value=\"" + value + "\"]," +
                  type + "[value=\"" + value + "\"]" +
               "', true);");
      }

      @SuppressWarnings("unchecked")
         ArrayList<WebElement> lst =
         (ArrayList<WebElement>)this.browser.executeJS(js, this.parent);

      return lst;
   }


   /**
    * Perform the element search in the context of a parent element.
    *
    * @param by       element search parameters
    * @param timeout  maximum time to spend finding matches
    * @return {@link List} of matching {@link WebElement}
    */

   private List<WebElement> findElementsInParent(By by, int timeout) {
      long end = System.currentTimeMillis() + timeout * 1000;
      
      do {
         try {
            return parent.findElements(by);
         } catch (Exception exp) {}
      } while (System.currentTimeMillis() < end);

      return new ArrayList<WebElement>(0);
   }


   /**
    * Determine whether tag is found in filter
    *
    * @param tag    HTML tag to search for
    * @param filter list of acceptable HTML tags
    * @return true if tag is in filter, false otherwise
    */

   private boolean checkMatch(String tag, String filter) {
      if (filter == null) {
         return true;
      } else if (tag == null) {
         return false;
      }
      String filters[] = filter.split("\\|");
      for (int k = 0; k < filters.length; k++) {
         if (tag.equals(filters[k])) {
            return true;
         }
      }
      return false;
   }


   /**
    * Filter elements by HTML element type
    *
    * Take a list of {@link WebElement}s found using the event selection
    * criteria and filter through by HTML tag/type.
    *
    * @param elements  element list returned based on event
    * @return list of elements filtered by HTML tag
    */

   private List<WebElement> filterByTag(List<WebElement> elements) {
      String tag = (String)this.selectors.get("html_tag");
      String type = (String)this.selectors.get("html_type");

      ArrayList<WebElement> filtered =
         new ArrayList<WebElement>(elements.size());

      for (int k = 0; k < elements.size(); k++) {
         WebElement e = elements.get(k);
         if (checkMatch(e.getTagName(), tag) &&
             checkMatch(e.getAttribute("type"), type)) {
            filtered.add(e);
         }
      }

      return filtered;
   }


   /**
    * Filter elements by VDD-specific selectors.
    *
    * The other selectors that can be used to filter and the HTML
    * elements they can be used with are:
    *    <dl>
    *    <dt>action</dt><dd>form</dd>
    *    <dt>alt   </dt><dd>image</dd>
    *    <dt>for   </dt><dd>label</dd>
    *    <dt>href  </dt><dd>link</dd>
    *    <dt>method</dt><dd>form</dd>
    *    <dt>title </dt><dd>filefield</dd>
    *    </dl>
    *
    * @param elements  element list returned based on event
    * @return list of elements filtered by HTML tag
    */

   private List<WebElement> filterByVDDSelectors(List<WebElement> elements) {
      String sel = null;

      if (this.event.equals("filefield") &&
          this.selectors.containsKey("title")) {
         sel = "title";
      } else if (this.event.equals("form")) {
         if (this.selectors.containsKey("action")) {
            sel = "action";
         } else if (this.selectors.containsKey("method")) {
            sel = "method";
         }
      } else if (this.event.equals("image") &&
                 this.selectors.containsKey("alt")) {
         sel = "alt";
      } else if (this.event.equals("label") &&
                 this.selectors.containsKey("for")) {
         sel = "for";
      } else if (this.event.equals("link") &&
                 this.selectors.containsKey("href")) {
         sel = "href";
      }

      if (sel == null) {
         return elements;
      }

      ArrayList<WebElement> filtered =
         new ArrayList<WebElement>(elements.size());
      String val = (String)this.selectors.get(sel);

      for (int k = 0; k < elements.size(); k++) {
         WebElement e = elements.get(k);
         String attr = e.getAttribute(sel);
         if (attr != null && attr.equals(val)) {
            filtered.add(e);
         }
      }

      return filtered;
   }


   /**
    * Find the HTML element that corresponds to the current selectors.
    *
    * @return the matching {@link WebElement} or null if none was found
    */

   public WebElement findElement() {
      boolean exists = true;
      boolean existsModified = false;
      int index = 0;
      boolean required = true;
      boolean requiredModified = false;
      int timeout = 5;
      By by = null;
      boolean searchByValue = false;
      List<WebElement> elements;
      WebElement element = null;

      /*
       * VDD element search modifiers.
       */

      if (this.selectors.containsKey("exists")) {
         exists = (Boolean)this.selectors.get("exists");
         existsModified = true;
      }

      if (selectors.containsKey("index")) {
         try {
            index = Integer.valueOf((String)selectors.get("index"));
         } catch (NumberFormatException e) {
            this.r.ReportError("Invalid value for 'index': " + e);
            return null;
         }
      }

      if (this.selectors.containsKey("required")) {
         required = (Boolean)this.selectors.get("required");
         requiredModified = true;
      }

      if (selectors.containsKey("timeout")) {
         try {
            timeout = Integer.valueOf((String)selectors.get("timeout"));
         } catch (NumberFormatException e) {
            this.r.ReportError("Invalid value for 'timeout': " + e);
            return null;
         }
      }

      /*
       * Start the search using the Selenium-approved selectors.  The
       * results from this get filtered with the additional selectors
       * (if any) below.
       *
       * VDD selector name to selenium By list:
       *    class  => className
       *    css    => cssSelector
       *    id     => id
       *    ???    => linkText
       *    name   => name
       *    ???    => partialLinkText
       *    <none> => tagName
       *    xpath  => xpath
       *
       * "value", though not Selenium-approved is a primary selector
       * and so is handled here.
       */

      if (this.selectors.containsKey("class")) {
         /*
          * If the class name contains whitespace, the search is done
          * using ByCssSelector rather than ByClassName.
          */
         String className = (String)selectors.get("class");

         if (className.matches("\\s")) {
            String tag = (String)selectors.get("html_tag");
            String sel = null;

            if (tag.equals("input|button")) {
               /*
                * The button event can match either a button element
                * or an input element.
                */
               sel = String.format("input[class=\"%s\"],button[class=\"%s\"]",
                                   className, className);
            } else {
               sel = String.format("%s[class=\"%s\"]", tag, className);
            }

            by = By.cssSelector(sel);
         } else {
            by = By.className(className);
         }
      } else if (this.selectors.containsKey("css")) {
         by = By.cssSelector((String)selectors.get("css"));
      } else if (this.selectors.containsKey("id")) {
         by = By.id((String)selectors.get("id"));
      } else if (this.selectors.containsKey("link")) {
         by = By.linkText((String)selectors.get("link"));
      } else if (this.selectors.containsKey("name")) {
         by = By.name((String)selectors.get("name"));
      } else if (this.selectors.containsKey("value")) {
         searchByValue = true;
      } else if (this.selectors.containsKey("xpath")) {
         by = By.xpath((String)selectors.get("xpath"));
      } else {
         by = By.tagName((String)selectors.get("html_tag"));
      }

      /*
       * Perform the element search.
       */

      if (searchByValue) {
         elements = findElementsByValue((String)this.selectors.get("value"));
      } else if (this.parent == null) {
         elements = this.browser.findElements(by, timeout);
      } else {
         elements = findElementsInParent(by, timeout);
      }

      /*
       * Filter the resulting elements list by the HTML tag and type.
       */

      elements = filterByTag(elements);

      /*
       * Filter using VDD-specific selectors.
       */

      elements = filterByVDDSelectors(elements);

      /*
       * Apply index to the final list.
       */

      try {
         element = elements.get(index);
      } catch (IndexOutOfBoundsException e) {
         if (index > 0) {
            this.r.ReportError("Failed to find element: " +
                               String.format("Index (%d) out of bounds.",
                                             index));
            return null;
         }
      }

      /*
       * Now check the exists and required selectors and return the element.
       *
       * Element exists:
       *    exists => true:    ok
       *    exists => false:   error
       *    required => true:  ok
       *    required => false: ok
       * Element does not exist:
       *    exists => true:    error
       *    exists => false:   ok
       *    required => true:  error
       *    required => false: ok
       */

      if (element == null && (required == true ||
                              (existsModified && exists == true))) {
         this.r.ReportError("Failed to find element" +
                            (requiredModified ? " and required => true" :
                             (existsModified ? " and exists => true" : "")));
      } else if (element == null) {
         this.r.Log("Element not found but" +
                    (existsModified ? " exists and" : "") +
                    " required => false");
      } else if (exists == false) {
         this.r.ReportError("Elements exists and exists => false");
      } else {
         this.r.Log("Found element");
      }

      return element;
   }
}
