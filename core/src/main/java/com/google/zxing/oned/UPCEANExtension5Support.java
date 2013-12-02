/*
 * Copyright (C) 2010 ZXing authors
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
import com.google.zxing.Result;
import com.google.zxing.ResultMetadataType;
import com.google.zxing.ResultPoint;
import com.google.zxing.common.BitArray;

import java.util.EnumMap;
import java.util.Map;

/**
 * @see UPCEANExtension2Support
 */
final class UPCEANExtension5Support {

  private static final int[] CHECK_DIGIT_ENCODINGS = {
      0x18, 0x14, 0x12, 0x11, 0x0C, 0x06, 0x03, 0x0A, 0x09, 0x05
  };

  private final int[] decodeMiddleCounters = new int[4];
  private final StringBuilder decodeRowStringBuffer = new StringBuilder();

  Result decodeRow(int rowNumber, BitArray row, int[] extensionStartRange) throws NotFoundException {

    StringBuilder result = decodeRowStringBuffer;
    result.setLength(0);
    int end = decodeMiddle(row, extensionStartRange, result);

    String resultString = result.toString();
    Map<ResultMetadataType,Object> extensionData = parseExtensionString(resultString);

    Result extensionResult =
        new Result(resultString,
                   null,
                   new ResultPoint[] {
                       new ResultPoint((extensionStartRange[0] + extensionStartRange[1]) / 2.0f, (float) rowNumber),
                       new ResultPoint((float) end, (float) rowNumber),
                   },
                   BarcodeFormat.UPC_EAN_EXTENSION);
    if (extensionData != null) {
      extensionResult.putAllMetadata(extensionData);
    }
    return extensionResult;
  }

  int decodeMiddle(BitArray row, int[] startRange, StringBuilder resultString) throws NotFoundException {
    int[] counters = decodeMiddleCounters;
    counters[0] = 0;
    counters[1] = 0;
    counters[2] = 0;
    counters[3] = 0;
    int end = row.getSize();
    int rowOffset = startRange[1];

    int lgPatternFound = 0;

    for (int x = 0; x < 5 && rowOffset < end; x++) {
      int bestMatch = UPCEANReader.decodeDigit(row, counters, rowOffset, UPCEANReader.L_AND_G_PATTERNS);
      resultString.append((char) ('0' + bestMatch % 10));
      for (int counter : counters) {
        rowOffset += counter;
      }
      if (bestMatch >= 10) {
        lgPatternFound |= 1 << (4 - x);
      }
      if (x != 4) {
        // Read off separator if not last
        rowOffset = row.getNextSet(rowOffset);
        rowOffset = row.getNextUnset(rowOffset);
      }
    }

    if (resultString.length() != 5) {
      throw NotFoundException.getNotFoundInstance();
    }

    int checkDigit = determineCheckDigit(lgPatternFound);
    if (extensionChecksum(resultString.toString()) != checkDigit) {
      throw NotFoundException.getNotFoundInstance();
    }
    
    return rowOffset;
  }

  private static int extensionChecksum(CharSequence s) {
    int length = s.length();
    int sum = 0;
    for (int i = length - 2; i >= 0; i -= 2) {
      sum += (int) s.charAt(i) - (int) '0';
    }
    sum *= 3;
    for (int i = length - 1; i >= 0; i -= 2) {
      sum += (int) s.charAt(i) - (int) '0';
    }
    sum *= 3;
    return sum % 10;
  }

  private static int determineCheckDigit(int lgPatternFound)
      throws NotFoundException {
    for (int d = 0; d < 10; d++) {
      if (lgPatternFound == CHECK_DIGIT_ENCODINGS[d]) {
        return d;
      }
    }
    throw NotFoundException.getNotFoundInstance();
  }

  /**
   * @param raw raw content of extension
   * @return formatted interpretation of raw content as a {@link Map} mapping
   *  one {@link ResultMetadataType} to appropriate value, or {@code null} if not known
   */
  private static Map<ResultMetadataType,Object> parseExtensionString(String raw) {
    if (raw.length() != 5) {
      return null;
    }
    Object value = parseExtension5String(raw);
    if (value == null) {
      return null;
    }
    Map<ResultMetadataType,Object> result = new EnumMap<>(ResultMetadataType.class);
    result.put(ResultMetadataType.SUGGESTED_PRICE, value);
    return result;
  }

  private static String parseExtension5String(String raw) {
    String currency;
    switch (raw.charAt(0)) {
      case '0':
        currency = "£";
        break;
      case '5':
        currency = "$";
        break;
      case '9':
        // Reference: http://www.jollytech.com
        if ("90000".equals(raw)) {
          // No suggested retail price
          return null;
        }
        if ("99991".equals(raw)) {
          // Complementary
          return "0.00";
        }
        if ("99990".equals(raw)) {
          return "Used";
        }
        // Otherwise... unknown currency?
        currency = "";
        break;
      default:
        currency = "";
        break;
    }
    int rawAmount = Integer.parseInt(raw.substring(1));
    String unitsString = String.valueOf(rawAmount / 100);
    int hundredths = rawAmount % 100;
    String hundredthsString = hundredths < 10 ? "0" + hundredths : String.valueOf(hundredths);
    return currency + unitsString + '.' + hundredthsString;
  }

}
