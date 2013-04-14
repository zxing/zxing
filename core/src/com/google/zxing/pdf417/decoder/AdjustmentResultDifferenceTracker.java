package com.google.zxing.pdf417.decoder;

import com.google.zxing.pdf417.PDF417Common;

public class AdjustmentResultDifferenceTracker {
  private static final int BIT_COUNT_SIZE = 8;

  private final int[] moduleBitCount;
  private final int bitsPerModule;
  private final int bitCountDifference;

  private AdjustmentResultDifferenceTracker(int[] moduleBitCount, int bitsPerModule, int bitCountDifference) {
    this.moduleBitCount = moduleBitCount;
    this.bitsPerModule = bitsPerModule;
    this.bitCountDifference = bitCountDifference;
  }

  public static AdjustmentResult getAdjustmentResult(int[] moduleBitCount) {
    int bitCountSum = PDF417Common.getBitCountSum(moduleBitCount);

    int[] result = new int[moduleBitCount.length];
    float[] floatResult = new float[moduleBitCount.length];
    int sum = 0;
    float floatSum = 0.0f;
    Boolean favorEven = null;
    for (int i = 0; i < moduleBitCount.length; i++) {
      floatResult[i] = ((float) moduleBitCount[i]) * PDF417Common.MODULES_IN_CODEWORD / bitCountSum;
      floatSum += floatResult[i];
      result[i] = (int) floatResult[i];
      if (result[i] == 0) {
        favorEven = ((i % 2) == 0);
        result[i] = 1;
      }
      sum += result[i];
      if (floatSum - sum > 0.5) {
        if (favorEven != null) {
          if (favorEven && i % 2 == 0 || !favorEven && i % 2 != 0) {
            result[i]++;
          } else {
            result[i - 1]++;
          }
        } else {
          if (i > 0 && floatResult[i - 1] - result[i - 1] >= floatResult[i] - result[i]) {
            result[i - 1]++;
          } else {
            result[i]++;
          }
        }
        sum++;
      }
    }

    return AdjustmentResult.getAdjustmentResult(result);
  }
}
