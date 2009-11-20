package com.google.zxing.qrcode.detector
{
		import com.google.zxing.common.Comparator;
      /**
       * <p>Orders by {@link FinderPattern#getCount()}, descending.</p>
       */
      public class CenterComparator
      {
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
      }
}