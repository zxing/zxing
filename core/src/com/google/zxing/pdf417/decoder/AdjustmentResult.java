package com.google.zxing.pdf417.decoder;

import com.google.zxing.pdf417.PDF417Common;

import java.util.Arrays;

public class AdjustmentResult {
  private static final int BIT_COUNT_SIZE = 8;

  private final int[] moduleBitCount;
  private final int bitsPerModule;
  private int bitCountDifference;

  private AdjustmentResult(int[] moduleBitCount, int bitsPerModule, int bitCountDifference) {
    this.moduleBitCount = moduleBitCount;
    this.bitsPerModule = bitsPerModule;
    this.bitCountDifference = bitCountDifference;

    adjustToMinimumSize();
  }

  public static AdjustmentResult getAdjustmentResult(int[] moduleBitCount) {
    int bitCountSum = PDF417Common.getBitCountSum(moduleBitCount);
    int bitCountDifference = bitCountSum % PDF417Common.MODULES_IN_CODEWORD;
    int bitsPerModule = bitCountSum / PDF417Common.MODULES_IN_CODEWORD;
    if (bitsPerModule == 0) {
      return null;
    }
    for (int bitCount : moduleBitCount) {
      if (bitCount == 0) {
        return null;
      }
    }
    if ((bitCountDifference << 1) > PDF417Common.MODULES_IN_CODEWORD) {
      bitCountDifference = bitCountDifference - PDF417Common.MODULES_IN_CODEWORD;
      bitsPerModule++;
    }
    // TODO is this guaranteed to always stop eventually?
    //    if (bitsPerModule == 1 || Math.abs(bitCountDifference) > 5) {
    if (Math.abs(bitCountDifference) > 5) {
      if (bitsPerModule % 2 == 0) {
        bitsPerModule++;
      }
      for (int i = 0; i < moduleBitCount.length; i++) {
        moduleBitCount[i] = moduleBitCount[i] << 1;
      }
      return getAdjustmentResult(moduleBitCount);
    }
    return new AdjustmentResult(moduleBitCount, bitsPerModule, bitCountDifference);
  }

  public boolean adjustWholeModule() {
    int index = -1;
    int biggestModuleSize = -1;
    for (int i = 0; i < moduleBitCount.length; i++) {
      if (moduleBitCount[i] > biggestModuleSize) {
        index = i;
        biggestModuleSize = moduleBitCount[i];
      }
    }
    if (index != -1) {
      if (isToSmall()) {
        add(index, bitsPerModule);
      } else {
        subtract(index, bitsPerModule);
      }
      return true;
    }
    return false;
  }

  /**
   * Not sure what the reasoning behind this was when I implemented it.
   * @return true if sth was changed, false otherwise.
   */
  public boolean adjustModuleWithBiggestDifference() {
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
      if (isToSmall()) {
        add(index, bitsPerModule - moduleDifference);
      } else {
        subtract(index, moduleDifference);
      }
      return true;
    }
    return false;
  }

  /**
   * 
   * @param adjustmentResults
   * @return true, if sth was changed, false otherwise
   */
  private boolean adjustHalfSizeModuleNeighbors(AdjustmentResults adjustmentResults) {
    boolean changed = false;
    int previousHalfSizeIndex = -1;
    for (int i = 0; i < BIT_COUNT_SIZE; i++) {
      int bitDifference = moduleBitCount[i] % bitsPerModule;
      if (bitDifference == 0 || (bitDifference << 1) != bitsPerModule) {
        continue;
      }
      if (previousHalfSizeIndex == -1 || previousHalfSizeIndex != i - 1) {
        previousHalfSizeIndex = i;
        continue;
      }

      // For now we just grow the bigger and shrink the smaller one and only
      // create a copy for equally sized neighbors
      if (moduleBitCount[i] > moduleBitCount[previousHalfSizeIndex]) {
        add(i, bitDifference);
        subtract(previousHalfSizeIndex, bitDifference);
      } else if (moduleBitCount[i] < moduleBitCount[previousHalfSizeIndex]) {
        subtract(i, bitDifference);
        add(previousHalfSizeIndex, bitDifference);
      } else {
        AdjustmentResult copy = new AdjustmentResult(Arrays.copyOf(moduleBitCount,
            moduleBitCount.length), bitsPerModule, bitCountDifference);
        adjustmentResults.add(copy);
        add(i, bitDifference);
        subtract(previousHalfSizeIndex, bitDifference);

        copy.subtract(i, bitDifference);
        copy.add(previousHalfSizeIndex, bitDifference);
        if (copy.isToLarge()) {
          PDF417CodewordDecoder.reduceBitCount(adjustmentResults, copy);
        } else {
          PDF417CodewordDecoder.enlargeBitCount(adjustmentResults, copy);
        }
      }
      changed = true;
      previousHalfSizeIndex = -1;
    }
    return changed;
  }

  /**
   * 
   * @param results
   * @return true, if a module was changed, false otherwise
   */
  public boolean adjustHalfSizeModule(AdjustmentResults results) {
    if (bitCountDifference != 0) {
      // adjust biggest half size module
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
        if (isToSmall()) {
          add(index, bitsPerModule >> 1);
        } else {
          subtract(index, bitsPerModule >> 1);
        }
        return true;
      }
      return false;
    }
    return adjustHalfSizeModuleNeighbors(results);
  }

  public void enlargeModulesWithSmallDifference() {
    for (int i = 0; i < BIT_COUNT_SIZE; i++) {
      int bitDifference = moduleBitCount[i] % bitsPerModule;
      if (bitDifference == 0) {
        continue;
      }
      if ((bitDifference << 1) > bitsPerModule) {
        add(i, bitsPerModule - bitDifference);
      }
    }
  }

  public void shrinkModulesWithSmallDifference() {
    for (int i = 0; i < BIT_COUNT_SIZE; i++) {
      int bitDifference = moduleBitCount[i] % bitsPerModule;
      if (bitDifference == 0) {
        continue;
      }
      if ((bitDifference << 1) < bitsPerModule) {
        subtract(i, bitDifference);
      }
    }
  }

  public void add(int index, int value) {
    bitCountDifference += value;
    moduleBitCount[index] += value;
  }

  public void subtract(int index, int value) {
    bitCountDifference -= value;
    moduleBitCount[index] -= value;
  }

  public boolean hasCorrectSize() {
    return bitCountDifference == 0;
  }

  public boolean isToSmall() {
    return bitCountDifference < 0;
  }

  public boolean isToLarge() {
    return bitCountDifference > 0;
  }

  public boolean isValid() {
    if (bitCountDifference != 0) {
      return false;
    }
    for (int i = 0; i < moduleBitCount.length; i++) {
      if (moduleBitCount[i] % bitsPerModule != 0) {
        return false;
      }
    }
    return true;
  }

  public int[] getModuleCount() {
    for (int i = 0; i < moduleBitCount.length; i++) {
      moduleBitCount[i] /= bitsPerModule;
    }
    return moduleBitCount;
  }

  private void adjustToMinimumSize() {
    for (int i = 0; i < BIT_COUNT_SIZE; i++) {
      if (moduleBitCount[i] >= bitsPerModule) {
        continue;
      }
      int bitDifference = bitsPerModule - moduleBitCount[i];
      add(i, bitDifference);
      int minNeighborSize = bitsPerModule + bitDifference;
      if (i == 0) {
        if (moduleBitCount[1] >= minNeighborSize) {
          subtract(1, bitDifference);
        }
      } else if (i == BIT_COUNT_SIZE - 1) {
        if (moduleBitCount[BIT_COUNT_SIZE - 2] >= minNeighborSize) {
          subtract(BIT_COUNT_SIZE - 2, bitDifference);
        }
      } else {
        int neigborIndex;
        if (moduleBitCount[i - 1] >= moduleBitCount[i + 1]) {
          neigborIndex = i - 1;
        } else {
          neigborIndex = i + 1;
        }
        if (moduleBitCount[neigborIndex] >= minNeighborSize) {
          subtract(neigborIndex, 1);
        }
      }
    }
  }
}
