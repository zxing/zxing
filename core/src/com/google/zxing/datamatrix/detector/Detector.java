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

import com.google.zxing.NotFoundException;
import com.google.zxing.ResultPoint;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.Collections;
import com.google.zxing.common.Comparator;
import com.google.zxing.common.DetectorResult;
import com.google.zxing.common.GridSampler;
import com.google.zxing.common.detector.MonochromeRectangleDetector;

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

  //private static final int MAX_MODULES = 32;

  // Trick to avoid creating new Integer objects below -- a sort of crude copy of
  // the Integer.valueOf(int) optimization added in Java 5, not in J2ME
  private static final Integer[] INTEGERS =
      { new Integer(0), new Integer(1), new Integer(2), new Integer(3), new Integer(4) };
  // No, can't use valueOf()

  private final BitMatrix image;
  private final MonochromeRectangleDetector rectangleDetector;

  public Detector(BitMatrix image) {
    this.image = image;
    rectangleDetector = new MonochromeRectangleDetector(image);
  }

  /**
   * <p>Detects a Data Matrix Code in an image.</p>
   *
   * @return {@link DetectorResult} encapsulating results of detecting a QR Code
   * @throws NotFoundException if no Data Matrix Code can be found
   */
  public DetectorResult detect() throws NotFoundException {

    ResultPoint[] cornerPoints = rectangleDetector.detect();
    ResultPoint pointA = cornerPoints[0];
    ResultPoint pointB = cornerPoints[1];
    ResultPoint pointC = cornerPoints[2];
    ResultPoint pointD = cornerPoints[3];

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
      throw NotFoundException.getNotFoundInstance();
    }

    // Bottom left is correct but top left and bottom right might be switched
    ResultPoint[] corners = { maybeTopLeft, bottomLeft, maybeBottomRight };
    // Use the dot product trick to sort them out
    ResultPoint.orderBestPatterns(corners);

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
    // or bottom right should work here. The number of transitions could be higher than it should be
    // due to noise. So we try both and take the min.

    int dimension = Math.min(transitionsBetween(topLeft, topRight).getTransitions(), 
                             transitionsBetween(bottomRight, topRight).getTransitions());
    if ((dimension & 0x01) == 1) {
      // it can't be odd, so, round... up?
      dimension++;
    }
    dimension += 2;

    BitMatrix bits = sampleGrid(image, topLeft, bottomLeft, bottomRight, dimension);
    return new DetectorResult(bits, new ResultPoint[] {pointA, pointB, pointC, pointD});
  }

  /**
   * Increments the Integer associated with a key by one.
   */
  private static void increment(Hashtable table, ResultPoint key) {
    Integer value = (Integer) table.get(key);
    table.put(key, value == null ? INTEGERS[1] : INTEGERS[value.intValue() + 1]);
  }

  private static BitMatrix sampleGrid(BitMatrix image,
                                      ResultPoint topLeft,
                                      ResultPoint bottomLeft,
                                      ResultPoint bottomRight,
                                      int dimension) throws NotFoundException {

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
    boolean inBlack = image.get(steep ? fromY : fromX, steep ? fromX : fromY);
    for (int x = fromX, y = fromY; x != toX; x += xstep) {
      boolean isBlack = image.get(steep ? y : x, steep ? x : y);
      if (isBlack != inBlack) {
        transitions++;
        inBlack = isBlack;
      }
      error += dy;
      if (error > 0) {
        if (y == toY) {
          break;
        }
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
