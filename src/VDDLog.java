/*
 * Copyright 2012 SugarCRM Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * Please see the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;

/**
 * Logging class for VooDooDriver.
 *
 * This class is a drop-in replacement for the {@link PrintStream}
 * used by {@link System.out} and {@link System.err}.  Rather than
 * printing to the console only, this class prints to the console and,
 * once opened, to a log file.  Prior to the log file being opened,
 * output is captured into a queue that is flushed to the log file
 * once it is opened.  The purpose here is to ensure that all output
 * from VooDooDriver is captured.  This will greatly assist with
 * debugging.
 *
 * @author Jon duSaint
 */

class VDDLog extends PrintStream {

   /**
    * The original {@link PrintStream} that this object is replacing.
    */

   protected PrintStream out;

   /**
    * Stream for the log file.
    */

   protected FileOutputStream log;

   /**
    * The queue used to store pre-logfile output.
    */

   protected ArrayList<byte[]> queue;


   /**
    * Create a VDDLog object with the specified {@link PrintStream}.
    *
    * @param out  either {@link System.out} or {@link System.err}
    */

   public VDDLog(PrintStream out) {
      super(out);
      queue = new ArrayList<byte[]>();
   }


   /**
    * Open the log file and flush the queue.
    *
    * @param log  the log file
    */

   public void openLog(FileOutputStream log) {
      this.log = log;

      /*
       * Now that the log is open, flush the queue into it.
       */
      try {
         for (byte[] msg: this.queue) {
            this.log.write(msg, 0, msg.length);
         }
         this.log.flush();
      } catch (java.io.IOException e) {
         this.setError();
      }
      this.queue.clear();
   }


   /**
    * Close the log file.
    */

   public void closeLog() {
      try {
         this.log.close();
      } catch (java.io.IOException e) {
         this.setError();
      }
   }


   /**
    * Write {@code len} bytes from the specified byte array.
    *
    * @param buf  data to write
    * @param off  start offset in the data
    * @param len  number of bytes to write
    * @throws java.io.IOException
    */

   public void write(byte buf[], int off, int len) {
      super.write(buf, off, len);

      if (this.log == null) {
         this.queue.add(java.util.Arrays.copyOfRange(buf, off, off + len));
      } else {
         try {
            synchronized (this.log) {
               this.log.write(buf, off, len);
            }
         } catch (java.io.IOException e) {
            this.setError();
         }
      }
   }


   /**
    * Flush this output sream.
    */

   public void flush() {
      super.flush();
      if (this.log != null) {
         try {
            this.log.flush();
         } catch (java.io.IOException e) {
            this.setError();
         }
      }
   }
}
