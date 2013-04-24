package com.google.zxing.pdf417.decoder;

import com.google.zxing.ResultPoint;
import com.google.zxing.pdf417.decoder.SimpleLog.LEVEL;

public class DetectionResultRowIndicatorColumn extends DetectionResultColumn {

  private static final int MIN_BARCODE_ROWS = 3;
  private static final int MAX_BARCODE_ROWS = 90;

  private final boolean isLeft;

  public DetectionResultRowIndicatorColumn(final BoundingBox boundingBox, boolean isLeft) {
    super(boundingBox);
    this.isLeft = isLeft;
  }

  public void setRowNumbers() {
    for (Codeword codeword : getCodewords()) {
      if (codeword != null) {
        codeword.setRowNumberAsRowIndicatorColumn();
      }
    }
  }

  public int[] getRowHeights() {
    BarcodeMetadata barcodeMetadata = getBarcodeMetadata();
    if (barcodeMetadata == null) {
      return null;
    }
    adjustIndicatorColumnRowNumbers(barcodeMetadata);
    int[] result = new int[barcodeMetadata.getRowCount()];
    for (Codeword codeword : getCodewords()) {
      if (codeword != null) {
        result[codeword.getRowNumber()]++;
      }
    }
    return result;
  }

  // TODO maybe we should add missing codewords to store the correct row number to make
  // finding row numbers for other columns easier
  // use row height count to make detection of invalid row numbers more reliable
  public int adjustIndicatorColumnRowNumbers(BarcodeMetadata barcodeMetadata) {
    ResultPoint top = isLeft ? boundingBox.getTopLeft() : boundingBox.getTopRight();
    ResultPoint bottom = isLeft ? boundingBox.getBottomLeft() : boundingBox.getBottomRight();
    int firstRow = getCodewordsIndex((int) top.getY());
    int lastRow = getCodewordsIndex((int) bottom.getY());
    float averageRowHeight = (lastRow - firstRow) / (float) barcodeMetadata.getRowCount();
    Codeword[] codewords = getCodewords();
    int barcodeRow = -1;
    int maxRowHeight = 1;
    int currentRowHeight = 0;
    for (int codewordsRow = firstRow; codewordsRow < lastRow; codewordsRow++) {
      if (codewords[codewordsRow] == null) {
        continue;
      }
      Codeword codeword = codewords[codewordsRow];

      codeword.setRowNumberAsRowIndicatorColumn();

      // This only works if we have a complete RI column. If the column is cut off at the beginning or end, it
      // will calculate the wrong numbers and delete correct codewords. Could be used once the barcode height has
      // been calculated properly.
      //      float expectedRowNumber = (codewordsRow - firstRow) / averageRowHeight;
      //      if (Math.abs(codeword.getRowNumber() - expectedRowNumber) > 2) {
      //        SimpleLog.log(LEVEL.WARNING,
      //            "Removing codeword, rowNumberSkew too high, codeword[" + codewordsRow + "]: Expected Row: " +
      //                expectedRowNumber + ", RealRow: " + codeword.getRowNumber() + ", value: " + codeword.getValue());
      //        codewords[codewordsRow] = null;
      //      }

      int rowDifference = codeword.getRowNumber() - barcodeRow;

      // TODO deal with case where first row indicator doesn't start with 0

      if (rowDifference == 0) {
        currentRowHeight++;
      } else if (rowDifference == 1) {
        maxRowHeight = Math.max(maxRowHeight, currentRowHeight);
        currentRowHeight = 1;
        barcodeRow = codeword.getRowNumber();
      } else if (rowDifference < 0) {
        SimpleLog.log(LEVEL.WARNING, "Removing codeword, rowNumber should not decrease, codeword[" + codewordsRow +
            "]: " + codeword.getRowNumber() + ", value: " + codeword.getValue());
        codewords[codewordsRow] = null;
      } else if (codeword.getRowNumber() >= barcodeMetadata.getRowCount()) {
        SimpleLog.log(LEVEL.WARNING, "Removing codeword, rowNumber too big, codeword[" + codewordsRow + "]: " +
            codeword.getRowNumber() + ", value: " + codeword.getValue());
        codewords[codewordsRow] = null;
      } else if (rowDifference > codewordsRow) {
        SimpleLog.log(LEVEL.WARNING,
            "Row number jump bigger than codeword row, codeword[" + codewordsRow + "]: previous row: " + barcodeRow +
                ", new row: " + codeword.getRowNumber() + ", value: " + codeword.getValue());
        codewords[codewordsRow] = null;
      } else {
        int checkedRows;
        if (maxRowHeight > 2) {
          checkedRows = (maxRowHeight - 2) * rowDifference;
        } else {
          checkedRows = rowDifference;
        }
        boolean closePreviousCodewordFound = checkedRows >= codewordsRow;
        for (int i = 1; i <= checkedRows && !closePreviousCodewordFound; i++) {
          // there must be (height * rowDifference) number of codewords missing. For now we assume height = 1.
          // This should hopefully get rid of most problems already.
          closePreviousCodewordFound = (codewords[codewordsRow - i] != null);
        }
        if (closePreviousCodewordFound) {
          SimpleLog.log(LEVEL.WARNING,
              "Row number jump bigger than codeword row gap, codeword[" + codewordsRow + "]: previous row: " +
                  barcodeRow + ", new row: " + codeword.getRowNumber() + ", value: " + codeword.getValue());
          codewords[codewordsRow] = null;
        } else {
          SimpleLog.log(LEVEL.WARNING,
              "Setting new row number after bigger jump, codeword[" + codewordsRow + "]: previous row: " + barcodeRow +
                  ", new row: " + codeword.getRowNumber() + ", value: " + codeword.getValue());
          barcodeRow = codeword.getRowNumber();
          currentRowHeight = 1;
        }
      }
    }
    return (int) (averageRowHeight + .5);
  }

  protected BarcodeMetadata getBarcodeMetadata() {
    Codeword[] codewords = getCodewords();
    BarcodeValue barcodeColumnCount = new BarcodeValue();
    BarcodeValue barcodeRowCountUpperPart = new BarcodeValue();
    BarcodeValue barcodeRowCountLowerPart = new BarcodeValue();
    BarcodeValue barcodeECLevel = new BarcodeValue();
    for (Codeword codeword : codewords) {
      if (codeword == null) {
        continue;
      }
      codeword.setRowNumberAsRowIndicatorColumn();
      int rowIndicatorValue = codeword.getValue() % 30;
      int codewordRowNumber = codeword.getRowNumber();
      if (!isLeft) {
        codewordRowNumber += 2;
      }
      switch (codewordRowNumber % 3) {
        case 0:
          barcodeRowCountUpperPart.setValue(rowIndicatorValue * 3 + 1);
          break;
        case 1:
          barcodeECLevel.setValue(rowIndicatorValue / 3);
          barcodeRowCountLowerPart.setValue(rowIndicatorValue % 3);
          break;
        case 2:
          barcodeColumnCount.setValue(rowIndicatorValue + 1);
          break;
      }
    }
    if ((barcodeColumnCount.getValue() == null) || (barcodeRowCountUpperPart.getValue() == null) ||
        (barcodeRowCountLowerPart.getValue() == null) || (barcodeECLevel.getValue() == null) ||
        barcodeColumnCount.getValue() < 1 ||
        barcodeRowCountUpperPart.getValue() + barcodeRowCountLowerPart.getValue() < MIN_BARCODE_ROWS ||
        barcodeRowCountUpperPart.getValue() + barcodeRowCountLowerPart.getValue() > MAX_BARCODE_ROWS) {
      return null;
    }
    BarcodeMetadata barcodeMetadata = new BarcodeMetadata(barcodeColumnCount.getValue(),
        barcodeRowCountUpperPart.getValue(), barcodeRowCountLowerPart.getValue(), barcodeECLevel.getValue());
    removeIncorrectCodewords(codewords, barcodeMetadata);
    return barcodeMetadata;
  }

  private void removeIncorrectCodewords(Codeword[] codewords, BarcodeMetadata barcodeMetadata) {
    // Remove codewords which do not match the metadata
    // TODO Maybe we should keep the incorrect codewords for the start and end positions?
    for (int codewordRow = 0; codewordRow < codewords.length; codewordRow++) {
      Codeword codeword = codewords[codewordRow];
      if (codewords[codewordRow] == null) {
        continue;
      }
      int rowIndicatorValue = codeword.getValue() % 30;
      int codewordRowNumber = codeword.getRowNumber();
      if (codewordRowNumber > barcodeMetadata.getRowCount()) {
        codewords[codewordRow] = null;
        continue;
      }
      if (!isLeft) {
        codewordRowNumber += 2;
      }
      switch (codewordRowNumber % 3) {
        case 0:
          if (rowIndicatorValue * 3 + 1 != barcodeMetadata.getRowCountUpperPart()) {
            codewords[codewordRow] = null;
          }
          break;
        case 1:
          if (rowIndicatorValue / 3 != barcodeMetadata.getErrorCorrectionLevel() ||
              rowIndicatorValue % 3 != barcodeMetadata.getRowCountLowerPart()) {
            codewords[codewordRow] = null;
          }
          break;
        case 2:
          if (rowIndicatorValue + 1 != barcodeMetadata.getColumnCount()) {
            codewords[codewordRow] = null;
          }
          break;
      }
    }
  }

  public boolean isLeft() {
    return isLeft;
  }

  @Override
  public String getLogString() {
    return "IsLeft: " + isLeft + "\n" + super.getLogString();
  }
}
