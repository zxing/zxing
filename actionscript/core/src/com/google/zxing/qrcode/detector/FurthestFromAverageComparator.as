package com.google.zxing.qrcode.detector
{
	  import com.google.zxing.common.Comparator;
      public class FurthestFromAverageComparator
      {
      	public static var average:Number = 0;
          
          public static function setAverage(average:Number):void
          {
          		FurthestFromAverageComparator.average = average;
          }
          
          public static function compare(center1:Object, center2:Object):int
		  {
				var dA:Number = Math.abs((center2 as FinderPattern).getEstimatedModuleSize() - FurthestFromAverageComparator.average);
      			var dB:Number = Math.abs((center1 as FinderPattern).getEstimatedModuleSize() - FurthestFromAverageComparator.average);
      			return dA < dB ? -1 : dA == dB ? 0 : 1;
    		}
      }
}