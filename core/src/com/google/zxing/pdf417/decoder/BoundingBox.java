package com.google.zxing.pdf417.decoder;

import com.google.zxing.ResultPoint;

public class BoundingBox {
  private final ResultPoint topLeft;
  private final ResultPoint bottomLeft;
  private ResultPoint topRight;
  private ResultPoint bottomRight;
  private int minX;
  private int maxX;
  private int minY;
  private int maxY;
  private final int maxCodewordWidth;

  public BoundingBox(final ResultPoint topLeft,
                     final ResultPoint bottomLeft,
                     final ResultPoint topRight,
                     final ResultPoint bottomRight,
                     int maxCodewordWidth) {
    this.topLeft = topLeft;
    this.bottomLeft = bottomLeft;
    this.topRight = topRight;
    this.bottomRight = bottomRight;
    this.maxCodewordWidth = maxCodewordWidth;
    calculateMinMaxValues();
  }

  private void calculateMinMaxValues() {
    minX = (int) Math.min(topLeft.getX(), bottomLeft.getX());

    if (topRight != null && bottomRight != null) {
      maxX = (int) Math.max(topRight.getX(), bottomRight.getX());
      minY = (int) Math.min(topLeft.getY(), topRight.getY());
      maxY = (int) Math.max(bottomLeft.getY(), bottomRight.getY());
    } else {
      maxX = (int) topLeft.getX() + maxCodewordWidth;
      minY = (int) topLeft.getY();
      maxY = (int) bottomLeft.getY();
    }
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

  public int getMaxCodewordWidth() {
    return maxCodewordWidth;
  }
}
