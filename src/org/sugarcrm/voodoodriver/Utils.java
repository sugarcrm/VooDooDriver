/*
Copyright 2011-2012 SugarCRM Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
Please see the License for the specific language governing permissions and
limitations under the License.
*/

package org.sugarcrm.voodoodriver;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import org.apache.commons.io.FilenameUtils;
import org.openqa.selenium.WebElement;

/**
 * A simple class for housing one off needed util functions, that can
 * all be called statically.
 *
 * @author trampus
 *
 */
public class Utils {

   /**
    * Reads a text file into a String object.
    *
    * @param filename  the file to read in.
    * @return {@link String} containing the contents of the file
    *
    */

   public static String FileToStr(String filename) throws java.io.FileNotFoundException, java.io.IOException {
      String result = "";
      BufferedReader reader = null;

      filename = FilenameUtils.separatorsToSystem(filename);
      reader = new BufferedReader(new FileReader(filename));
      String line = "";
      while ((line = reader.readLine()) != null) {
         result = result.concat(line + "\n");
      }
      reader.close();

      return result;
   }

   /**
    * Generates a string containing the run time based on a start & stop time.
    *
    * @param starttime The start time.
    * @param stoptime   The stop time.
    * @return A String formatted as: hh:mm:ss:m (hours, minutes, seconds, micro seconds).
    */
   public static String GetRunTime(Date starttime, Date stoptime) {
      String result = "";
      long diff = 0;
      long mills = 0;
      long x = 0;
      long seconds = 0;
      long minutes = 0;
      long hours = 0;

      diff = stoptime.getTime() - starttime.getTime();
      mills = diff % 1000;
      x = diff / 1000;
      seconds = x % 60;
      x /= 60;
      minutes = x % 60;
      x /= 60;
      hours = x % 24;

      result = String.format("%d:%d:%d.%d", hours, minutes, seconds, mills);

      return result;
   }


   /**
    * Take a screen shot and save it to the specified file.
    *
    * @param outputFile  file to save the screenshot into
    * @param reporter    {@link Reporter} object for logging errors
    * @return whether the screenshot was taken successfully
    */

   public static boolean takeScreenShot(String outputFile, Reporter reporter) {
      return takeScreenShot(outputFile, reporter, true);
   }


   /**
    * Take a screen shot and save it to the specified file.
    *
    * @param outputFile  file to save the screenshot into
    * @param reporter    {@link Reporter} object for logging errors
    * @param logOK whether logging to reporter is OK.  false if this
    *              is called from a reporter object.
    * @return whether the screenshot was taken successfully
    */

   public static boolean takeScreenShot(String outputFile, Reporter reporter,
                                        boolean logOK) {
      Robot r = null;

      reporter.Log("Taking Screenshot.");

      File tmp = new File(outputFile);
      if (tmp.exists() && logOK) {
         String msg;
         msg = String.format("Existing screenshot '%s' will be overwritten.",
                             outputFile);
         reporter.Warn(msg);
      }

      try {
         r = new Robot();
      } catch (java.awt.AWTException e) {
         if (logOK) {
            reporter.ReportError("Screenshot failed (running headless?)");
            reporter.ReportException(e);
         }
         return false;
      }

      Rectangle rec = new Rectangle();
      Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
      dim.setSize(dim);
      rec.setSize(dim);
      BufferedImage img = r.createScreenCapture(rec);

      try {
         ImageIO.write(img, "png", new File(outputFile));
      } catch (java.io.IOException e) {
         if (logOK) {
            reporter.ReportError("Screenshot failed (I/O Error)");
            reporter.ReportException(e);
         }
         return false;
      }

      reporter.Log(String.format("Screenshot file: %s", outputFile));
      reporter.Log("Screenshot finished.");

      return true;
   }


   /**
    * Record JVM information.
    *
    * @return {@link VDDHash} containing JVM information
    */

   public static VDDHash getJavaInfo() {
      VDDHash data = new VDDHash();

      String[] keys = {"java.vendor", "java.version", "os.arch", "os.name",
                       "os.version", "user.name", "user.home", "user.dir"};

      for (String key: keys) {
         data.put(key, System.getProperty(key));
      }

      return data;
   }


   public static boolean isInt(String str) {
      boolean result = false;

      try {
         Integer.parseInt(str);
         result = true;
      } catch (NumberFormatException exp) {
         result = false;
      }

      return result;
   }

   public static String replaceString(String str, VDDHash hijacks) {
      String result = str;
      Pattern patt = null;
      Matcher matcher = null;

      patt = Pattern.compile("\\{@[\\w\\.]+\\}", Pattern.CASE_INSENSITIVE);
      matcher = patt.matcher(str);

      while (matcher.find()) {
         String m = matcher.group();
         String tmp = m;
         tmp = tmp.replace("{@", "");
         tmp = tmp.replace("}", "");

         if (hijacks.containsKey(tmp)) {
            String value = hijacks.get(tmp).toString();
            result = result.replace(m, value);
         }
      }

      result = result.replaceAll("\\\\n", "\n");

      return result;
   }

   public static void isEnabled(WebElement element, Reporter reporter, boolean state) {
      boolean eleState = false;
      String msg = "";

      if (element == null) {
         return;
      }

      if (state) {
         state = false;
      } else {
         state = true;
      }

      msg = String.format("Element Enabled => '%s' was expecting Enabled => '%s'!", eleState, state);
      reporter.Assert(msg, eleState, state);
   }

}
