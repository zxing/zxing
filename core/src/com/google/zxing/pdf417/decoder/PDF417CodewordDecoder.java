package com.google.zxing.pdf417.decoder;

public class PDF417CodewordDecoder {

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
