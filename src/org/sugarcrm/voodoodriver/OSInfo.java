/*
 * Copyright 2011-2013 SugarCRM Inc.
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

package org.sugarcrm.voodoodriver;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OSInfo {

   /**
    * Our <code>OSInfo</code> object.
    */

   private static OSInfo osinfo;


   /**
    * Initialize our <code>OSInfo</code> object.
    */

   static {
      osinfo = new OSInfo();
   }


   /**
    * Operating system names.
    */

   private enum OS { WINDOWS, UNIX; }

   /**
    * Current operating system.
    */

   private OS os;


   /**
    * Instantiate an OSInfo object.
    */

   private OSInfo() {
      String osname = System.getProperty("os.name").toLowerCase();
      if (osname.contains("win")) {
         this.os = OS.WINDOWS;
      } else if (osname.contains("linux") || osname.contains("mac")) {
         this.os = OS.UNIX;
      } else {
         System.err.println("(!)Unknown operating system '" + osname + "'");
      }
   }


   /**
    * Read a line from a <code>BufferedReader</code>
    *
    * @param r  the <code>BufferedReader</code>
    * @return the next line or <code>null</code>
    */

   private String readLine(BufferedReader r) {
      try {
         return r.readLine();
      } catch (IOException e) {
      }
      return null;
   }


   /**
    * Get the entire output of a command.
    *
    * @param cmd  the command
    * @return the output
    */

   private ArrayList<String> exec(String cmd[]) {
      ArrayList<String> lines = new ArrayList<String>();
      BufferedReader r;

      try {
         r = new BufferedReader(new InputStreamReader(Runtime.getRuntime()
                                                      .exec(cmd)
                                                      .getInputStream()));
      } catch (IOException e) {
         System.err.println("(!)Failed to exec command '" + cmd[0] + "': " + e);
         return lines;
      }

      String line;
      while ((line = readLine(r)) != null) {
         lines.add(line);
      }

      return lines;
   }


   /**
    * Get a list of process ids with the specified process name.
    *
    * @param process  process name
    * @return a list of process ids
    */

   private ArrayList<String> unixPids(String process) {
      ArrayList<String> pids = new ArrayList<String>();
      String cmd[] = {"ps", "x", "-o", "pid,comm"};
      Pattern p = Pattern.compile("^\\s*(\\d+)\\s+.*" + process);

      for (String line: exec(cmd)) {
         Matcher m = p.matcher(line);
         if (m.find()) {
            pids.add(m.group(1));
         }
      }

      return pids;
   }


   /**
    * Get a list of process ids with the specified process name.
    *
    * @param process  process name
    * @return a list of process ids
    */

   private ArrayList<String> windPids(String process) {
      ArrayList<String> pids = new ArrayList<String>();
      String[] cmd = {"tasklist.exe", "/FO", "CSV", "/NH"};

      for (String line: exec(cmd)) {
         String data[] = line.split(",", 3);

         if (data[0].contains(process)) {
            pids.add(data[1].replaceAll("^\"(.+)\"$", "$1"));
         }
      }

      return pids;
   }


   /**
    * Kill a process
    *
    * <p>Kill a process using the provided kill command.  This method
    * has functionality common to Unix and Windows.</p>
    *
    * @param cmd  OS-specific kill command
    * @return whether the process was successfully killed
    */

   private boolean killProcess(String[] killCmd) {
      Process p = null;

      try {
         p = Runtime.getRuntime().exec(killCmd);
      } catch (IOException e) {
         return false;
      }

      if (os == OS.WINDOWS) {
         /* Work around a Windows wait issue... */
         try {
            System.out.println("(*)Sleeping 5 seconds...");
            Thread.sleep(5000);
         } catch (InterruptedException e) {
         }
      }

      return p.exitValue() == 0;
   }


   /**
    * Kill a process with the specified name
    *
    * @param proc  the process name
    */

   private void killProcess1(String proc) {
      for (String pid: (os == OS.UNIX ? unixPids(proc) : windPids(proc))) {
         String unixKill[] = {"kill", "-9", pid};
         String winKill[] = {"taskkill.exe", "/T", "/F", "/PID", pid};

         if (killProcess(os == OS.UNIX ? unixKill : winKill)) {
            System.out.println("(*)Killed process by pid " + pid);
         } else {
            System.out.println("(!)Failed to kill process by pid " + pid);
         }
      }
   }


   /**
    * Kill a process with the specified name
    *
    * @param proc  the process name
    */

   public static void killProcess(String proc) {
      osinfo.killProcess1(proc);
   }
}
