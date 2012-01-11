package com.google.zxing.qrcode.detector
{
		import com.google.zxing.common.Comparator;
      /**
       * <p>Orders by {@link FinderPattern#getCount()}, descending.</p>
       */
      public class CenterComparator
      {
      	public static var average:Number = 0;
      	  /*
          public static function compare(center1:Object, center2:Object):int
          {
          	var res:int = FinderPattern(center2).getCount() - (FinderPattern( center1)).getCount();
          	if (res > 0) 
          	{
          		return 1;
          	}
          	else if (res < 0)
          	{
          		return -1;
          	}
          	else 
          	{
          		return 0;
          	}
          }
          */
          public static function compare(center1:Object, center2:Object):int
          {
                if (((center2 as FinderPattern).getCount()) == ((center1 as FinderPattern).getCount())) 
                {
        			var dA:Number = Math.abs((center2 as FinderPattern).getEstimatedModuleSize() - CenterComparator.average);
        			var dB:Number = Math.abs((center1 as FinderPattern).getEstimatedModuleSize() - CenterComparator.average);
        			return dA < dB ? 1 : dA == dB ? 0 : -1;
      			} 
      			else 
      			{
        			return ((center2 as FinderPattern).getCount()) - ((center1 as FinderPattern).getCount());
      			}

          }
          
          public static function setAverage(average:Number):void
          {
          	CenterComparator.average = average;
          }
          /**
   * <p>Orders by furthest from average</p>
   */
   /*
  private static class FurthestFromAverageComparator implements Comparator {
    private final float average;
    private FurthestFromAverageComparator(float f) {
      average = f;
    }
    public int compare(Object center1, Object center2) {
      float dA = Math.abs(((FinderPattern) center2).getEstimatedModuleSize() - average);
      float dB = Math.abs(((FinderPattern) center1).getEstimatedModuleSize() - average);
      return dA < dB ? -1 : dA == dB ? 0 : 1;
    }
  }*/
      }
}