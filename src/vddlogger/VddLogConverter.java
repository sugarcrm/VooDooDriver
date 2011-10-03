package vddlogger;

import java.io.File;
import java.util.ArrayList;

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
			
			if (!suiteFD.exists()) {
				System.out.printf("(!)Error: Failed to find suite file: '%s'!\n\n");
				System.exit(3);
			}
			
		}
		
		if (suitedir != null) {
			File suiteDir = new File(suitedir);
			
			if (!suiteDir.exists()) {
				System.out.printf("(!)Error: Failed to find suite directory: '%s'!\n\n");
				System.exit(4);
			}
			
			System.out.printf("(*)Opening suitedir: '%s'.\n", suitedir);
			handleSuiteDir(suitedir);
			
		}
		
		
	}

	public static void handleSuiteDir(String dir) {
		File dirFD = new File(dir);
		ArrayList<String> suiteFiles = new ArrayList<String>();
		String[] files = null;

		files = dirFD.list();
		for (int i = 0; i <= files.length -1; i++) {
			String tmp = files[i];
			tmp = tmp.toLowerCase();
			if (!tmp.endsWith("xml")) {
				continue;
			}
			
			System.out.printf("(*)Found Suite File: '%s'.\n", files[i]);
			suiteFiles.add(files[i]);
		}
		
		
		
		
	}
	
	public static void handleSuiteFile(String filename) {
		
	}
	
	public static void PrintHelp() {
		String msg = "This is a help message!";
		System.out.printf("%s\n", msg);
	}
	
}
