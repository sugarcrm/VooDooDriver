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

/*
   Needed C headers
*/

#include <stdlib.h>
#include <stdio.h>
#include <unistd.h>
#include <X11/Xlib.h>
#include <X11/X.h>
#include <X11/Xutil.h>
#include <pcre.h>
#include <string.h>
#include <X11/extensions/XTest.h>
#include <X11/keysymdef.h>

/*
getWinTitle:
   This function gets the window title for a given Window handle.

Input:
   dsp: The current XDisplay.
   win: The window handle that you want the title for.

Output:
   Returns a char* containing the window's title.
 
*/
char *getWinTitle(Display *dsp, Window win) {
   Status status;
   XTextProperty prop;
   char *err = "";
   char **list = NULL;
   int count;
   int ret;

   status = XGetWMName(dsp, win, &prop);
   ret = XmbTextPropertyToTextList(dsp, &prop, &list, &count);
   if (ret == Success || ret > 0 && list != NULL) {
      int i = 0;
      
      for (i = 0; i < count; i++) {
         if (list[i] == NULL) {
            err = "null";
         } else {
            err = (char*)malloc(strlen(list[i])+1);
            err = strcpy(err, list[i]);
         }
      }

      if (list != NULL) {
         XFreeStringList(list);
      }
   } 

   return err;
}

/*
findFireFoxWindows:
   This function looks for all open window titles to find the firefox
   save file dialog box.

Input:
   dsp: The Curent XWindow Display.
   root: The root window which to look at.
   regex: A perl regex to match for finding the window.

Output:
   returns the window handle that matches your regex if any.

*/
Window findFireFoxWindows(Display *dsp, Window root, char *regex) {
   Window result = -1;
   Window tmp_res = -1;
   int err = 0;
   int i = 0;
   Status status;
   Window root_return;
   Window parent_return;
   Window *kids;
   unsigned int kidcount;
   Window *ffmainWindows = NULL;
   int ffmainCount = 0;
   pcre *reg_compiled;
   const char *reg_err;
   int reg_erroff = 0;
   int ovector[100];

   reg_compiled = pcre_compile(regex, PCRE_CASELESS, &reg_err,
         &reg_erroff, NULL);

   status = XQueryTree(dsp, root, &root_return, &parent_return,
         &kids, &kidcount);

   if (kidcount < 1) {
      return 0;
   }

   for (i = 0; i <= kidcount -1; i++) {
      char *name;
      char *tmp;
      int match_res = 0;
      tmp = getWinTitle(dsp, kids[i]);

      name = (char*)malloc(strlen(tmp)+1);
      name = strcpy(name, tmp);
      
      if (name == NULL || strlen(name) < 1) {
         name = "";
      }

      match_res = pcre_exec(reg_compiled, 0, name, strlen(name),
            0, 0, ovector, sizeof(ovector));
      if (match_res > 0) {
         printf("(*)NAME: %s\n", name);
         ffmainCount++;
         Window *win = (Window*)realloc(ffmainWindows, 
               ffmainCount * sizeof(Window));
         ffmainWindows = win;
         ffmainWindows[ffmainCount -1] = kids[i];
         result = kids[i];
         break;
      }

      result = findFireFoxWindows(dsp, kids[i], regex);
      if (result > 0) {
         break;
      }
   }

   if (ffmainWindows != NULL) {
      free(ffmainWindows);
   }

   pcre_free(reg_compiled);

   return result;
}

/*
SaveFile:
   This function sends key commands to the FireFox save file dialog.

Input:
   dsp: The current XWindows Display.
   win: The window to send the key commands to.

Output:
   returns 0 on success and -1 on failure.

*/
int SaveFile(Display *dsp, Window win) {
   int err = 0;
   Window parent;
   Window root;
   Window *kids;
   int count;
   KeyCode keycode_return = XKeysymToKeycode(dsp, XK_Return);
   KeyCode keycode_tab = XKeysymToKeycode(dsp, XK_Tab);

   XRaiseWindow(dsp, win);
   XFlush(dsp);
   XSetInputFocus(dsp, win, RevertToNone, CurrentTime);
   XFlush(dsp);
   XTestGrabControl(dsp, True);

   printf("Sending Tab key\n");
   XTestFakeKeyEvent(dsp, keycode_tab, True, 0);
   XFlush(dsp);
   XTestFakeKeyEvent(dsp, keycode_tab, False, 0);
   XFlush(dsp);
   sleep(1);
   printf("Finished.\n");

   printf("Sending Tab key\n");
   XTestFakeKeyEvent(dsp, keycode_tab, True, 0);
   XTestFakeKeyEvent(dsp, keycode_tab, False, 0);
   XFlush(dsp);
   sleep(1);
   printf("Finished.\n");

   printf("Sending Tab key\n");
   XTestFakeKeyEvent(dsp, keycode_tab, True, 0);
   XTestFakeKeyEvent(dsp, keycode_tab, False, 0);
   XFlush(dsp);
   sleep(1);
   printf("Finished.\n");

   printf("Sending Tab key\n");
   XTestFakeKeyEvent(dsp, keycode_tab, True, 0);
   XTestFakeKeyEvent(dsp, keycode_tab, False, 0);
   XFlush(dsp);
   sleep(1);
   printf("Finished.\n");

   printf("Sending Tab Return\n");
   XTestFakeKeyEvent(dsp, keycode_return, True, 0);
   XTestFakeKeyEvent(dsp, keycode_return, False, 0);
   XFlush(dsp);
   printf("Finished\n"); 

   return err;
}

/*
_IOErrorHandler:
   This function is a call back to the XWindows IOError handler.

Input:
   dsp: The current XWindows display.

Output:
   None.  Exists this program with an error code.

*/
int _IOErrorHandler(Display *dsp) {
   printf("XLib IO Error!\n");
   exit(2);
}

/*
_ErrorHandler:
   This function is a call back to the XWindows Error handler.

Input:
   dsp: The current XWindows display.

Output:
   None.  Exists this program with an error code.

*/
int _XErrorHandler(Display *dsp, XErrorEvent *event) {
   char *buffer;

   printf("XLib Error!\n");
   exit(2);
}

/*
main:
   A C main function...
*/
int main (int argc, char *argv[]) {
   int err = 0;
   int screen = 0;
   char *title = NULL;
   Display *dsp = NULL;
   Window root;
   Window res = -1;

   if (argc < 2) {
      printf("(!)Error: Missing command line argument for the window Title!");
      printf("\n\n");
      exit(1);
   }

   title = argv[1];
   printf("(*)Looking for window title: \"%s\"\n", title);

   dsp = XOpenDisplay(NULL);
   XSetIOErrorHandler(_IOErrorHandler);
   XSetErrorHandler(_XErrorHandler);
   screen = XDefaultScreen(dsp);
   root = XRootWindow(dsp, screen);
   res = findFireFoxWindows(dsp, root, title);
   printf("RES: %d\n", res);

   SaveFile(dsp, res);

   return err;

}

