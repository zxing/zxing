package com.google.zxing.multi.qrcode.detector
{
	import com.google.zxing.common.Comparator;
	import com.google.zxing.qrcode.detector.FinderPattern;
	
	public class ModuleSizeComparator implements Comparator
	{
		  /**
   * A comparator that orders FinderPatterns by their estimated module size.
   */
    public function compare(center1:Object,center2:Object):int {
      var value:Number = ( center2 as FinderPattern).getEstimatedModuleSize() -
                    ( center1 as FinderPattern).getEstimatedModuleSize();
      return value < 0.0 ? -1 : value > 0.0 ? 1 : 0;
  		}


	}
}