package com.google.zxing.pdf417.decoder;

import com.google.zxing.pdf417.PDF417Common;

import java.util.Arrays;

public class AdjustmentResultFloat {
  private static final int BIT_COUNT_SIZE = 8;

  private final boolean[] sampleArray;
  private final double[] sampleIndexes;

  private AdjustmentResultFloat(boolean[] sampleArray, double[] sampleIndexes) {
    this.sampleArray = sampleArray;
    this.sampleIndexes = sampleIndexes;
  }

  public int[] getModuleBitCount() {
    return getModuleBitCount(sampleArray);
  }

  public int[] getClosestModuleBitCount() {
    return getModuleBitCount(getClosestSampleArray());
  }

  private int[] getModuleBitCount(boolean[] sampleArray) {
    try {
      int[] result = new int[BIT_COUNT_SIZE];
      boolean previousPixelValue = true;
      int resultIndex = 0;
      for (int i = 0; i < PDF417Common.MODULES_IN_CODEWORD; i++) {
        if (previousPixelValue == sampleArray[(int) sampleIndexes[i]]) {
          result[resultIndex]++;
        } else {
          previousPixelValue = !previousPixelValue;
          result[++resultIndex]++;
        }
      }
      return result;
    } catch (Exception e) {
      e.printStackTrace();
      getClosestSampleArray();
    }
    return null;
  }

  private boolean[] getClosestSampleArray() {
    boolean[] result = Arrays.copyOf(sampleArray, sampleArray.length);
    int smallestCornerDifferenceIndex = -1;
    double smallestCornerDifference = Double.MAX_VALUE;
    for (int i = 1; i < PDF417Common.MODULES_IN_CODEWORD - 1; i++) {
      double difference = Math.min(getLeftDifference(i), getRightDifference(i));
      if (difference < smallestCornerDifference &&
          result[(int) sampleIndexes[i - 1]] != result[(int) sampleIndexes[i + 1]]) {
        smallestCornerDifference = difference;
        smallestCornerDifferenceIndex = i;
      }
    }
    if (smallestCornerDifferenceIndex != -1) {
      result[(int) sampleIndexes[smallestCornerDifferenceIndex]] = !result[(int) sampleIndexes[smallestCornerDifferenceIndex]];
    }
    return result;
  }

  private double getLeftDifference(int sampleIndex) {
    int sampleArrayIndex = (int) sampleIndexes[sampleIndex];
    boolean pixelValue = sampleArray[sampleArrayIndex];
    double difference = sampleIndexes[sampleIndex] - sampleArrayIndex;
    sampleArrayIndex--;
    while (sampleArrayIndex > 0 && pixelValue == sampleArray[sampleArrayIndex]) {
      difference++;
      sampleArrayIndex--;
    }
    return difference;
  }

  private double getRightDifference(int sampleIndex) {
    int sampleArrayIndex = (int) sampleIndexes[sampleIndex];
    boolean pixelValue = sampleArray[sampleArrayIndex];
    double difference = sampleArrayIndex + 1 - sampleIndexes[sampleIndex];
    sampleArrayIndex++;
    while (sampleArrayIndex < sampleArray.length && pixelValue == sampleArray[sampleArrayIndex]) {
      difference++;
      sampleArrayIndex++;
    }
    return difference;
  }

  public static AdjustmentResultFloat getAdjustmentResultFloat(int[] moduleBitCount) {
    int bitCountSum = PDF417Common.getBitCountSum(moduleBitCount);
    /*
    int[] moduleBitCount = new int[BIT_COUNT_SIZE];
    int loop;
    for (loop = 1; ((bitCountSum * loop) % PDF417Common.MODULES_IN_CODEWORD > 3) &&
        ((bitCountSum * loop) % PDF417Common.MODULES_IN_CODEWORD < PDF417Common.MODULES_IN_CODEWORD - 3); loop++) {
    }
    bitCountSum *= loop;
    for (int i = 0; i < BIT_COUNT_SIZE; i++) {
      moduleBitCount[i] = tmpModuleBitCount[i] * loop;
    }
    */
    boolean[] sampleArray = new boolean[bitCountSum];
    int sampleIndex = 0;
    for (int i = 0; i < BIT_COUNT_SIZE - 1; i++) {
      if (i % 2 == 0) {
        for (int j = 0; j < moduleBitCount[i]; j++) {
          sampleArray[sampleIndex++] = true;
        }
      } else {
        sampleIndex += moduleBitCount[i];
      }
    }

    final double[] sampleIndexes = new double[PDF417Common.MODULES_IN_CODEWORD];
    for (int i = 0; i < PDF417Common.MODULES_IN_CODEWORD; i++) {
      sampleIndexes[i] = ((double) bitCountSum) / (2 * PDF417Common.MODULES_IN_CODEWORD) +
          ((double) (i * bitCountSum)) / PDF417Common.MODULES_IN_CODEWORD;
      //      System.out.println(String.format("%2d: %f", i, sampleIndexes[i]));
    }
    return new AdjustmentResultFloat(sampleArray, sampleIndexes);
  }
}
