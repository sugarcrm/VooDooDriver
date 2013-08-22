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
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.sugarcrm.voodoodriver.Browser;
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

   private Browser browser;


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
    * Timeout value. Set by <code>timeout</code> attribute. Default is
    * five seconds.
    */

   private int timeout;


   /**
    * Instantiate an ElementFinder object.
    */

   public ElementFinder() {
      this.parent = null;
   }


   /**
    * Instantiate an ElementFinder object.
    *
    * <p>This constructor accepts the various arguments needed for
    * ElementFinder to work as a convenience.</p>
    *
    * @param browser    the current browser
    * @param selectors  element selectors
    * @param r          the {@link Reporter} object
    * @param timeout    the search timeout in seconds
    * @param parent     the parent element
    * @param event      the name of the event
    */

   public ElementFinder(Browser browser, VDDHash selectors, Reporter r,
                        int timeout, WebElement parent, String event) {
      this.browser = browser;
      this.selectors = selectors;
      this.r = r;
      this.timeout = timeout;
      this.parent = parent;
      this.event = event;
   }


   /**
    * Set the current browser.
    *
    * @param browser  the current browser
    */

   public void setBrowser(Browser browser) {
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
    * Set the element search timeout.
    *
    * @param timeout  the search timeout in seconds
    */

   public void setTimeout(int timeout) {
      this.timeout = timeout;
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
    * Find elements matching the specified parameters.
    *
    * <p>This method uses the By object provided to find the matching
    * elements either on the current page, or as children of
    * <code>this.parent</code> if it exists.</p>
    *
    * @param by  match parameters
    * @return list of matching elements
    * @throws NoSuchElementException if no matching elements are found
    *                                before the expiry of timeout
    */

   private List<WebElement> findElements(By by) throws NoSuchElementException {
      long end = System.currentTimeMillis() + this.timeout * 1000;
      
      do {
         List<WebElement> lst = null;

         try {
            if (this.parent != null) {
               lst = this.parent.findElements(by);
            } else {
               lst = this.browser.getDriver().findElements(by);
            }

            if (lst != null && lst.size() > 0) {
               return lst;
            }

            Thread.sleep(100);
         } catch (Exception e) {}
      } while (System.currentTimeMillis() < end);

      throw new NoSuchElementException("Failed to find element by " + by);
   }


   /**
    * Find images element based on alt text.
    *
    * @param tagName  HTML tag
    * @param alt      alt text to search for
    * @return matching {@link WebElement} or null
    */

   private List<WebElement> findElementsByAlt(String tagName, String alt)
      throws NoSuchElementException {
      List<WebElement> elements = new ArrayList<WebElement>();

      this.r.log("Searching for " + tagName + " with alt text '" + alt + "'");

      for (WebElement element: findElements(By.tagName(tagName))) {
         String elementAlt = element.getAttribute("alt");
         if (elementAlt != null && elementAlt.equals(alt)) {
            elements.add(element);
         }
      }

      return elements;
   }


   /**
    * Find an element by its innerText attribute.
    *
    * @param text  element text to search for
    * @return matching {@link WebElement} or null
    */

   private List<WebElement> findElementsByText(String text)
      throws NoSuchElementException {
      By by = null;
      List<WebElement> elements = null;
      String tag = (String)this.selectors.get("html_tag");

      this.r.log("Searching for " + tag + " with text '" + text + "'");

      if (tag.equals("a")) {
         by = By.linkText(text);
      } else {
         by = By.tagName(tag);
      }

      elements = findElements(by);

      if (!tag.equals("a")) {
         ArrayList<WebElement> discard = new ArrayList<WebElement>();

         for (WebElement e: elements) {
            if (!e.getText().contains(text)) {
               discard.add(e); // Can't delete elements while iterating
            }
         }

         elements.removeAll(discard);
      }

      return elements;
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
      String root = (this.parent == null) ? "document" : "arguments[0]";
      String tag = (String)this.selectors.get("html_tag");
      String type = (String)this.selectors.get("html_type");
      String js = null;

      this.r.log("Searching all " + (type == null ? tag : type) +
                 " elements for value='" + value +
                 "'. This could take a while...");

      String[] tags = tag.split("\\|");
      String[] types = (type == null) ? new String[0] : type.split("\\|");

      if (types.length == 0 ||
          this.browser instanceof org.sugarcrm.voodoodriver.IE) {
         /*
          * IE only supports the Selectors API with versions 8 and
          * greater, and then only when rendering the document in
          * standards mode.  Given the special tags required for IE to
          * enter standards mode, it is safe to assume that any page
          * VDD is testing will probably be in quirks mode, making the
          * Selectors API unavailable.  So in the case of IE, look for
          * matching elements by iterating through all elements.
          * Slow, but it'll at least work.
          */

         String tagFinder = "";
         for (int k = 0; k < tags.length; k++) {
            tagFinder += String.format("finder(%s.getElementsByTagName('%s'))%s",
                                       root, tags[k],
                                       (k == tags.length - 1) ? "" : ",");
         }

         String typeFilter = "";
         for (int k = 0; k < types.length; k++) {
            typeFilter += String.format("args[k].type == '%s'%s", types[k],
                                        (k == types.length - 1) ? "" : " || ");
         }
         if (typeFilter.length() == 0) {
            typeFilter = "true"; // Accept all types if none was specified
         }

         js = String.format("function finder(args) {\n" +
                            "   var found = [];\n" +
                            "   for (var k = 0; k < args.length; k++) {\n" +
                            "      if (args[k].value == '%s' &&\n" +
                            "          (%s)) {\n" +
                            "         found.push(args[k]);\n" +
                            "      }\n" +
                            "   }\n" +
                            "   return found;\n" +
                            "}\n" +
                            "return [].concat(%s);\n",
                            value, typeFilter, tagFinder);
      } else {
         /*
          * Not IE, use the Selectors API.
          */
         String selectors = "";

         for (int i = 0; i < tags.length; i++) {
            for (int j = 0; j < types.length; j++) {
               selectors += String.format("%s[type=\"%s\"][value=\"%s\"]%s",
                                          tags[i], types[j], value,
                                          ((i == tags.length - 1) &&
                                           (j == types.length - 1)) ?
                                          "" : ",");
            }
         }

         js = String.format("return %s.querySelectorAll('%s');",
                            root, selectors);
      }

      @SuppressWarnings("unchecked")
         ArrayList<WebElement> lst =
         (ArrayList<WebElement>)this.browser.executeJS(js, this.parent);

      return lst;
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
    * Cached value of the required attribute.
    */

   private boolean required = true;


   /**
    * Return whether the required attribute is true.
    *
    * @return the value of the required attribute
    */

   public boolean isRequired() {
      return this.required;
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
      boolean requiredModified = false;
      By by = null;
      boolean searchByAlt = false;
      boolean searchByText = false;
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
            this.r.error("Invalid value for 'index': " + e);
            return null;
         }
      }

      if (this.selectors.containsKey("required")) {
         this.required = (Boolean)this.selectors.get("required");
         requiredModified = true;
      }

      if (selectors.containsKey("timeout")) {
         try {
            this.timeout = Integer.valueOf((String)selectors.get("timeout"));
         } catch (NumberFormatException e) {
            this.r.error("Invalid value for 'timeout': " + e);
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
       *    text   => linkText
       *    name   => name
       *    ???    => partialLinkText
       *    <none> => tagName
       *    xpath  => xpath
       *
       * "value", though not Selenium-approved is a primary selector
       * and so is handled here.
       */

      if (this.selectors.containsKey("alt")) {
         searchByAlt = true;
      } else if (this.selectors.containsKey("class")) {
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
         this.r.log("Searching for class '" + className + "'");
      } else if (this.selectors.containsKey("css")) {
         String sel = (String)selectors.get("css");
         by = By.cssSelector(sel);
         this.r.log("Searching for css '" + sel + "'");
      } else if (this.selectors.containsKey("id")) {
         String id = (String)selectors.get("id");
         by = By.id(id);
         this.r.log("Searching for id '" + id + "'");
      } else if (this.selectors.containsKey("text")) {
         searchByText = true;
      } else if (this.selectors.containsKey("name")) {
         by = By.name((String)selectors.get("name"));
      } else if (this.selectors.containsKey("value")) {
         searchByValue = true;
      } else if (this.selectors.containsKey("xpath")) {
         String xp = (String)selectors.get("xpath");
         by = By.xpath(xp);
         this.r.log("Searching for xpath '" + xp + "'");
      } else {
         String tag = (String)selectors.get("html_tag");
         by = By.tagName(tag);
         this.r.log("Searching for HTML tag '" + tag + "'");
      }

      /*
       * Perform the element search.
       */

      try {
         if (searchByValue) {
            elements = findElementsByValue((String)this.selectors.get("value"));
         } else if (searchByAlt) {
            elements = findElementsByAlt((String)selectors.get("html_tag"),
                                         (String)this.selectors.get("alt"));
         } else if (searchByText) {
            elements = findElementsByText((String)this.selectors.get("text"));
         } else {
            elements = findElements(by);
         }
      } catch (NoSuchElementException e) {
         elements = new ArrayList<WebElement>(0);
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
            this.r.error("Failed to find element: " +
                         String.format("Index (%d) out of bounds.", index));
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

      if (element == null && (this.required == true ||
                              (existsModified && exists == true))) {
         this.r.error("Failed to find element" +
                            (requiredModified ? " and required => true" :
                             (existsModified ? " and exists => true" : "")));
      } else if (element == null) {
         this.r.log("Element not found but" +
                    (existsModified ? " exists and" : "") +
                    " required => false");
      } else if (exists == false) {
         this.r.error("Element exists and exists => false");
         element = null;
      } else {
         this.r.log("Found element");
      }

      return element;
   }
}
