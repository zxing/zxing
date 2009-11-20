package com.google.zxing.qrcode.detector
{
	import com.google.zxing.ResultPoint;
	
    public class FinderPattern extends ResultPoint
    { 
          private  var estimatedModuleSize:Number;
          private var count:int;

          public function FinderPattern(posX:Number, posY:Number, estimatedModuleSize:Number) {
			super(posX,posY);
            this.estimatedModuleSize = estimatedModuleSize;
            this.count = 1;
          }

          public function getEstimatedModuleSize():Number
          {
            return estimatedModuleSize;
          }

          public function getCount():int
          {
            return count;
          }

          public function incrementCount():void
          {
            this.count++;
          }

          /**
           * <p>Determines if this finder pattern "about equals" a finder pattern at the stated
           * position and size -- meaning, it is at nearly the same center with nearly the same size.</p>
           */
           public function aboutEquals(moduleSize:Number, i:Number, j:Number):Boolean 
           {
    			if (Math.abs(i - getY()) <= moduleSize && Math.abs(j - getX()) <= moduleSize) 
    			{
      				var moduleSizeDiff:Number = Math.abs(moduleSize - estimatedModuleSize);
      				return moduleSizeDiff <= 1 || moduleSizeDiff / estimatedModuleSize <= 1;
    			}
    			return false;
           }
  }
    
}
