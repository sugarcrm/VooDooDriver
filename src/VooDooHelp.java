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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;


public class VooDooHelp {

	public static String HELP_MESSAGE_FILE = "helpmessage.txt";
	
	public VooDooHelp() {
		
	}
	
	public void printHelp() {
		InputStream stream = null;
		String className = this.getClass().getName().replace('.', '/');
		String classJar =  this.getClass().getResource("/" + className + ".class").toString();
		
		try {
			if (classJar.startsWith("jar:")) {
				stream = getClass().getResourceAsStream(HELP_MESSAGE_FILE);
			} else {
				File header_fd = new File(getClass().getResource(HELP_MESSAGE_FILE).getFile());
				stream = new FileInputStream(header_fd);
			}
			
			InputStreamReader in = new InputStreamReader(stream);
			BufferedReader br = new BufferedReader(in);
			String line = "";
			String msg = "";
			while ((line = br.readLine()) != null) {
				msg += line;
				msg += "\n";
			}
			
			System.out.printf("%s\n", msg);
		} catch (Exception exp) {
			exp.printStackTrace();
		}
	}
	
}
