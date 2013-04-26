/*
 * Copyright 2013 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.zxing.pdf417.decoder;

import com.google.zxing.pdf417.PDF417Common;

/**
 * @author Guenther Grau
 * @author creatale GmbH (christoph.schulz@creatale.de)
 */
final class PDF417CodewordDecoder {

  private static final float[][] RATIOS_TABLE = 
      new float[PDF417Common.SYMBOL_TABLE.length][PDF417Common.BARS_IN_MODULE];

  static {
    // Pre-computes the symbol ratio table.
    for (int i = 0; i < PDF417Common.SYMBOL_TABLE.length; i++) {
      int currentSymbol = PDF417Common.SYMBOL_TABLE[i];
      int currentBit = currentSymbol & 0x1;
      for (int j = 0; j < PDF417Common.BARS_IN_MODULE; j++) {
        float size = 0.0f;
        while ((currentSymbol & 0x1) == currentBit) {
          size += 1.0f;
          currentSymbol >>= 1;
        }
        currentBit = currentSymbol & 0x1;
        RATIOS_TABLE[i][PDF417Common.BARS_IN_MODULE - j - 1] = size / PDF417Common.MODULES_IN_CODEWORD;
      }
    }
  }

  private PDF417CodewordDecoder() {
  }

  static int getDecodedValue(int[] moduleBitCount) {
    int decodedValue = getDecodedCodewordValue(sampleBitCounts(moduleBitCount));
    if (decodedValue != -1) {
      return decodedValue;
    }
    return getClosestDecodedValue(moduleBitCount);
  }

  private static int[] sampleBitCounts(int[] moduleBitCount) {
    float bitCountSum = PDF417Common.getBitCountSum(moduleBitCount);
    int[] result = new int[PDF417Common.BARS_IN_MODULE];
    int bitCountIndex = 0;
    int sumPreviousBits = 0;
    for (int i = 0; i < PDF417Common.MODULES_IN_CODEWORD; i++) {
      float sampleIndex = 
          bitCountSum / (2 * PDF417Common.MODULES_IN_CODEWORD) + 
          (i * bitCountSum) / PDF417Common.MODULES_IN_CODEWORD;
      if (sumPreviousBits + moduleBitCount[bitCountIndex] <= sampleIndex) {
        sumPreviousBits += moduleBitCount[bitCountIndex];
        bitCountIndex++;
      }
      result[bitCountIndex]++;
    }
    return result;
  }

  private static int getDecodedCodewordValue(int[] moduleBitCount) {
    int decodedValue = getBitValue(moduleBitCount);
    return PDF417Common.getCodeword(decodedValue) == -1 ? -1 : decodedValue;
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

  private static int getClosestDecodedValue(int[] moduleBitCount) {
    int bitCountSum = PDF417Common.getBitCountSum(moduleBitCount);
    float[] bitCountRatios = new float[PDF417Common.BARS_IN_MODULE];
    for (int i = 0; i < bitCountRatios.length; i++) {
      bitCountRatios[i] = moduleBitCount[i] / (float) bitCountSum;
    }
    float bestMatchError = Float.MAX_VALUE;
    int bestMatch = -1;
    for (int j = 0; j < RATIOS_TABLE.length; j++) {
      float error = 0.0f;
      for (int k = 0; k < PDF417Common.BARS_IN_MODULE; k++) {
        float diff = RATIOS_TABLE[j][k] - bitCountRatios[k];
        error += diff * diff;
      }
      if (error < bestMatchError) {
        bestMatchError = error;
        bestMatch = PDF417Common.SYMBOL_TABLE[j];
      }
    }
    return bestMatch;
  }

}
