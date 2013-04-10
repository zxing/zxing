package com.google.zxing.pdf417.decoder;

public class SimpleLog {
  // The level have the following meaning
  // 0: All messages
  // 1: The more interesting debug messages only
  // 2: Messages which indicate small problems during barcode detection. A perfect barcode should not output
  //    any messages on this level
  // 3: More serious problems
  // 4: Really bad problems :-)
  // 5: GAU
  static enum LEVEL {
    ALL,
    DEBUG,
    INFO,
    WARNING,
    ERROR,
    FATAL
  }

  protected static LEVEL LOG_LEVEL = LEVEL.DEBUG;

  public static void log(LEVEL level, String string, int imageRow, int imageColumn, int barcodeRow,
                         int barcodeColumn) {
    log(level, string + ", barcodeRow: " + barcodeRow + ", barcodeColumn: " + barcodeColumn, imageRow,
        imageColumn);
  }

  public static void log(LEVEL level, Loggable loggable) {
    if (level.compareTo(LOG_LEVEL) >= 0) {
      log(level, loggable.getLogString(), -1, -1);
    }
  }

  public static void log(LEVEL level, String string) {
    log(level, string, -1, -1);
  }

  public static void log(LEVEL level, String string, int imageRow, int imageColumn) {
    if (level.compareTo(LOG_LEVEL) >= 0) {
      if (imageRow != -1) {
        string += ", imageRow: " + imageRow;
      }
      if (imageColumn != -1) {
        string += ", imageColumn: " + imageColumn;
      }
      System.err.println(string);
    }
  }

  public static interface Loggable {
    String getLogString();
  }
}
