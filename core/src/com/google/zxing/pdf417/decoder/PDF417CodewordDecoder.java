package com.google.zxing.pdf417.decoder;

import com.google.zxing.pdf417.PDF417Common;

public class PDF417CodewordDecoder {

  private static final int MODULES_IN_SYMBOL = 17;
  private static final int BARS_IN_SYMBOL = 8;
  private static final float[][] RATIOS_TABLE = new float[BitMatrixParser.SYMBOL_TABLE.length][BARS_IN_SYMBOL];

  static {
    // Pre-computes the symbol ratio table.
    for (int i = 0; i < BitMatrixParser.SYMBOL_TABLE.length; i++) {
      int currentSymbol = BitMatrixParser.SYMBOL_TABLE[i];
      int currentBit = currentSymbol & 0x1;
      for (int j = 0; j < BARS_IN_SYMBOL; j++) {
        float size = 0.0f;
        while ((currentSymbol & 0x1) == currentBit) {
          size += 1.0f;
          currentSymbol >>= 1;
        }
        currentBit = currentSymbol & 0x1;
        RATIOS_TABLE[i][BARS_IN_SYMBOL - j - 1] = size / MODULES_IN_SYMBOL;
      }
    }
  }

  static int getDecodedValue(int[] moduleBitCount) {
    AdjustmentResultFloat resultFloat = AdjustmentResultFloat.getAdjustmentResultFloat(moduleBitCount);
    int decodedValue = getDecodedValue(AdjustmentResult.getAdjustmentResult(resultFloat.getModuleBitCount()));
    if (decodedValue != -1) {
      return decodedValue;
    }
    decodedValue = getDecodedValue(AdjustmentResult.getAdjustmentResult(resultFloat.getClosestModuleBitCount()));
    if (decodedValue != -1) {
      return decodedValue;
    }

    return getClosestDecodedValue(moduleBitCount);
  }

  private static int getDecodedValue(AdjustmentResult adjustmentResult) {
    if (adjustmentResult == null) {
      return -1;
    }
    int decodedValue = getBitValue(adjustmentResult.getModuleCount());
    return BitMatrixParser.getCodeword(decodedValue) == -1 ? -1 : decodedValue;
  }

  private static int getBitValue(int[] moduleBitCount) {
    long result = 0;
    for (int i = 0; i < moduleBitCount.length; i++) {
      for (int bit = 0; bit < moduleBitCount[i]; bit++) {
        result = (result << 1) | (i % 2 == 0 ? 1 : 0);
      }
    }
    return (int) result;
  }

  static int getClosestDecodedValue(int[] moduleBitCount) {
    int bitCountSum = PDF417Common.getBitCountSum(moduleBitCount);
    float[] bitCountRatios = new float[BARS_IN_SYMBOL];
    for (int i = 0; i < bitCountRatios.length; i++) {
      bitCountRatios[i] = moduleBitCount[i] / (float) bitCountSum;
    }
    float bestMatchError = Float.MAX_VALUE;
    int bestMatch = -1;
    for (int j = 0; j < RATIOS_TABLE.length; j++) {
      float error = 0.0f;
      for (int k = 0; k < BARS_IN_SYMBOL; k++) {
        float diff = RATIOS_TABLE[j][k] - bitCountRatios[k];
        error += diff * diff;
      }
      if (error < bestMatchError) {
        bestMatchError = error;
        bestMatch = BitMatrixParser.SYMBOL_TABLE[j];
      }
    }
    return bestMatch;
  }

  static AdjustmentResults adjustBitCount(int[] moduleBitCount) {
    return adjustBitCountFloat(moduleBitCount);
  }

  static AdjustmentResults adjustBitCountFloat(int[] moduleBitCount) {
    AdjustmentResults adjustmentResults = new AdjustmentResults();
    AdjustmentResultFloat resultFloat = AdjustmentResultFloat.getAdjustmentResultFloat(moduleBitCount);
    addToResult(adjustmentResults, AdjustmentResult.getAdjustmentResult(resultFloat.getModuleBitCount()));
    addToResult(adjustmentResults, AdjustmentResult.getAdjustmentResult(resultFloat.getClosestModuleBitCount()));
    addToResult(adjustmentResults, AdjustmentResultDifferenceTracker.getAdjustmentResult(moduleBitCount));
    addToResult(adjustmentResults, AdjustmentResult.getAdjustmentResult(moduleBitCount));
    return adjustmentResults;
  }

  private static void addToResult(AdjustmentResults adjustmentResults, AdjustmentResult result) {
    if (result == null) {
      return;
    }
    if (!result.isValid()) {
      if (result.isToLarge()) {
        reduceBitCount(adjustmentResults, result);
      } else {
        enlargeBitCount(adjustmentResults, result);
      }
    }
    adjustmentResults.add(result);
  }

  static AdjustmentResults adjustBitCountManual(int[] moduleBitCount) {
    // TODO might be worthwhile keeping track of how many modules don't have the correct bits
    // TODO might be worthwhile remembering which columns had bits added or removed. This information
    // could be used to remove bits from neighbor modules if required
    AdjustmentResults adjustmentResults = new AdjustmentResults();
    AdjustmentResult result = AdjustmentResult.getAdjustmentResult(moduleBitCount);
    if (result == null) {
      return adjustmentResults;
    }
    adjustmentResults.add(result);
    if (result.isValid()) {
      return adjustmentResults;
    }

    if (result.isToLarge()) {
      reduceBitCount(adjustmentResults, result);
    } else {
      enlargeBitCount(adjustmentResults, result);
    }
    return adjustmentResults;
  }

  protected static void enlargeBitCount(AdjustmentResults adjustmentResults, AdjustmentResult result) {
    result.enlargeModulesWithSmallDifference();
    if (result.isValid()) {
      return;
    }
    if (result.hasCorrectSize()) {
      result.shrinkModulesWithSmallDifference();
      if (result.isValid()) {
        return;
      }
    }
    if (result.isToLarge()) {
      reduceBitCount(adjustmentResults, result);
      return;
    }

    while (result.adjustHalfSizeModule(adjustmentResults)) {
      if (result.isValid()) {
        return;
      }
      if (result.isToLarge()) {
        break;
      }
    }
    if (result.isToLarge()) {
      reduceBitCount(adjustmentResults, result);
      return;
    }

    while (result.adjustModuleWithBiggestDifference()) {
      if (result.isValid()) {
        return;
      }
      if (result.isToLarge()) {
        break;
      }
    }
    if (result.isToLarge()) {
      reduceBitCount(adjustmentResults, result);
      return;
    }

    // here we should have all moduleBitCounts % bitsPerModule == 0
    while (result.adjustWholeModule()) {
      if (result.isValid()) {
        return;
      }
    }

    // I hope we never get here :-)
    throw new RuntimeException();
  }

  protected static void reduceBitCount(AdjustmentResults adjustmentResults, AdjustmentResult result) {
    result.shrinkModulesWithSmallDifference();
    if (result.isValid()) {
      return;
    }
    if (result.isToSmall()) {
      enlargeBitCount(adjustmentResults, result);
      return;
    }

    if (result.hasCorrectSize()) {
      // it has the correct size, but it is invalid
      result.enlargeModulesWithSmallDifference();
      if (result.isValid()) {
        return;
      }
    }

    while (result.adjustHalfSizeModule(adjustmentResults)) {
      if (result.isValid()) {
        return;
      }
      if (result.isToSmall()) {
        break;
      }
    }

    if (result.isToSmall()) {
      enlargeBitCount(adjustmentResults, result);
      return;
    }

    while (result.adjustModuleWithBiggestDifference()) {
      if (result.isValid()) {
        return;
      }
      if (result.isToSmall()) {
        break;
      }
    }

    if (result.isToSmall()) {
      enlargeBitCount(adjustmentResults, result);
      return;
    }

    // here we should have all moduleBitCounts % bitsPerModule == 0
    while (result.adjustWholeModule()) {
      if (result.isValid()) {
        return;
      }
    }

    // I hope we never get here :-)
    throw new RuntimeException();
  }
}
