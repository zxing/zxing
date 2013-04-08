package com.google.zxing.pdf417.decoder;

import com.google.zxing.pdf417.PDF417Common;

import org.junit.Assert;
import org.junit.Test;

public class PDF417CodewordDecoderTest {

  private static final int[][] inputValid = {
      { 17, 3, 5, 3, 9, 3, 8, 18 },
      { 2, 2, 3, 1, 4, 6, 8, 2 },
      { 2, 6, 4, 4, 4, 3, 2, 3 },
      { 10, 4, 4, 10, 6, 12, 4, 8 },
      { 5, 2, 2, 5, 3, 6, 2, 4 },
      { 2, 1, 2, 3, 4, 3, 9, 4 },
      { 10, 2, 2, 3, 2, 1, 5, 3 },
      { 7, 1, 2, 3, 2, 2, 7, 4 },
      { 2, 3, 7, 5, 2, 4, 1, 2 },
      { 7, 2, 1, 2, 1, 3, 8, 1 },
      { 2, 2, 2, 8, 8, 6, 2, 4 },
      { 8, 2, 2, 2, 3, 7, 2, 9 },
      { 5, 1, 13, 5, 8, 13, 8, 3 },
      { 21, 5, 3, 4, 3, 12, 11, 4 },
      { 7, 4, 3, 5, 2, 5, 18, 19 },
      { 7, 2, 1, 2, 1, 3, 8, 1 },
      { 6, 1, 7, 2, 8, 1, 2, 1 },
      { 3, 5, 19, 5, 3, 4, 6, 15 },
      { 4, 4, 2, 3, 6, 3, 3, 10 }
  };

  private static final int[][] expectedValid = {
      { 4, 1, 1, 1, 2, 1, 2, 5 },
      { 1, 2, 1, 1, 2, 4, 5, 1 },
      { 1, 4, 2, 3, 2, 2, 1, 2 },
      { 3, 1, 1, 3, 2, 4, 1, 2 },
      { 3, 1, 1, 3, 2, 4, 1, 2 },
      { 1, 1, 1, 2, 2, 2, 5, 3 },
      { 6, 1, 1, 2, 1, 1, 3, 2 },
      { 4, 1, 1, 2, 1, 1, 4, 3 },
      { 1, 2, 5, 3, 2, 2, 1, 1 },
      { 5, 1, 1, 1, 1, 2, 5, 1 },
      { 1, 1, 1, 4, 4, 3, 1, 2 },
      { 4, 1, 1, 1, 1, 4, 1, 4 },
      { 1, 1, 4, 2, 2, 4, 2, 1 },
      { 6, 1, 1, 1, 1, 3, 3, 1 },
      { 2, 1, 1, 1, 1, 1, 5, 5 },
      { 5, 1, 1, 1, 1, 2, 5, 1 },
      { 4, 1, 4, 1, 4, 1, 1, 1 },
      { 1, 1, 6, 1, 1, 1, 2, 4 },
      { 2, 2, 1, 1, 3, 1, 2, 5 },

  };

  private static final int[][] inputInvalid = {
      { 3, 1, 2, 8, 1, 2, 2, 6 },
      { 2, 3, 7, 5, 2, 4, 1, 2 },
  };

  @Test
  public void adjustBitcountTestOneResult() {
    for (int i = 0; i < inputValid.length; i++) {
      System.out.println("Input: " + getBitCounts(inputValid[i]) + ", Expected: " + getBitCounts(expectedValid[i]));
      AdjustmentResults adjustmentResults = PDF417CodewordDecoder.adjustBitCount(inputValid[i]);
      boolean foundMatch = false;
      for (int resultIndex = 0; resultIndex < adjustmentResults.size() && !foundMatch; resultIndex++) {
        int[] result = adjustmentResults.get(resultIndex).getModuleCount();
        System.out.println(getBitCounts(result));
        foundMatch = comapareArrays(expectedValid[i], result);
      }
      Assert.assertTrue("No matching record found:" + getBitCounts(expectedValid[i]), foundMatch);
    }
  }

  //{2,1,1,5,1,2,1,4}
  //{2,1,1,6,1,1,1,4}

  @Test
  public void testValues() {
    System.out.println(BitMatrixParser.getCodeword(getDecodedValue(new int[] { 2, 1, 1, 5, 1, 2, 1, 4 })));
  }

  @Test
  public void testValidExpected() {
    for (int[] expectedResult : expectedValid) {
      Assert.assertTrue("Invalid Codeword " + getBitCounts(expectedResult),
          BitMatrixParser.getCodeword(getDecodedValue(expectedResult)) != -1);
    }
  }

  /**
   * This test will fail, if we recognize a pattern properly that we previously didn't recognize.
   * So, failure can be a good thing :-) Move the input row in question to the valid inputs
   */
  @Test
  public void testInvalidExpected() {
    for (int[] expectedResult : inputInvalid) {
      Assert.assertTrue("Valid Codeword " + getBitCounts(expectedResult),
          BitMatrixParser.getCodeword(getDecodedValue(expectedResult)) == -1);
    }
  }

  private boolean comapareArrays(int[] expected, int[] result) {
    for (int i = 0; i < expected.length; i++) {
      if (expected[i] != result[i]) {
        return false;
      }
    }
    return true;
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
