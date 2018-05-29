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

  private final BitMatrix image;
  private final ResultPoint topLeft;
  private final ResultPoint bottomLeft;
  private final ResultPoint topRight;
  private final ResultPoint bottomRight;
  private final int minX;
  private final int maxX;
  private final int minY;
  private final int maxY;

  BoundingBox(BitMatrix image,
              ResultPoint topLeft,
              ResultPoint bottomLeft,
              ResultPoint topRight,
              ResultPoint bottomRight) throws NotFoundException {
    boolean leftUnspecified = topLeft == null || bottomLeft == null;
    boolean rightUnspecified = topRight == null || bottomRight == null;
    if (leftUnspecified && rightUnspecified) {
      throw NotFoundException.getNotFoundInstance();
    }
    if (leftUnspecified) {
      topLeft = new ResultPoint(0, topRight.getY());
      bottomLeft = new ResultPoint(0, bottomRight.getY());
    } else if (rightUnspecified) {
      topRight = new ResultPoint(image.getWidth() - 1, topLeft.getY());
      bottomRight = new ResultPoint(image.getWidth() - 1, bottomLeft.getY());
    }
    this.image = image;
    this.topLeft = topLeft;
    this.bottomLeft = bottomLeft;
    this.topRight = topRight;
    this.bottomRight = bottomRight;
    this.minX = (int) Math.min(topLeft.getX(), bottomLeft.getX());
    this.maxX = (int) Math.max(topRight.getX(), bottomRight.getX());
    this.minY = (int) Math.min(topLeft.getY(), topRight.getY());
    this.maxY = (int) Math.max(bottomLeft.getY(), bottomRight.getY());
  }

  BoundingBox(BoundingBox boundingBox) {
    this.image = boundingBox.image;
    this.topLeft = boundingBox.getTopLeft();
    this.bottomLeft = boundingBox.getBottomLeft();
    this.topRight = boundingBox.getTopRight();
    this.bottomRight = boundingBox.getBottomRight();
    this.minX = boundingBox.getMinX();
    this.maxX = boundingBox.getMaxX();
    this.minY = boundingBox.getMinY();
    this.maxY = boundingBox.getMaxY();
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
      ResultPoint newBottom = new ResultPoint(bottom.getX(), newMaxY);
      if (isLeft) {
        newBottomLeft = newBottom;
      } else {
        newBottomRight = newBottom;
      }
    }

    return new BoundingBox(image, newTopLeft, newBottomLeft, newTopRight, newBottomRight);
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
