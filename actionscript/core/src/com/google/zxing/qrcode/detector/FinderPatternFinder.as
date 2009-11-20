package com.google.zxing.qrcode.detector
{
    public  class FinderPatternFinder
    { 
    	  import com.google.zxing.common.flexdatatypes.HashTable;
    	  import com.google.zxing.common.flexdatatypes.ArrayList;
    	  import com.google.zxing.ResultPoint;
    	  import com.google.zxing.common.BitArray;
    	  import com.google.zxing.common.BitMatrix;
    	  import com.google.zxing.DecodeHintType;
    	  import com.google.zxing.ReaderException;
    	  
          private static var CENTER_QUORUM:int = 2;
          protected static var MIN_SKIP:int = 3; // 1 pixel/module times 3 modules/center
          protected static var MAX_MODULES:int = 57; // support up to version 10 for mobile clients
          private static var INTEGER_MATH_SHIFT:int = 8;

          private var image:BitMatrix ;
          private var possibleCenters:ArrayList;
          private var hasSkipped:Boolean;
          private var crossCheckStateCount:Array;
          
          protected function getImage():BitMatrix 
          {
    		return image;
  		  }
  		  
  		  protected function getPossibleCenters():ArrayList 
  		  {
    		return possibleCenters;
 	 	  }


          /**
           * <p>Creates a finder that will search the image for three finder patterns.</p>
           *
           * @param image image to search
           */
          public function FinderPatternFinder(image:BitMatrix) 
          {
            this.image = image;
            this.possibleCenters = new ArrayList();
            this.crossCheckStateCount = new Array(5);
          }

          public function find(hints:HashTable):FinderPatternInfo 
          {
            var tryHarder:Boolean = hints != null && hints.ContainsKey(DecodeHintType.TRY_HARDER);
            var maxI:int = image.getHeight();
            var maxJ:int = image.getWidth();
            // We are looking for black/white/black/white/black modules in
            // 1:1:3:1:1 ratio; this tracks the number of such modules seen so far

            // Let's assume that the maximum version QR Code we support takes up 1/4 the height of the
            // image, and then account for the center being 3 modules in size. This gives the smallest
            // number of pixels the center could be, so skip this often. When trying harder, look for all
            // QR versions regardless of how dense they are.
            var iSkip:int = int(maxI / (MAX_MODULES * 4) * 3);
            if (iSkip < MIN_SKIP || tryHarder) {
              iSkip = MIN_SKIP;
            }

            var done:Boolean = false;
            var stateCount:Array = new Array(5);
              // Get a row of black/white values
            for (var i:int = iSkip - 1; i < maxI && !done; i += iSkip) {
              stateCount[0] = 0;
              stateCount[1] = 0;
              stateCount[2] = 0;
              stateCount[3] = 0;
              stateCount[4] = 0;
              var currentState:int = 0;
              for (var j:int = 0; j < maxJ; j++) {
                if (image._get(j,i)) 
                {
                  // Black pixel
                  if ((currentState & 1) == 1) 
                  { // Counting white pixels
                    currentState++;
                  }
                  stateCount[currentState] = stateCount[currentState] + 1;
                } 
                else 
                { // White pixel
                  if ((currentState & 1) == 0) 
                  { // Counting black pixels
                    if (currentState == 4) 
                    { // A winner?
                      if (foundPatternCross(stateCount)) 
                      { // Yes
                        var confirmed:Boolean = handlePossibleCenter(stateCount, i, j);
                        if (confirmed) 
                        {
                          // Start examining every other line. Checking each line turned out to be too
                          // expensive and didn't improve performance.
                          iSkip = 2;
                          if (hasSkipped) {
                            done = haveMulitplyConfirmedCenters();
                          } else {
                            var rowSkip:int = findRowSkip();
                            if (rowSkip > stateCount[2]) {
                              // Skip rows between row of lower confirmed center
                              // and top of presumed third confirmed center
                              // but back up a bit to get a full chance of detecting
                              // it, entire width of center of finder pattern

                              // Skip by rowSkip, but back off by stateCount[2] (size of last center
                              // of pattern we saw) to be conservative, and also back off by iSkip which
                              // is about to be re-added
                              i += (rowSkip - stateCount[2] - iSkip);
                              j = maxJ - 1;
                            }
                          }
                        } else {
                          // Advance to next black pixel
                          do {
                            j++;
                          } while (j < maxJ && !image._get(j,i));
                          j--; // back up to that last white pixel
                        }
                        // Clear state to start looking again
                        currentState = 0;
                        stateCount[0] = 0;
                        stateCount[1] = 0;
                        stateCount[2] = 0;
                        stateCount[3] = 0;
                        stateCount[4] = 0;
                      } 
                      else 
                      { // No, shift counts back by two
                        stateCount[0] = stateCount[2];
                        stateCount[1] = stateCount[3];
                        stateCount[2] = stateCount[4];
                        stateCount[3] = 1;
                        stateCount[4] = 0;
                        currentState = 3;
                      }
                    } 
                    else 
                    {
                      currentState++;
                      stateCount[currentState] = stateCount[currentState] + 1;
                    }
                  } else 
                  { // Counting white pixels
                    stateCount[currentState] = stateCount[currentState] + 1;
                  }
                }
              }
              
              if (foundPatternCross(stateCount)) {
                var confirmed3:Boolean = handlePossibleCenter(stateCount, i, maxJ);
                if (confirmed3) {
                  iSkip = stateCount[0];
                  if (hasSkipped) {
                    // Found a third one
                    done = haveMulitplyConfirmedCenters();
                  }
                }
              }
            }

            var patternInfo:Array = selectBestPatterns();
            ResultPoint.orderBestPatterns(patternInfo);

            return new FinderPatternInfo(patternInfo);
          }

          /**
           * Given a count of black/white/black/white/black pixels just seen and an end position,
           * figures the location of the center of this run.
           */
          private static function centerFromEnd(stateCount:Array, end:int):Number {
            return (end - stateCount[4] - stateCount[3]) - stateCount[2] / 2;
          }

          /**
           * @param stateCount count of black/white/black/white/black pixels just read
           * @return true iff the proportions of the counts is close enough to the 1/1/3/1/1 ratios
           *         used by finder patterns to be considered a match
           */
          public static function foundPatternCross(stateCount:Array):Boolean {
            var totalModuleSize:int = 0;
            for (var i:int = 0; i < 5; i++) {
              var count:int = stateCount[i];
              if (count == 0) {
                return false;
              }
              totalModuleSize += count;
            }
            if (totalModuleSize < 7) {
              return false;
            }
            var moduleSize:int = (totalModuleSize << INTEGER_MATH_SHIFT) / 7;
            var maxVariance:int = moduleSize / 2;
            // Allow less than 50% variance from 1-1-3-1-1 proportions
            return Math.abs(moduleSize - (stateCount[0] << INTEGER_MATH_SHIFT)) < maxVariance &&
                Math.abs(moduleSize - (stateCount[1] << INTEGER_MATH_SHIFT)) < maxVariance &&
                Math.abs(3 * moduleSize - (stateCount[2] << INTEGER_MATH_SHIFT)) < 3 * maxVariance &&
                Math.abs(moduleSize - (stateCount[3] << INTEGER_MATH_SHIFT)) < maxVariance &&
                Math.abs(moduleSize - (stateCount[4] << INTEGER_MATH_SHIFT)) < maxVariance;
          }

          private function getCrossCheckStateCount():Array 
          {
            crossCheckStateCount[0] = 0;
            crossCheckStateCount[1] = 0;
            crossCheckStateCount[2] = 0;
            crossCheckStateCount[3] = 0;
            crossCheckStateCount[4] = 0;
            return crossCheckStateCount;
          }

          /**
           * <p>After a horizontal scan finds a potential finder pattern, this method
           * "cross-checks" by scanning down vertically through the center of the possible
           * finder pattern to see if the same proportion is detected.</p>
           *
           * @param startI row where a finder pattern was detected
           * @param centerJ center of the section that appears to cross a finder pattern
           * @param maxCount maximum reasonable number of modules that should be
           * observed in any reading state, based on the results of the horizontal scan
           * @return vertical center of finder pattern, or {@link Float#NaN} if not found
           */
          private function crossCheckVertical(startI:int, centerJ:int, maxCount:int, originalStateCountTotal:int ):Number {
            var image:BitMatrix  = this.image;

            var maxI:int = image.getHeight();
            var stateCount:Array = getCrossCheckStateCount();

            // Start counting up from center
            var i:int = startI;
            while (i >= 0 && image._get(centerJ, i)) {
              stateCount[2] = stateCount[2] + 1;
              i--;
            }
            if (i < 0) {
              return Number.NaN;
            }
            while (i >= 0 && !image._get(centerJ, i) && stateCount[1] <= maxCount) {
              stateCount[1] = stateCount[1] + 1;
              i--;
            }
            // If already too many modules in this state or ran off the edge:
            if (i < 0 || stateCount[1] > maxCount) {
              return Number.NaN;
            }
            while (i >= 0 && image._get(centerJ, i) && stateCount[0] <= maxCount) {
              stateCount[0] = stateCount[0] + 1;
              i--;
            }
            if (stateCount[0] > maxCount) {
              return Number.NaN;
            }

            // Now also count down from center
            i = startI + 1;
            while (i < maxI && image._get(centerJ, i)) {
              stateCount[2] = stateCount[2] + 1;
              i++;
            }
            if (i == maxI) {
              return Number.NaN;
            }
            while (i < maxI && !image._get(centerJ, i) && stateCount[3] < maxCount) {
              stateCount[3] =  stateCount[3] + 1;
              i++;
            }
            if (i == maxI || stateCount[3] >= maxCount) {
              return Number.NaN;
            }
            while (i < maxI && image._get(centerJ, i) && stateCount[4] < maxCount) {
              stateCount[4] = stateCount[4] + 1;
              i++;
            }
            if (stateCount[4] >= maxCount) {
              return Number.NaN;
            }

            // If we found a finder-pattern-like section, but its size is more than 20% different than
            // the original, assume it's a false positive
            var stateCountTotal:int = stateCount[0] + stateCount[1] + stateCount[2] + stateCount[3] + stateCount[4];
            if (5 * Math.abs(stateCountTotal - originalStateCountTotal) >= originalStateCountTotal) {
              return Number.NaN;
            }

            return foundPatternCross(stateCount) ? centerFromEnd(stateCount, i) : Number.NaN;
          }

          /**
           * <p>Like {@link #crossCheckVertical(int, int, int, int)}, and in fact is basically identical,
           * except it reads horizontally instead of vertically. This is used to cross-cross
           * check a vertical cross check and locate the real center of the alignment pattern.</p>
           */
          private function crossCheckHorizontal( startJ:int, centerI:int,maxCount:int, originalStateCountTotal:int):Number {
            var image:BitMatrix = this.image;

            var maxJ:int = image.getWidth();
            var stateCount:Array = getCrossCheckStateCount();

           var j:int = startJ;
            while (j >= 0 && image._get(j, centerI)) {
              stateCount[2] = stateCount[2] + 1;
              j--;
            }
            if (j < 0) {
              return Number.NaN;
            }
            while (j >= 0 && !image._get(j, centerI) && stateCount[1] <= maxCount) {
              stateCount[1] = stateCount[1] + 1;
              j--;
            }
            if (j < 0 || stateCount[1] > maxCount) {
              return Number.NaN;
            }
            while (j >= 0 && image._get(j, centerI) && stateCount[0] <= maxCount) {
              stateCount[0] = stateCount[0] + 1;
              j--;
            }
            if (stateCount[0] > maxCount) {
              return Number.NaN;
            }

            j = startJ + 1;
            while (j < maxJ && image._get(j, centerI)) {
              stateCount[2]= stateCount[2]+ 1;
              j++;
            }
            if (j == maxJ) {
              return Number.NaN;
            }
            while (j < maxJ && !image._get(j, centerI) && stateCount[3] < maxCount) {
              stateCount[3]= stateCount[3] + 1;
              j++;
            }
            if (j == maxJ || stateCount[3] >= maxCount) {
              return Number.NaN;
            }
            while (j < maxJ && image._get(j, centerI) && stateCount[4] < maxCount) {
              stateCount[4] = stateCount[4] + 1;
              j++;
            }
            if (stateCount[4] >= maxCount) {
              return Number.NaN;
            }

            // If we found a finder-pattern-like section, but its size is significantly different than
            // the original, assume it's a false positive
            var stateCountTotal:int = stateCount[0] + stateCount[1] + stateCount[2] + stateCount[3] + stateCount[4];
            if (5 * Math.abs(stateCountTotal - originalStateCountTotal) >= originalStateCountTotal) {
              return Number.NaN;
            }

            return foundPatternCross(stateCount) ? centerFromEnd(stateCount, j) : Number.NaN;
          }

          /**
           * <p>This is called when a horizontal scan finds a possible alignment pattern. It will
           * cross check with a vertical scan, and if successful, will, ah, cross-cross-check
           * with another horizontal scan. This is needed primarily to locate the real horizontal
           * center of the pattern in cases of extreme skew.</p>
           *
           * <p>If that succeeds the finder pattern location is added to a list that tracks
           * the number of times each location has been nearly-matched as a finder pattern.
           * Each additional find is more evidence that the location is in fact a finder
           * pattern center
           *
           * @param stateCount reading state module counts from horizontal scan
           * @param i row where finder pattern may be found
           * @param j end of possible finder pattern in row
           * @return true if a finder pattern candidate was found this time
           */
          public function handlePossibleCenter(stateCount:Array,
                                               i:int,
                                               j:int):Boolean {
            var stateCountTotal:int = stateCount[0] + stateCount[1] + stateCount[2] + stateCount[3] + stateCount[4];
            var centerJ:Number = centerFromEnd(stateCount, j);
            var centerI:Number = crossCheckVertical(i, int(centerJ), stateCount[2], stateCountTotal);
            if (!isNaN(centerI)) {
              // Re-cross check
              centerJ = crossCheckHorizontal(int(centerJ), int(centerI), stateCount[2], stateCountTotal);
              if (!isNaN(centerJ))
              {
                var estimatedModuleSize:Number = Number(stateCountTotal / 7);
                var found:Boolean = false;
                var max:int = possibleCenters.Count;
                for (var index:int = 0; index < max; index++) {
                  var center:FinderPattern = FinderPattern(possibleCenters.getObjectByIndex(index));
                  // Look for about the same center and module size:
                  if (center.aboutEquals(estimatedModuleSize, centerI, centerJ)) {
                    center.incrementCount();
                    found = true;
                    break;
                  }
                }
                if (!found) {
                  possibleCenters.Add(new FinderPattern(centerJ, centerI, estimatedModuleSize));
                }
                return true;
              }
            }
            return false;
          }

          /**
           * @return number of rows we could safely skip during scanning, based on the first
           *         two finder patterns that have been located. In some cases their position will
           *         allow us to infer that the third pattern must lie below a certain point farther
           *         down in the image.
           */
          private function findRowSkip():int {
            var max:int = possibleCenters.Count;
            if (max <= 1) {
              return 0;
            }
            var firstConfirmedCenter:FinderPattern = null;
            for (var i:int = 0; i < max; i++) {
              var center:FinderPattern = FinderPattern(possibleCenters.getObjectByIndex(i));
              if (center.getCount() >= CENTER_QUORUM) {
                if (firstConfirmedCenter == null) {
                  firstConfirmedCenter = center;
                } else {
                  // We have two confirmed centers
                  // How far down can we skip before resuming looking for the next
                  // pattern? In the worst case, only the difference between the
                  // difference in the x / y coordinates of the two centers.
                    // This is the case where you find top left last.
                  hasSkipped = true;
                  return int( (Math.abs(firstConfirmedCenter.getX() - center.getX()) -
                      Math.abs(firstConfirmedCenter.getY() - center.getY())) / 2);
                }
              }
            }
            return 0;
          }

          /**
           * @return true iff we have found at least 3 finder patterns that have been detected
           *         at least {@link #CENTER_QUORUM} times each, and, the estimated module size of the
           *         candidates is "pretty similar"
           */
          private function haveMulitplyConfirmedCenters():Boolean {
            var confirmedCount:int = 0;
            var totalModuleSize:Number = 0;
            var max:int = possibleCenters.Count;
            for (var i:int = 0; i < max; i++) {
              var pattern:FinderPattern = FinderPattern(possibleCenters.getObjectByIndex(i));
              if (pattern.getCount() >= CENTER_QUORUM) {
                confirmedCount++;
                totalModuleSize += pattern.getEstimatedModuleSize();
              }
            }
            if (confirmedCount < 3) {
              return false;
            }
            // OK, we have at least 3 confirmed centers, but, it's possible that one is a "false positive"
            // and that we need to keep looking. We detect this by asking if the estimated module sizes
            // vary too much. We arbitrarily say that when the total deviation from average exceeds
            // 15% of the total module size estimates, it's too much.
            var average:Number = totalModuleSize / max;
            var totalDeviation:Number = 0;
            for (var i2:int = 0; i2 < max; i2++) {
              var pattern2:FinderPattern = FinderPattern(possibleCenters.getObjectByIndex(i2));
              totalDeviation += Math.abs(pattern2.getEstimatedModuleSize() - average);
            }
            return totalDeviation <= 0.15 * totalModuleSize;
          }

          /**
           * @return the 3 best {@link FinderPattern}s from our list of candidates. The "best" are
           *         those that have been detected at least {@link #CENTER_QUORUM} times, and whose module
           *         size differs from the average among those patterns the least
           * @throws ReaderException if 3 such finder patterns do not exist
           */
          private function selectBestPatterns():Array
          {
          	possibleCenters.sort_CenterComparator();
            //Collections.insertionSort(possibleCenters, new CenterComparator());
          
            var size:int = 0;
            var max:int = possibleCenters.Count;
            while (size < max) {
              if ((FinderPattern( possibleCenters.getObjectByIndex(size))).getCount() < CENTER_QUORUM) {
                break;
              }
              size++;
            }

            if (size < 3) {
              // Couldn't find enough finder patterns
              throw new ReaderException("FinderPatternFinder : selectBestPatterns : Couldn't find enough finder patterns");
            }

            if (size > 3) 
            {
              // Throw away all but those first size candidate points we found.
               // SupportClass.SetCapacity(possibleCenters, size);
              //  We need to pick the best three. Find the most
              // popular ones whose module size is nearest the average
              var averageModuleSize:Number = 0;
              for (var i:int = 0; i < size; i++) {
                averageModuleSize += (FinderPattern(possibleCenters.getObjectByIndex(i))).getEstimatedModuleSize();
              }
              averageModuleSize /= Number(size);
              // We don't have java.util.Collections in J2ME
              //Collections.insertionSort(possibleCenters, new ClosestToAverageComparator(averageModuleSize));
              var listlength:int = possibleCenters.length;
              var temp:Object;
              for (var position:int=listlength-1;position>=0;position--)
              {
              	for(var scan:int = 0;scan < position -1; scan++)
              	{
              		if (Math.abs(FinderPattern( possibleCenters.getObjectByIndex(scan)).getEstimatedModuleSize() - averageModuleSize) < 
              		    Math.abs(FinderPattern( possibleCenters.getObjectByIndex(scan+1)).getEstimatedModuleSize() - averageModuleSize)
              		    )
              		{
              			temp = possibleCenters.getObjectByIndex(scan);
              			possibleCenters.setObjectByIndex(scan,possibleCenters.getObjectByIndex(scan+1));
              			possibleCenters.setObjectByIndex(scan+1,temp);
              		}
              	}
              }
            }

            return [
                FinderPattern( possibleCenters.getObjectByIndex(0)),
                FinderPattern( possibleCenters.getObjectByIndex(1)),
                FinderPattern( possibleCenters.getObjectByIndex(2))];
          }
          
           public function compare(center1:Object, center2:Object,averageModuleSize:Number):int
            {
              return (Math.abs(FinderPattern( center1).getEstimatedModuleSize() - averageModuleSize) < Math.abs(FinderPattern( center2).getEstimatedModuleSize() - averageModuleSize)) ? -1 : 1;
            }

    }

}