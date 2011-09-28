package vddlogger;

public class VddLogConverter {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		VddLogCmdOpts opts = null;
		VddLogCmdParser parser = null;
		
		parser = new VddLogCmdParser(args);
		opts = parser.parse();

		if (opts.containsKey("help")) {
			PrintHelp();
			System.exit(0);
		}
		
	}

	public static void PrintHelp() {
		String msg = "This is a help message!";
		System.out.printf("%s\n", msg);
	}
	
}
