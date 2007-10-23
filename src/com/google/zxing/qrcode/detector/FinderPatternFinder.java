/*
 * Copyright 2007 Google Inc.
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

import com.google.zxing.MonochromeBitmapSource;
import com.google.zxing.ReaderException;
import com.google.zxing.ResultPoint;
import com.google.zxing.common.BitArray;
import com.google.zxing.common.Collections;
import com.google.zxing.common.Comparator;

import java.util.Vector;

/**
 * <p>This class is not thread-safe and should not be reused.</p>
 *
 * @author srowen@google.com (Sean Owen)
 */
final class FinderPatternFinder {

  private static final int CENTER_QUORUM = 2;
  private static final int BIG_SKIP = 3;

  private final MonochromeBitmapSource image;
  private final Vector possibleCenters;
  private boolean hasSkipped;

  FinderPatternFinder(MonochromeBitmapSource image) {
    this.image = image;
    this.possibleCenters = new Vector(5);
  }

  FinderPatternInfo find() throws ReaderException {
    int maxI = image.getHeight();
    int maxJ = image.getWidth();
    int[] stateCount = new int[5]; // looking for 1 1 3 1 1
    boolean done = false;
    // We can afford to examine every few lines until we've started finding
    // the patterns
    int iSkip = BIG_SKIP;
    for (int i = iSkip - 1; i < maxI && !done; i += iSkip) {
      BitArray luminanceRow = image.getBlackRow(i, null, 0, maxJ);
      stateCount[0] = 0;
      stateCount[1] = 0;
      stateCount[2] = 0;
      stateCount[3] = 0;
      stateCount[4] = 0;
      int currentState = 0;
      for (int j = 0; j < maxJ; j++) {
        if (luminanceRow.get(j)) {
          // Black pixel
          if ((currentState & 1) == 1) { // Counting white pixels
            currentState++;
          }
          stateCount[currentState]++;
        } else { // White pixel
          if ((currentState & 1) == 0) { // Counting black pixels
            if (currentState == 4) { // A winner?
              if (foundPatternCross(stateCount)) { // Yes
                boolean confirmed =
                    handlePossibleCenter(stateCount, i, j);
                if (confirmed) {
                  iSkip = 1; // Go back to examining each line
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
                  } while (j < maxJ && !luminanceRow.get(j));
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
        boolean confirmed = handlePossibleCenter(stateCount, i, maxJ);
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
    patternInfo = orderBestPatterns(patternInfo);
    float totalModuleSize = 0.0f;
    for (int i = 0; i < patternInfo.length; i++) {
      totalModuleSize += patternInfo[i].getEstimatedModuleSize();
    }

    return new FinderPatternInfo(totalModuleSize / (float) patternInfo.length,
        patternInfo);
  }

  private static float centerFromEnd(int[] stateCount, int end) {
    return (float) (end - stateCount[4] - stateCount[3]) - stateCount[2] / 2.0f;
  }

  private static boolean foundPatternCross(int[] stateCount) {
    int totalModuleSize = 0;
    for (int i = 0; i < 5; i++) {
      if (stateCount[i] == 0) {
        return false;
      }
      totalModuleSize += stateCount[i];
    }
    if (totalModuleSize < 7) {
      return false;
    }
    int moduleSize = totalModuleSize / 7;
    // Allow less than 50% deviance from 1-1-3-1-1 pattern
    return
        Math.abs(moduleSize - stateCount[0]) << 1 <= moduleSize &&
            Math.abs(moduleSize - stateCount[1]) << 1 <= moduleSize &&
            Math.abs(3 * moduleSize - stateCount[2]) << 1 <= 3 * moduleSize &&
            Math.abs(moduleSize - stateCount[3]) << 1 <= moduleSize &&
            Math.abs(moduleSize - stateCount[4]) << 1 <= moduleSize;
  }

  private float crossCheckVertical(int startI, int centerJ, int maxCount) {
    MonochromeBitmapSource image = this.image;

    int maxI = image.getHeight();
    int[] stateCount = new int[5];

    int i = startI;
    while (i >= 0 && image.isBlack(centerJ, i)) {
      stateCount[2]++;
      i--;
    }
    if (i < 0) {
      return Float.NaN;
    }
    while (i >= 0 && !image.isBlack(centerJ, i) && stateCount[1] <= maxCount) {
      stateCount[1]++;
      i--;
    }
    // If already too many modules in this state or ran off the edge:
    if (i < 0 || stateCount[1] > maxCount) {
      return Float.NaN;
    }
    while (i >= 0 && image.isBlack(centerJ, i) && stateCount[0] <= maxCount) {
      stateCount[0]++;
      i--;
    }
    if (i < 0 || stateCount[0] > maxCount) {
      return Float.NaN;
    }

    i = startI + 1;
    while (i < maxI && image.isBlack(centerJ, i)) {
      stateCount[2]++;
      i++;
    }
    if (i == maxI) {
      return Float.NaN;
    }
    while (i < maxI && !image.isBlack(centerJ, i) && stateCount[3] < maxCount) {
      stateCount[3]++;
      i++;
    }
    if (i == maxI || stateCount[3] >= maxCount) {
      return Float.NaN;
    }
    while (i < maxI && image.isBlack(centerJ, i) && stateCount[4] < maxCount) {
      stateCount[4]++;
      i++;
    }
    if (stateCount[4] >= maxCount) {
      return Float.NaN;
    }

    return foundPatternCross(stateCount) ? centerFromEnd(stateCount, i) : Float.NaN;
  }

  private float crossCheckHorizontal(int startJ, int centerI, int maxCount) {
    MonochromeBitmapSource image = this.image;

    int maxJ = image.getWidth();
    int[] stateCount = new int[5];

    int j = startJ;
    while (j >= 0 && image.isBlack(j, centerI)) {
      stateCount[2]++;
      j--;
    }
    if (j < 0) {
      return Float.NaN;
    }
    while (j >= 0 && !image.isBlack(j, centerI) && stateCount[1] <= maxCount) {
      stateCount[1]++;
      j--;
    }
    // If already too many modules in this state or ran off the edge:
    if (j < 0 || stateCount[1] > maxCount) {
      return Float.NaN;
    }
    while (j >= 0 && image.isBlack(j, centerI) && stateCount[0] <= maxCount) {
      stateCount[0]++;
      j--;
    }
    if (j < 0 || stateCount[0] > maxCount) {
      return Float.NaN;
    }

    j = startJ + 1;
    while (j < maxJ && image.isBlack(j, centerI)) {
      stateCount[2]++;
      j++;
    }
    if (j == maxJ) {
      return Float.NaN;
    }
    while (j < maxJ && !image.isBlack(j, centerI) && stateCount[3] < maxCount) {
      stateCount[3]++;
      j++;
    }
    if (j == maxJ || stateCount[3] >= maxCount) {
      return Float.NaN;
    }
    while (j < maxJ && image.isBlack(j, centerI) && stateCount[4] < maxCount) {
      stateCount[4]++;
      j++;
    }
    if (stateCount[4] >= maxCount) {
      return Float.NaN;
    }

    return foundPatternCross(stateCount) ? centerFromEnd(stateCount, j) : Float.NaN;
  }

  private boolean handlePossibleCenter(int[] stateCount,
                                       int i,
                                       int j) {
    float centerJ = centerFromEnd(stateCount, j);
    float centerI = crossCheckVertical(i, (int) centerJ, stateCount[2]);
    if (!Float.isNaN(centerI)) {
      // Re-cross check
      centerJ = crossCheckHorizontal((int) centerJ, (int) centerI, stateCount[2]);
      if (!Float.isNaN(centerJ)) {
        float estimatedModuleSize = (float) (stateCount[0] +
            stateCount[1] +
            stateCount[2] +
            stateCount[3] +
            stateCount[4]) / 7.0f;
        boolean found = false;
        int max = possibleCenters.size();
        for (int index = 0; index < max; index++) {
          FinderPattern center = (FinderPattern) possibleCenters.elementAt(index);
          // Look for about the same center and module size:
          if (center.aboutEquals(estimatedModuleSize, centerI, centerJ)) {
            center.incrementCount();
            found = true;
            break;
          }
        }
        if (!found) {
          possibleCenters.addElement(
              new FinderPattern(centerJ, centerI, estimatedModuleSize));
        }
        return true;
      }
    }
    return false;
  }

  private int findRowSkip() {
    int max = possibleCenters.size();
    if (max <= 1) {
      return 0;
    }
    FinderPattern firstConfirmedCenter = null;
    for (int i = 0; i < max; i++) {
      FinderPattern center = (FinderPattern) possibleCenters.elementAt(i);
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
          return (int) Math.abs(Math.abs(firstConfirmedCenter.getX() - center.getX()) -
                                Math.abs(firstConfirmedCenter.getY() - center.getY()));
        }
      }
    }
    return 0;
  }

  private boolean haveMulitplyConfirmedCenters() {
    int count = 0;
    int max = possibleCenters.size();
    for (int i = 0; i < max; i++) {
      if (((FinderPattern) possibleCenters.elementAt(i)).getCount() >= CENTER_QUORUM) {
        if (++count == 3) {
          return true;
        }
      }
    }
    return false;
  }

  private FinderPattern[] selectBestPatterns() throws ReaderException {
    Collections.insertionSort(possibleCenters, new CenterComparator());
    int size = 0;
    int max = possibleCenters.size();
    while (size < max) {
      if (((FinderPattern) possibleCenters.elementAt(size)).getCount() < CENTER_QUORUM) {
        break;
      }
      size++;
    }

    if (size < 3) {
      // Couldn't find enough finder patterns
      throw new ReaderException("Could not find three finder patterns");
    }

    if (size == 3) {
      // Found just enough -- hope these are good!
      // toArray() is not available
      FinderPattern[] result = new FinderPattern[possibleCenters.size()];
      for (int i = 0; i < possibleCenters.size(); i++) {
        result[i] = (FinderPattern) possibleCenters.elementAt(i);
      }
      return result;
    }

    possibleCenters.setSize(size);

    // Hmm, multiple found. We need to pick the best three. Find the most
    // popular ones whose module size is nearest the average

    float averageModuleSize = 0.0f;
    for (int i = 0; i < size; i++) {
      averageModuleSize += ((FinderPattern) possibleCenters.elementAt(i)).getEstimatedModuleSize();
    }
    averageModuleSize /= (float) size;

    Collections.insertionSort(
        possibleCenters,
        new ClosestToAverageComparator(averageModuleSize));

    //return confirmedCenters.subList(0, 3).toArray(new FinderPattern[3]);
    FinderPattern[] result = new FinderPattern[3];
    for (int i = 0; i < 3; i++) {
      result[i] = (FinderPattern) possibleCenters.elementAt(i);
    }
    return result;
  }

  private static FinderPattern[] orderBestPatterns(FinderPattern[] patterns) {

    // Find distances between pattern centers
    float abDistance = distance(patterns[0], patterns[1]);
    float bcDistance = distance(patterns[1], patterns[2]);
    float acDistance = distance(patterns[0], patterns[2]);

    FinderPattern topLeft;
    FinderPattern topRight;
    FinderPattern bottomLeft;
    // Assume one closest to other two is top left
    if (bcDistance >= abDistance && bcDistance >= acDistance) {
      topLeft = patterns[0];
      topRight = patterns[1]; // These two are guesses at the moment
      bottomLeft = patterns[2];
    } else if (acDistance >= bcDistance && acDistance >= abDistance) {
      topLeft = patterns[1];
      topRight = patterns[0];
      bottomLeft = patterns[2];
    } else {
      topLeft = patterns[2];
      topRight = patterns[0];
      bottomLeft = patterns[1];
    }

    // Use cross product to figure out which of other1/2 is the bottom left
    // pattern. The vector "top-left -> bottom-left" x "top-left -> top-right"
    // should yield a vector with positive z component
    if ((bottomLeft.getY() - topLeft.getY()) * (topRight.getX() - topLeft.getX()) <
        (bottomLeft.getX() - topLeft.getX()) * (topRight.getY() - topLeft.getY())) {
      FinderPattern temp = topRight;
      topRight = bottomLeft;
      bottomLeft = temp;
    }

    return new FinderPattern[]{bottomLeft, topLeft, topRight};
  }

  static float distance(ResultPoint pattern1, ResultPoint pattern2) {
    float xDiff = pattern1.getX() - pattern2.getX();
    float yDiff = pattern1.getY() - pattern2.getY();
    return (float) Math.sqrt((double) (xDiff * xDiff + yDiff * yDiff));
  }

  private static class CenterComparator implements Comparator {
    public int compare(Object center1, Object center2) {
      return ((FinderPattern) center2).getCount() - ((FinderPattern) center1).getCount();
    }
  }

  private static class ClosestToAverageComparator implements Comparator {
    private float averageModuleSize;

    private ClosestToAverageComparator(float averageModuleSize) {
      this.averageModuleSize = averageModuleSize;
    }

    public int compare(Object center1, Object center2) {
      return
          Math.abs(((FinderPattern) center1).getEstimatedModuleSize() - averageModuleSize) <
              Math.abs(((FinderPattern) center2).getEstimatedModuleSize() - averageModuleSize) ?
              -1 :
              1;
    }
  }

}
