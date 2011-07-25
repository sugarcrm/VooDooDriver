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

import org.openqa.selenium.ie.InternetExplorerDriver;

public class SodaIE extends SodaBrowser implements SodaBrowserInterface {
	
	public SodaIE() {
		
	}
	
	public void setDownloadDirectory(String dir) {
		
	}
	
	public void newBrowser() {
		this.setDriver(new InternetExplorerDriver());
		this.setBrowserState(false);
	}
	
	public void forceClose() {
		SodaOSInfo.killProcesses(SodaOSInfo.getProcessIDs("iexplorer"));
		this.setBrowserClosed();
	}
	
	public void alertHack(boolean alert) {
		String alert_js =  "var old_alert = window.alert;\n" +
			"var old_confirm = window.confirm;\n"+
			"window.alert = function(){return " + alert + ";};\n"+
			"window.confirm = function(){return " + alert + ";};\n"+
			"window.onbeforeunload = null;\n"+
			"var result = 0;\nresult;\n";
		
		this.executeJS(alert_js, null);
	}
	
	
}
