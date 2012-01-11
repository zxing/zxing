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

package com.google.zxing.oned
{

import com.google.zxing.BarcodeFormat;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.ResultMetadataType;
import com.google.zxing.ResultPoint;
import com.google.zxing.common.BitArray;
import com.google.zxing.common.flexdatatypes.HashTable;
import com.google.zxing.common.flexdatatypes.StringBuilder;

public class UPCEANExtensionSupport {

  private var EXTENSION_START_PATTERN:Array = [1,1,2];
  private static var CHECK_DIGIT_ENCODINGS:Array = [
      0x18, 0x14, 0x12, 0x11, 0x0C, 0x06, 0x03, 0x0A, 0x09, 0x05
  ];

  private var decodeMiddleCounters:Array = new Array(4);
  private var decodeRowStringBuffer:StringBuilder = new StringBuilder();

  public function decodeRow(rowNumber:int, row:BitArray, rowOffset:int):Result {

    var extensionStartRange:Array = UPCEANReader.findGuardPattern(row, rowOffset, false, EXTENSION_START_PATTERN);

    var result:StringBuilder = decodeRowStringBuffer;
    result.length = 0;
   var end:int = decodeMiddle(row, extensionStartRange, result);

    var resultString:String = result.toString();
    var extensionData:HashTable = parseExtensionString(resultString);

    var extensionResult:Result =
        new Result(resultString,
                   null,
                   [
                       new ResultPoint((extensionStartRange[0] + extensionStartRange[1]) / 2.0, rowNumber),
                       new ResultPoint( end, rowNumber),
                   ],
                   BarcodeFormat.UPC_EAN_EXTENSION);
    if (extensionData != null) {
      extensionResult.putAllMetadata(extensionData);
    }
    return extensionResult;
  }

  public function decodeMiddle(row:BitArray, startRange:Array, resultString:StringBuilder):int {
    var counters:Array = decodeMiddleCounters;
    counters[0] = 0;
    counters[1] = 0;
    counters[2] = 0;
    counters[3] = 0;
    var end:int = row.getSize();
    var rowOffset:int = startRange[1];

    var lgPatternFound:int = 0;

    for (var x:int = 0; x < 5 && rowOffset < end; x++) {
      var bestMatch:int = UPCEANReader.decodeDigit(row, counters, rowOffset, UPCEANReader.L_AND_G_PATTERNS);
      resultString.Append((('0' as String).charCodeAt(0) + bestMatch % 10));
      for (var i:int = 0; i < counters.length; i++) {
        rowOffset += counters[i];
      }
      if (bestMatch >= 10) {
        lgPatternFound |= 1 << (4 - x);
      }
      if (x != 4) {
        // Read off separator if not last
        while (rowOffset < end && !row._get(rowOffset)) {
          rowOffset++;
        }
        while (rowOffset < end && row._get(rowOffset)) {
          rowOffset++;
        }
      }
    }

    if (resultString.length != 5) {
      throw NotFoundException.getNotFoundInstance();
    }

    var checkDigit:int = determineCheckDigit(lgPatternFound);
    if (extensionChecksum(resultString.toString()) != checkDigit) {
      throw NotFoundException.getNotFoundInstance();
    }
    
    return rowOffset;
  }

  private static function extensionChecksum(s:String):int {
    var length:int = s.length;
    var sum:int = 0;
    for (var i:int = length - 2; i >= 0; i -= 2) {
      sum +=  s.charAt(i).charCodeAt(0) - ('0' as String).charCodeAt(0);
    }
    sum *= 3;
    for (i = length - 1; i >= 0; i -= 2) {
      sum += s.charAt(i).charCodeAt(0) - ('0' as String).charCodeAt(0);
    }
    sum *= 3;
    return sum % 10;
  }

  private static function determineCheckDigit(lgPatternFound:int):int {
    for (var d:int = 0; d < 10; d++) {
      if (lgPatternFound == UPCEANExtensionSupport.CHECK_DIGIT_ENCODINGS[d]) {
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
  private static function parseExtensionString(raw:String):HashTable {
    var type:ResultMetadataType;
    var value:Object ;
    switch (raw.length) {
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
    var result:HashTable = new HashTable(1);
    result._put(type, value);
    return result;
  }

  private static function parseExtension2String(raw:String):int {
    return int(raw);
  }

  private static function parseExtension5String(raw:String):String {
    var currency:String;
    switch (raw.charAt(0)) {
      case '0':
        currency = "£";
        break;
      case '5':
        currency = "$";
        break;
      case '9':
        // Reference: http://www.jollytech.com
        if (raw == "90000") {
          // No suggested retail price
          return null;
        } else if (raw == "99991") {
          // Complementary
          return "0.00";
        } else if (raw == "99990") {
          return "Used";
        }
        // Otherwise... unknown currency?
        currency = "";
        break;
      default:
        currency = "";
        break;
    }
    var rawAmount:int = int(raw.substring(1));
    var unitsString:String = (Number(rawAmount / 100)).toString();
    var hundredths:int = rawAmount % 100;
    var hundredthsString:String = hundredths < 10 ? String.fromCharCode(("0" as String).charCodeAt(0)+ hundredths)  : String(hundredths);
    return currency + unitsString + '.' + hundredthsString;
  }

}
}