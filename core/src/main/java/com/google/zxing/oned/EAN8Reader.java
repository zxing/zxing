/*
 * Copyright 2008 ZXing authors
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

package com.google.zxing.oned;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.NotFoundException;
import com.google.zxing.common.BitArray;

/**
 * <p>Implements decoding of the EAN-8 format.</p>
 *
 * @author Sean Owen
 */
public final class EAN8Reader extends UPCEANReader {

  private final int[] decodeMiddleCounters;

  public EAN8Reader() {
    decodeMiddleCounters = new int[4];
  }

  @Override
  protected int decodeMiddle(BitArray row,
                             int[] startRange,
                             StringBuilder result) throws NotFoundException {
    int[] counters = decodeMiddleCounters;
    counters[0] = 0;
    counters[1] = 0;
    counters[2] = 0;
    counters[3] = 0;
    int end = row.getSize();
    int rowOffset = startRange[1];

    for (int x = 0; x < 4 && rowOffset < end; x++) {
      int bestMatch = decodeDigit(row, counters, rowOffset, L_PATTERNS);
      result.append((char) ('0' + bestMatch));
      for (int counter : counters) {
        rowOffset += counter;
      }
    }

    int[] middleRange = findGuardPattern(row, rowOffset, true, MIDDLE_PATTERN);
    rowOffset = middleRange[1];

    for (int x = 0; x < 4 && rowOffset < end; x++) {
      int bestMatch = decodeDigit(row, counters, rowOffset, L_PATTERNS);
      result.append((char) ('0' + bestMatch));
      for (int counter : counters) {
        rowOffset += counter;
      }
    }

    return rowOffset;
  }

  @Override
  BarcodeFormat getBarcodeFormat() {
    return BarcodeFormat.EAN_8;
  }

}
