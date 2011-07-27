/*
  Copyright (c) 2011, SugarCRM, Inc.
  All rights reserved.

  Redistribution and use in source and binary forms, with or without
  modification, are permitted provided that the following conditions are met:
  * Redistributions of source code must retain the above copyright
  notice, this list of conditions and the following disclaimer.
  * Redistributions in binary form must reproduce the above copyright
  notice, this list of conditions and the following disclaimer in the
  documentation and/or other materials provided with the distribution.
  * Neither the name of SugarCRM, Inc. nor the
  names of its contributors may be used to endorse or promote products
  derived from this software without specific prior written permission.

  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
  AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
  IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
  ARE DISCLAIMED. IN NO EVENT SHALL SugarCRM, Inc. BE LIABLE FOR ANY
  DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
  (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
  ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package javaplugins;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.JavascriptExecutor;

import voodoodriver.SodaBrowser;
import voodoodriver.VDDPluginInterface;
import java.lang.System;

/**
 * When this plugin is enabled, it causes the browser to wait on all ajax requests, then continue when 
 * it's finished
 * @author Lehan
 *
 */
public class SugarwaitPlugin implements VDDPluginInterface {
	
	Boolean done = false;
	int result = 0, undef_count = 0;
	long t1, t2;
	String jscript = "if(typeof(SUGAR) != 'undefined' && SUGAR.util && !SUGAR.util.ajaxCallInProgress())"+
					"return 'true';" + 
					"else if (typeof(SUGAR) == 'undefined')" +
					"return 'undefined';" +
					"else return 'false';";
	String str_res = "";
	WebDriver driver = null;
	
	/**
	 * Constructor
	 */
	public SugarwaitPlugin(){
		
	}
	
	/**
	 * function called by SodaEvent to execute sugarwait
	 * 
	 * @param args - additional arguments, probably not used.
	 * @param browser - the current browser instance
	 * @param element - a WebElement this function has control to. probably not used either
	 */
	@Override
	public int execute(String[] args, SodaBrowser browser, WebElement element) {
		//pretty important: get the WebDriver from SodaBrowser browser
		//String storing javascript execution result
		String temp = "";
		
		System.out.printf("(*)Sugarwait starting... \n");
		t1 = System.currentTimeMillis();	
		try {
			//this is to make sure the browser has enough time to start the ajax call. probably don't need it
			Thread.sleep(100);
			//maximum 15 second wait time
			for (int i = 0; i < 31; i ++){
				/**
				 * add wait for page load?
				 * WebDriver is designed to automatically wait for page loads before actions are executed, so may 
				 * not need in this case
				 */
				//execute script
				temp = (String)browser.executeJS(jscript, null);
				
				if (temp.compareToIgnoreCase("false") == 0){
					temp = "false";
					str_res = "false";
				}
				else if (temp.compareToIgnoreCase("true") == 0){
					temp = "true";
					str_res = "true";
					done = true;
					break;
				}
				else if (temp.compareToIgnoreCase("undefined") == 0){
					str_res = "undefined";
					temp = null;
					undef_count ++;
				}else{
					System.out.printf("(W)Sugarwait failed: unknown result: "+temp+"!\n");
				}
				
				if (undef_count > 30){
					System.out.printf("(W)Sugarwait failed: Can't find SUGAR object after 30 tries! \n");
					done = false;
					break;
				}
				
				Thread.sleep(500);
			}
			t2 = System.currentTimeMillis();
			System.out.printf("(*)Sugarwait finished. Result: "+str_res+", Total time: "+(t2-t1)+"\n");
			
		} catch (InterruptedException e) {
			System.out.printf("(!)Problem in calling sugarwait, thread cannot sleep \n");
			e.printStackTrace();
		} catch (Exception e){
			System.out.printf("(!)Error in calling sugarwait \n");
			e.printStackTrace();
		}
		
		if (done){
			result = 0;
		}else{
			result = -1;
		}
		return result;
	}
	
	/**
	 * Executes the given script in the browser
	 * 
	 * @param jscript - javascript to get executed
	 * @param browser - the WebDriver browser to execute the script in
	 * @return -1 on the error else the javascript result.
	 */
	private String executeScript(String jscript, WebDriver browser){
		String result = "";
		
		if (jscript.length() > 0){
			//might need to escape the javascript
			String js = "current_browser_id = 0;" +
					"if (current_browser_id > -1){" +
						"var target = this.selectWindow();" +
						"var browser = target.getBrowser();" +
						"var content = target.content;" +
						"var doc = browser.contentDocument;" +
						"var d = doc.createElement(\"script\");" +
						"var tmp = null;" +
						"" +
						"tmp = doc.getElementById(\"Sodahack\");" +
						"if (tmp != null) {" +
							"doc.body.removeChild(tmp);" +
							"}" +
						"" +
						"d.setAttribute(\"id\", \"Sodahack\");" +
						"var src = \"document.soda_js_result = (function(){"+jscript+"})()\";" +
						"d.innerHTML = src;" +
						"if(typeof(doc) != \"undefined\" && typeof(doc.body) != \"undefined\")" +
						"{" +
							"doc.body.appendChild(d);" +
							"result = doc.soda_js_result;" +
						"}else{" +
							"result = \"No Document Object to use\";" +
						"}" +
					"}else {" +
						"result = \"No Browser to use\";" +
					"}" +
					"return result;";
			//create a javascriptExecutor for WebDriver
			JavascriptExecutor jse = (JavascriptExecutor) driver;
			//actually execute the js in browser
			result = (String)jse.executeScript(js);		
		}else{
			result = "No script passed";
		}
		
		return result;
	}

}
