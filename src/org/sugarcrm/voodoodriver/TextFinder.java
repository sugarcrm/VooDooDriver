/*
 * Copyright 2012 SugarCRM Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you
 * may not use this file except in compliance with the License.  You
 * may may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  Please see the License for the specific language
 * governing permissions and limitations under the License.
 */

package org.sugarcrm.voodoodriver;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>TextFinder is a class that abstracts the process of finding text
 * within a string.  If the search string is a regular expression,
 * then Java's regex package is used for the matching, otherwise a
 * simple text search is performed.</p>
 *
 * <p>The added complexity of this code is offset by its performance
 * gains.  Microbenchmarking indicates that a regular expression
 * search involving no metacharacters is more than four times slower
 * than a simple text search using the same search string.</p>.
 *
 * @author Jon duSaint
 */

public class TextFinder {

   /**
    * The Pattern to be used for regex searches.
    */

   private Pattern pattern;

   /**
    * The string to be used for text searches.
    */

   private String search;

   /**
    * True if this object will use a regex search.
    */

   private boolean isRegex = false;


   /**
    * Instantiate a TextFinder object.
    *
    * @param search  the string or regex to use during the text search
    */

   public TextFinder(String search) {
      if (isRegex(search) && compileRegex(search)) {
         this.isRegex = true;
      }
      this.search = search;
   }


   /**
    * Use a heuristic to determine whether the string is intended to be a regex.
    *
    * <p>The following characters within the string will cause it to
    * be recognized as a regular expression: <code>^</code> at the
    * beginning of the string, <code>$</code> at the end of the
    * string, or any of <code>[, ], *, (, ), ?, +, |, \, .</code>
    * anywhere within the string.</p>
    *
    * @param str  the string to look at
    * @return true if the string is a regex, false otherwise
    */

   private boolean isRegex(String str) {
      Pattern p = Pattern.compile("^/\\^?.*(?:[\\[\\]*()?+|\\\\.]).*\\$?/$");
      Matcher m = p.matcher(str);
      return m.find();
   }


   /**
    * Compile a regex into a Pattern object and store it.
    *
    * @param regex  the regex string
    * @return true if the regex compiles successfully, false otherwise
    */

   private boolean compileRegex(String regex) {
      try {
         this.pattern = Pattern.compile(regex.replaceAll("^/|/$", ""),
                                        Pattern.MULTILINE |
                                        Pattern.DOTALL |
                                        Pattern.UNICODE_CASE);
      } catch (java.util.regex.PatternSyntaxException e) {
         return false;
      }

      return true;
   }


   /**
    * Find the search string or regex within the provided text.
    *
    * @param text  the text to search through
    * @return true if there is a match
    */

   public boolean find(String text) {
      if (this.isRegex) {
         Matcher m = this.pattern.matcher(text);
         return m.find();
      }

      return text.contains(this.search);
   }


   /**
    * Determine whether the search string or regex matches the entire
    * provided text.
    *
    * @param text  the text to search through
    * @return true if there is a match
    */

   public boolean findExact(String text) {
      if (this.isRegex) {
         Matcher m = this.pattern.matcher(text);
         return m.matches();
      }

      return text.equals(this.search);
   }


   /**
    * Replace all occurrences of the search string or regex with the
    * provided text.
    *
    * @param text         the text to search through
    * @param replacement  the replacement text
    * @return the modified text
    */

   public String replaceAll(String text, String replacement) {
      if (this.isRegex) {
         Matcher m = this.pattern.matcher(text);
         return m.replaceAll(replacement);
      }

      return text.replace(this.search, replacement);
   }


   /**
    * Return the search string.
    *
    * @return  the search string
    */

   public String toString() {
      return this.search;
   }
}
