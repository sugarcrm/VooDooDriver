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

import voodoodriver.SodaBrowser;
import voodoodriver.VDDPluginInterface;

/**
 * When this plugin is enabled, it causes the browser to wait on all ajax requests, then continue when 
 * it's finished
 * @author Lehan
 *
 */
public class SugarwaitPlugin implements VDDPluginInterface {

	
	/**
	 * function called by SodaEvent to execute sugarwait
	 * 
	 * @param args - additional arguments, probably not used.
	 * @param browser - the current browser instance
	 * @param element - a WebElement this function has control to. probably not used either
	 */
	@Override
	public int execute(String[] args, SodaBrowser browser, WebElement element) {
		
		
		return 0;
	}
	
	/**
	 * Executes the given script in the browser
	 * 
	 * @param jscript - javascript to get executed
	 * @param browser - the WebDriver browser to execute the script in
	 * @return -1 on the error else the javascript result.
	 */
	private String executeScript(String jscript, SodaBrowser browser){
		String result = "";
		
		
		return result;
	}

}
