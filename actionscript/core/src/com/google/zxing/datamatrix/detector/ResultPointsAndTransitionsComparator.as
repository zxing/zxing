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
          var result:int = (ResultPointsAndTransitions( o1)).getTransitions() - (ResultPointsAndTransitions( o2)).getTransitions();
          if (result > 0) { return 1; }
          else if (result < 0) { return -1; }
          else {return 0;}
     	}
          
     }
}
