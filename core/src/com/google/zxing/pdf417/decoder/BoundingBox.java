package com.google.zxing.pdf417.decoder;

import com.google.zxing.NotFoundException;
import com.google.zxing.ResultPoint;
import com.google.zxing.common.BitMatrix;

public class BoundingBox {
  private BitMatrix image;
  private ResultPoint topLeft;
  private ResultPoint bottomLeft;
  private ResultPoint topRight;
  private ResultPoint bottomRight;
  private int minX;
  private int maxX;
  private int minY;
  private int maxY;

  public BoundingBox(BitMatrix image,
                     final ResultPoint topLeft,
                     final ResultPoint bottomLeft,
                     final ResultPoint topRight,
                     final ResultPoint bottomRight) throws NotFoundException {
    if ((topLeft == null && topRight == null) || (bottomLeft == null && bottomRight == null) ||
        (topLeft != null && bottomLeft == null) || (topRight != null && bottomRight == null)) {
      throw NotFoundException.getNotFoundInstance();
    }
    init(image, topLeft, bottomLeft, topRight, bottomRight);
  }

  public BoundingBox(BoundingBox boundingBox) {
    init(boundingBox.image, boundingBox.topLeft, boundingBox.bottomLeft, boundingBox.topRight, boundingBox.bottomRight);
  }

  private void init(BitMatrix image, final ResultPoint topLeft, final ResultPoint bottomLeft,
                    final ResultPoint topRight, final ResultPoint bottomRight) {
    this.image = image;
    this.topLeft = topLeft;
    this.bottomLeft = bottomLeft;
    this.topRight = topRight;
    this.bottomRight = bottomRight;
    calculateMinMaxValues();
  }

  public static BoundingBox merge(BoundingBox leftBox, BoundingBox rightBox) throws NotFoundException {
    if (leftBox == null) {
      return rightBox;
    }
    if (rightBox == null) {
      return leftBox;
    }
    return new BoundingBox(leftBox.image, leftBox.topLeft, leftBox.bottomLeft, rightBox.topRight, rightBox.bottomRight);
  }

  public void addMissingRows(int missingStartRows, int missingEndRows, boolean isLeft) {
    if (missingStartRows > 0) {
      ResultPoint top = isLeft ? topLeft : topRight;
      int newMinY = (int) top.getY() - missingStartRows;
      if (newMinY < 0) {
        newMinY = 0;
      }
      // TODO use existing points to better interpolate the new x positions
      ResultPoint newTop = new ResultPoint(top.getX(), newMinY);
      if (isLeft) {
        topLeft = newTop;
      } else {
        topRight = newTop;
      }
    }

    if (missingEndRows > 0) {
      ResultPoint bottom = isLeft ? bottomLeft : bottomRight;
      int newMaxY = (int) bottom.getY() - missingStartRows;
      if (newMaxY >= image.getHeight()) {
        newMaxY = image.getHeight() - 1;
      }
      // TODO use existing points to better interpolate the new x positions
      ResultPoint newBottom = new ResultPoint(bottom.getX(), newMaxY);
      if (isLeft) {
        bottomLeft = newBottom;
      } else {
        bottomRight = newBottom;
      }
    }
    calculateMinMaxValues();
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

  public void setTopRight(ResultPoint topRight) {
    this.topRight = topRight;
    calculateMinMaxValues();
  }

  public void setBottomRight(ResultPoint bottomRight) {
    this.bottomRight = bottomRight;
    calculateMinMaxValues();
  }

  public int getMinX() {
    return minX;
  }

  public int getMaxX() {
    return maxX;
  }

  public int getMinY() {
    return minY;
  }

  public int getMaxY() {
    return maxY;
  }

  public ResultPoint getTopLeft() {
    return topLeft;
  }

  public ResultPoint getTopRight() {
    return topRight;
  }

  public ResultPoint getBottomLeft() {
    return bottomLeft;
  }

  public ResultPoint getBottomRight() {
    return bottomRight;
  }
}
