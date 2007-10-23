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
import com.google.zxing.common.BitArray;

import java.util.Vector;

/**
 * <p>At the moment this only looks for the bottom-right alignment pattern.</p>
 *
 * <p>This class is not thread-safe.</p>
 *
 * @author srowen@google.com (Sean Owen)
 */
final class AlignmentPatternFinder {

  private final MonochromeBitmapSource image;
  private final Vector possibleCenters;
  private final int startX;
  private final int startY;
  private final int width;
  private final int height;
  private final float moduleSize;

  AlignmentPatternFinder(MonochromeBitmapSource image,
                         int startX,
                         int startY,
                         int width,
                         int height,
                         float moduleSize) {
    this.image = image;
    this.possibleCenters = new Vector(5);
    this.startX = startX;
    this.startY = startY;
    this.width = width;
    this.height = height;
    this.moduleSize = moduleSize;
  }

  AlignmentPattern find() throws ReaderException {
    int startX = this.startX;
    int height = this.height;
    int maxJ = startX + width;
    int middleI = startY + (height >> 1);
    BitArray luminanceRow = new BitArray(width);
    int[] stateCount = new int[3]; // looking for 1 1 1
    for (int iGen = 0; iGen < height; iGen++) {
      // Search from middle outwards
      int i = middleI +
          ((iGen & 0x01) == 0 ? ((iGen + 1) >> 1) : -((iGen + 1) >> 1));
      image.getBlackRow(i, luminanceRow, startX, width);
      stateCount[0] = 0;
      stateCount[1] = 0;
      stateCount[2] = 0;
      int currentState = 0;
      int j = startX;
      // Burn off leading white pixels before anything else; if we start in the middle of
      // a white run, it doesn't make sense to count its length, since we don't know if the
      // white run continued to the left of the start point
      while (!luminanceRow.get(j - startX) && j < maxJ) {
        j++;
      }
      while (j < maxJ) {
        if (luminanceRow.get(j - startX)) {
          // Black pixel
          if (currentState == 1) { // Counting black pixels
            stateCount[currentState]++;
          } else { // Counting white pixels
            if (currentState == 2) { // A winner?
              if (foundPatternCross(stateCount)) { // Yes
                AlignmentPattern confirmed =
                    handlePossibleCenter(stateCount, i, j);
                if (confirmed != null) {
                  return confirmed;
                }
              }
              stateCount[0] = stateCount[2];
              stateCount[1] = 1;
              stateCount[2] = 0;
              currentState = 1;
            } else {
              stateCount[++currentState]++;
            }
          }
        } else { // White pixel
          if (currentState == 1) { // Counting black pixels
            currentState++;
          }
          stateCount[currentState]++;
        }
        j++;
      }
      if (foundPatternCross(stateCount)) {
        AlignmentPattern confirmed = handlePossibleCenter(stateCount, i, maxJ);
        if (confirmed != null) {
          return confirmed;
        }
      }

    }

    // Hmm, nothing we saw was observed and confirmed twice. If we had
    // any guess at all, return it.
    if (!possibleCenters.isEmpty()) {
      return (AlignmentPattern) possibleCenters.elementAt(0);
    }

    throw new ReaderException("Could not find alignment pattern");
  }

  private static float centerFromEnd(int[] stateCount, int end) {
    return (float) (end - stateCount[2]) - stateCount[1] / 2.0f;
  }

  private boolean foundPatternCross(int[] stateCount) {
    float moduleSize = this.moduleSize;
    for (int i = 0; i < 3; i++) {
      if (2.0f * Math.abs(moduleSize - stateCount[i]) >= moduleSize) {
        return false;
      }
    }
    return true;
  }

  private float crossCheckVertical(int startI, int centerJ, int maxCount) {
    MonochromeBitmapSource image = this.image;

    int maxI = image.getHeight();
    int[] stateCount = new int[3];
    int i = startI;
    while (i >= 0 && image.isBlack(centerJ, i) && stateCount[1] <= maxCount) {
      stateCount[1]++;
      i--;
    }
    if (i < 0 || stateCount[1] > maxCount) {
      return Float.NaN;
    }
    while (i >= 0 && !image.isBlack(centerJ, i) && stateCount[0] <= maxCount) {
      stateCount[0]++;
      i--;
    }
    if (stateCount[0] > maxCount) {
      return Float.NaN;
    }

    i = startI + 1;
    while (i < maxI && image.isBlack(centerJ, i) &&
        stateCount[1] <= maxCount) {
      stateCount[1]++;
      i++;
    }
    if (i == maxI || stateCount[1] > maxCount) {
      return Float.NaN;
    }
    while (i < maxI && !image.isBlack(centerJ, i) &&
        stateCount[2] <= maxCount) {
      stateCount[2]++;
      i++;
    }
    if (stateCount[2] > maxCount) {
      return Float.NaN;
    }

    return
        foundPatternCross(stateCount) ?
            centerFromEnd(stateCount, i) :
            Float.NaN;
  }

  private AlignmentPattern handlePossibleCenter(int[] stateCount,
                                                int i,
                                                int j) {
    float centerJ = centerFromEnd(stateCount, j);
    float centerI = crossCheckVertical(i, (int) centerJ, 2 * stateCount[1]);
    if (!Float.isNaN(centerI)) {
      float estimatedModuleSize = (float) (stateCount[0] +
          stateCount[1] +
          stateCount[2]) / 3.0f;
      int max = possibleCenters.size();
      for (int index = 0; index < max; index++) {
        AlignmentPattern center = (AlignmentPattern) possibleCenters.elementAt(index);
        // Look for about the same center and module size:
        if (center.aboutEquals(estimatedModuleSize, centerI, centerJ)) {
          return new AlignmentPattern(centerJ, centerI, estimatedModuleSize);
        }
      }
      // Hadn't found this before; save it
      possibleCenters.addElement(new AlignmentPattern(centerJ, centerI, estimatedModuleSize));
    }
    return null;
  }

}