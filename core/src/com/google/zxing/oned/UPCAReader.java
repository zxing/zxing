/*
 * Copyright 2008 Google Inc.
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

import com.google.zxing.ReaderException;
import com.google.zxing.common.BitArray;

/**
 * <p>Implements decoding of the UPC-A format.</p>
 *
 * @author dswitkin@google.com (Daniel Switkin)
 * @author srowen@google.com (Sean Owen)
 */
public final class UPCAReader extends AbstractUPCEANReader {

  protected int decodeMiddle(BitArray row, int[] startRange, StringBuffer resultString) throws ReaderException {
    int middleStart = decodeDigits(row, startRange[1], resultString);
    int[] middleRange = findGuardPattern(row, middleStart, true, MIDDLE_PATTERN);
    return decodeDigits(row, middleRange[1], resultString);
  }

  /**
   * @param row row of black/white values to decode
   * @param start horizontal offset from which decoding starts
   * @param result {@link StringBuffer} to append decoded digits to
   * @return horizontal offset of first pixel after the six decoded digits
   * @throws ReaderException if six digits could not be decoded from the row
   */
  private static int decodeDigits(BitArray row, int start, StringBuffer result) throws ReaderException {
    int[] counters = new int[4];
    int end = row.getSize();
    int rowOffset = start;
    for (int x = 0; x < 6 && rowOffset < end; x++) {
      int bestMatch = decodeDigit(row, counters, rowOffset, L_PATTERNS);
      result.append((char) ('0' + bestMatch));
      for (int i = 0; i < counters.length; i++) {
        rowOffset += counters[i];
      }
    }
    return rowOffset;
  }

}