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

// needed include files //
#include "VDD-DND.h"
#include "vdd-lib.h"
#include <getopt.h>

static char *opt_string = "h";
static int help_flag;

int main (int argc, char *argv[]) {
   int err = 0;
   int option_index = 0;
   int current_opt = 0;
   static struct option long_options[] = {
      {"help", no_argument, 0, 'h'}
   };

   TRACE("Starting: %s", argv[0]);

   for (option_index = 0; option_index <= argc-1; option_index++) {
      current_opt = getopt_long_only(argc, argv, opt_string, long_options, 
         &option_index);

      switch (current_opt) {
      case 'h':

      break;


      default:
         TRACE("Unknown option value: %d!", current_opt);
      }

   }



   return err;

}


