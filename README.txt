Voodoo Driver

Voodoo Driver is a web software testing automation framework.  It is coded in Java and based on Selenium WebDriver.  It is designed to allow non-programmers to write automated tests without having to learn to program.  It uses an simple XML-based syntax which closely mirrors the HTML elements with which it interacts.

Voodoo Driver is a replacement for the now-deprecated Soda testing framework.  https://github.com/sugarcrm/SODA

(*)Things that are different from Ruby Soda:
1.) The row soda element is no longer supported, you need to use "tr" now.  It just makes more sense.
2.) The cell soda element is no longer supported, you need to use "td" now.  It just makes more sense.
3.) <a href> links can not be found in a TR unless the TR owns the link, if the link is owned by a TD then
   you have to parent the link with the proper td.
4.) When using set for text fields in ruby soda, soda would clear the field of any existing text before 
   setting the new value.  VooDooDriver does not do this, as it will just append text to the current field.
5.)VooDooDriver will only allow you to find an element by one selector where Soda ruby would allow you more then one.  
6.)VooDooDriver now supports accessing elements using css selectors.  Example:
<button css="input[type=button][value='Search']" />
7.)VooDooDriver supports plugins.  Please see the Plugin.txt doc.
8.)VooDooDriver supports storing off dragable HTML elements.  This can be done by using the "save" attribute for supported soda
elements.  Example:
   <div id="foo" save="my-div" />, this stores the div element under the ref "my-div", which can only be used by the <dnd>
   soda command.  See Drag'n Drop example for more info.
9.)VooDooDriver now supports the <execute> command.  See Notes for more info on this.
10.)assertPage: The assertPage code from Soda is no longer hardcoded into the product.  Now you have to use a command line
option called: --assertpagefile=<some file>  See more on assertPage in the Notes section of this readme.
11.)The <javascript> command now supports loading external js files.  See the Notes section for more info.
12.)<select> now supports assert & assertnot, assert(not) checks that a given option is selected or not.
13.)<radio> now supports checked="true/false", for asserting if the control is checked or not.
14.)<select> setreal: see the Notes section about this.
15.)Finding elements using regex's is not supported by VooDooDriver at all.  You can however use css selectors for doing just this.
16.)VooDooDriver now supports java class plugins.  See JavaPlugins.txt for more information.
17.)--savehtml: The savehtml feature of VDD has changed from how SODA performed saves.  SODA used to preform saves every time
	an assert would fail and a new html saved page would be written to disk.  In the case of VDD if there are more then one failed
	asserts on a given page and the page source has not changed then only one html file will be written to disk.  This is to
	save disk space over all and improve performance.
	
Known Issues:
Windows:
(*)http://code.google.com/p/selenium/issues/detail?id=2064
(*)http://code.google.com/p/selenium/issues/detail?id=1884

Notes:
(*)Browser Closing:
   Currently if you are using the --test command line option the browser you are using will be closed
   for you after each test is run even if you do now call the SODA command <browser action="close" />.  
   This is not the case of SODA suites.  SODA suites keep the browser open until the end of the suite.
   
(*)Execute Command:
   The new command <execute> is for executing any program with arguments from inside a VooDooDriver test.
   The command being executed !MUST! return a proper exit integer value.  This follows the Unix system of return
   values so success is always a zero: 0, and anything other then success is non=zero.
   
   The execute command can have childern elements but they con only be of type <arg>, type arg is the list of
   arguments to be passed to the execute command, where the first argument in the list is always going to be 
   the command to execute, and all following <arg>'s are the parameters to said command.
   
   Example:  Calling a simple bash script:
   <execute>
      <arg>bash</arg>
      <arg>-c</arg>
      <arg>/Users/me/foobar.bash</arg>
   </execute>
   
   Example: Calling a Unix command:
   <execute>
      <arg>ls</arg>
      <arg>-la</arg>
      <arg>/Users/me</arg>
   </execute>
   
(*)assertPage:
   Old soda used to have hard coded values for asserting if a page contained errors or not.  Since I wanted this project to not have things like this hard coded I have added the --assertpagefile command line option which take an XML file containing things regex's that you want to either ignore or assert an error on after each VooDooDriver event.
   
   The ignore options tell VooDooDriver to no cause an assert of the values in the page exist.
   The checks options tell VooDooDriver which errors in the page you want to report on.
   
   Warning: This can make your code run slow as a check is done for every regex after each event is fired in VooDooDriver.
   So the more options you add the slower things are going to run.
   
   Example:
   <soda>
   <ignore>
      <regex>Expiration Notice:</regex>
      <regex>Notice: Your license expires</regex>
      <regex>Warning: Please upgrade</regex>
      <regex>/(Fatal|Error): Your license expired|/</regex>
      <regex>isError</regex>
      <regex>errors</regex>
      <regex>ErrorLink</regex>
      <regex>Warning: Your email settings are not configured to send email</regex>
      <regex>Warning: Missing username and password</regex>
      <regex>Warning: You are modifying your automatic</regex>
      <regex>Warning: Auto import must be enabled when automatically creating cases</regex>
   </ignore>

   <checks>
      <regex>/(Notice:.*line.*)/i</regex>
      <regex>/(Warning:)/i</regex>
      <regex>/(.*Error:.*line.*)/i</regex>
      <regex>/(Error retrieving)/i</regex>
      <regex>/(SQL Error)/i</regex>
   </checks>
</soda> 
   
(*)Javascript:
   The <javascript> command now supports the "file" attribute which allows you to load a javascript file from the
   file system.  Now having your javascript code in the contents of the <javascript> command will generate a warning.
   It just seems to make more since having javascript code loaded from an external file, as using XML escape codes can
   make debugging a pain.
   
(*)Select: "setreal":   
   Soda would use the attribute "value" for selecting a value from a select list but it would use the displayed value
   in the select list, not the HTML element "value".  In order to keeps tests working across platforms this is still the
   case will VooDooDriver.  If you want to select an option based the the HTML element's real value you can now use the 
   "setreal" for selecting which value you want to click.
   
   
   