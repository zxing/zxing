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
          public function aboutEquals(moduleSize:Number, i:Number, j:Number):Boolean {
            return  Math.abs(i - getY()) <= moduleSize &&
                    Math.abs(j - getX()) <= moduleSize &&
                    (Math.abs(moduleSize - estimatedModuleSize) <= 1 ||
                        Math.abs(moduleSize - estimatedModuleSize) / estimatedModuleSize <= 0.1);
          }
    }
    

}