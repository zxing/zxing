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

package com.google.zxing.common;

import com.google.zxing.ResultPoint;

/**
 * <p>Simple implementation of {@link ResultPoint} for applications that don't need
 * to use anything more complex.</p>
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class GenericResultPoint implements ResultPoint {

  private final float posX;
  private final float posY;

  public GenericResultPoint(float posX, float posY) {
    this.posX = posX;
    this.posY = posY;
  }

  public float getX() {
    return posX;
  }

  public float getY() {
    return posY;
  }

  public String toString() {
    StringBuffer result = new StringBuffer(25);
    result.append('(');
    result.append(posX);
    result.append(',');
    result.append(posY);
    result.append(')');
    return result.toString();
  }

  public boolean equals(Object other) {
    if (other instanceof GenericResultPoint) {
      GenericResultPoint otherPoint = (GenericResultPoint) other;
      return posX == otherPoint.posX && posY == otherPoint.posY;
    }
    return false;
  }

  public int hashCode() {
    return 31 * Float.floatToIntBits(posX) + Float.floatToIntBits(posY);
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

    ResultPoint pointA, pointB, pointC;
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
    float xDiff = pattern1.getX() - pattern2.getX();
    float yDiff = pattern1.getY() - pattern2.getY();
    return (float) Math.sqrt((double) (xDiff * xDiff + yDiff * yDiff));
  }

  /**
   * Returns the z component of the cross product between vectors BC and BA.
   */
  public static float crossProductZ(ResultPoint pointA, ResultPoint pointB, ResultPoint pointC) {
    float bX = pointB.getX();
    float bY = pointB.getY();
    return ((pointC.getX() - bX) * (pointA.getY() - bY)) - ((pointC.getY() - bY) * (pointA.getX() - bX));
  }

}