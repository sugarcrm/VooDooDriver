package org.sugarcrm.vddlogger;

public class VddLogCmdParser {

   private VddLogCmdOpts opts = null;


   public VddLogCmdParser (String[] args) {
      this.opts = new VddLogCmdOpts();

      for (int i = 0; i <= args.length -1; i++) {
         if (args[i].startsWith("--suitedir")) {
            this.opts.put("suitedir", args[i].replace("--suitedir=", ""));
         } else if (args[i].startsWith("--suitefile")) {
            this.opts.put("suitefile", args[i].replace("--suitefile=", ""));
         } else if (args[i].equals("--help")) {
            this.opts.put("help", "true");
         }
      }
   }

   public VddLogCmdOpts parse() {
      return this.opts;
   }

}
