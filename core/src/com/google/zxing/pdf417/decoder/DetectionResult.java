package com.google.zxing.pdf417.decoder;

import com.google.zxing.ResultPoint;
import com.google.zxing.common.TransformableBitMatrix;
import com.google.zxing.pdf417.decoder.SimpleLog.LEVEL;

import java.util.Formatter;

public class DetectionResult implements SimpleLog.Loggable {
  private static final int ADJUST_ROW_NUMBER_SKIP = 2;
  private final int barcodeColumnCount;
  private final int barcodeRowCount;
  private final int barcodeECLevel;
  private final DetectionResultColumn[] detectionResultColumns;
  private final BoundingBox boundingBox;

  public DetectionResult(TransformableBitMatrix image, int barcodeColumnCount,
                         int barcodeRowCount,
                         int barcodeECLevel,
                         BoundingBox boundingBox) {
    this.barcodeColumnCount = barcodeColumnCount;
    this.barcodeRowCount = barcodeRowCount;
    this.barcodeECLevel = barcodeECLevel;
    this.boundingBox = boundingBox;
    detectionResultColumns = new DetectionResultColumn[barcodeColumnCount + 2];

    if (boundingBox.getTopRight() == null) {
      int rightX = Math.min(image.getWidth(),
          boundingBox.getMinX() + (barcodeColumnCount + 1) * boundingBox.getMaxCodewordWidth());
      boundingBox.setTopRight(new ResultPoint(rightX, boundingBox.getMinY()));
      boundingBox.setBottomRight(new ResultPoint(rightX, boundingBox.getMaxY()));
    }
  }

  public int getImageStartRow(int barcodeColumn) {
    DetectionResultColumn detectionResultColumn = null;
    while (barcodeColumn > 0) {
      detectionResultColumn = detectionResultColumns[--barcodeColumn];
      // TODO compare start row with previous result columns
      // Could try detecting codewords from right to left
      // if all else fails, could calculate estimate
      Codeword[] codewords = detectionResultColumn.getCodewords();
      for (int rowNumber = 0; rowNumber < codewords.length; rowNumber++) {
        if (codewords[rowNumber] != null) {
          // next column might start earlier if barcode is not aligned with image
          if (rowNumber > 0) {
            rowNumber--;
          }
          return detectionResultColumn.getImageRow(rowNumber);
        }
      }
    }
    return -1;
  }

  public void setDetectionResultColumn(int barcodeColumn, DetectionResultColumn detectionResultColumn) {
    detectionResultColumns[barcodeColumn] = detectionResultColumn;
  }

  public DetectionResultColumn getDetectionResultColumn(int barcodeColumn) {
    return detectionResultColumns[barcodeColumn];
  }

  public DetectionResultColumn[] getDetectionResultColumns() {
    SimpleLog.log(LEVEL.DEBUG, "Before adjustRowNumbers");
    SimpleLog.log(LEVEL.DEBUG, this);
    adjustIndicatorColumnRowNumbers(detectionResultColumns[0]);
    adjustIndicatorColumnRowNumbers(detectionResultColumns[barcodeColumnCount + 1]);
    int unadjustedCount = 900;
    int previousUnadjustedCount;
    do {
      previousUnadjustedCount = unadjustedCount;
      unadjustedCount = adjustRowNumbers();
    } while (unadjustedCount > 0 && unadjustedCount < previousUnadjustedCount);

    if (unadjustedCount > 0) {
      SimpleLog.log(LEVEL.INFO, unadjustedCount + " codewords without valid row number. Values will be ignored!");
    }

    SimpleLog.log(LEVEL.DEBUG, "After adjustRowNumbers");
    SimpleLog.log(LEVEL.DEBUG, this);
    return detectionResultColumns;
  }

  // TODO ensure that no detected codewords with unknown row number are left
  // we should be able to estimate the row height and use it as a hint for the row number
  // we should also fill the rows top to bottom and bottom to top
  /**
   * @return number of codewords which don't have a valid row number. Note that the count is not accurate as codewords 
   * will be counted several times. It just serves as an indicator to see when we can stop adjusting row numbers 
   */
  private int adjustRowNumbers() {
    int unadjustedCount = adjustRowNumbersByRow();
    if (unadjustedCount == 0) {
      return 0;
    }
    for (int barcodeColumn = 1; barcodeColumn < barcodeColumnCount + 1; barcodeColumn++) {
      Codeword[] codewords = detectionResultColumns[barcodeColumn].getCodewords();
      for (int codewordsRow = 0; codewordsRow < codewords.length; codewordsRow++) {
        if (codewords[codewordsRow] == null) {
          continue;
        }
        if (!codewords[codewordsRow].hasValidRowNumber()) {
          adjustRowNumbers(barcodeColumn, codewordsRow, codewords);
        }
      }
    }
    return unadjustedCount;
  }

  private int adjustRowNumbersByRow() {
    int unadjustedCount = adjustRowNumbersFromLRI();
    return unadjustedCount + adjustRowNumbersFromRRI();
  }

  private int adjustRowNumbersFromRRI() {
    if (detectionResultColumns[barcodeColumnCount + 1] == null) {
      return 0;
    }
    int unadjustedCount = 0;
    Codeword[] codewords = detectionResultColumns[barcodeColumnCount + 1].getCodewords();
    for (int codewordsRow = 0; codewordsRow < codewords.length; codewordsRow++) {
      if (codewords[codewordsRow] == null) {
        continue;
      }
      int rowIndicatorRowNumber = codewords[codewordsRow].getRowNumber();
      int invalidRowCounts = 0;
      for (int barcodeColumn = barcodeColumnCount + 1; barcodeColumn > 0 &&
          invalidRowCounts < ADJUST_ROW_NUMBER_SKIP; barcodeColumn--) {
        Codeword codeword = detectionResultColumns[barcodeColumn].getCodewords()[codewordsRow];
        if (codeword != null) {
          invalidRowCounts = adjustRowNumberIfValid(codewordsRow, rowIndicatorRowNumber, invalidRowCounts,
              barcodeColumn, codeword);
          if (!codeword.hasValidRowNumber()) {
            unadjustedCount++;
          }
        }
      }
    }
    return unadjustedCount;
  }

  private int adjustRowNumbersFromLRI() {
    int unadjustedCount = 0;
    Codeword[] codewords = detectionResultColumns[0].getCodewords();
    for (int codewordsRow = 0; codewordsRow < codewords.length; codewordsRow++) {
      if (codewords[codewordsRow] == null) {
        continue;
      }
      int rowIndicatorRowNumber = codewords[codewordsRow].getRowNumber();
      int invalidRowCounts = 0;
      for (int barcodeColumn = 1; barcodeColumn < barcodeColumnCount + 1 &&
          invalidRowCounts < ADJUST_ROW_NUMBER_SKIP; barcodeColumn++) {
        Codeword codeword = detectionResultColumns[barcodeColumn].getCodewords()[codewordsRow];
        if (codeword != null) {
          invalidRowCounts = adjustRowNumberIfValid(codewordsRow, rowIndicatorRowNumber, invalidRowCounts,
              barcodeColumn, codeword);
          if (!codeword.hasValidRowNumber()) {
            unadjustedCount++;
          }
        }
      }
    }
    return unadjustedCount;
  }

  private int adjustRowNumberIfValid(int codewordsRow, int rowIndicatorRowNumber, int invalidRowCounts,
                                     int barcodeColumn, Codeword codeword) {

    if (codeword == null) {
      return invalidRowCounts;
    }
    if (!codeword.hasValidRowNumber()) {
      if (codeword.isValidRowNumber(rowIndicatorRowNumber)) {
        codeword.setRowNumber(rowIndicatorRowNumber);
        invalidRowCounts = 0;
      } else {
        if (++invalidRowCounts >= ADJUST_ROW_NUMBER_SKIP) {
          SimpleLog.log(LEVEL.INFO, "to many consecutive invalid row counts, skipping further columns for this row",
              codewordsRow, barcodeColumn);
        }
      }
    }
    return invalidRowCounts;
  }

  private void adjustRowNumbers(int barcodeColumn, int codewordsRow, Codeword[] codewords) {
    Codeword codeword = codewords[codewordsRow];
    Codeword[] previousColumnCodewords = detectionResultColumns[barcodeColumn - 1].getCodewords();
    Codeword[] nextColumnCodewords = previousColumnCodewords;
    if (detectionResultColumns[barcodeColumn + 1] != null) {
      nextColumnCodewords = detectionResultColumns[barcodeColumn + 1].getCodewords();
    }

    Codeword[] otherCodewords = new Codeword[14];

    otherCodewords[2] = previousColumnCodewords[codewordsRow];
    otherCodewords[3] = nextColumnCodewords[codewordsRow];

    if (codewordsRow > 0) {
      otherCodewords[0] = codewords[codewordsRow - 1];
      otherCodewords[4] = previousColumnCodewords[codewordsRow - 1];
      otherCodewords[5] = nextColumnCodewords[codewordsRow - 1];
    }
    if (codewordsRow > 1) {
      otherCodewords[8] = codewords[codewordsRow - 2];
      otherCodewords[10] = previousColumnCodewords[codewordsRow - 2];
      otherCodewords[11] = nextColumnCodewords[codewordsRow - 2];
    }
    if (codewordsRow < codewords.length - 1) {
      otherCodewords[1] = codewords[codewordsRow + 1];
      otherCodewords[6] = previousColumnCodewords[codewordsRow + 1];
      otherCodewords[7] = nextColumnCodewords[codewordsRow + 1];
    }
    if (codewordsRow < codewords.length - 2) {
      otherCodewords[9] = codewords[codewordsRow + 2];
      otherCodewords[12] = previousColumnCodewords[codewordsRow + 2];
      otherCodewords[13] = nextColumnCodewords[codewordsRow + 2];
    }
    for (Codeword otherCodeword : otherCodewords) {
      if (adjustRowNumber(codeword, otherCodeword)) {
        return;
      }
    }
  }

  // TODO maybe we should add missing codeword to store the correct row number to make
  // finding row numbers for other columns easier
  // use row height count to make detection of invalid row numbers more reliable
  private void adjustIndicatorColumnRowNumbers(DetectionResultColumn detectionResultColumn) {
    if (detectionResultColumn == null) {
      return;
    }
    Codeword[] codewords = detectionResultColumn.getCodewords();
    int barcodeRow = -1;
    int previousRowHeight = 0;
    int currentRowHeight = 0;
    for (int codewordsRow = 0; codewordsRow < codewords.length; codewordsRow++) {
      if (codewords[codewordsRow] == null) {
        continue;
      }
      Codeword codeword = codewords[codewordsRow];
      codeword.setRowNumberAsRowIndicatorColumn();

      if (codeword.getRowNumber() == barcodeRow || barcodeRow == -1) {
        currentRowHeight++;
      } else if (codeword.getRowNumber() < barcodeRow) {
        SimpleLog.log(LEVEL.WARNING, "Removing codeword, rowNumber should not decrease, codeword[" + codewordsRow +
            "]: " + codeword.getRowNumber() + ", value: " + codeword.getValue());
        codewords[codewordsRow] = null;
      } else if (codeword.getRowNumber() > barcodeRowCount) {
        SimpleLog.log(LEVEL.WARNING, "Removing codeword, rowNumber too big, codeword[" + codewordsRow + "]: " +
            codeword.getRowNumber() + ", value: " + codeword.getValue());
        codewords[codewordsRow] = null;
      } else if (codeword.getRowNumber() == barcodeRow + 1) {
        previousRowHeight = Math.max(previousRowHeight, currentRowHeight);
        barcodeRow = codeword.getRowNumber();
      } else {
        // the row number has increased by more than one

        // TODO add proper check to prevent jumping of row numbers.
        // Accept increase by more than 1 only if we have previous skipped codewords
        SimpleLog.log(LEVEL.WARNING, "row number jump, codeword[" + codewordsRow + "]: previous row: " +
            barcodeRow + ", new row: " + codeword.getRowNumber() + ", value: " + codeword.getValue());
      }
    }
  }

  /**
   * 
   * @param codeword
   * @param otherCodeword
   * @return true, if row number was adjusted, false otherwise
   */
  private boolean adjustRowNumber(Codeword codeword, Codeword otherCodeword) {
    if (otherCodeword == null) {
      return false;
    }
    if (otherCodeword.hasValidRowNumber() && otherCodeword.getBucket() == codeword.getBucket()) {
      codeword.setRowNumber(otherCodeword.getRowNumber());
      SimpleLog.log(LEVEL.ALL,
          "corrected rowNumber: codeword: " + codeword.getRowNumber() + ", value: " + codeword.getValue());
      return true;
    }
    return false;
  }

  public int getBarcodeColumnCount() {
    return barcodeColumnCount;
  }

  public int getBarcodeRowCount() {
    return barcodeRowCount;
  }

  public int getBarcodeECLevel() {
    return barcodeECLevel;
  }

  public BoundingBox getBoundingBox() {
    return boundingBox;
  }

  @Override
  public String getLogString() {
    Formatter formatter = new Formatter();
    for (int codewordsRow = 0; codewordsRow < detectionResultColumns[0].getCodewords().length; codewordsRow++) {
      formatter.format("CW %3d:", codewordsRow);
      for (int barcodeColumn = 0; barcodeColumn < barcodeColumnCount + 2; barcodeColumn++) {
        if (detectionResultColumns[barcodeColumn] == null) {
          formatter.format("    |   ");
          continue;
        }
        Codeword codeword = detectionResultColumns[barcodeColumn].getCodewords()[codewordsRow];
        if (codeword == null) {
          formatter.format("    |   ");
          continue;
        }
        formatter.format(" %3d|%3d", codeword.getRowNumber(), codeword.getValue());
      }
      formatter.format("\n");
    }
    String result = formatter.toString();
    formatter.close();
    return result;
  }
}
