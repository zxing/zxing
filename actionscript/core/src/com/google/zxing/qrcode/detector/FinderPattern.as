package com.google.zxing.qrcode.detector
{
	import com.google.zxing.ResultPoint;
	
    public class FinderPattern extends ResultPoint
    { 
          protected  var estimatedModuleSize:Number;
          protected var count:int;

          public function FinderPattern(posX:Number, posY:Number, estimatedModuleSize:Number, count:int = 1) {
			super(posX,posY);
            this.estimatedModuleSize = estimatedModuleSize;
            this.count = count;
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
      				var result:Boolean = moduleSizeDiff <= 1 || moduleSizeDiff / estimatedModuleSize <= 1;
      				return result;
    			}
    			return false;
           }
           
             /**
			   * Combines this object's current estimate of a finder pattern position and module size
			   * with a new estimate. It returns a new {@code FinderPattern} containing a weighted average
			   * based on count.
			   */
			  public function combineEstimate(i:Number,j:Number,newModuleSize:Number):FinderPattern 
			  {
			    var combinedCount:int = count + 1;
			    var combinedX:Number = (count * getX() + j) / combinedCount;
			    var combinedY:Number = (count * getY() + i) / combinedCount;
			    var combinedModuleSize:Number = (count * getEstimatedModuleSize() + newModuleSize) / combinedCount;
			    return new FinderPattern(combinedX, combinedY, combinedModuleSize, combinedCount);
			  }

  }
    
}
