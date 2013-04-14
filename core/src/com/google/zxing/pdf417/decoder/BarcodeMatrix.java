package com.google.zxing.pdf417.decoder;

import java.util.Formatter;
import java.util.HashMap;
import java.util.Map;

public class BarcodeMatrix implements SimpleLog.Loggable {
  Map<String,BarcodeValue> values = new HashMap<String,BarcodeValue>();
  int maxRow = -1;
  int maxColumn = -1;

  private String getKey(int barcodeRow, int barcodeColumn) {
    return barcodeRow + "," + barcodeColumn;
  }

  public void setValue(int row, int column, int value) {
    maxRow = Math.max(maxRow, row);
    maxColumn = Math.max(maxColumn, column);
    String key = getKey(row, column);
    BarcodeValue barcodeValue = values.get(key);
    if (barcodeValue == null) {
      barcodeValue = new BarcodeValue();
      values.put(key, barcodeValue);
    }
    //    SimpleLog.log(LEVEL.ALL, "setting value " + value, row, column);
    barcodeValue.setValue(value);
  }

  public Integer getValue(int row, int column) {
    BarcodeValue barcodeValue = values.get(getKey(row, column));
    if (barcodeValue == null) {
      return null;
    }
    return barcodeValue.getValue();
  }

  @Override
  public String getLogString() {
    Formatter formatter = new Formatter();
    for (int row = 0; row <= maxRow; row++) {
      formatter.format("Row %2d: ", row);
      for (int column = 0; column <= maxColumn; column++) {
        BarcodeValue barcodeValue = values.get(getKey(row, column));
        if (barcodeValue == null || barcodeValue.getValue() == null) {
          formatter.format("        ", (Object[]) null);
        } else {
          formatter.format("%4d(%2d)", barcodeValue.getValue(), barcodeValue.getConfidence(barcodeValue.getValue()));
        }
      }
      formatter.format("\n");
    }
    String result = formatter.toString();
    formatter.close();
    return result;
  }
}
