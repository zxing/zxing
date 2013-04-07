package com.google.zxing.pdf417.decoder;

public class Codeword {
  protected static final int BARCODE_ROW_UNKNOWN = -1;

  private final int startX;
  private final int endX;
  private final int bucket;
  private final int value;
  private int rowNumber = BARCODE_ROW_UNKNOWN;

  public Codeword(int startX, int endX, int bucket, int value) {
    this.startX = startX;
    this.endX = endX;
    this.bucket = bucket;
    this.value = value;
  }

  public boolean hasValidRowNumber() {
    return isValidRowNumber(rowNumber);
  }

  public boolean isValidRowNumber(int rowNumber) {
    return BARCODE_ROW_UNKNOWN != rowNumber && bucket == (rowNumber % 3) * 3;
  }

  public void setRowNumberAsRowIndicatorColumn() {
    rowNumber = (value / 30) * 3 + bucket / 3;
  }

  public int getWidth() {
    return endX - startX;
  }

  public int getStartX() {
    return startX;
  }

  public int getEndX() {
    return endX;
  }

  public int getBucket() {
    return bucket;
  }

  public int getValue() {
    return value;
  }

  public int getRowNumber() {
    return rowNumber;
  }

  public void setRowNumber(int rowNumber) {
    this.rowNumber = rowNumber;
  }
}
