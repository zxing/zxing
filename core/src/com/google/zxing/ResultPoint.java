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

package com.google.zxing;

/**
 * <p>Encapsulates a point of interest in an image containing a barcode. Typically, this
 * would be the location of a finder pattern or the corner of the barcode, for example.</p>
 *
 * @author Sean Owen
 */
public class ResultPoint {

  private final float x;
  private final float y;

  public ResultPoint(float x, float y) {
    this.x = x;
    this.y = y;
  }

  public final float getX() {
    return x;
  }

  public final float getY() {
    return y;
  }

  @Override
  public boolean equals(Object other) {
    if (other instanceof ResultPoint) {
      ResultPoint otherPoint = (ResultPoint) other;
      return x == otherPoint.x && y == otherPoint.y;
    }
    return false;
  }

  @Override
  public int hashCode() {
    return 31 * Float.floatToIntBits(x) + Float.floatToIntBits(y);
  }

  @Override
  public String toString() {
    StringBuilder result = new StringBuilder(25);
    result.append('(');
    result.append(x);
    result.append(',');
    result.append(y);
    result.append(')');
    return result.toString();
  }

  /**
   * <p>Orders an array of three ResultPoints in an order [A,B,C] such that AB < AC and
   * BC < AC and the angle between BC and BA is less than 180 degrees.
   */
  public static void orderBestPatterns(ResultPoint[] patterns) {

    // Find distances between pattern centers
    float zeroOneDistance = distance(patterns[0], patterns[1]);
    float oneTwoDistance = distance(patterns[1], patterns[2]);
    float zeroTwoDistance = distance(patterns[0], patterns[2]);

    ResultPoint pointA;
    ResultPoint pointB;
    ResultPoint pointC;
    // Assume one closest to other two is B; A and C will just be guesses at first
    if (oneTwoDistance >= zeroOneDistance && oneTwoDistance >= zeroTwoDistance) {
      pointB = patterns[0];
      pointA = patterns[1];
      pointC = patterns[2];
    } else if (zeroTwoDistance >= oneTwoDistance && zeroTwoDistance >= zeroOneDistance) {
      pointB = patterns[1];
      pointA = patterns[0];
      pointC = patterns[2];
    } else {
      pointB = patterns[2];
      pointA = patterns[0];
      pointC = patterns[1];
    }

    // Use cross product to figure out whether A and C are correct or flipped.
    // This asks whether BC x BA has a positive z component, which is the arrangement
    // we want for A, B, C. If it's negative, then we've got it flipped around and
    // should swap A and C.
    if (crossProductZ(pointA, pointB, pointC) < 0.0f) {
      ResultPoint temp = pointA;
      pointA = pointC;
      pointC = temp;
    }

    patterns[0] = pointA;
    patterns[1] = pointB;
    patterns[2] = pointC;
  }


  /**
   * @return distance between two points
   */
  public static float distance(ResultPoint pattern1, ResultPoint pattern2) {
    float xDiff = pattern1.x - pattern2.x;
    float yDiff = pattern1.y - pattern2.y;
    return (float) Math.sqrt((double) (xDiff * xDiff + yDiff * yDiff));
  }

  /**
   * Returns the z component of the cross product between vectors BC and BA.
   */
  private static float crossProductZ(ResultPoint pointA,
                                     ResultPoint pointB,
                                     ResultPoint pointC) {
    float bX = pointB.x;
    float bY = pointB.y;
    return ((pointC.x - bX) * (pointA.y - bY)) - ((pointC.y - bY) * (pointA.x - bX));
  }


}
