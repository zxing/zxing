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

package com.google.zxing.common.detector;

import com.google.zxing.ReaderException;
import com.google.zxing.ResultPoint;
import com.google.zxing.common.BitArray;
import com.google.zxing.common.BitMatrix;

/**
 * <p>A somewhat generic detector that looks for a barcode-like rectangular region within an image.
 * It looks within a mostly white region of an image for a region of black and white, but mostly
 * black. It returns the four corners of the region, as best it can determine.</p>
 *
 * @author Sean Owen
 */
public final class MonochromeRectangleDetector {

  private static final int MAX_MODULES = 32;

  private final BitMatrix image;

  public MonochromeRectangleDetector(BitMatrix image) {
    this.image = image;
  }

  /**
   * <p>Detects a rectangular region of black and white -- mostly black -- with a region of mostly
   * white, in an image.</p>
   *
   * @return {@link ResultPoint}[] describing the corners of the rectangular region. The first and
   *  last points are opposed on the diagonal, as are the second and third. The first point will be
   *  the topmost point and the last, the bottommost. The second point will be leftmost and the
   *  third, the rightmost
   * @throws ReaderException if no Data Matrix Code can be found
   */
  public ResultPoint[] detect() throws ReaderException {
    int height = image.getHeight();
    int width = image.getWidth();
    int halfHeight = height >> 1;
    int halfWidth = width >> 1;
    int iSkip = Math.max(1, height / (MAX_MODULES << 3));
    int jSkip = Math.max(1, width / (MAX_MODULES << 3));

    int minI = 0;
    int maxI = height;
    int minJ = 0;
    int maxJ = width;
    ResultPoint pointA = findCornerFromCenter(halfHeight, -iSkip, minI, maxI, halfWidth,      0,
        minJ, maxJ, halfWidth >> 1);
    minI = (int) pointA.getY() - 1;
    ResultPoint pointB = findCornerFromCenter(halfHeight, 0,      minI, maxI, halfWidth, -jSkip,
        minJ, maxJ, halfHeight >> 1);
    minJ = (int) pointB.getX() - 1;
    ResultPoint pointC = findCornerFromCenter(halfHeight, 0,      minI, maxI, halfWidth,  jSkip,
        minJ, maxJ, halfHeight >> 1);
    maxJ = (int) pointC.getX() + 1;
    ResultPoint pointD = findCornerFromCenter(halfHeight,  iSkip, minI, maxI, halfWidth,      0,
        minJ, maxJ, halfWidth >> 1);
    maxI = (int) pointD.getY() + 1;
    // Go try to find point A again with better information -- might have been off at first.
    pointA = findCornerFromCenter(halfHeight, -iSkip, minI, maxI, halfWidth, 0, minJ, maxJ,
        halfWidth >> 2);

    return new ResultPoint[] { pointA, pointB, pointC, pointD };
  }

  /**
   * Attempts to locate a corner of the barcode by scanning up, down, left or right from a center
   * point which should be within the barcode.
   *
   * @param centerI center's i componennt (vertical)
   * @param di change in i per step. If scanning up this is negative; down, positive;
   *  left or right, 0
   * @param minI minimum value of i to search through (meaningless when di == 0)
   * @param maxI maximum value of i
   * @param centerJ center's j component (horizontal)
   * @param dj same as di but change in j per step instead
   * @param minJ see minI
   * @param maxJ see minJ
   * @param maxWhiteRun maximum run of white pixels that can still be considered to be within
   *  the barcode
   * @return a {@link com.google.zxing.ResultPoint} encapsulating the corner that was found
   * @throws com.google.zxing.ReaderException if such a point cannot be found
   */
  private ResultPoint findCornerFromCenter(int centerI, int di, int minI, int maxI,
                                           int centerJ, int dj, int minJ, int maxJ,
                                           int maxWhiteRun) throws ReaderException {
    int[] lastRange = null;
    for (int i = centerI, j = centerJ;
         i < maxI && i >= minI && j < maxJ && j >= minJ;
         i += di, j += dj) {
      int[] range;
      if (dj == 0) {
        // horizontal slices, up and down
        range = blackWhiteRange(i, maxWhiteRun, minJ, maxJ, true);
      } else {
        // vertical slices, left and right
        range = blackWhiteRange(j, maxWhiteRun, minI, maxI, false);
      }
      if (range == null) {
        if (lastRange == null) {
          throw ReaderException.getInstance();
        }
        // lastRange was found
        if (dj == 0) {
          int lastI = i - di;
          if (lastRange[0] < centerJ) {
            if (lastRange[1] > centerJ) {
              // straddle, choose one or the other based on direction
              return new ResultPoint(di > 0 ? lastRange[0] : lastRange[1], lastI);
            }
            return new ResultPoint(lastRange[0], lastI);
          } else {
            return new ResultPoint(lastRange[1], lastI);
          }
        } else {
          int lastJ = j - dj;
          if (lastRange[0] < centerI) {
            if (lastRange[1] > centerI) {
              return new ResultPoint(lastJ, dj < 0 ? lastRange[0] : lastRange[1]);
            }
            return new ResultPoint(lastJ, lastRange[0]);
          } else {
            return new ResultPoint(lastJ, lastRange[1]);
          }
        }
      }
      lastRange = range;
    }
    throw ReaderException.getInstance();
  }

  /**
   * Computes the start and end of a region of pixels, either horizontally or vertically, that could
   * be part of a Data Matrix barcode.
   *
   * @param fixedDimension if scanning horizontally, this is the row (the fixed vertical location)
   *  where we are scanning. If scanning vertically it's the colummn, the fixed horizontal location
   * @param maxWhiteRun largest run of white pixels that can still be considered part of the
   *  barcode region
   * @param minDim minimum pixel location, horizontally or vertically, to consider
   * @param maxDim maximum pixel location, horizontally or vertically, to consider
   * @param horizontal if true, we're scanning left-right, instead of up-down
   * @return int[] with start and end of found range, or null if no such range is found
   *  (e.g. only white was found)
   */
  private int[] blackWhiteRange(int fixedDimension, int maxWhiteRun, int minDim, int maxDim,
      boolean horizontal) {

    int center = (minDim + maxDim) >> 1;

    // Scan left/up first
    int start = center;
    while (start >= minDim) {
      if (horizontal ? image.get(start, fixedDimension) : image.get(fixedDimension, start)) {
        start--;
      } else {
        int whiteRunStart = start;
        do {
          start--;
        } while (start >= minDim && !(horizontal ? image.get(start, fixedDimension) : image.get(fixedDimension, start)));
        int whiteRunSize = whiteRunStart - start;
        if (start < minDim || whiteRunSize > maxWhiteRun) {
          start = whiteRunStart;
          break;
        }
      }
    }
    start++;

    // Then try right/down
    int end = center;
    while (end < maxDim) {
      if (horizontal ? image.get(end, fixedDimension) : image.get(fixedDimension, end)) {
        end++;
      } else {
        int whiteRunStart = end;
        do {
          end++;
        } while (end < maxDim && !(horizontal ? image.get(end, fixedDimension) : image.get(fixedDimension, end)));
        int whiteRunSize = end - whiteRunStart;
        if (end >= maxDim || whiteRunSize > maxWhiteRun) {
          end = whiteRunStart;
          break;
        }
      }
    }
    end--;

    return end > start ? new int[]{start, end} : null;
  }

}