package com.google.zxing.pdf417.decoder;

public class BarcodeMetadata {
  private final int columnCount;
  private final int errorCorrectionLevel;
  private final int rowCountUpperPart;
  private final int rowCountLowerPart;
  private final int rowCount;

  public BarcodeMetadata(int columnCount, int rowCountUpperPart, int rowCountLowerPart, int errorCorrectionLevel) {
    this.columnCount = columnCount;
    this.errorCorrectionLevel = errorCorrectionLevel;
    this.rowCountUpperPart = rowCountUpperPart;
    this.rowCountLowerPart = rowCountLowerPart;
    this.rowCount = rowCountUpperPart + rowCountLowerPart;
  }

  public int getColumnCount() {
    return columnCount;
  }

  public int getErrorCorrectionLevel() {
    return errorCorrectionLevel;
  }

  public int getRowCount() {
    return rowCount;
  }

  public int getRowCountUpperPart() {
    return rowCountUpperPart;
  }

  public int getRowCountLowerPart() {
    return rowCountLowerPart;
  }
}
