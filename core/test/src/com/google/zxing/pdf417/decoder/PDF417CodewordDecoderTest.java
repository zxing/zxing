package com.google.zxing.pdf417.decoder;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

public class PDF417CodewordDecoderTest {

  private static final int[][] input = {
      { 5, 1, 13, 5, 8, 13, 8, 3 },
      { 21, 5, 3, 4, 3, 12, 11, 4 },
      { 7, 4, 3, 5, 2, 5, 18, 19 },
      { 7, 2, 1, 2, 1, 3, 8, 1 },
      { 6, 1, 7, 2, 8, 1, 2, 1 },
      { 3, 5, 19, 5, 3, 4, 6, 15 }
  };

  private static final int[][] expectedResult = {
      { 1, 1, 4, 2, 2, 4, 2, 1 },
      { 6, 1, 1, 1, 1, 3, 3, 1 },
      { 2, 1, 1, 1, 1, 1, 5, 5 },
      { 5, 1, 1, 1, 1, 2, 5, 1 },
      { 4, 1, 4, 1, 4, 1, 1, 1 },
      { 1, 1, 6, 1, 1, 1, 2, 4 },
  };

  @Test
  public void adjustBitcountTest() {
    for (int i = 0; i < input.length; i++) {
      int[] bitCount = Arrays.copyOf(input[i], input[i].length);
      PDF417CodewordDecoder.adjustBitCount(bitCount);
      System.out.println(getBitCounts(bitCount));
      Assert.assertArrayEquals(expectedResult[i], bitCount);
    }
  }

  private String getBitCounts(int[] moduleBitCount) {
    StringBuilder result = new StringBuilder("{");
    for (int bitCount : moduleBitCount) {
      result.append(bitCount).append(',');
    }
    result.setLength(result.length() - 1);
    return result.append('}').toString();
  }
}
