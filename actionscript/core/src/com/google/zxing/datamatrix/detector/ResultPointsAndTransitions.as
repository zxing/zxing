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
	  import com.google.zxing.ResultPoint;
      /**
       * Simply encapsulates two points and a number of transitions between them.
       */
      public class ResultPointsAndTransitions 
      {
        private  var from:ResultPoint;
        private  var to:ResultPoint;
        private  var transitions:int;

        public function ResultPointsAndTransitions(from:ResultPoint,  to:ResultPoint, transitions:int) {
          this.from = from;
          this.to = to;
          this.transitions = transitions;
        }

        public function getFrom():ResultPoint {
          return from;
        }
        public function getTo():ResultPoint {
          return to;
        }
        public function getTransitions():int {
          return transitions;
        }
        public function toString():String {
          return from + "/" + to + '/' + transitions;
        }
      }
}