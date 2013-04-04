package com.google.zxing.pdf417;

public class PDF417Common {

  public static final int MODULES_IN_CODEWORD = 17;
  public static final int MAX_CODEWORD_COUNT_ROW = 32;
  public static final int MAX_ROWS = 90;

  public static int getBitCountSum(int[] moduleBitCount) {
    int bitCountSum = 0;
    for (int count : moduleBitCount) {
      bitCountSum += count;
    }
    return bitCountSum;
  }

}
