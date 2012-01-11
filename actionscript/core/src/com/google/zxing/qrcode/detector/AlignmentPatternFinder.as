package com.google.zxing.qrcode.detector
{
    public class AlignmentPatternFinder
    { 
    	
    	import com.google.zxing.common.flexdatatypes.ArrayList;
    	import com.google.zxing.common.BitArray;
    	import com.google.zxing.common.BitMatrix;
    	import com.google.zxing.ReaderException;
    	import com.google.zxing.ResultPointCallback;
    	import com.google.zxing.ResultPoint;
    	
    	
          private  var image:BitMatrix;
          private  var possibleCenters:ArrayList;
          private  var startX:int;
          private  var startY:int;
          private  var width:int;
          private  var height:int;
          private  var moduleSize:Number;
          private  var crossCheckStateCount:Array;
		  private  var resultPointCallback:ResultPointCallback;
         

          /**
           * <p>Creates a finder that will look in a portion of the whole image.</p>
           *
           * @param image image to search
           * @param startX left column from which to start searching
           * @param startY top row from which to start searching
           * @param width width of region to search
           * @param height height of region to search
           * @param moduleSize estimated module size so far
           */
          public function AlignmentPatternFinder(image:BitMatrix ,
                                 startX:int,
                                 startY:int,
                                 width:int,
                                 height:int,
                                 moduleSize:Number,
								 resultPointCallback:ResultPointCallback) {
            this.image = image;
            this.possibleCenters = new ArrayList(); // no fixed length : was 5
            this.startX = startX;
            this.startY = startY;
            this.width = width;
            this.height = height;
            this.moduleSize = moduleSize;
            this.crossCheckStateCount = new Array(3);
			this.resultPointCallback = resultPointCallback;
          }

          /**
           * <p>This method attempts to find the bottom-right alignment pattern in the image. It is a bit messy since
           * it's pretty performance-critical and so is written to be fast foremost.</p>
           *
           * @return {@link AlignmentPattern} if found
           * @throws ReaderException if not found
           */
          public function find():AlignmentPattern {
            var startX:int = this.startX;
            var height:int = this.height;
            var maxJ:int = startX + width;
            var middleI:int = startY + (height >> 1);
            // We are looking for black/white/black modules in 1:1:1 ratio;
            // this tracks the number of black/white/black modules seen so far
            var stateCount:Array = new Array(3);
            for (var iGen:int = 0; iGen < height; iGen++) 
            {
            	
              // Search from middle outwards
              var i:int = middleI + ((iGen & 0x01) == 0 ? ((iGen + 1) >> 1) : -((iGen + 1) >> 1));
              stateCount[0] = 0;
              stateCount[1] = 0;
              stateCount[2] = 0;
              var j:int = startX;
              // Burn off leading white pixels before anything else; if we start in the middle of
              // a white run, it doesn't make sense to count its length, since we don't know if the
              // white run continued to the left of the start point
              while (j < maxJ && !image._get(j,i)) {
                j++;
              }
              var currentState:int = 0;
              while (j < maxJ) 
              {
         /*     	
if (i==140 && j==210)
{
	var wop:int=0;
}           
*/   	
                if (image._get(j,i)) 
                {
                  // Black pixel
                  if (currentState == 1) 
                  { 
                  	// Counting black pixels
                  	stateCount[currentState] = stateCount[currentState] + 1;                  	
                  } 
                  else 
                  { // Counting white pixels
                    if (currentState == 2) 
                    { // A winner?
                      if (foundPatternCross(stateCount)) 
                      { // Yes
                        var confirmed:AlignmentPattern  = handlePossibleCenter(stateCount, i, j);
                        if (confirmed != null) 
                        {
                          return confirmed;
                        }
                      }
                      stateCount[0] = stateCount[2];
                      stateCount[1] = 1;
                      stateCount[2] = 0;
                      currentState = 1;
                    } 
                    else 
                    {
                    	stateCount[++currentState]++;
                    }
                  }
                } 
                else { // White pixel
                  if (currentState == 1) { // Counting black pixels
                    currentState++;
                  }
                  stateCount[currentState]++;
                }
                j++;
              }
              if (foundPatternCross(stateCount)) {
                var confirmed2:AlignmentPattern = handlePossibleCenter(stateCount, i, maxJ);
                if (confirmed2 != null) {
                  return confirmed2;
                }
              }

            }

            // Hmm, nothing we saw was observed and confirmed twice. If we had
            // any guess at all, return it.
            if (!(possibleCenters.Count==0)) {
              return AlignmentPattern(possibleCenters.getObjectByIndex(0));
            }

            throw new ReaderException("AlignmentPatternFinder : find : could not find pattern");
          }

          /**
           * Given a count of black/white/black pixels just seen and an end position,
           * figures the location of the center of this black/white/black run.
           */
          private static function centerFromEnd(stateCount:Array, end:int):Number 
          {
            return (end - stateCount[2]) - stateCount[1] / 2;
          }

          /**
           * @param stateCount count of black/white/black pixels just read
           * @return true iff the proportions of the counts is close enough to the 1/1/1 ratios
           *         used by alignment patterns to be considered a match
           */
          private function foundPatternCross(stateCount:Array):Boolean {
            var moduleSize:Number = this.moduleSize;
            var maxVariance:Number = moduleSize / 2;
            for (var i:int = 0; i < 3; i++) {
              if (Math.abs(moduleSize - stateCount[i]) >= maxVariance) {
                return false;
              }
            }
            return true;
          }

          /**
           * <p>After a horizontal scan finds a potential alignment pattern, this method
           * "cross-checks" by scanning down vertically through the center of the possible
           * alignment pattern to see if the same proportion is detected.</p>
           *
           * @param startI row where an alignment pattern was detected
           * @param centerJ center of the section that appears to cross an alignment pattern
           * @param maxCount maximum reasonable number of modules that should be
           * observed in any reading state, based on the results of the horizontal scan
           * @return vertical center of alignment pattern, or {@link Float#NaN} if not found
           */
          private function crossCheckVertical(startI:int, centerJ:int, maxCount:int, originalStateCountTotal:int):Number {
            var image:BitMatrix = this.image;

            var maxI:int = image.getHeight();
            var stateCount:Array = crossCheckStateCount;
            stateCount[0] = 0;
            stateCount[1] = 0;
            stateCount[2] = 0;

            // Start counting up from center
            var i:int = startI;
            while (i >= 0 && image._get(centerJ, i) && stateCount[1] <= maxCount) {
              stateCount[1]++;
              i--;
            }
            // If already too many modules in this state or ran off the edge:
            if (i < 0 || stateCount[1] > maxCount) {
              return Number.NaN;
            }
            while (i >= 0 && !image._get(centerJ, i) && stateCount[0] <= maxCount) {
              stateCount[0] = stateCount[0] + 1;
              i--;
            }
            if (stateCount[0] > maxCount) {
                return Number.NaN;
            }

            // Now also count down from center
            i = startI + 1;
            while (i < maxI && image._get(centerJ, i) && stateCount[1] <= maxCount) {
              stateCount[1]++;
              i++;
            }
            if (i == maxI || stateCount[1] > maxCount) {
                return Number.NaN;
            }
            while (i < maxI && !image._get(centerJ, i) && stateCount[2] <= maxCount) {
              stateCount[2]++;
              i++;
            }
            if (stateCount[2] > maxCount) {
                return Number.NaN;
            }

            var stateCountTotal:int = stateCount[0] + stateCount[1] + stateCount[2];
            if (5 * Math.abs(stateCountTotal - originalStateCountTotal) >= 2*originalStateCountTotal) {
                return Number.NaN;
            }

            return foundPatternCross(stateCount) ? centerFromEnd(stateCount, i) : Number.NaN;
          }

          /**
           * <p>This is called when a horizontal scan finds a possible alignment pattern. It will
           * cross check with a vertical scan, and if successful, will see if this pattern had been
           * found on a previous horizontal scan. If so, we consider it confirmed and conclude we have
           * found the alignment pattern.</p>
           *
           * @param stateCount reading state module counts from horizontal scan
           * @param i row where alignment pattern may be found
           * @param j end of possible alignment pattern in row
           * @return {@link AlignmentPattern} if we have found the same pattern twice, or null if not
           */
          private function handlePossibleCenter(stateCount:Array, i:int, j:int):AlignmentPattern 
		  {
            var stateCountTotal:int = stateCount[0] + stateCount[1] + stateCount[2];
            var centerJ:Number = centerFromEnd(stateCount, j);
            var centerI:Number = crossCheckVertical(i, int(centerJ), 2 * stateCount[1], stateCountTotal);
            if (!(isNaN(centerI)))
            {
              var estimatedModuleSize:Number = Number((stateCount[0] + stateCount[1] + stateCount[2]) / 3);
              var max:int = possibleCenters.Count;
              for (var index:int = 0; index < max; index++) 
              {
              		var center:AlignmentPattern = (possibleCenters.getObjectByIndex(index) as AlignmentPattern);
                	// Look for about the same center and module size:
                	if ((center != null) && (center.aboutEquals(estimatedModuleSize, centerI, centerJ))) {
						return center.combineEstimate(centerI, centerJ, estimatedModuleSize);
                	}
              }
              // Hadn't found this before; save it
			  var point:ResultPoint = new AlignmentPattern(centerJ, centerI, estimatedModuleSize);
			  possibleCenters.addElement(point);
		      if (resultPointCallback != null) 
			  {
				resultPointCallback.foundPossibleResultPoint(point);
				}
			}
			return null;
          }
		  
    
    
    }

}