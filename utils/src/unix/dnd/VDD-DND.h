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


#ifndef __VDD_HEADER__

#define __VDD_HEADER__ 1

#include <stdlib.h>
#include <stdio.h>
#include <unistd.h>

/*
TRACE: 
   A debug macro for turning debug prints on when compiled with the
   DEBUG flag set.  Really it is just a printf macro.
*/
#ifdef DEBUG
   #define TRACE(a,args...) printf("[%s:%d]"#a"\n", __FUNCTION__,\
         __LINE__, ##args) 
#else
   #define TRACE(a,args...) 
#endif

#endif


