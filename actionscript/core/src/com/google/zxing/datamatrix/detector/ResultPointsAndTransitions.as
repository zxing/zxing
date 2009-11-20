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