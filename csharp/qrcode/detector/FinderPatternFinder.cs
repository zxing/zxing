/*
* Copyright 2007 ZXing authors
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
namespace com.google.zxing.qrcode.detector
{
    using System;
    using com.google.zxing;
    using com.google.zxing.common;

    public sealed class FinderPatternFinder
    { 
          private static  int CENTER_QUORUM = 2;
          private static  int MIN_SKIP = 3; // 1 pixel/module times 3 modules/center
          private static  int MAX_MODULES = 57; // support up to version 10 for mobile clients
          private static  int INTEGER_MATH_SHIFT = 8;

          private  MonochromeBitmapSource image;
          private  System.Collections.ArrayList possibleCenters;
          private bool hasSkipped;
          private  int[] crossCheckStateCount;

          /**
           * <p>Creates a finder that will search the image for three finder patterns.</p>
           *
           * @param image image to search
           */
          public FinderPatternFinder(MonochromeBitmapSource image) {
            this.image = image;
            this.possibleCenters = new System.Collections.ArrayList();
            this.crossCheckStateCount = new int[5];
          }

          public FinderPatternInfo find(System.Collections.Hashtable hints) {
            bool tryHarder = hints != null && hints.ContainsKey(DecodeHintType.TRY_HARDER);
            int maxI = image.getHeight();
            int maxJ = image.getWidth();
            // We are looking for black/white/black/white/black modules in
            // 1:1:3:1:1 ratio; this tracks the number of such modules seen so far

            // Let's assume that the maximum version QR Code we support takes up 1/4 the height of the
            // image, and then account for the center being 3 modules in size. This gives the smallest
            // number of pixels the center could be, so skip this often. When trying harder, look for all
            // QR versions regardless of how dense they are.
            int iSkip = (int) (maxI / (MAX_MODULES * 4.0f) * 3);
            if (iSkip < MIN_SKIP || tryHarder) {
              iSkip = MIN_SKIP;
            }

            bool done = false;
            int[] stateCount = new int[5];
            BitArray blackRow = new BitArray(maxJ);
            for (int i = iSkip - 1; i < maxI && !done; i += iSkip) {
              // Get a row of black/white values
              blackRow = image.getBlackRow(i, blackRow, 0, maxJ);
              stateCount[0] = 0;
              stateCount[1] = 0;
              stateCount[2] = 0;
              stateCount[3] = 0;
              stateCount[4] = 0;
              int currentState = 0;
              for (int j = 0; j < maxJ; j++) {
                if (blackRow.get(j)) {
                  // Black pixel
                  if ((currentState & 1) == 1) { // Counting white pixels
                    currentState++;
                  }
                  stateCount[currentState]++;
                } else { // White pixel
                  if ((currentState & 1) == 0) { // Counting black pixels
                    if (currentState == 4) { // A winner?
                      if (foundPatternCross(stateCount)) { // Yes
                        bool confirmed = handlePossibleCenter(stateCount, i, j);
                        if (confirmed) {
                          // Start examining every other line. Checking each line turned out to be too
                          // expensive and didn't improve performance.
                          iSkip = 2;
                          if (hasSkipped) {
                            done = haveMulitplyConfirmedCenters();
                          } else {
                            int rowSkip = findRowSkip();
                            if (rowSkip > stateCount[2]) {
                              // Skip rows between row of lower confirmed center
                              // and top of presumed third confirmed center
                              // but back up a bit to get a full chance of detecting
                              // it, entire width of center of finder pattern

                              // Skip by rowSkip, but back off by stateCount[2] (size of last center
                              // of pattern we saw) to be conservative, and also back off by iSkip which
                              // is about to be re-added
                              i += rowSkip - stateCount[2] - iSkip;
                              j = maxJ - 1;
                            }
                          }
                        } else {
                          // Advance to next black pixel
                          do {
                            j++;
                          } while (j < maxJ && !blackRow.get(j));
                          j--; // back up to that last white pixel
                        }
                        // Clear state to start looking again
                        currentState = 0;
                        stateCount[0] = 0;
                        stateCount[1] = 0;
                        stateCount[2] = 0;
                        stateCount[3] = 0;
                        stateCount[4] = 0;
                      } else { // No, shift counts back by two
                        stateCount[0] = stateCount[2];
                        stateCount[1] = stateCount[3];
                        stateCount[2] = stateCount[4];
                        stateCount[3] = 1;
                        stateCount[4] = 0;
                        currentState = 3;
                      }
                    } else {
                      stateCount[++currentState]++;
                    }
                  } else { // Counting white pixels
                    stateCount[currentState]++;
                  }
                }
              }
              if (foundPatternCross(stateCount)) {
                bool confirmed = handlePossibleCenter(stateCount, i, maxJ);
                if (confirmed) {
                  iSkip = stateCount[0];
                  if (hasSkipped) {
                    // Found a third one
                    done = haveMulitplyConfirmedCenters();
                  }
                }
              }
            }

            FinderPattern[] patternInfo = selectBestPatterns();
            GenericResultPoint.orderBestPatterns(patternInfo);

            return new FinderPatternInfo(patternInfo);
          }

          /**
           * Given a count of black/white/black/white/black pixels just seen and an end position,
           * figures the location of the center of this run.
           */
          private static float centerFromEnd(int[] stateCount, int end) {
            return (float) (end - stateCount[4] - stateCount[3]) - stateCount[2] / 2.0f;
          }

          /**
           * @param stateCount count of black/white/black/white/black pixels just read
           * @return true iff the proportions of the counts is close enough to the 1/1/3/1/1 ratios
           *         used by finder patterns to be considered a match
           */
          private static bool foundPatternCross(int[] stateCount) {
            int totalModuleSize = 0;
            for (int i = 0; i < 5; i++) {
              int count = stateCount[i];
              if (count == 0) {
                return false;
              }
              totalModuleSize += count;
            }
            if (totalModuleSize < 7) {
              return false;
            }
            int moduleSize = (totalModuleSize << INTEGER_MATH_SHIFT) / 7;
            int maxVariance = moduleSize / 2;
            // Allow less than 50% variance from 1-1-3-1-1 proportions
            return Math.Abs(moduleSize - (stateCount[0] << INTEGER_MATH_SHIFT)) < maxVariance &&
                Math.Abs(moduleSize - (stateCount[1] << INTEGER_MATH_SHIFT)) < maxVariance &&
                Math.Abs(3 * moduleSize - (stateCount[2] << INTEGER_MATH_SHIFT)) < 3 * maxVariance &&
                Math.Abs(moduleSize - (stateCount[3] << INTEGER_MATH_SHIFT)) < maxVariance &&
                Math.Abs(moduleSize - (stateCount[4] << INTEGER_MATH_SHIFT)) < maxVariance;
          }

          private int[] getCrossCheckStateCount() {
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
          private float crossCheckVertical(int startI, int centerJ, int maxCount, int originalStateCountTotal) {
            MonochromeBitmapSource image = this.image;

            int maxI = image.getHeight();
            int[] stateCount = getCrossCheckStateCount();

            // Start counting up from center
            int i = startI;
            while (i >= 0 && image.isBlack(centerJ, i)) {
              stateCount[2]++;
              i--;
            }
            if (i < 0) {
              return float.NaN;
            }
            while (i >= 0 && !image.isBlack(centerJ, i) && stateCount[1] <= maxCount) {
              stateCount[1]++;
              i--;
            }
            // If already too many modules in this state or ran off the edge:
            if (i < 0 || stateCount[1] > maxCount) {
              return float.NaN;
            }
            while (i >= 0 && image.isBlack(centerJ, i) && stateCount[0] <= maxCount) {
              stateCount[0]++;
              i--;
            }
            if (stateCount[0] > maxCount) {
              return float.NaN;
            }

            // Now also count down from center
            i = startI + 1;
            while (i < maxI && image.isBlack(centerJ, i)) {
              stateCount[2]++;
              i++;
            }
            if (i == maxI) {
              return float.NaN;
            }
            while (i < maxI && !image.isBlack(centerJ, i) && stateCount[3] < maxCount) {
              stateCount[3]++;
              i++;
            }
            if (i == maxI || stateCount[3] >= maxCount) {
              return float.NaN;
            }
            while (i < maxI && image.isBlack(centerJ, i) && stateCount[4] < maxCount) {
              stateCount[4]++;
              i++;
            }
            if (stateCount[4] >= maxCount) {
              return float.NaN;
            }

            // If we found a finder-pattern-like section, but its size is more than 20% different than
            // the original, assume it's a false positive
            int stateCountTotal = stateCount[0] + stateCount[1] + stateCount[2] + stateCount[3] + stateCount[4];
            if (5 * Math.Abs(stateCountTotal - originalStateCountTotal) >= originalStateCountTotal) {
              return float.NaN;
            }

            return foundPatternCross(stateCount) ? centerFromEnd(stateCount, i) : float.NaN;
          }

          /**
           * <p>Like {@link #crossCheckVertical(int, int, int, int)}, and in fact is basically identical,
           * except it reads horizontally instead of vertically. This is used to cross-cross
           * check a vertical cross check and locate the real center of the alignment pattern.</p>
           */
          private float crossCheckHorizontal(int startJ, int centerI, int maxCount, int originalStateCountTotal) {
            MonochromeBitmapSource image = this.image;

            int maxJ = image.getWidth();
            int[] stateCount = getCrossCheckStateCount();

            int j = startJ;
            while (j >= 0 && image.isBlack(j, centerI)) {
              stateCount[2]++;
              j--;
            }
            if (j < 0) {
              return float.NaN;
            }
            while (j >= 0 && !image.isBlack(j, centerI) && stateCount[1] <= maxCount) {
              stateCount[1]++;
              j--;
            }
            if (j < 0 || stateCount[1] > maxCount) {
              return float.NaN;
            }
            while (j >= 0 && image.isBlack(j, centerI) && stateCount[0] <= maxCount) {
              stateCount[0]++;
              j--;
            }
            if (stateCount[0] > maxCount) {
              return float.NaN;
            }

            j = startJ + 1;
            while (j < maxJ && image.isBlack(j, centerI)) {
              stateCount[2]++;
              j++;
            }
            if (j == maxJ) {
              return float.NaN;
            }
            while (j < maxJ && !image.isBlack(j, centerI) && stateCount[3] < maxCount) {
              stateCount[3]++;
              j++;
            }
            if (j == maxJ || stateCount[3] >= maxCount) {
              return float.NaN;
            }
            while (j < maxJ && image.isBlack(j, centerI) && stateCount[4] < maxCount) {
              stateCount[4]++;
              j++;
            }
            if (stateCount[4] >= maxCount) {
              return float.NaN;
            }

            // If we found a finder-pattern-like section, but its size is significantly different than
            // the original, assume it's a false positive
            int stateCountTotal = stateCount[0] + stateCount[1] + stateCount[2] + stateCount[3] + stateCount[4];
            if (5 * Math.Abs(stateCountTotal - originalStateCountTotal) >= originalStateCountTotal) {
              return float.NaN;
            }

            return foundPatternCross(stateCount) ? centerFromEnd(stateCount, j) : float.NaN;
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
          private bool handlePossibleCenter(int[] stateCount,
                                               int i,
                                               int j) {
            int stateCountTotal = stateCount[0] + stateCount[1] + stateCount[2] + stateCount[3] + stateCount[4];
            float centerJ = centerFromEnd(stateCount, j);
            float centerI = crossCheckVertical(i, (int) centerJ, stateCount[2], stateCountTotal);
            if (!Single.IsNaN(centerI)) {
              // Re-cross check
              centerJ = crossCheckHorizontal((int) centerJ, (int) centerI, stateCount[2], stateCountTotal);
              if (!Single.IsNaN(centerJ))
              {
                float estimatedModuleSize = (float) stateCountTotal / 7.0f;
                bool found = false;
                int max = possibleCenters.Count;
                for (int index = 0; index < max; index++) {
                  FinderPattern center = (FinderPattern) possibleCenters[index];
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
          private int findRowSkip() {
            int max = possibleCenters.Count;
            if (max <= 1) {
              return 0;
            }
            FinderPattern firstConfirmedCenter = null;
            for (int i = 0; i < max; i++) {
              FinderPattern center = (FinderPattern) possibleCenters[i];
              if (center.getCount() >= CENTER_QUORUM) {
                if (firstConfirmedCenter == null) {
                  firstConfirmedCenter = center;
                } else {
                  // We have two confirmed centers
                  // How far down can we skip before resuming looking for the next
                  // pattern? In the worst case, only the difference between the
                  // difference in the x / y coordinates of the two centers.
                  // This is the case where you find top left first. Draw it out.
                  hasSkipped = true;
                  return (int) (Math.Abs(firstConfirmedCenter.getX() - center.getX()) -
                      Math.Abs(firstConfirmedCenter.getY() - center.getY()));
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
          private bool haveMulitplyConfirmedCenters() {
            int confirmedCount = 0;
            float totalModuleSize = 0.0f;
            int max = possibleCenters.Count;
            for (int i = 0; i < max; i++) {
              FinderPattern pattern = (FinderPattern) possibleCenters[i];
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
            float average = totalModuleSize / max;
            float totalDeviation = 0.0f;
            for (int i = 0; i < max; i++) {
              FinderPattern pattern = (FinderPattern) possibleCenters[i];
              totalDeviation += Math.Abs(pattern.getEstimatedModuleSize() - average);
            }
            return totalDeviation <= 0.15f * totalModuleSize;
          }

          /**
           * @return the 3 best {@link FinderPattern}s from our list of candidates. The "best" are
           *         those that have been detected at least {@link #CENTER_QUORUM} times, and whose module
           *         size differs from the average among those patterns the least
           * @throws ReaderException if 3 such finder patterns do not exist
           */
          private FinderPattern[] selectBestPatterns(){
            Collections.insertionSort(possibleCenters, new CenterComparator());
            int size = 0;
            int max = possibleCenters.Count;
            while (size < max) {
              if (((FinderPattern) possibleCenters[size]).getCount() < CENTER_QUORUM) {
                break;
              }
              size++;
            }

            if (size < 3) {
              // Couldn't find enough finder patterns
              throw new ReaderException();
            }

            if (size > 3) {
              // Throw away all but those first size candidate points we found.
                SupportClass.SetCapacity(possibleCenters, size);
              //  We need to pick the best three. Find the most
              // popular ones whose module size is nearest the average
              float averageModuleSize = 0.0f;
              for (int i = 0; i < size; i++) {
                averageModuleSize += ((FinderPattern) possibleCenters[i]).getEstimatedModuleSize();
              }
              averageModuleSize /= (float) size;
              // We don't have java.util.Collections in J2ME
              Collections.insertionSort(possibleCenters, new ClosestToAverageComparator(averageModuleSize));
            }

            return new FinderPattern[]{
                (FinderPattern) possibleCenters[0],
                (FinderPattern) possibleCenters[1],
                (FinderPattern) possibleCenters[2]
            };
          }

          /**
           * <p>Orders by {@link FinderPattern#getCount()}, descending.</p>
           */
          private class CenterComparator : Comparator {
              public int compare(object center1, object center2)
              {
              return ((FinderPattern) center2).getCount() - ((FinderPattern) center1).getCount();
            }
          }

          /**
           * <p>Orders by variance from average module size, ascending.</p>
           */
          private class ClosestToAverageComparator : Comparator {
            private  float averageModuleSize;

            public ClosestToAverageComparator(float averageModuleSize) {
              this.averageModuleSize = averageModuleSize;
            }

            public int compare(object center1, object center2)
            {
              return Math.Abs(((FinderPattern) center1).getEstimatedModuleSize() - averageModuleSize) <
                  Math.Abs(((FinderPattern) center2).getEstimatedModuleSize() - averageModuleSize) ?
                  -1 :
                  1;
            }
          }

    }

}