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

import com.google.zxing.common.detector.MathUtils;
import com.google.zxing.pdf417.PDF417Common;

/**
 * @author Guenther Grau
 * @author creatale GmbH (christoph.schulz@creatale.de)
 */
final class PDF417CodewordDecoder {

  private static final float[] RATIOS_TABLE =
      new float[PDF417Common.SYMBOL_TABLE.length * PDF417Common.BARS_IN_MODULE];

  static {
    // Pre-computes the symbol ratio table.
    for (int i = 0; i < PDF417Common.SYMBOL_TABLE.length; i++) {
      int currentSymbol = PDF417Common.SYMBOL_TABLE[i];
      int currentBit = currentSymbol & 0x1;
      int rowBase = i * PDF417Common.BARS_IN_MODULE;
      for (int j = 0; j < PDF417Common.BARS_IN_MODULE; j++) {
        float size = 0.0f;
        while ((currentSymbol & 0x1) == currentBit) {
          size += 1.0f;
          currentSymbol >>= 1;
        }
        currentBit = currentSymbol & 0x1;
        RATIOS_TABLE[rowBase + PDF417Common.BARS_IN_MODULE - j - 1] =
            size / PDF417Common.MODULES_IN_CODEWORD;
      }
    }
  }

  private PDF417CodewordDecoder() {
  }

  static int getDecodedValue(int[] moduleBitCount) {
    int bitCountSum = MathUtils.sum(moduleBitCount);
    int decodedValue = sampleBitValue(moduleBitCount, bitCountSum);
    if (PDF417Common.getCodeword(decodedValue) != -1) {
      return decodedValue;
    }
    return getClosestDecodedValue(moduleBitCount, bitCountSum);
  }

  private static int sampleBitValue(int[] moduleBitCount, int bitCountSum) {
    int bitCountIndex = 0;
    int sumPreviousBits = 0;
    int bitValue = 0;
    int scale = 2 * PDF417Common.MODULES_IN_CODEWORD;
    for (int i = 0; i < PDF417Common.MODULES_IN_CODEWORD; i++) {
      if ((sumPreviousBits + moduleBitCount[bitCountIndex]) * scale <= (2 * i + 1) * bitCountSum) {
        sumPreviousBits += moduleBitCount[bitCountIndex];
        bitCountIndex++;
      }
      bitValue = (bitValue << 1) | ((bitCountIndex & 1) ^ 1);
    }
    return bitValue;
  }

  private static int getClosestDecodedValue(int[] moduleBitCount, int bitCountSum) {
    float r0 = 0.0f;
    float r1 = 0.0f;
    float r2 = 0.0f;
    float r3 = 0.0f;
    float r4 = 0.0f;
    float r5 = 0.0f;
    float r6 = 0.0f;
    float r7 = 0.0f;
    if (bitCountSum > 1) {
      float sum = bitCountSum;
      r0 = moduleBitCount[0] / sum;
      r1 = moduleBitCount[1] / sum;
      r2 = moduleBitCount[2] / sum;
      r3 = moduleBitCount[3] / sum;
      r4 = moduleBitCount[4] / sum;
      r5 = moduleBitCount[5] / sum;
      r6 = moduleBitCount[6] / sum;
      r7 = moduleBitCount[7] / sum;
    }
    float bestMatchError = Float.MAX_VALUE;
    int bestMatch = -1;
    float[] table = RATIOS_TABLE;
    int[] symbols = PDF417Common.SYMBOL_TABLE;
    for (int j = 0, offset = 0; j < symbols.length; j++, offset += PDF417Common.BARS_IN_MODULE) {
      float diff = table[offset] - r0;
      float error = diff * diff;
      if (error >= bestMatchError) {
        continue;
      }
      diff = table[offset + 1] - r1;
      error += diff * diff;
      if (error >= bestMatchError) {
        continue;
      }
      diff = table[offset + 2] - r2;
      error += diff * diff;
      if (error >= bestMatchError) {
        continue;
      }
      diff = table[offset + 3] - r3;
      error += diff * diff;
      if (error >= bestMatchError) {
        continue;
      }
      diff = table[offset + 4] - r4;
      error += diff * diff;
      diff = table[offset + 5] - r5;
      error += diff * diff;
      diff = table[offset + 6] - r6;
      error += diff * diff;
      diff = table[offset + 7] - r7;
      error += diff * diff;
      if (error < bestMatchError) {
        bestMatchError = error;
        bestMatch = symbols[j];
      }
    }
    return bestMatch;
  }

}
