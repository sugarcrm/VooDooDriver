package vddlogger;

import java.io.File;

public class VddLogConverter {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		VddLogCmdOpts opts = null;
		VddLogCmdParser parser = null;
		String suitedir = null;
		String suitefile = null;
		
		parser = new VddLogCmdParser(args);
		opts = parser.parse();

		if (opts.get("help") != null) {
			PrintHelp();
			System.exit(0);
		}
		
		suitefile = opts.get("suitefile");
		suitedir = opts.get("suitedir");
		if (suitefile == null && suitedir == null) {
			System.out.printf("(!)Error: Missing needed command line options: --suitefile or --suitedir!\n\n\n");
			PrintHelp();
			System.exit(2);
		}
		
		if (suitefile != null) {
			File suiteFD = new File(suitefile);
		}
		
		if (suitedir != null) {
			File suiteDir = new File(suitedir);
		}
		
		
	}

	public static void handleSuiteDir(String dir) {
		
	}
	
	public static void handleSuiteFile(String filename) {
		
	}
	
	public static void PrintHelp() {
		String msg = "This is a help message!";
		System.out.printf("%s\n", msg);
	}
	
}
