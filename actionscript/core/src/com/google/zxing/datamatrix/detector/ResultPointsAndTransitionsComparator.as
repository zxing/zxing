/*
 * Copyright 2013 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.zxing.datamatrix.detector
{
	import com.google.zxing.common.Comparator;
	 /**
      * Orders ResultPointsAndTransitions by number of transitions, ascending.
      */
      public class ResultPointsAndTransitionsComparator
      {
      	
        public static function  compare(o1:Object,  o2:Object):int 
        {
          var result:int = ((o1 as ResultPointsAndTransitions)).getTransitions() - ((o2 as ResultPointsAndTransitions)).getTransitions();
          if (result > 0) { return 1; }
          else if (result < 0) { return -1; }
          else {return 0;}
     	}
          
     }
}
