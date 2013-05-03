/*
 * Copyright 2013 ZXing authors
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

package com.google.zxing.pdf417.decoder;

import com.google.zxing.NotFoundException;
import com.google.zxing.ResultPoint;
import com.google.zxing.common.BitMatrix;

/**
 * @author Guenther Grau
 */
final class BoundingBox {

  private BitMatrix image;
  private ResultPoint topLeft;
  private ResultPoint bottomLeft;
  private ResultPoint topRight;
  private ResultPoint bottomRight;
  private int minX;
  private int maxX;
  private int minY;
  private int maxY;

  BoundingBox(BitMatrix image,
              ResultPoint topLeft,
              ResultPoint bottomLeft,
              ResultPoint topRight,
              ResultPoint bottomRight) throws NotFoundException {
    if ((topLeft == null && topRight == null) ||
        (bottomLeft == null && bottomRight == null) ||
        (topLeft != null && bottomLeft == null) ||
        (topRight != null && bottomRight == null)) {
      throw NotFoundException.getNotFoundInstance();
    }
    init(image, topLeft, bottomLeft, topRight, bottomRight);
  }

  BoundingBox(BoundingBox boundingBox) {
    init(boundingBox.image, boundingBox.topLeft, boundingBox.bottomLeft, boundingBox.topRight, boundingBox.bottomRight);
  }

  private void init(BitMatrix image,
                    ResultPoint topLeft,
                    ResultPoint bottomLeft,
                    ResultPoint topRight,
                    ResultPoint bottomRight) {
    this.image = image;
    this.topLeft = topLeft;
    this.bottomLeft = bottomLeft;
    this.topRight = topRight;
    this.bottomRight = bottomRight;
    calculateMinMaxValues();
  }

  static BoundingBox merge(BoundingBox leftBox, BoundingBox rightBox) throws NotFoundException {
    if (leftBox == null) {
      return rightBox;
    }
    if (rightBox == null) {
      return leftBox;
    }
    return new BoundingBox(leftBox.image, leftBox.topLeft, leftBox.bottomLeft, rightBox.topRight, rightBox.bottomRight);
  }

  BoundingBox addMissingRows(int missingStartRows, int missingEndRows, boolean isLeft) throws NotFoundException {
    ResultPoint newTopLeft = topLeft;
    ResultPoint newBottomLeft = bottomLeft;
    ResultPoint newTopRight = topRight;
    ResultPoint newBottomRight = bottomRight;

    if (missingStartRows > 0) {
      ResultPoint top = isLeft ? topLeft : topRight;
      int newMinY = (int) top.getY() - missingStartRows;
      if (newMinY < 0) {
        newMinY = 0;
      }
      // TODO use existing points to better interpolate the new x positions
      ResultPoint newTop = new ResultPoint(top.getX(), newMinY);
      if (isLeft) {
        newTopLeft = newTop;
      } else {
        newTopRight = newTop;
      }
    }

    if (missingEndRows > 0) {
      ResultPoint bottom = isLeft ? bottomLeft : bottomRight;
      int newMaxY = (int) bottom.getY() + missingEndRows;
      if (newMaxY >= image.getHeight()) {
        newMaxY = image.getHeight() - 1;
      }
      // TODO use existing points to better interpolate the new x positions
      ResultPoint newBottom = new ResultPoint(bottom.getX(), newMaxY);
      if (isLeft) {
        newBottomLeft = newBottom;
      } else {
        newBottomRight = newBottom;
      }
    }

    calculateMinMaxValues();
    return new BoundingBox(image, newTopLeft, newBottomLeft, newTopRight, newBottomRight);
  }

  private void calculateMinMaxValues() {
    if (topLeft == null) {
      topLeft = new ResultPoint(0, topRight.getY());
      bottomLeft = new ResultPoint(0, bottomRight.getY());
    } else if (topRight == null) {
      topRight = new ResultPoint(image.getWidth() - 1, topLeft.getY());
      bottomRight = new ResultPoint(image.getWidth() - 1, bottomLeft.getY());
    }

    minX = (int) Math.min(topLeft.getX(), bottomLeft.getX());
    maxX = (int) Math.max(topRight.getX(), bottomRight.getX());
    minY = (int) Math.min(topLeft.getY(), topRight.getY());
    maxY = (int) Math.max(bottomLeft.getY(), bottomRight.getY());
  }

  void setTopRight(ResultPoint topRight) {
    this.topRight = topRight;
    calculateMinMaxValues();
  }

  void setBottomRight(ResultPoint bottomRight) {
    this.bottomRight = bottomRight;
    calculateMinMaxValues();
  }

  int getMinX() {
    return minX;
  }

  int getMaxX() {
    return maxX;
  }

  int getMinY() {
    return minY;
  }

  int getMaxY() {
    return maxY;
  }

  ResultPoint getTopLeft() {
    return topLeft;
  }

  ResultPoint getTopRight() {
    return topRight;
  }

  ResultPoint getBottomLeft() {
    return bottomLeft;
  }

  ResultPoint getBottomRight() {
    return bottomRight;
  }

}
