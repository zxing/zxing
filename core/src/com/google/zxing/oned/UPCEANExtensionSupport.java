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

import java.util.Hashtable;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.ResultMetadataType;
import com.google.zxing.ResultPoint;
import com.google.zxing.common.BitArray;

final class UPCEANExtensionSupport {

  private static final int[] EXTENSION_START_PATTERN = {1,1,2};
  private static final int[] CHECK_DIGIT_ENCODINGS = {
      0x18, 0x14, 0x12, 0x11, 0x0C, 0x06, 0x03, 0x0A, 0x09, 0x05
  };

  private final int[] decodeMiddleCounters = new int[4];
  private final StringBuffer decodeRowStringBuffer = new StringBuffer();

  Result decodeRow(int rowNumber, BitArray row, int rowOffset) throws NotFoundException {

    int[] extensionStartRange = UPCEANReader.findGuardPattern(row, rowOffset, false, EXTENSION_START_PATTERN);

    StringBuffer result = decodeRowStringBuffer;
    result.setLength(0);
    int end = decodeMiddle(row, extensionStartRange, result);

    String resultString = result.toString();
    Hashtable extensionData = parseExtensionString(resultString);

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

  int decodeMiddle(BitArray row, int[] startRange, StringBuffer resultString) throws NotFoundException {
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
      for (int i = 0; i < counters.length; i++) {
        rowOffset += counters[i];
      }
      if (bestMatch >= 10) {
        lgPatternFound |= 1 << (4 - x);
      }
      if (x != 4) {
        // Read off separator if not last
        while (rowOffset < end && !row.get(rowOffset)) {
          rowOffset++;
        }
        while (rowOffset < end && row.get(rowOffset)) {
          rowOffset++;
        }
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

  private static int extensionChecksum(String s) {
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
   * @return formatted interpretation of raw content as a {@link Hashtable} mapping
   *  one {@link ResultMetadataType} to appropriate value, or <code>null</code> if not known
   */
  private static Hashtable parseExtensionString(String raw) {
    ResultMetadataType type;
    Object value;
    switch (raw.length()) {
      case 2:
        type = ResultMetadataType.ISSUE_NUMBER;
        value = parseExtension2String(raw);
        break;
      case 5:
        type = ResultMetadataType.SUGGESTED_PRICE;
        value = parseExtension5String(raw);
        break;
      default:
        return null;
    }
    if (value == null) {
      return null;
    }
    Hashtable result = new Hashtable(1);
    result.put(type, value);
    return result;
  }

  private static Integer parseExtension2String(String raw) {
    return Integer.valueOf(raw);
  }

  private static String parseExtension5String(String raw) {
    String currency = null;
    switch (raw.charAt(0)) {
      case '0':
        currency = "£";
        break;
      case '5':
        currency = "$";
        break;
      case '9':
        if ("99991".equals(raw)) {
          return "0.00";
        } else if ("99990".equals(raw)) {
          return "Used";
        }
        break;
      default:
        currency = "";
        break;
    }
    int rawAmount = Integer.parseInt(raw.substring(1));
    return currency + (rawAmount / 100) + '.' + (rawAmount % 100);
  }

}
