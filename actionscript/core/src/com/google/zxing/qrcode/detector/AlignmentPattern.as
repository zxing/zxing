package com.google.zxing.qrcode.detector
{
	import com.google.zxing.ResultPoint;
	
    public class AlignmentPattern extends ResultPoint
    {
    	  private var estimatedModuleSize:Number;

          
          public function AlignmentPattern(posX:Number, posY:Number, estimatedModuleSize:Number) 
          {
			super(posX,posY);
            this.estimatedModuleSize = estimatedModuleSize;
          }

          /**
           * <p>Determines if this alignment pattern "about equals" an alignment pattern at the stated
           * position and size -- meaning, it is at nearly the same center with nearly the same size.</p>
           */
          public function aboutEquals(moduleSize:Number, i:Number, j:Number):Boolean 
		  {
				if (Math.abs(i - getY()) <= moduleSize && Math.abs(j - getX()) <= moduleSize) 
				{
					var moduleSizeDiff:Number = Math.abs(moduleSize - estimatedModuleSize);
					return moduleSizeDiff <= 1 || moduleSizeDiff <= estimatedModuleSize;
				}
				return false;
          }
		  
		  
		   /**
		   * Combines this object's current estimate of a finder pattern position and module size
		   * with a new estimate. It returns a new {@code FinderPattern} containing an average of the two.
		   */
		  public function combineEstimate(i:Number, j:Number, newModuleSize:Number ):AlignmentPattern {
			var combinedX:Number = (getX() + j) / 2;
			var combinedY:Number = (getY() + i) / 2;
			var combinedModuleSize:Number = (estimatedModuleSize + newModuleSize) / 2;
			return new AlignmentPattern(combinedX, combinedY, combinedModuleSize);
		  }
		  
  
    }
    

}