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

import org.openqa.selenium.WebDriverException;
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
					str_res = "failed";
				}
				else if (temp.compareToIgnoreCase("true") == 0){
					temp = "true";
					str_res = "passed";
					done = true;
					break;
				}
				else if (temp.compareToIgnoreCase("undefined") == 0){
					str_res = "undefined";
					temp = null;
					undef_count ++;
				}else{
					System.out.printf("(W)Sugarwait failed: unknown result: "+temp+"!\n");
					done = false;
					break;
				}
				
				if (undef_count > 30){
					System.out.printf("(W)Sugarwait failed: Can't find SUGAR object after 30 tries! \n");
					done = false;
					break;
				}
				
				Thread.sleep(50);
			}
			t2 = System.currentTimeMillis();
			System.out.printf("(*)Sugarwait finished. Result: "+str_res+", Total time: "+(t2-t1)+"\n");
			
		} catch (InterruptedException e) {
			System.out.printf("(!)Problem in calling sugarwait, thread cannot sleep. \n");
			e.printStackTrace();
		}catch (WebDriverException e){
			System.out.printf("(W)Sugarwait plugin session has no driver. \n");
			return 0;
		}catch (Exception e){
			System.out.printf("(!)Unknown error calling sugarwait. \n");
			e.printStackTrace();
		}
		
		if (done){
			result = 0;
		}else{
			result = -1;
		}
		return result;
	}

}
