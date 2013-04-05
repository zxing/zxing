package com.google.zxing.pdf417.decoder;

import com.google.zxing.pdf417.PDF417Common;

public class PDF417CodewordDecoder {

  static boolean adjustBitCount(int[] moduleBitCount) {
    // TODO might be worthwhile keeping track of how many modules don't have the correct bits
    //int[] parameter = Arrays.copyOf(moduleBitCount, moduleBitCount.length);
    // TODO might be worthwhile remembering which columns had bits added or removed. This information
    // could be used to remove bits from neighbor modules if required
    int bitCountSum = PDF417Common.getBitCountSum(moduleBitCount);
    int bitCountDifference = bitCountSum % PDF417Common.MODULES_IN_CODEWORD;
    int bitsPerModule = bitCountSum / PDF417Common.MODULES_IN_CODEWORD;
    if (bitsPerModule == 0) {
      return false;
    }
    if (checkBitCountDifference(moduleBitCount, bitCountDifference, bitsPerModule)) {
      return true;
    }
    // TODO is this guaranteed to always stop eventually?
    if (bitsPerModule == 1 || (bitCountDifference > 4) && (bitCountDifference < 13)) {
      for (int i = 0; i < moduleBitCount.length; i++) {
        moduleBitCount[i] <<= 1;
        //moduleBitCount[i] *= 5;
      }
      return adjustBitCount(moduleBitCount);
    }

    boolean takeAway = (bitCountDifference << 1) < PDF417Common.MODULES_IN_CODEWORD;
    if (takeAway) {
      reduceBitCount(moduleBitCount, bitsPerModule);
    } else {
      enlargeBitCount(moduleBitCount, ++bitsPerModule);
    }
    return true;
  }

  private static void enlargeBitCount(int[] moduleBitCount, int bitsPerModule) {
    final boolean takeAway = false;
    adjustSmallDifference(moduleBitCount, bitsPerModule, takeAway);
    int bitCountDifference = getBitCountDifference(moduleBitCount, bitsPerModule);
    if (checkBitCountDifference(moduleBitCount, bitCountDifference, bitsPerModule)) {
      return;
    }
    if (bitCountDifference == 0) {
      adjustSmallDifference(moduleBitCount, bitsPerModule, !takeAway);
      bitCountDifference = getBitCountDifference(moduleBitCount, bitsPerModule);
      if (checkBitCountDifference(moduleBitCount, bitCountDifference, bitsPerModule)) {
        return;
      }
    }
    if (bitCountDifference > 0) {
      reduceBitCount(moduleBitCount, bitsPerModule);
      return;
    }

    while (adjustHalfSizeModule(moduleBitCount, bitsPerModule, takeAway)) {
      bitCountDifference = getBitCountDifference(moduleBitCount, bitsPerModule);
      if (checkBitCountDifference(moduleBitCount, bitCountDifference, bitsPerModule)) {
        return;
      }
      if (bitCountDifference > 0) {
        reduceBitCount(moduleBitCount, bitsPerModule);
        return;
      }
    }

    bitCountDifference = getBitCountDifference(moduleBitCount, bitsPerModule);
    if (checkBitCountDifference(moduleBitCount, bitCountDifference, bitsPerModule)) {
      return;
    }
    if (bitCountDifference > 0) {
      reduceBitCount(moduleBitCount, bitsPerModule);
      return;
    }

    while (adjustBiggestModule(moduleBitCount, bitsPerModule, takeAway)) {
      bitCountDifference = getBitCountDifference(moduleBitCount, bitsPerModule);
      if (checkBitCountDifference(moduleBitCount, bitCountDifference, bitsPerModule)) {
        return;
      }
      if (bitCountDifference > 0) {
        reduceBitCount(moduleBitCount, bitsPerModule);
        return;
      }
    }
    bitCountDifference = getBitCountDifference(moduleBitCount, bitsPerModule);
    if (checkBitCountDifference(moduleBitCount, bitCountDifference, bitsPerModule)) {
      return;
    }

    // here we should have all moduleBitCounts % bitsPerModule == 0
    while (adjustWholeModule(moduleBitCount, bitsPerModule, takeAway)) {
      bitCountDifference = getBitCountDifference(moduleBitCount, bitsPerModule);
      if (checkBitCountDifference(moduleBitCount, bitCountDifference, bitsPerModule)) {
        return;
      }
    }

    // I hope we never get here :-)
    throw new RuntimeException();
  }

  private static void reduceBitCount(int[] moduleBitCount, int bitsPerModule) {
    final boolean takeAway = true;
    adjustSmallDifference(moduleBitCount, bitsPerModule, takeAway);
    int bitCountDifference = getBitCountDifference(moduleBitCount, bitsPerModule);
    if (checkBitCountDifference(moduleBitCount, bitCountDifference, bitsPerModule)) {
      return;
    }
    if (bitCountDifference == 0) {
      adjustSmallDifference(moduleBitCount, bitsPerModule, !takeAway);
      bitCountDifference = getBitCountDifference(moduleBitCount, bitsPerModule);
      if (checkBitCountDifference(moduleBitCount, bitCountDifference, bitsPerModule)) {
        return;
      }
    }
    if (bitCountDifference < 0) {
      enlargeBitCount(moduleBitCount, bitsPerModule);
      return;
    }

    while (adjustHalfSizeModule(moduleBitCount, bitsPerModule, takeAway)) {
      bitCountDifference = getBitCountDifference(moduleBitCount, bitsPerModule);
      if (checkBitCountDifference(moduleBitCount, bitCountDifference, bitsPerModule)) {
        return;
      }
      if (bitCountDifference < 0) {
        enlargeBitCount(moduleBitCount, bitsPerModule);
        return;
      }
    }

    bitCountDifference = getBitCountDifference(moduleBitCount, bitsPerModule);
    if (checkBitCountDifference(moduleBitCount, bitCountDifference, bitsPerModule)) {
      return;
    }
    if (bitCountDifference < 0) {
      enlargeBitCount(moduleBitCount, bitsPerModule);
      return;
    }

    while (adjustBiggestModule(moduleBitCount, bitsPerModule, takeAway)) {
      bitCountDifference = getBitCountDifference(moduleBitCount, bitsPerModule);
      if (checkBitCountDifference(moduleBitCount, bitCountDifference, bitsPerModule)) {
        return;
      }
      if (bitCountDifference < 0) {
        enlargeBitCount(moduleBitCount, bitsPerModule);
        return;
      }
    }
    bitCountDifference = getBitCountDifference(moduleBitCount, bitsPerModule);
    if (checkBitCountDifference(moduleBitCount, bitCountDifference, bitsPerModule)) {
      return;
    }
    if (bitCountDifference < 0) {
      enlargeBitCount(moduleBitCount, bitsPerModule);
      return;
    }

    // here we should have all moduleBitCounts % bitsPerModule == 0
    while (adjustWholeModule(moduleBitCount, bitsPerModule, takeAway)) {
      bitCountDifference = getBitCountDifference(moduleBitCount, bitsPerModule);
      if (checkBitCountDifference(moduleBitCount, bitCountDifference, bitsPerModule)) {
        return;
      }
    }

    // I hope we never get here :-)
    throw new RuntimeException();
  }

  private static boolean adjustWholeModule(int[] moduleBitCount, int bitsPerModule,
                                           boolean takeAway) {
    int index = -1;
    int biggestModuleSize = -1;
    for (int i = 0; i < moduleBitCount.length; i++) {
      if (moduleBitCount[i] > biggestModuleSize) {
        index = i;
        biggestModuleSize = moduleBitCount[i];
      }
    }
    if (index != -1) {
      moduleBitCount[index] += takeAway ? -bitsPerModule : bitsPerModule;
      return true;
    }
    return false;
  }

  private static boolean adjustBiggestModule(int[] moduleBitCount, int bitsPerModule,
                                             boolean takeAway) {
    int index = -1;
    int biggestModuleSize = -1;
    int moduleDifference = -1;
    for (int i = 0; i < moduleBitCount.length; i++) {
      int bitDifference = moduleBitCount[i] % bitsPerModule;
      if (bitDifference == 0) {
        continue;
      }

      if (bitDifference > moduleDifference) {
        moduleDifference = bitDifference;
        index = i;
        biggestModuleSize = moduleBitCount[i];
      } else if (bitDifference == moduleDifference && moduleBitCount[i] > biggestModuleSize) {
        index = i;
        biggestModuleSize = moduleBitCount[i];
      }
    }
    if (index != -1) {
      moduleBitCount[index] += takeAway ? -moduleDifference : bitsPerModule -
          moduleDifference;
      return true;
    }
    return false;
  }

  private static boolean adjustHalfSizeModule(int[] moduleBitCount, int bitsPerModule,
                                              boolean takeAway) {
    int index = -1;
    int biggestModuleSize = -1;
    for (int i = 0; i < moduleBitCount.length; i++) {
      int bitDifference = moduleBitCount[i] % bitsPerModule;
      if (bitDifference == 0) {
        continue;
      }
      if ((bitDifference << 1) == bitsPerModule) {
        if (biggestModuleSize < moduleBitCount[i]) {
          biggestModuleSize = moduleBitCount[i];
          index = i;
        }
      }
    }
    if (index != -1) {
      moduleBitCount[index] += takeAway ? -bitsPerModule / 2 : bitsPerModule / 2;
      return true;
    }
    return false;
  }

  /**
   * 
   * @param moduleBitCount
   * @return <0 if bits are missing, 0 if bitcount is ok, >0 is bits must be removed
   */
  private static int getBitCountDifference(int[] moduleBitCount, int bitsPerModule) {
    int bitCountSum = PDF417Common.getBitCountSum(moduleBitCount);
    return bitCountSum - bitsPerModule * PDF417Common.MODULES_IN_CODEWORD;
  }

  private static void adjustSmallDifference(int[] moduleBitCount, int bitsPerModule,
                                            boolean takeAway) {
    if (takeAway) {
      shrinkModulesWithSmallDifference(moduleBitCount, bitsPerModule);
    } else {
      enlargeModulesWithSmallDifference(moduleBitCount, bitsPerModule);
    }
  }

  private static boolean checkBitCountDifference(int[] moduleBitCount,
                                                 int bitCountDifference, int bitsPerModule) {
    int bitCountSum = 0;
    if (bitCountDifference == 0) {
      for (int i = 0; i < moduleBitCount.length; i++) {
        if (moduleBitCount[i] % bitsPerModule != 0) {
          return false;
        }
        bitCountSum += moduleBitCount[i];
      }
      if (bitCountSum / PDF417Common.MODULES_IN_CODEWORD != bitsPerModule) {
        return false;
      }
      for (int i = 0; i < moduleBitCount.length; i++) {
        moduleBitCount[i] /= bitsPerModule;
      }
      return true;
    }
    return false;
  }

  private static void enlargeModulesWithSmallDifference(int[] moduleBitCount,
                                                        int bitsPerModule) {
    for (int i = 0; i < moduleBitCount.length; i++) {
      int bitDifference = moduleBitCount[i] % bitsPerModule;
      if (bitDifference == 0) {
        continue;
      }
      if ((bitDifference << 1) > bitsPerModule) {
        moduleBitCount[i] += (bitsPerModule - bitDifference);
      } else if (moduleBitCount[i] == bitDifference) {
        moduleBitCount[i] = bitsPerModule;
      }
    }
  }

  private static void shrinkModulesWithSmallDifference(int[] moduleBitCount,
                                                       int bitsPerModule) {
    for (int i = 0; i < moduleBitCount.length; i++) {
      int bitDifference = moduleBitCount[i] % bitsPerModule;
      if (bitDifference == 0) {
        continue;
      }
      if (moduleBitCount[i] < bitsPerModule) {
        moduleBitCount[i] = bitsPerModule;
        continue;
      }
      if ((bitDifference << 1) < bitsPerModule) {
        moduleBitCount[i] -= bitDifference;
      } else if (moduleBitCount[i] == bitDifference) {
        moduleBitCount[i] = bitsPerModule;
      }
    }
  }
}
