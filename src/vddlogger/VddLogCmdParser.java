package vddlogger;

public class VddLogCmdParser {

	private String suiteDir = "";
	private String suiteFile = "";
	private String help = null;
	private VddLogCmdOpts opts = null;
	
	
	public VddLogCmdParser (String[] args) {
		opts = new VddLogCmdOpts();
		
		for (int i = 0; i <= args.length -1; i++) {
			if (args[i].contains("--suitedir")) {
				this.suiteDir = args[i];
				this.suiteDir = this.suiteDir.replace("--suitedir=", "");
			} else if (args[i].contains("--suitefile")) {
				this.suiteFile = args[i];
				this.suiteFile = this.suiteFile.replace("--suitefile=", "");
			} else if (args[i].contains("--help")) {
				this.help = "true";
			}
		}
		
		opts.put("suitefile", this.suiteFile);
		opts.put("suitedir", this.suiteDir);
		opts.put("help", this.help);
	}
	
	public VddLogCmdOpts parse() {
		return this.opts;
	}

}
