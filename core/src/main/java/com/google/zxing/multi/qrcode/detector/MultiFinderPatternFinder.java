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

package com.google.zxing.multi.qrcode.detector;

import com.google.zxing.DecodeHintType;
import com.google.zxing.NotFoundException;
import com.google.zxing.ResultPoint;
import com.google.zxing.ResultPointCallback;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.detector.FinderPattern;
import com.google.zxing.qrcode.detector.FinderPatternFinder;
import com.google.zxing.qrcode.detector.FinderPatternInfo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

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
final class MultiFinderPatternFinder extends FinderPatternFinder {

  private static final FinderPatternInfo[] EMPTY_RESULT_ARRAY = new FinderPatternInfo[0];
  private static final FinderPattern[] EMPTY_FP_ARRAY = new FinderPattern[0];
  private static final FinderPattern[][] EMPTY_FP_2D_ARRAY = new FinderPattern[0][];

  // TODO MIN_MODULE_COUNT and MAX_MODULE_COUNT would be great hints to ask the user for
  // since it limits the number of regions to decode

  // max. legal count of modules per QR code edge (177)
  private static final float MAX_MODULE_COUNT_PER_EDGE = 180;
  // min. legal count per modules per QR code edge (11)
  private static final float MIN_MODULE_COUNT_PER_EDGE = 9;

  /**
   * More or less arbitrary cutoff point for determining if two finder patterns might belong
   * to the same code if they differ less than DIFF_MODSIZE_CUTOFF_PERCENT percent in their
   * estimated modules sizes.
   */
  private static final float DIFF_MODSIZE_CUTOFF_PERCENT = 0.05f;

  /**
   * More or less arbitrary cutoff point for determining if two finder patterns might belong
   * to the same code if they differ less than DIFF_MODSIZE_CUTOFF pixels/module in their
   * estimated modules sizes.
   */
  private static final float DIFF_MODSIZE_CUTOFF = 0.5f;


  /**
   * A comparator that orders FinderPatterns by their estimated module size.
   */
  private static final class ModuleSizeComparator implements Comparator<FinderPattern>, Serializable {
    @Override
    public int compare(FinderPattern center1, FinderPattern center2) {
      float value = center2.getEstimatedModuleSize() - center1.getEstimatedModuleSize();
      return value < 0.0 ? -1 : value > 0.0 ? 1 : 0;
    }
  }

  MultiFinderPatternFinder(BitMatrix image, ResultPointCallback resultPointCallback) {
    super(image, resultPointCallback);
  }

  /**
   * @return the 3 best {@link FinderPattern}s from our list of candidates. The "best" are
   *         those that have been detected at least 2 times, and whose module
   *         size differs from the average among those patterns the least
   * @throws NotFoundException if 3 such finder patterns do not exist
   */
  private FinderPattern[][] selectMultipleBestPatterns() throws NotFoundException {
    List<FinderPattern> possibleCenters = getPossibleCenters();
    int size = possibleCenters.size();

    if (size < 3) {
      // Couldn't find enough finder patterns
      throw NotFoundException.getNotFoundInstance();
    }

    /*
     * Begin HE modifications to safely detect multiple codes of equal size
     */
    if (size == 3) {
      return new FinderPattern[][] { possibleCenters.toArray(EMPTY_FP_ARRAY) };
    }

    // Sort by estimated module size to speed up the upcoming checks
    Collections.sort(possibleCenters, new ModuleSizeComparator());

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
     * a QR code, or are just by chance laid out so it looks like there might be a QR code there.
     * So, if the layout seems right, lets have the decoder try to decode.
     */

     List<FinderPattern[]> results = new ArrayList<>(); // holder for the results

    for (int i1 = 0; i1 < (size - 2); i1++) {
      FinderPattern p1 = possibleCenters.get(i1);
      if (p1 == null) {
        continue;
      }

      for (int i2 = i1 + 1; i2 < (size - 1); i2++) {
        FinderPattern p2 = possibleCenters.get(i2);
        if (p2 == null) {
          continue;
        }

        // Compare the expected module sizes; if they are really off, skip
        float vModSize12 = (p1.getEstimatedModuleSize() - p2.getEstimatedModuleSize()) /
            Math.min(p1.getEstimatedModuleSize(), p2.getEstimatedModuleSize());
        float vModSize12A = Math.abs(p1.getEstimatedModuleSize() - p2.getEstimatedModuleSize());
        if (vModSize12A > DIFF_MODSIZE_CUTOFF && vModSize12 >= DIFF_MODSIZE_CUTOFF_PERCENT) {
          // break, since elements are ordered by the module size deviation there cannot be
          // any more interesting elements for the given p1.
          break;
        }

        for (int i3 = i2 + 1; i3 < size; i3++) {
          FinderPattern p3 = possibleCenters.get(i3);
          if (p3 == null) {
            continue;
          }

          // Compare the expected module sizes; if they are really off, skip
          float vModSize23 = (p2.getEstimatedModuleSize() - p3.getEstimatedModuleSize()) /
              Math.min(p2.getEstimatedModuleSize(), p3.getEstimatedModuleSize());
          float vModSize23A = Math.abs(p2.getEstimatedModuleSize() - p3.getEstimatedModuleSize());
          if (vModSize23A > DIFF_MODSIZE_CUTOFF && vModSize23 >= DIFF_MODSIZE_CUTOFF_PERCENT) {
            // break, since elements are ordered by the module size deviation there cannot be
            // any more interesting elements for the given p1.
            break;
          }

          FinderPattern[] test = {p1, p2, p3};
          ResultPoint.orderBestPatterns(test);

          // Calculate the distances: a = topleft-bottomleft, b=topleft-topright, c = diagonal
          FinderPatternInfo info = new FinderPatternInfo(test);
          float dA = ResultPoint.distance(info.getTopLeft(), info.getBottomLeft());
          float dC = ResultPoint.distance(info.getTopRight(), info.getBottomLeft());
          float dB = ResultPoint.distance(info.getTopLeft(), info.getTopRight());

          // Check the sizes
          float estimatedModuleCount = (dA + dB) / (p1.getEstimatedModuleSize() * 2.0f);
          if (estimatedModuleCount > MAX_MODULE_COUNT_PER_EDGE ||
              estimatedModuleCount < MIN_MODULE_COUNT_PER_EDGE) {
            continue;
          }

          // Calculate the difference of the edge lengths in percent
          float vABBC = Math.abs((dA - dB) / Math.min(dA, dB));
          if (vABBC >= 0.1f) {
            continue;
          }

          // Calculate the diagonal length by assuming a 90° angle at topleft
          float dCpy = (float) Math.sqrt((double) dA * dA + (double) dB * dB);
          // Compare to the real distance in %
          float vPyC = Math.abs((dC - dCpy) / Math.min(dC, dCpy));

          if (vPyC >= 0.1f) {
            continue;
          }

          // All tests passed!
          results.add(test);
        }
      }
    }

    if (!results.isEmpty()) {
      return results.toArray(EMPTY_FP_2D_ARRAY);
    }

    // Nothing found!
    throw NotFoundException.getNotFoundInstance();
  }

  public FinderPatternInfo[] findMulti(Map<DecodeHintType,?> hints) throws NotFoundException {
    boolean tryHarder = hints != null && hints.containsKey(DecodeHintType.TRY_HARDER);
    BitMatrix image = getImage();
    int maxI = image.getHeight();
    int maxJ = image.getWidth();
    // We are looking for black/white/black/white/black modules in
    // 1:1:3:1:1 ratio; this tracks the number of such modules seen so far

    // Let's assume that the maximum version QR Code we support takes up 1/4 the height of the
    // image, and then account for the center being 3 modules in size. This gives the smallest
    // number of pixels the center could be, so skip this often. When trying harder, look for all
    // QR versions regardless of how dense they are.
    int iSkip = (3 * maxI) / (4 * MAX_MODULES);
    if (iSkip < MIN_SKIP || tryHarder) {
      iSkip = MIN_SKIP;
    }

    int[] stateCount = new int[5];
    for (int i = iSkip - 1; i < maxI; i += iSkip) {
      // Get a row of black/white values
      clearCounts(stateCount);
      int currentState = 0;
      for (int j = 0; j < maxJ; j++) {
        if (image.get(j, i)) {
          // Black pixel
          if ((currentState & 1) == 1) { // Counting white pixels
            currentState++;
          }
          stateCount[currentState]++;
        } else { // White pixel
          if ((currentState & 1) == 0) { // Counting black pixels
            if (currentState == 4) { // A winner?
              if (foundPatternCross(stateCount) && handlePossibleCenter(stateCount, i, j)) { // Yes
                // Clear state to start looking again
                currentState = 0;
                clearCounts(stateCount);
              } else { // No, shift counts back by two
                shiftCounts2(stateCount);
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
      }
    } // for i=iSkip-1 ...
    FinderPattern[][] patternInfo = selectMultipleBestPatterns();
    List<FinderPatternInfo> result = new ArrayList<>();
    for (FinderPattern[] pattern : patternInfo) {
      ResultPoint.orderBestPatterns(pattern);
      result.add(new FinderPatternInfo(pattern));
    }

    if (result.isEmpty()) {
      return EMPTY_RESULT_ARRAY;
    } else {
      return result.toArray(EMPTY_RESULT_ARRAY);
    }
  }

}
