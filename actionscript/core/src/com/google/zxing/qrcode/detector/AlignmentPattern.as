/*
 * Copyright 2013 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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