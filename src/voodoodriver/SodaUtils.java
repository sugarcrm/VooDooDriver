/*
Copyright 2011 SugarCRM Inc.

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

package voodoodriver;

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

import javax.imageio.ImageIO;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;

/**
 * A simple class for housing one off needed util functions, that can
 * all be called statically.
 * 
 * @author trampus
 *
 */
public class SodaUtils {
	
	/**
	 * Creates an MD5 sum of your string data.
	 * 
	 * @param data	a string that you want to get the MD5 sum of.
	 * 
	 * @return The MD5 sum of your data string.
	 */
	public static String MD5(String data) {
		String res = "";
		int len = 0;
		
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			md.update(data.getBytes());
			byte[] bytes = md.digest();
			
			len = bytes.length -1;
			for (int i = 0; i <= len; i++) {
				res += Integer.toString((bytes[i] & 0xff ) + 0x100, 16).substring( 1 );
			}
		} catch (Exception exp) {
			exp.printStackTrace();
		}
		
		return res;
	}

	public static void PrintSuiteReportToConsole(String suitename, ArrayList<SodaTestResults> list) {
		String linemarker = StringUtils.repeat("#", 80);
		
		System.out.printf("\n%s\n", linemarker);
		System.out.printf("# Suite Name: %s\n", suitename);
		System.out.printf("%s\n", linemarker);
		
	}
	
	/**
	 * Reads a text file into a String object.
	 * 
	 * @param filename 	The file to read in.
	 * @return Returns a String containing the contents of the text file.
	 * 
	 */
	public static String FileToStr(String filename) {
		String result = "";
		BufferedReader reader = null;
		
		filename = FilenameUtils.separatorsToSystem(filename);
		
		try {
			reader = new BufferedReader(new FileReader(filename));
			String line = "";
			while ((line = reader.readLine()) != null) {
				result = result.concat(line + "\n");
			}
			reader.close();
		} catch (Exception exp) {
			exp.printStackTrace();
			result = null;
		}
		
		return result;
	}

	/**
	 * Generates a string containing the run time based on a start & stop time.
	 * 
	 * @param starttime The start time.
	 * @param stoptime	The stop time.
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
	
	public static boolean takeScreenShot(String outputFile, SodaReporter reporter) {
		boolean result = false;
		Robot r = null;
		String msg = "";

		reporter.Log("Taking Screenshot.");
		
		try {
			
			File tmp = new File(outputFile);
			if (tmp.exists()) {
				msg = String.format("Existing screenshot file will be over written: '%s'.", outputFile);
				reporter.Warn(msg);
				tmp = null;
			}
			
			r = new Robot();
			Rectangle rec = new Rectangle();
			Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
			dim.setSize(dim);
			rec.setSize(dim);
			BufferedImage img = r.createScreenCapture(rec);
			ImageIO.write(img, "png", new File(outputFile));
			msg = String.format("Screenshot file: %s", outputFile);
			reporter.Log(msg);
		} catch (Exception exp) {
			reporter.ReportException(exp);
			result = false;
		}
		
		reporter.Log("Screenshot finished.");
		
		return result;
		
	}
}
