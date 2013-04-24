package com.google.zxing.pdf417.decoder;

import com.google.zxing.pdf417.decoder.SimpleLog.Loggable;

import java.util.Formatter;

public class DetectionResultColumn implements Loggable {
  private static final int MAX_NEARBY_DISTANCE = 5;
  protected final BoundingBox boundingBox;
  private final Codeword[] codewords;

  public DetectionResultColumn(final BoundingBox boundingBox) {
    this.boundingBox = new BoundingBox(boundingBox);
    codewords = new Codeword[boundingBox.getMaxY() - boundingBox.getMinY() + 1];
  }

  public Codeword getCodewordNearby(int imageRow) {
    Codeword codeword = getCodeword(imageRow);
    if (codeword != null) {
      return codeword;
    }
    for (int i = 1; i < MAX_NEARBY_DISTANCE; i++) {
      int nearImageRow = getCodewordsIndex(imageRow) - i;
      if (nearImageRow >= 0) {
        codeword = codewords[nearImageRow];
        if (codeword != null) {
          return codeword;
        }
      }
      nearImageRow = getCodewordsIndex(imageRow) + i;
      if (nearImageRow < codewords.length) {
        codeword = codewords[nearImageRow];
        if (codeword != null) {
          return codeword;
        }
      }
    }
    return null;
  }

  protected int getCodewordsIndex(int imageRow) {
    return imageRow - boundingBox.getMinY();
  }

  public int getImageRow(int codewordIndex) {
    return boundingBox.getMinY() + codewordIndex;
  }

  public void setCodeword(int imageRow, Codeword codeword) {
    codewords[getCodewordsIndex(imageRow)] = codeword;
  }

  public Codeword getCodeword(int imageRow) {
    return codewords[getCodewordsIndex(imageRow)];
  }

  public BoundingBox getBoundingBox() {
    return boundingBox;
  }

  public Codeword[] getCodewords() {
    return codewords;
  }

  @Override
  public String getLogString() {
    Formatter formatter = new Formatter();
    int row = 0;
    for (Codeword codeword : codewords) {
      if (codeword == null) {
        formatter.format("%3d:    |   \n", row++);
        continue;
      }
      formatter.format("%3d: %3d|%3d\n", row++, codeword.getRowNumber(), codeword.getValue());
    }
    String result = formatter.toString();
    formatter.close();
    return result;
  }
}
