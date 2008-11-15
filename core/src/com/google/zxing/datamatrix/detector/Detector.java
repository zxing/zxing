/*
 * Copyright 2008 ZXing authors
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

package com.google.zxing.datamatrix.detector;

import com.google.zxing.MonochromeBitmapSource;
import com.google.zxing.ReaderException;
import com.google.zxing.ResultPoint;
import com.google.zxing.BlackPointEstimationMethod;
import com.google.zxing.common.BitArray;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.Collections;
import com.google.zxing.common.Comparator;
import com.google.zxing.common.DetectorResult;
import com.google.zxing.common.GenericResultPoint;
import com.google.zxing.common.GridSampler;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

/**
 * <p>Encapsulates logic that can detect a Data Matrix Code in an image, even if the Data Matrix Code
 * is rotated or skewed, or partially obscured.</p>
 *
 * @author Sean Owen
 */
public final class Detector {

  private static final int MAX_MODULES = 32;

  // Trick to avoid creating new Integer objects below -- a sort of crude copy of
  // the Integer.valueOf(int) optimization added in Java 5, not in J2ME
  private static final Integer[] INTEGERS =
      { new Integer(0), new Integer(1), new Integer(2), new Integer(3), new Integer(4) };

  private final MonochromeBitmapSource image;

  public Detector(MonochromeBitmapSource image) {
    this.image = image;
  }

  /**
   * <p>Detects a Data Matrix Code in an image.</p>
   *
   * @return {@link DetectorResult} encapsulating results of detecting a QR Code
   * @throws ReaderException if no Data Matrix Code can be found
   */
  public DetectorResult detect() throws ReaderException {

    if (!BlackPointEstimationMethod.TWO_D_SAMPLING.equals(image.getLastEstimationMethod())) {
      image.estimateBlackPoint(BlackPointEstimationMethod.TWO_D_SAMPLING, 0);
    }

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
    ResultPoint pointA = findCornerFromCenter(halfHeight, -iSkip, minI, maxI, halfWidth,      0, minJ, maxJ, halfWidth >> 1);
    minI = (int) pointA.getY() - 1;
    ResultPoint pointB = findCornerFromCenter(halfHeight, 0,      minI, maxI, halfWidth, -jSkip, minJ, maxJ, halfHeight >> 1);
    minJ = (int) pointB.getX() - 1;
    ResultPoint pointC = findCornerFromCenter(halfHeight, 0,      minI, maxI, halfWidth,  jSkip, minJ, maxJ, halfHeight >> 1);
    maxJ = (int) pointC.getX() + 1;
    ResultPoint pointD = findCornerFromCenter(halfHeight,  iSkip, minI, maxI, halfWidth,      0, minJ, maxJ, halfWidth >> 1);
    maxI = (int) pointD.getY() + 1;
    // Go try to find point A again with better information -- might have been off at first.
    pointA = findCornerFromCenter(halfHeight, -iSkip, minI, maxI, halfWidth,      0, minJ, maxJ, halfWidth >> 2);

    // Point A and D are across the diagonal from one another,
    // as are B and C. Figure out which are the solid black lines
    // by counting transitions
    Vector transitions = new Vector(4);
    transitions.addElement(transitionsBetween(pointA, pointB));
    transitions.addElement(transitionsBetween(pointA, pointC));
    transitions.addElement(transitionsBetween(pointB, pointD));
    transitions.addElement(transitionsBetween(pointC, pointD));
    Collections.insertionSort(transitions, new ResultPointsAndTransitionsComparator());

    // Sort by number of transitions. First two will be the two solid sides; last two
    // will be the two alternating black/white sides
    ResultPointsAndTransitions lSideOne = (ResultPointsAndTransitions) transitions.elementAt(0);
    ResultPointsAndTransitions lSideTwo = (ResultPointsAndTransitions) transitions.elementAt(1);

    // Figure out which point is their intersection by tallying up the number of times we see the
    // endpoints in the four endpoints. One will show up twice.
    Hashtable pointCount = new Hashtable();
    increment(pointCount, lSideOne.getFrom());
    increment(pointCount, lSideOne.getTo());
    increment(pointCount, lSideTwo.getFrom());
    increment(pointCount, lSideTwo.getTo());

    ResultPoint maybeTopLeft = null;
    ResultPoint bottomLeft = null;
    ResultPoint maybeBottomRight = null;
    Enumeration points = pointCount.keys();
    while (points.hasMoreElements()) {
      ResultPoint point = (ResultPoint) points.nextElement();
      Integer value = (Integer) pointCount.get(point);
      if (value.intValue() == 2) {
        bottomLeft = point; // this is definitely the bottom left, then -- end of two L sides
      } else {
        // Otherwise it's either top left or bottom right -- just assign the two arbitrarily now
        if (maybeTopLeft == null) {
          maybeTopLeft = point;
        } else {
          maybeBottomRight = point;
        }
      }
    }

    if (maybeTopLeft == null || bottomLeft == null || maybeBottomRight == null) {
      throw new ReaderException("Could not find three corners");
    }

    // Bottom left is correct but top left and bottom right might be switched
    ResultPoint[] corners = new ResultPoint[] { maybeTopLeft, bottomLeft, maybeBottomRight };
    // Use the dot product trick to sort them out
    GenericResultPoint.orderBestPatterns(corners);

    // Now we know which is which:
    ResultPoint bottomRight = corners[0];
    bottomLeft = corners[1];
    ResultPoint topLeft = corners[2];

    // Which point didn't we find in relation to the "L" sides? that's the top right corner
    ResultPoint topRight;
    if (!pointCount.containsKey(pointA)) {
      topRight = pointA;
    } else if (!pointCount.containsKey(pointB)) {
      topRight = pointB;
    } else if (!pointCount.containsKey(pointC)) {
      topRight = pointC;
    } else {
      topRight = pointD;
    }

    // Next determine the dimension by tracing along the top or right side and counting black/white
    // transitions. Since we start inside a black module, we should see a number of transitions
    // equal to 1 less than the code dimension. Well, actually 2 less, because we are going to
    // end on a black module:

    // The top right point is actually the corner of a module, which is one of the two black modules
    // adjacent to the white module at the top right. Tracing to that corner from either the top left
    // or bottom right should work here, but, one will be more reliable since it's traced straight
    // up or across, rather than at a slight angle. We use dot products to figure out which is
    // better to use:
    int dimension;
    if (GenericResultPoint.crossProductZ(bottomLeft, bottomRight, topRight) <
        GenericResultPoint.crossProductZ(topRight, topLeft, bottomLeft)) {
      dimension = transitionsBetween(topLeft, topRight).getTransitions();
    } else {
      dimension = transitionsBetween(bottomRight, topRight).getTransitions();
    }
    dimension += 2;

    BitMatrix bits = sampleGrid(image, topLeft, bottomLeft, bottomRight, dimension);
    return new DetectorResult(bits, new ResultPoint[] {pointA, pointB, pointC, pointD});
  }

  /**
   * Attempts to locate a corner of the barcode by scanning up, down, left or right from a center
   * point which should be within the barcode.
   *
   * @param centerI center's i componennt (vertical)
   * @param di change in i per step. If scanning up this is negative; down, positive; left or right, 0
   * @param minI minimum value of i to search through (meaningless when di == 0)
   * @param maxI maximum value of i
   * @param centerJ center's j component (horizontal)
   * @param dj same as di but change in j per step instead
   * @param minJ see minI
   * @param maxJ see minJ
   * @param maxWhiteRun maximum run of white pixels that can still be considered to be within
   *  the barcode
   * @return a {@link ResultPoint} encapsulating the corner that was found
   * @throws ReaderException if such a point cannot be found
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
          throw new ReaderException("Center of image not within barcode");
        }
        // lastRange was found
        if (dj == 0) {
          int lastI = i - di;
          if (lastRange[0] < centerJ) {
            if (lastRange[1] > centerJ) {
              // straddle, choose one or the other based on direction
              return new GenericResultPoint(di > 0 ? lastRange[0] : lastRange[1], lastI);
            }
            return new GenericResultPoint(lastRange[0], lastI);
          } else {
            return new GenericResultPoint(lastRange[1], lastI);
          }
        } else {
          int lastJ = j - dj;
          if (lastRange[0] < centerI) {
            if (lastRange[1] > centerI) {
              return new GenericResultPoint(lastJ, dj < 0 ? lastRange[0] : lastRange[1]);
            }
            return new GenericResultPoint(lastJ, lastRange[0]);
          } else {
            return new GenericResultPoint(lastJ, lastRange[1]);
          }
        }
      }
      lastRange = range;
    }
    throw new ReaderException("Couldn't find an end to barcode");
  }

  /**
   * Increments the Integer associated with a key by one.
   */
  private static void increment(Hashtable table, ResultPoint key) {
    Integer value = (Integer) table.get(key);
    table.put(key, value == null ? INTEGERS[1] : INTEGERS[value.intValue() + 1]);
  }

  /**
   * Computes the start and end of a region of pixels, either horizontally or vertically, that could be
   * part of a Data Matrix barcode.
   *
   * @param fixedDimension if scanning horizontally, this is the row (the fixed vertical location) where
   *  we are scanning. If scanning vertically it's the colummn, the fixed horizontal location
   * @param maxWhiteRun largest run of white pixels that can still be considered part of the barcode region
   * @param minDim minimum pixel location, horizontally or vertically, to consider
   * @param maxDim maximum pixel location, horizontally or vertically, to consider
   * @param horizontal if true, we're scanning left-right, instead of up-down
   * @return int[] with start and end of found range, or null if no such range is found (e.g. only white was found)
   */
  private int[] blackWhiteRange(int fixedDimension, int maxWhiteRun, int minDim, int maxDim, boolean horizontal) {

    int center = (minDim + maxDim) / 2;

    BitArray rowOrColumn = horizontal ? image.getBlackRow(fixedDimension, null, 0, image.getWidth())
                                      : image.getBlackColumn(fixedDimension, null, 0, image.getHeight());

    // Scan left/up first
    int start = center;
    while (start >= minDim) {
      if (rowOrColumn.get(start)) {
        start--;
      } else {
        int whiteRunStart = start;
        do {
          start--;
        } while (start >= minDim && !rowOrColumn.get(start));
        int whiteRunSize = whiteRunStart - start;
        if (start < minDim || whiteRunSize > maxWhiteRun) {
          start = whiteRunStart + 1; // back up
          break;
        }
      }
    }
    start++;

    // Then try right/down
    int end = center;
    while (end < maxDim) {
      if (rowOrColumn.get(end)) {
        end++;
      } else {
        int whiteRunStart = end;
        do {
          end++;
        } while (end < maxDim && !rowOrColumn.get(end));
        int whiteRunSize = end - whiteRunStart;
        if (end >= maxDim || whiteRunSize > maxWhiteRun) {
          end = whiteRunStart - 1;
          break;
        }
      }
    }
    end--;

    if (end > start) {
      return new int[] { start, end };
    } else {
      return null;
    }
  }

  private static BitMatrix sampleGrid(MonochromeBitmapSource image,
                                      ResultPoint topLeft,
                                      ResultPoint bottomLeft,
                                      ResultPoint bottomRight,
                                      int dimension) throws ReaderException {

    // We make up the top right point for now, based on the others.
    // TODO: we actually found a fourth corner above and figured out which of two modules
    // it was the corner of. We could use that here and adjust for perspective distortion.
    float topRightX = (bottomRight.getX() - bottomLeft.getX()) + topLeft.getX();
    float topRightY = (bottomRight.getY() - bottomLeft.getY()) + topLeft.getY();

    // Note that unlike in the QR Code sampler, we didn't find the center of modules, but the
    // very corners. So there is no 0.5f here; 0.0f is right.
    GridSampler sampler = GridSampler.getInstance();
    return sampler.sampleGrid(
        image,
        dimension,
        0.0f,
        0.0f,
        dimension,
        0.0f,
        dimension,
        dimension,
        0.0f,
        dimension,
        topLeft.getX(),
        topLeft.getY(),
        topRightX,
        topRightY,
        bottomRight.getX(),
        bottomRight.getY(),
        bottomLeft.getX(),
        bottomLeft.getY());
  }

  /**
   * Counts the number of black/white transitions between two points, using something like Bresenham's algorithm.
   */
  private ResultPointsAndTransitions transitionsBetween(ResultPoint from, ResultPoint to) {
    // See QR Code Detector, sizeOfBlackWhiteBlackRun()
    int fromX = (int) from.getX();
    int fromY = (int) from.getY();
    int toX = (int) to.getX();
    int toY = (int) to.getY();
    boolean steep = Math.abs(toY - fromY) > Math.abs(toX - fromX);
    if (steep) {
      int temp = fromX;
      fromX = fromY;
      fromY = temp;
      temp = toX;
      toX = toY;
      toY = temp;
    }

    int dx = Math.abs(toX - fromX);
    int dy = Math.abs(toY - fromY);
    int error = -dx >> 1;
    int ystep = fromY < toY ? 1 : -1;
    int xstep = fromX < toX ? 1 : -1;
    int transitions = 0;
    boolean inBlack = image.isBlack(steep ? fromY : fromX, steep ? fromX : fromY);
    for (int x = fromX, y = fromY; x != toX; x += xstep) {
      boolean isBlack = image.isBlack(steep ? y : x, steep ? x : y);
      if (isBlack == !inBlack) {
        transitions++;
        inBlack = isBlack;
      }
      error += dy;
      if (error > 0) {
        y += ystep;
        error -= dx;
      }
    }
    return new ResultPointsAndTransitions(from, to, transitions);
  }

  /**
   * Simply encapsulates two points and a number of transitions between them.
   */
  private static class ResultPointsAndTransitions {
    private final ResultPoint from;
    private final ResultPoint to;
    private final int transitions;
    private ResultPointsAndTransitions(ResultPoint from, ResultPoint to, int transitions) {
      this.from = from;
      this.to = to;
      this.transitions = transitions;
    }
    public ResultPoint getFrom() {
      return from;
    }
    public ResultPoint getTo() {
      return to;
    }
    public int getTransitions() {
      return transitions;
    }
    public String toString() {
      return from + "/" + to + '/' + transitions;
    }
  }

  /**
   * Orders ResultPointsAndTransitions by number of transitions, ascending.
   */
  private static class ResultPointsAndTransitionsComparator implements Comparator {
    public int compare(Object o1, Object o2) {
      return ((ResultPointsAndTransitions) o1).getTransitions() - ((ResultPointsAndTransitions) o2).getTransitions();
    }
  }

}
