package com.google.zxing.pdf417.decoder;

import com.google.zxing.pdf417.PDF417Common;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

public class PDF417CodewordDecoderTest {

  private static final int[][] inputOneResult = {
      { 2, 2, 3, 1, 4, 6, 8, 2 },
      { 2, 6, 4, 4, 4, 3, 2, 3 },
      { 10, 4, 4, 10, 6, 12, 4, 8 },
      { 5, 2, 2, 5, 3, 6, 2, 4 },
      { 2, 1, 2, 3, 4, 3, 9, 4 },
      { 10, 2, 2, 3, 2, 1, 5, 3 },
      { 7, 1, 2, 3, 2, 2, 7, 4 },
      { 2, 3, 7, 5, 2, 4, 1, 2 },
      { 3, 1, 2, 8, 1, 2, 2, 6 },
      { 7, 2, 1, 2, 1, 3, 8, 1 },
      { 2, 2, 2, 8, 8, 6, 2, 4 },
      { 8, 2, 2, 2, 3, 7, 2, 9 },
      { 5, 1, 13, 5, 8, 13, 8, 3 }, // fails
      { 21, 5, 3, 4, 3, 12, 11, 4 },
      { 7, 4, 3, 5, 2, 5, 18, 19 },
      { 7, 2, 1, 2, 1, 3, 8, 1 },
      { 6, 1, 7, 2, 8, 1, 2, 1 },
      { 3, 5, 19, 5, 3, 4, 6, 15 }
  };

  private static final int[][] expectedOneResult = {
      { 1, 4, 2, 2, 3, 2, 1, 2 }, // invalid codeword, valid is { 1, 4, 2, 3, 2, 2, 1, 2 },
      { 3, 1, 1, 3, 2, 4, 1, 2 },
      { 3, 1, 1, 3, 2, 4, 1, 2 },
      { 1, 1, 1, 2, 2, 2, 6, 2 }, // invalid codeword
      { 6, 1, 1, 2, 1, 1, 3, 2 },
      { 4, 1, 1, 2, 1, 1, 4, 3 },
      { 1, 2, 6, 3, 1, 2, 1, 1 }, // invalid codeword
      { 2, 1, 1, 6, 1, 1, 1, 4 },
      { 5, 1, 1, 1, 1, 2, 5, 1 },
      { 1, 1, 1, 4, 4, 3, 1, 2 },
      { 4, 1, 1, 1, 1, 4, 1, 4 },
      { 1, 1, 4, 2, 2, 4, 2, 1 }, // valid
      { 6, 1, 1, 1, 1, 3, 3, 1 },
      { 2, 1, 1, 1, 1, 1, 5, 5 },
      { 5, 1, 1, 1, 1, 2, 5, 1 },
      { 4, 1, 4, 1, 4, 1, 1, 1 },
      { 1, 1, 6, 1, 1, 1, 2, 4 },
  };
  private static final int[][] inputTwoResults = {
      { 4, 4, 2, 3, 6, 3, 3, 10 }
  };

  private static final int[][][] expectedTwoResults = {
      {
      { 2, 2, 1, 1, 3, 1, 2, 5 },
      { 2, 2, 1, 1, 3, 2, 1, 5 }
  },
  };

  @Test
  public void adjustBitcountTestOneResult() {
    for (int i = 0; i < inputOneResult.length; i++) {
      AdjustmentResults adjustmentResults = PDF417CodewordDecoder.adjustBitCount(inputOneResult[i]);
      Assert.assertEquals(1, adjustmentResults.size());
      int[] result = adjustmentResults.get(0).getModuleCount();
      System.out.println(getBitCounts(result));
      Assert.assertArrayEquals(expectedOneResult[i], result);
    }
  }

  @Test
  public void adjustBitcountTestTwoResults() {
    for (int i = 0; i < inputTwoResults.length; i++) {
      int[] bitCount = Arrays.copyOf(inputTwoResults[i], inputTwoResults[i].length);
      AdjustmentResults adjustmentResults = PDF417CodewordDecoder
          .adjustBitCount(bitCount);
      Assert.assertEquals(expectedTwoResults[i].length, adjustmentResults.size());
      for (int resultIndex = 0; resultIndex < expectedTwoResults[i].length; resultIndex++) {
        int[] result = adjustmentResults.get(resultIndex).getModuleCount();
        System.out.println(getBitCounts(result));
        Assert.assertArrayEquals(expectedTwoResults[i][resultIndex], result);
      }
    }
  }

  @Test
  public void testValues() {
    System.out.println(BitMatrixParser.getCodeword(getDecodedValue(new int[] { 1, 2, 1, 1, 2, 4, 5, 1 })));
  }

  @Test
  public void printSampleIndex() {
    for (int i = 0; i < PDF417Common.MODULES_IN_CODEWORD; i++) {
      System.out.println(String.format("%2d: %f", i,
          1. * 28 / 34 + (i * 28. / PDF417Common.MODULES_IN_CODEWORD)));
    }
  }

  private static long getDecodedValue(int[] moduleBitCount) {
    long result = 0;
    for (int i = 0; i < moduleBitCount.length; i++) {
      for (int bit = 0; bit < moduleBitCount[i]; bit++) {
        result = (result << 1) | (i % 2 == 0 ? 1 : 0);
      }
    }
    return result;
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
