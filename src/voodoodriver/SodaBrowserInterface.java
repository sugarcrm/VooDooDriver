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

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * Interface needed to add supported browsers to VooDooDriver.
 * 
 * @author trampus
 *
 */
public interface SodaBrowserInterface {

	public void newBrowser();
	
	public void refresh();
	
	public void back();
	
	public void forward();
	
	public void close();
	
	public void url(String url);
	
	public WebDriver getDriver();
	
	public WebElement findElement(By by, int retryTime);
	
	public void setDriver(WebDriver driver);
	
	public String generateUIEvent(UIEvents type);
	
	public void alertHack(boolean alert);
	
	public String getPageSource();
	
	public boolean assertPage();
	
	public boolean Assert(String value);
	
	public boolean AssertNot(String value);
	
	public void setProfile(String profile);
	
	public void forceClose();
	
	public String getProfile();
	
	public boolean isClosed();
	
	public void setBrowserState(boolean state);
	
	public void setDownloadDirectory(String dir);
	
	public void setAssertPageFile(String filename, SodaReporter reporter);
	
	public String getAssertPageFile();
	
}
