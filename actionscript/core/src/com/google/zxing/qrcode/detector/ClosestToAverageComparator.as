package com.google.zxing.qrcode.detector
{
	import com.google.zxing.common.Comparator;
          /**
           * <p>Orders by variance from average module size, ascending.</p>
           */
	public class ClosestToAverageComparator implements Comparator 
	{
            private  var averageModuleSize:Number;

            public function ClosestToAverageComparator(averageModuleSize:Number) 
            {
              this.averageModuleSize = averageModuleSize;
            }

            public function compare(center1:Object, center2:Object):int
            {
              return (Math.abs(FinderPattern( center1).getEstimatedModuleSize() - averageModuleSize) < Math.abs(FinderPattern( center2).getEstimatedModuleSize() - averageModuleSize)) ? -1 : 1;
            }
	}
}