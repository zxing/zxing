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

package com.google.zxing.qrcode.detector;

import com.google.zxing.DecodeHintType;
import com.google.zxing.NotFoundException;
import com.google.zxing.ResultPoint;
import com.google.zxing.ResultPointCallback;
import com.google.zxing.common.BitMatrix;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
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
 * @author Sean Owen
 */
public class FinderPatternFinder {

  private static final int CENTER_QUORUM = 2;
  private static final EstimatedModuleComparator moduleComparator = new EstimatedModuleComparator();
  protected static final int MIN_SKIP = 3; // 1 pixel/module times 3 modules/center
  protected static final int MAX_MODULES = 97; // support up to version 20 for mobile clients

  private final BitMatrix image;
  private final List<FinderPattern> possibleCenters;
  private boolean hasSkipped;
  private final int[] crossCheckStateCount;
  private final ResultPointCallback resultPointCallback;

  /**
   * <p>Creates a finder that will search the image for three finder patterns.</p>
   *
   * @param image image to search
   */
  public FinderPatternFinder(BitMatrix image) {
    this(image, null);
  }

  public FinderPatternFinder(BitMatrix image, ResultPointCallback resultPointCallback) {
    this.image = image;
    this.possibleCenters = new ArrayList<>();
    this.crossCheckStateCount = new int[5];
    this.resultPointCallback = resultPointCallback;
  }

  protected final BitMatrix getImage() {
    return image;
  }

  protected final List<FinderPattern> getPossibleCenters() {
    return possibleCenters;
  }

  final FinderPatternInfo find(Map<DecodeHintType,?> hints) throws NotFoundException {
    boolean tryHarder = hints != null && hints.containsKey(DecodeHintType.TRY_HARDER);
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

    boolean done = false;
    int[] stateCount = new int[5];
    for (int i = iSkip - 1; i < maxI && !done; i += iSkip) {
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
              if (foundPatternCross(stateCount)) { // Yes
                boolean confirmed = handlePossibleCenter(stateCount, i, j);
                if (confirmed) {
                  // Start examining every other line. Checking each line turned out to be too
                  // expensive and didn't improve performance.
                  iSkip = 2;
                  if (hasSkipped) {
                    done = haveMultiplyConfirmedCenters();
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
                  shiftCounts2(stateCount);
                  currentState = 3;
                  continue;
                }
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
      }
      if (foundPatternCross(stateCount)) {
        boolean confirmed = handlePossibleCenter(stateCount, i, maxJ);
        if (confirmed) {
          iSkip = stateCount[0];
          if (hasSkipped) {
            // Found a third one
            done = haveMultiplyConfirmedCenters();
          }
        }
      }
    }

    FinderPattern[] patternInfo = selectBestPatterns();
    ResultPoint.orderBestPatterns(patternInfo);

    return new FinderPatternInfo(patternInfo);
  }

  /**
   * Given a count of black/white/black/white/black pixels just seen and an end position,
   * figures the location of the center of this run.
   */
  private static float centerFromEnd(int[] stateCount, int end) {
    return (end - stateCount[4] - stateCount[3]) - stateCount[2] / 2.0f;
  }

  /**
   * @param stateCount count of black/white/black/white/black pixels just read
   * @return true iff the proportions of the counts is close enough to the 1/1/3/1/1 ratios
   *         used by finder patterns to be considered a match
   */
  protected static boolean foundPatternCross(int[] stateCount) {
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
    float moduleSize = totalModuleSize / 7.0f;
    float maxVariance = moduleSize / 2.0f;
    // Allow less than 50% variance from 1-1-3-1-1 proportions
    return
        Math.abs(moduleSize - stateCount[0]) < maxVariance &&
        Math.abs(moduleSize - stateCount[1]) < maxVariance &&
        Math.abs(3.0f * moduleSize - stateCount[2]) < 3 * maxVariance &&
        Math.abs(moduleSize - stateCount[3]) < maxVariance &&
        Math.abs(moduleSize - stateCount[4]) < maxVariance;
  }

  /**
   * @param stateCount count of black/white/black/white/black pixels just read
   * @return true iff the proportions of the counts is close enough to the 1/1/3/1/1 ratios
   *         used by finder patterns to be considered a match
   */
  protected static boolean foundPatternDiagonal(int[] stateCount) {
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
    float moduleSize = totalModuleSize / 7.0f;
    float maxVariance = moduleSize / 1.333f;
    // Allow less than 75% variance from 1-1-3-1-1 proportions
    return
            Math.abs(moduleSize - stateCount[0]) < maxVariance &&
                    Math.abs(moduleSize - stateCount[1]) < maxVariance &&
                    Math.abs(3.0f * moduleSize - stateCount[2]) < 3 * maxVariance &&
                    Math.abs(moduleSize - stateCount[3]) < maxVariance &&
                    Math.abs(moduleSize - stateCount[4]) < maxVariance;
  }

  private int[] getCrossCheckStateCount() {
    clearCounts(crossCheckStateCount);
    return crossCheckStateCount;
  }

  protected final void clearCounts(int[] counts) {
    for (int x = 0; x < counts.length; x++) {
      counts[x] = 0;
    }
  }

  protected final void shiftCounts2(int[] stateCount) {
    stateCount[0] = stateCount[2];
    stateCount[1] = stateCount[3];
    stateCount[2] = stateCount[4];
    stateCount[3] = 1;
    stateCount[4] = 0;
  }

  /**
   * After a vertical and horizontal scan finds a potential finder pattern, this method
   * "cross-cross-cross-checks" by scanning down diagonally through the center of the possible
   * finder pattern to see if the same proportion is detected.
   * 
   * @param centerI row where a finder pattern was detected
   * @param centerJ center of the section that appears to cross a finder pattern
   * @return true if proportions are withing expected limits
   */
  private boolean crossCheckDiagonal(int centerI, int centerJ) {
    int[] stateCount = getCrossCheckStateCount();

    // Start counting up, left from center finding black center mass
    int i = 0;
    while (centerI >= i && centerJ >= i && image.get(centerJ - i, centerI - i)) {
      stateCount[2]++;
      i++;
    }
    if (stateCount[2] == 0) {
      return false;
    }

    // Continue up, left finding white space
    while (centerI >= i && centerJ >= i && !image.get(centerJ - i, centerI - i)) {
      stateCount[1]++;
      i++;
    }
    if (stateCount[1] == 0) {
      return false;
    }

    // Continue up, left finding black border
    while (centerI >= i && centerJ >= i && image.get(centerJ - i, centerI - i)) {
      stateCount[0]++;
      i++;
    }
    if (stateCount[0] == 0) {
      return false;
    }

    int maxI = image.getHeight();
    int maxJ = image.getWidth();

    // Now also count down, right from center
    i = 1;
    while (centerI + i < maxI && centerJ + i < maxJ && image.get(centerJ + i, centerI + i)) {
      stateCount[2]++;
      i++;
    }

    while (centerI + i < maxI && centerJ + i < maxJ && !image.get(centerJ + i, centerI + i)) {
      stateCount[3]++;
      i++;
    }
    if (stateCount[3] == 0) {
      return false;
    }

    while (centerI + i < maxI && centerJ + i < maxJ && image.get(centerJ + i, centerI + i)) {
      stateCount[4]++;
      i++;
    }
    if (stateCount[4] == 0) {
      return false;
    }

    return foundPatternDiagonal(stateCount);
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
  private float crossCheckVertical(int startI, int centerJ, int maxCount,
      int originalStateCountTotal) {
    BitMatrix image = this.image;

    int maxI = image.getHeight();
    int[] stateCount = getCrossCheckStateCount();

    // Start counting up from center
    int i = startI;
    while (i >= 0 && image.get(centerJ, i)) {
      stateCount[2]++;
      i--;
    }
    if (i < 0) {
      return Float.NaN;
    }
    while (i >= 0 && !image.get(centerJ, i) && stateCount[1] <= maxCount) {
      stateCount[1]++;
      i--;
    }
    // If already too many modules in this state or ran off the edge:
    if (i < 0 || stateCount[1] > maxCount) {
      return Float.NaN;
    }
    while (i >= 0 && image.get(centerJ, i) && stateCount[0] <= maxCount) {
      stateCount[0]++;
      i--;
    }
    if (stateCount[0] > maxCount) {
      return Float.NaN;
    }

    // Now also count down from center
    i = startI + 1;
    while (i < maxI && image.get(centerJ, i)) {
      stateCount[2]++;
      i++;
    }
    if (i == maxI) {
      return Float.NaN;
    }
    while (i < maxI && !image.get(centerJ, i) && stateCount[3] < maxCount) {
      stateCount[3]++;
      i++;
    }
    if (i == maxI || stateCount[3] >= maxCount) {
      return Float.NaN;
    }
    while (i < maxI && image.get(centerJ, i) && stateCount[4] < maxCount) {
      stateCount[4]++;
      i++;
    }
    if (stateCount[4] >= maxCount) {
      return Float.NaN;
    }

    // If we found a finder-pattern-like section, but its size is more than 40% different than
    // the original, assume it's a false positive
    int stateCountTotal = stateCount[0] + stateCount[1] + stateCount[2] + stateCount[3] +
        stateCount[4];
    if (5 * Math.abs(stateCountTotal - originalStateCountTotal) >= 2 * originalStateCountTotal) {
      return Float.NaN;
    }

    return foundPatternCross(stateCount) ? centerFromEnd(stateCount, i) : Float.NaN;
  }

  /**
   * <p>Like {@link #crossCheckVertical(int, int, int, int)}, and in fact is basically identical,
   * except it reads horizontally instead of vertically. This is used to cross-cross
   * check a vertical cross check and locate the real center of the alignment pattern.</p>
   */
  private float crossCheckHorizontal(int startJ, int centerI, int maxCount,
      int originalStateCountTotal) {
    BitMatrix image = this.image;

    int maxJ = image.getWidth();
    int[] stateCount = getCrossCheckStateCount();

    int j = startJ;
    while (j >= 0 && image.get(j, centerI)) {
      stateCount[2]++;
      j--;
    }
    if (j < 0) {
      return Float.NaN;
    }
    while (j >= 0 && !image.get(j, centerI) && stateCount[1] <= maxCount) {
      stateCount[1]++;
      j--;
    }
    if (j < 0 || stateCount[1] > maxCount) {
      return Float.NaN;
    }
    while (j >= 0 && image.get(j, centerI) && stateCount[0] <= maxCount) {
      stateCount[0]++;
      j--;
    }
    if (stateCount[0] > maxCount) {
      return Float.NaN;
    }

    j = startJ + 1;
    while (j < maxJ && image.get(j, centerI)) {
      stateCount[2]++;
      j++;
    }
    if (j == maxJ) {
      return Float.NaN;
    }
    while (j < maxJ && !image.get(j, centerI) && stateCount[3] < maxCount) {
      stateCount[3]++;
      j++;
    }
    if (j == maxJ || stateCount[3] >= maxCount) {
      return Float.NaN;
    }
    while (j < maxJ && image.get(j, centerI) && stateCount[4] < maxCount) {
      stateCount[4]++;
      j++;
    }
    if (stateCount[4] >= maxCount) {
      return Float.NaN;
    }

    // If we found a finder-pattern-like section, but its size is significantly different than
    // the original, assume it's a false positive
    int stateCountTotal = stateCount[0] + stateCount[1] + stateCount[2] + stateCount[3] +
        stateCount[4];
    if (5 * Math.abs(stateCountTotal - originalStateCountTotal) >= originalStateCountTotal) {
      return Float.NaN;
    }

    return foundPatternCross(stateCount) ? centerFromEnd(stateCount, j) : Float.NaN;
  }

  /**
   * @param stateCount reading state module counts from horizontal scan
   * @param i row where finder pattern may be found
   * @param j end of possible finder pattern in row
   * @param pureBarcode ignored
   * @return true if a finder pattern candidate was found this time
   * @deprecated only exists for backwards compatibility
   * @see #handlePossibleCenter(int[], int, int)
   */
  @Deprecated
  protected final boolean handlePossibleCenter(int[] stateCount, int i, int j, boolean pureBarcode) {
    return handlePossibleCenter(stateCount, i, j);
  }

  /**
   * <p>This is called when a horizontal scan finds a possible alignment pattern. It will
   * cross check with a vertical scan, and if successful, will, ah, cross-cross-check
   * with another horizontal scan. This is needed primarily to locate the real horizontal
   * center of the pattern in cases of extreme skew.
   * And then we cross-cross-cross check with another diagonal scan.</p>
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
  protected final boolean handlePossibleCenter(int[] stateCount, int i, int j) {
    int stateCountTotal = stateCount[0] + stateCount[1] + stateCount[2] + stateCount[3] +
        stateCount[4];
    float centerJ = centerFromEnd(stateCount, j);
    float centerI = crossCheckVertical(i, (int) centerJ, stateCount[2], stateCountTotal);
    if (!Float.isNaN(centerI)) {
      // Re-cross check
      centerJ = crossCheckHorizontal((int) centerJ, (int) centerI, stateCount[2], stateCountTotal);
      if (!Float.isNaN(centerJ) && crossCheckDiagonal((int) centerI, (int) centerJ)) {
        float estimatedModuleSize = stateCountTotal / 7.0f;
        boolean found = false;
        for (int index = 0; index < possibleCenters.size(); index++) {
          FinderPattern center = possibleCenters.get(index);
          // Look for about the same center and module size:
          if (center.aboutEquals(estimatedModuleSize, centerI, centerJ)) {
            possibleCenters.set(index, center.combineEstimate(centerI, centerJ, estimatedModuleSize));
            found = true;
            break;
          }
        }
        if (!found) {
          FinderPattern point = new FinderPattern(centerJ, centerI, estimatedModuleSize);
          possibleCenters.add(point);
          if (resultPointCallback != null) {
            resultPointCallback.foundPossibleResultPoint(point);
          }
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
    int max = possibleCenters.size();
    if (max <= 1) {
      return 0;
    }
    ResultPoint firstConfirmedCenter = null;
    for (FinderPattern center : possibleCenters) {
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
          return (int) (Math.abs(firstConfirmedCenter.getX() - center.getX()) -
              Math.abs(firstConfirmedCenter.getY() - center.getY())) / 2;
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
  private boolean haveMultiplyConfirmedCenters() {
    int confirmedCount = 0;
    float totalModuleSize = 0.0f;
    int max = possibleCenters.size();
    for (FinderPattern pattern : possibleCenters) {
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
    // 5% of the total module size estimates, it's too much.
    float average = totalModuleSize / max;
    float totalDeviation = 0.0f;
    for (FinderPattern pattern : possibleCenters) {
      totalDeviation += Math.abs(pattern.getEstimatedModuleSize() - average);
    }
    return totalDeviation <= 0.05f * totalModuleSize;
  }

  /**
   * Get square of distance between a and b.
   */
  private static double squaredDistance(FinderPattern a, FinderPattern b) {
    double x = a.getX() - b.getX();
    double y = a.getY() - b.getY();
    return x * x + y * y;
  }

  /**
   * @return the 3 best {@link FinderPattern}s from our list of candidates. The "best" are
   *         those have similar module size and form a shape closer to a isosceles right triangle.
   * @throws NotFoundException if 3 such finder patterns do not exist
   */
  private FinderPattern[] selectBestPatterns() throws NotFoundException {

    int startSize = possibleCenters.size();
    if (startSize < 3) {
      // Couldn't find enough finder patterns
      throw NotFoundException.getNotFoundInstance();
    }

    Collections.sort(possibleCenters, moduleComparator);

    double distortion = Double.MAX_VALUE;
    double[] squares = new double[3];
    FinderPattern[] bestPatterns = new FinderPattern[3];

    for (int i = 0; i < possibleCenters.size() - 2; i++) {
      FinderPattern fpi = possibleCenters.get(i);
      float minModuleSize = fpi.getEstimatedModuleSize();

      for (int j = i + 1; j < possibleCenters.size() - 1; j++) {
        FinderPattern fpj = possibleCenters.get(j);
        double squares0 = squaredDistance(fpi, fpj);

        for (int k = j + 1; k < possibleCenters.size(); k++) {
          FinderPattern fpk = possibleCenters.get(k);
          float maxModuleSize = fpk.getEstimatedModuleSize();
          if (maxModuleSize > minModuleSize * 1.4f) {
            // module size is not similar
            continue;
          }

          squares[0] = squares0;
          squares[1] = squaredDistance(fpj, fpk);
          squares[2] = squaredDistance(fpi, fpk);
          Arrays.sort(squares);

          // a^2 + b^2 = c^2 (Pythagorean theorem), and a = b (isosceles triangle).
          // Since any right triangle satisfies the formula c^2 - b^2 - a^2 = 0,
          // we need to check both two equal sides separately.
          // The value of |c^2 - 2 * b^2| + |c^2 - 2 * a^2| increases as dissimilarity
          // from isosceles right triangle.
          double d = Math.abs(squares[2] - 2 * squares[1]) + Math.abs(squares[2] - 2 * squares[0]);
          if (d < distortion) {
            distortion = d;
            bestPatterns[0] = fpi;
            bestPatterns[1] = fpj;
            bestPatterns[2] = fpk;
          }
        }
      }
    }

    if (distortion == Double.MAX_VALUE) {
        throw NotFoundException.getNotFoundInstance();
    }

    return bestPatterns;
  }

  /**
   * <p>Orders by {@link FinderPatternFinder#getEstimatedModuleSize()}</p>
   */
  private static final class EstimatedModuleComparator implements Comparator<FinderPattern>, Serializable {
    @Override
    public int compare(FinderPattern center1, FinderPattern center2) {
      return Float.compare(center1.getEstimatedModuleSize(), center2.getEstimatedModuleSize());
    }
  }

}
