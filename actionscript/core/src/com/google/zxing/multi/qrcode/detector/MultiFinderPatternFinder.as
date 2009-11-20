	/*
 * Copyright 2009 ZXing authors
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

package com.google.zxing.multi.qrcode.detector
{

import com.google.zxing.DecodeHintType;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.Collections;
import com.google.zxing.common.flexdatatypes.ArrayList;
import com.google.zxing.common.flexdatatypes.HashTable;
import com.google.zxing.ResultPoint;
import com.google.zxing.qrcode.detector.FinderPattern;
import com.google.zxing.qrcode.detector.FinderPatternFinder;
import com.google.zxing.qrcode.detector.FinderPatternInfo;
import com.google.zxing.ReaderException;

/**
 * <p>This class attempts to find finder patterns in a QR Code. Finder patterns are the square
 * markers at three corners of a QR Code.</p>
 *
 * <p>This class is thread-safe but not reentrant. Each thread must allocate its own object.
 *
 * <p>In contrast to {@link FinderPatternFinder}, this class will return an array of all possible
 * QR code locations in the image.</p>
 *
 * <p>Use the TRY_HARDER hint to ask for a more thorough detection.</p>
 *
 * @author Sean Owen
 * @author Hannes Erven
 */
public final class MultiFinderPatternFinder extends FinderPatternFinder {

  private static var EMPTY_RESULT_ARRAY:Array = new Array(0);

  // TODO MIN_MODULE_COUNT and MAX_MODULE_COUNT would be great hints to ask the user for
  // since it limits the number of regions to decode

  // max. legal count of modules per QR code edge (177)
  private static var MAX_MODULE_COUNT_PER_EDGE:Number = 180;
  // min. legal count per modules per QR code edge (11)
  private static var MIN_MODULE_COUNT_PER_EDGE:Number = 9;

  /**
   * More or less arbitrary cutoff point for determining if two finder patterns might belong
   * to the same code if they differ less than DIFF_MODSIZE_CUTOFF_PERCENT percent in their
   * estimated modules sizes.
   */
  private static  var DIFF_MODSIZE_CUTOFF_PERCENT:Number = 0.05;

  /**
   * More or less arbitrary cutoff point for determining if two finder patterns might belong
   * to the same code if they differ less than DIFF_MODSIZE_CUTOFF pixels/module in their
   * estimated modules sizes.
   */
  private static  var DIFF_MODSIZE_CUTOFF:Number = 0.5;



  /**
   * <p>Creates a finder that will search the image for three finder patterns.</p>
   *
   * @param image image to search
   */
  public function MultiFinderPatternFinder(image:BitMatrix) {
    super(image);
  }

  /**
   * @return the 3 best {@link FinderPattern}s from our list of candidates. The "best" are
   *         those that have been detected at least {@link #CENTER_QUORUM} times, and whose module
   *         size differs from the average among those patterns the least
   * @throws ReaderException if 3 such finder patterns do not exist
   */
  private function selectBestPatterns():Array {
    var possibleCenters:ArrayList = getPossibleCenters();
    var size:int = possibleCenters.size();

    if (size < 3) {
      // Couldn't find enough finder patterns
      throw new ReaderException("multi : qrcode : detector : MultiFinderPatternFinder : selectBestPatterns");
    }

    /*
     * Begin HE modifications to safely detect multiple codes of equal size
     */
    if (size == 3) {
      return [
              possibleCenters.elementAt(0) as FinderPattern,
              possibleCenters.elementAt(1) as FinderPattern,
              possibleCenters.elementAt(2) as FinderPattern
          ];
    }

    // Sort by estimated module size to speed up the upcoming checks
    Collections.insertionSort(possibleCenters, new ModuleSizeComparator());

    /*
     * Now lets start: build a list of tuples of three finder locations that
     *  - feature similar module sizes
     *  - are placed in a distance so the estimated module count is within the QR specification
     *  - have similar distance between upper left/right and left top/bottom finder patterns
     *  - form a triangle with 90° angle (checked by comparing top right/bottom left distance
     *    with pythagoras)
     *
     * Note: we allow each point to be used for more than one code region: this might seem
     * counterintuitive at first, but the performance penalty is not that big. At this point,
     * we cannot make a good quality decision whether the three finders actually represent
     * a QR code, or are just by chance layouted so it looks like there might be a QR code there.
     * So, if the layout seems right, lets have the decoder try to decode.     
     */

    var results:ArrayList = new ArrayList(); // holder for the results

    for (var i1:int = 0; i1 < (size - 2); i1++) {
      var p1:FinderPattern = possibleCenters.elementAt(i1) as FinderPattern;
      if (p1 == null) {
        continue;
      }

      for (var i2:int = i1 + 1; i2 < (size - 1); i2++) {
        var p2:FinderPattern  = possibleCenters.elementAt(i2) as FinderPattern;
        if (p2 == null) {
          continue;
        }

        // Compare the expected module sizes; if they are really off, skip
        var vModSize12:Number = (p1.getEstimatedModuleSize() - p2.getEstimatedModuleSize()) /
            (Math.min(p1.getEstimatedModuleSize(), p2.getEstimatedModuleSize()));
        var vModSize12A:Number = Math.abs(p1.getEstimatedModuleSize() - p2.getEstimatedModuleSize());
        if (vModSize12A > DIFF_MODSIZE_CUTOFF && vModSize12 >= DIFF_MODSIZE_CUTOFF_PERCENT) {
          // break, since elements are ordered by the module size deviation there cannot be
          // any more interesting elements for the given p1.
          break;
        }

        for (var i3:int  = i2 + 1; i3 < size; i3++) {
          var p3:FinderPattern =  possibleCenters.elementAt(i3) as FinderPattern;
          if (p3 == null) {
            continue;
          }

          // Compare the expected module sizes; if they are really off, skip
          var vModSize23:Number = (p2.getEstimatedModuleSize() - p3.getEstimatedModuleSize()) /
              (Math.min(p2.getEstimatedModuleSize(), p3.getEstimatedModuleSize()));
          var vModSize23A:Number = Math.abs(p2.getEstimatedModuleSize() - p3.getEstimatedModuleSize());
          if (vModSize23A > DIFF_MODSIZE_CUTOFF && vModSize23 >= DIFF_MODSIZE_CUTOFF_PERCENT) {
            // break, since elements are ordered by the module size deviation there cannot be
            // any more interesting elements for the given p1.
            break;
          }

          var test:Array = [p1, p2, p3];
          ResultPoint.orderBestPatterns(test);

          // Calculate the distances: a = topleft-bottomleft, b=topleft-topright, c = diagonal
          var info:FinderPatternInfo = new FinderPatternInfo(test);
          var dA:Number = ResultPoint.distance(info.getTopLeft(), info.getBottomLeft());
          var dC:Number = ResultPoint.distance(info.getTopRight(), info.getBottomLeft());
          var dB:Number = ResultPoint.distance(info.getTopLeft(), info.getTopRight());

          // Check the sizes
          var estimatedModuleCount:Number = ((dA + dB) / p1.getEstimatedModuleSize()) / 2;
          if (estimatedModuleCount > MAX_MODULE_COUNT_PER_EDGE ||
              estimatedModuleCount < MIN_MODULE_COUNT_PER_EDGE) {
            continue;
          }

          // Calculate the difference of the edge lengths in percent
          var vABBC:Number = Math.abs(((dA - dB) / Math.min(dA, dB)));
          if (vABBC >= 0.1) {
            continue;
          }

          // Calculate the diagonal length by assuming a 90° angle at topleft
          var dCpy:Number = Math.sqrt(dA * dA + dB * dB);
          // Compare to the real distance in %
          var vPyC:Number = Math.abs(((dC - dCpy) / Math.min(dC, dCpy)));

          if (vPyC >= 0.1) {
            continue;
          }

          // All tests passed!
          results.addElement(test);
        } // end iterate p3
      } // end iterate p2
    } // end iterate p1

    if (!results.isEmpty()) {
      var resultArray:Array = new Array(results.size());
      for (var i:int = 0; i < results.size(); i++) {
        resultArray[i] = results.elementAt(i) as Array;
      }
      return resultArray;
    }

    // Nothing found!
    throw new ReaderException("multi: qrcode : detector : Nothing found!");
  }

  public function  findMulti(hints:HashTable ):Array {
    var tryHarder:Boolean  = hints != null && hints.containsKey(DecodeHintType.TRY_HARDER);
    var image:BitMatrix  = getImage();
    var maxI:int  = image.getHeight();
    var maxJ:int = image.getWidth();
    // We are looking for black/white/black/white/black modules in
    // 1:1:3:1:1 ratio; this tracks the number of such modules seen so far

    // Let's assume that the maximum version QR Code we support takes up 1/4 the height of the
    // image, and then account for the center being 3 modules in size. This gives the smallest
    // number of pixels the center could be, so skip this often. When trying harder, look for all
    // QR versions regardless of how dense they are.
    var iSkip:int = int((maxI / (MAX_MODULES * 4) * 3));
    if (iSkip < MIN_SKIP || tryHarder) {
      iSkip = MIN_SKIP;
    }

    var stateCount:Array = new Array(5);
    for (var i:int = iSkip - 1; i < maxI; i += iSkip) {
      // Get a row of black/white values
      stateCount[0] = 0;
      stateCount[1] = 0;
      stateCount[2] = 0;
      stateCount[3] = 0;
      stateCount[4] = 0;
      var currentState:int = 0;
      for (var j:int = 0; j < maxJ; j++) {
        if (image._get(j, i)) {
          // Black pixel
          if ((currentState & 1) == 1) { // Counting white pixels
            currentState++;
          }
          stateCount[currentState]++;
        } else { // White pixel
          if ((currentState & 1) == 0) { // Counting black pixels
            if (currentState == 4) { // A winner?
              if (foundPatternCross(stateCount)) { // Yes
                var confirmed:Boolean = handlePossibleCenter(stateCount, i, j);
                if (!confirmed) {
                  do { // Advance to next black pixel
                    j++;
                  } while (j < maxJ && !image._get(j, i));
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
      } // for j=...

      if (foundPatternCross(stateCount)) {
        handlePossibleCenter(stateCount, i, maxJ);
      } // end if foundPatternCross
    } // for i=iSkip-1 ...
    var patternInfo:Array = selectBestPatterns();
    var result:ArrayList = new ArrayList();
    for (var i2:int = 0; i2 < patternInfo.length; i2++) {
      var pattern:Array = patternInfo[i2];
      ResultPoint.orderBestPatterns(pattern);
      result.addElement(new FinderPatternInfo(pattern));
    }

    if (result.isEmpty()) {
      return EMPTY_RESULT_ARRAY;
    } else {
      var resultArray:Array = new Array(result.size());
      for (var i3:int = 0; i3 < result.size(); i3++) {
        resultArray[i3] = result.elementAt(i3) as FinderPatternInfo;
      }
      return resultArray;
    }
  }

}

}