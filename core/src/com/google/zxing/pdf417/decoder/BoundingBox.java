package com.google.zxing.pdf417.decoder;

import com.google.zxing.NotFoundException;
import com.google.zxing.ResultPoint;
import com.google.zxing.common.BitMatrix;

public class BoundingBox {
  private final BitMatrix image;
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
    this.image = image;
    this.topLeft = topLeft;
    this.bottomLeft = bottomLeft;
    this.topRight = topRight;
    this.bottomRight = bottomRight;
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

  public ResultPoint getTopRight() {
    return topRight;
  }
}
