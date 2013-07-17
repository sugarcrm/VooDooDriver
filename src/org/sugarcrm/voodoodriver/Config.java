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

import java.io.File;
import java.io.InputStream;
import java.io.FileInputStream;
import java.util.ArrayList;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.commons.io.FilenameUtils;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;


/**
 * Actions to take during command line argument processing.
 *
 * @author Jon duSaint
 */

interface VooDooCmdLineArgAction {

   /**
    * Handle a command line argument.
    *
    * @param name     the name of the argument
    * @param arg      the argument's value, if any
    * @param isArray  whether this argument can appear multiple times
    */

   public void handle(String name, String arg, boolean isArray);
}


/**
 * VooDooDriver command line option/argument processing
 *
 * @author trampus
 * @author Jon duSaint
 */

public class Config {

   /**
    * File containing the canonical list of VooDooDriver command line options.
    */
   private String validOptsFile = "options.xml";

   /**
    * Data structure created from validOptsFile.
    */
   private VDDHash validOpts;

   /**
    * Command line options after processing command line.
    */
   private VDDHash options = null;


   /**
    * Process a file or directory command line argument.
    */

   private class VooDooCmdLineFileArgAction implements VooDooCmdLineArgAction {
      private boolean isDir;

      /**
       * Create a VooDooCmdLineFileArgAction, differentiating file and dir.
       *
       * @param action  either file or dir
       */

      public VooDooCmdLineFileArgAction(String action) {
         isDir = action.equals("dir");
      }

      public void handle(String name, String arg, boolean isArray) {
         if (arg.isEmpty()) {
            System.err.printf("(!)Missing argument to --%s\n", name);
            System.exit(1);
         }

         File f = new File(FilenameUtils.separatorsToSystem(arg));
         if (isDir) {
            if (f.exists()) {
               if (!f.isDirectory()) {
                  System.err.printf("(!)Directory '%s' for --%s is a file\n",
                                    arg, name);
                  System.exit(1);
               }
            }
         } else if (!f.exists()) {
            System.err.printf("(!)Failed to find --%s file '%s'\n", name, arg);
            System.exit(1);
         }

         if (isArray) {
            /*
             * Once all the code that uses this is fixed, this will be
             * stored as ArrayList<File>.
             */
            if (!options.containsKey(name)) {
               options.put(name, new ArrayList<String>());
            }
            @SuppressWarnings("unchecked")
               ArrayList<String> a = (ArrayList<String>)options.get(name);
            a.add(arg);
         } else {
            options.put(name, arg);
         }
      }
   }


   /**
    * Process a string command line argument.
    */

   private class VooDooCmdLineStringAction implements VooDooCmdLineArgAction {
      public void handle(String name, String arg, boolean isArray) {
         if (arg.isEmpty()) {
            System.err.printf("(!)Missing argument to --%s\n", name);
            System.exit(1);
         }
         if (isArray) {
            if (!options.containsKey(name)) {
               options.put(name, new ArrayList<String>());
            }
            @SuppressWarnings("unchecked")
               ArrayList<String> a = (ArrayList<String>)options.get(name);
            a.add(arg);
         } else {
            options.put(name, arg);
         }
      }
   }


   /**
    * Process an integer command line argument.
    */

   private class VooDooCmdLineIntegerAction implements VooDooCmdLineArgAction {
      public void handle(String name, String arg, boolean isArray) {
         int i = 0;
         try {
            i = Integer.valueOf(arg);
         } catch (java.lang.NumberFormatException e) {
            System.err.printf("(!)Invalid argument to --%s '%s'\n", name, arg);
            System.exit(1);
         }

         if (isArray) {
            if (!options.containsKey(name)) {
               options.put(name, new ArrayList<Integer>());
            }
            @SuppressWarnings("unchecked")
               ArrayList<Integer> a = (ArrayList<Integer>)options.get(name);
            a.add(i);
         } else {
            options.put(name, i);
         }
      }
   }


   /**
    * Process a key-value pair command line argument.
    */

   private class VooDooCmdLineKVPAction implements VooDooCmdLineArgAction {
      public void handle(String name, String arg, boolean isArray) {
         if (!options.containsKey(name)) {
            options.put(name, new VDDHash());
         }

         String[] kvp = arg.split("::");
         if (kvp.length != 2) {
            System.err.printf("(!)Invalid argument to --%s '%s'\n", name, arg);
            System.exit(1);
         }
         String key = kvp[0];
         String value = kvp[1];

         /* Hack: gvars need "global." prepended. */
         if (name.equals("gvar")) {
            key = "global." + key;
         }

         ((VDDHash)options.get(name)).put(key, value);
      }
   }


   /**
    * Process a boolean command line argument (a switch).
    */

   private class VooDooCmdLineBooleanAction implements VooDooCmdLineArgAction {
      public void handle(String name, String arg, boolean isArray) {
         options.put(name, true);
      }
   }


   /**
    * Load valid command line options from options.xml.
    *
    * <p>XXX: Although options.xml is not currently validated, it
    * really should be.  This routine assumes the file is valid and
    * conforms to the block comment at the top of the file.</p>
    */

   private void loadCommandLineOpts() {
      InputStream optFile = null;
      String name, jar;
      DocumentBuilder db;
      Document doc = null;
      NodeList optNodes;

      /* Get an InputStream for options.xml */
      name = getClass().getName().replace('.', '/');
      jar = getClass().getResource("/" + name + ".class").toString();

      if (jar.startsWith("jar:")) {
         optFile = getClass().getResourceAsStream(validOptsFile);
      } else {
         File f = new File(getClass().getResource(validOptsFile).getFile());
         try {
            optFile = new FileInputStream(f);
         } catch (java.io.FileNotFoundException e) {
            System.err.println("(!)Failed to find options.xml: " + e);
            System.exit(1);
         }
      }

      /* Parse the XML */
      try {
         db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
         doc = db.parse(optFile);
      } catch (javax.xml.parsers.ParserConfigurationException e) {
         System.err.println("(!)Trouble loading XML parser: " + e);
         System.exit(1);
      } catch (java.io.IOException e) {
         System.err.println("(!)Error reading options.xml: " + e);
      } catch (org.xml.sax.SAXException e) {
         System.err.println("(!)options.xml malformed? " + e);
         System.exit(1);
      }

      optNodes = doc.getDocumentElement().getElementsByTagName("option");

      /* Stick all the options into a hash. */
      this.validOpts = new VDDHash();

      for (int k = 0; k < optNodes.getLength(); k++) {
         NamedNodeMap e = optNodes.item(k).getAttributes();
         VDDHash opt = new VDDHash();
         VooDooCmdLineArgAction action = null;
         String arg = e.getNamedItem("arg").getNodeValue();
         boolean isArray = arg.endsWith("s");

         if (arg.startsWith("file") || arg.startsWith("dir")) {
            action = new VooDooCmdLineFileArgAction(arg);
         } else if (arg.startsWith("string")) {
            action = new VooDooCmdLineStringAction();
         } else if (arg.startsWith("integer")) {
            action = new VooDooCmdLineIntegerAction();
         } else if (arg.equals("kvp")) {
            action = new VooDooCmdLineKVPAction();
         } else if (arg.equals("none")) {
            action = new VooDooCmdLineBooleanAction();
         } else {
            System.err.println("(!)Invalid arg: " + arg);
         }

         opt.put("action", action);
         opt.put("isArray", isArray);
         opt.put("help", e.getNamedItem("help").getNodeValue());
         this.validOpts.put(e.getNamedItem("name").getNodeValue(), opt);
      }
   }


   /**
    * Determine a command line argument's name.
    *
    * This involves stripping off the leading '--' and any trailing
    * '=' with everything after.
    *
    * @param arg  the command line argument
    * @return the command line argument's name
    */

   private String getArgName(String arg) {
      return arg.replaceAll("^--([^=]+).*$", "$1");
   }


   /**
    * Determine a command line argument's value.
    *
    * The value is everything after the first '='.  If there is no
    * '=', an empty string is returned.
    *
    * @param arg  the command line argument
    * @return the command line argument's value
    */

   private String getArgValue(String arg) {
      int i = arg.indexOf('=');
      return (i < 0) ? "" : arg.substring(i + 1);
   }


   /**
    * Word wrap a string, starting at the specified column.
    *    
    * @param s    the string to word wrap
    * @param col  the number of spaces to insert after each newline
    * @return  a nicely word-wrapped and padded string
    */

   private String wrap (String s, int col) {
      if (col > 50) {
         col = 50;
      }
      String spc = ((col > 0) ?
                    String.format("%" + String.valueOf(col) + "s", "") :
                    "");
      s = s.replaceAll("(.{" + String.valueOf(68 - col) + "}\\S*)\\s+", "$1\n");
      s = s.replaceAll("\n", "\n" + spc);
      return s.trim();
   }


   /**
    * Print VooDooDriver's help message.
    *
    * The option strings and their help text come from options.txt.
    * This routine merely formats them nicely and prints some external
    * information.
    */

   private void printHelp() {
      System.out.println("Usage: java -jar VooDooDriver.jar [options]\n");
      System.out.println("Options:");

      String[] keys = this.validOpts.keySet().toArray(new String[0]);
      java.util.Arrays.sort(keys);
      int col = 0;
      /* Once through to size the options, once through to print them. Sigh. */
      for (String key: keys) {
         if (key.length() > col) {
            col = key.length();
         }
      }
      for (String key: keys) {
         String h = wrap((String)((VDDHash)this.validOpts.get(key)).get("help"),
                         col + 5); // Initial space, "--", and two spaces after
         System.out.printf(" %" + String.valueOf(col + 2) + "s  %s\n",
                           "--" + key, h);
      }

      System.out.println("\n" +
                         wrap("Either --suite or --test is required.  " +
                              "'=' joins options and their arguments. " +
                              "'::' joins key-value pairs.", 0));
      System.out.println("\nExamples:\n");
      System.out.println("   " + wrap("java -jar VooDooDriver.jar " +
                                      "--browser=firefox " +
                                      "--gvar=scriptsdir::/home/me/soda " +
                                      "--suite=testSuite.xml", 8));
      System.out.println("\n" + wrap("Runs testSuite.xml on firefox using " +
                                     "/home/me/soda as the internal global " +
                                     "variable 'scriptsdir'.", 0));
   }


   /**
    * Print VooDooDriver's version information.
    */

   private void printVersion() {
      VersionInfo v = new VersionInfo();
      System.out.println("(*)VooDooDriver Version: " + v.getVDDVersion() +
                         ", " + v.getVDDCommit());
   }


   /**
    * Constructor for Config.
    */

   public Config() {
      loadCommandLineOpts();
   }


   /**
    * Parse command line options using our option specification.
    *
    * Both --help and --version are handled here.  The values for all
    * other command line options are stored in an internal data
    * structure that can be accessed with getOptions().
    *
    * @param args  arguments from the command line
    */

   public void parse(String[] args) {
      this.options = new VDDHash();

      for (String arg: args) {
         VDDHash opt;
         String argName = getArgName(arg);
         String argValue = getArgValue(arg);

         opt = (VDDHash)this.validOpts.get(argName);
         if (opt == null) {
            System.err.println("(!)Error: Unknown command line argument \"" +
                               argName + "\"");
            System.exit(1);
         }

         VooDooCmdLineArgAction action =
            (VooDooCmdLineArgAction)opt.get("action");
         action.handle(argName, argValue, (Boolean)opt.get("isArray"));
      }


      if (options.containsKey("help") &&
          (Boolean)options.get("help")) {
         printHelp();
         System.exit(0);
      } else if (options.containsKey("version") &&
                 (Boolean)options.get("version")) {
         printVersion();
         System.exit(0);
      }
   }


   /**
    * Return the parsed command line arguments.
    *
    * @return {@link VDDHash}
    */
   public VDDHash getOptions() {
      return this.options;
   }
}
