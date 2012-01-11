/*
 * Copyright 2010 ZXing authors
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
import com.google.zxing.ChecksumException;
import com.google.zxing.FormatException;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.ResultPoint;
import com.google.zxing.common.BitArray;
import com.google.zxing.common.flexdatatypes.StringBuilder;

/**
 * <p>Decodes Code 93 barcodes.</p>
 *
 * @author Sean Owen
 * @see Code39Reader
 */
public final class Code93Reader extends OneDReader {

  // Note that 'abcd' are dummy characters in place of control characters.
  private static var ALPHABET_STRING:String = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ-. $/+%abcd*";
  private static var ALPHABET:Array = ALPHABET_STRING.split("");

  /**
   * These represent the encodings of characters, as patterns of wide and narrow bars.
   * The 9 least-significant bits of each int correspond to the pattern of wide and narrow.
   */
  private static var CHARACTER_ENCODINGS:Array = [
      0x114, 0x148, 0x144, 0x142, 0x128, 0x124, 0x122, 0x150, 0x112, 0x10A, // 0-9
      0x1A8, 0x1A4, 0x1A2, 0x194, 0x192, 0x18A, 0x168, 0x164, 0x162, 0x134, // A-J
      0x11A, 0x158, 0x14C, 0x146, 0x12C, 0x116, 0x1B4, 0x1B2, 0x1AC, 0x1A6, // K-T
      0x196, 0x19A, 0x16C, 0x166, 0x136, 0x13A, // U-Z
      0x12E, 0x1D4, 0x1D2, 0x1CA, 0x16E, 0x176, 0x1AE, // - - %
      0x126, 0x1DA, 0x1D6, 0x132, 0x15E, // Control chars? $-*
  ];
  private static var ASTERISK_ENCODING:int = CHARACTER_ENCODINGS[47];

  public override function decodeRow(rowNumber:Object, row:BitArray, hints:Object):Result
  {

    var start:Array = findAsteriskPattern(row);
    var nextStart:int = start[1];
    var end:int = row.getSize();

    // Read off white space
    while (nextStart < end && !row._get(nextStart)) {
      nextStart++;
    }

    var result:StringBuilder  = new StringBuilder(20);
    var counters:Array = new Array(6);
    var decodedChar:String;
    var lastStart:int;
    do {
      recordPattern(row, nextStart, counters);
      var pattern:int = toPattern(counters);
      if (pattern < 0) {
        throw NotFoundException.getNotFoundInstance();
      }
      decodedChar = patternToChar(pattern);
      result.Append(decodedChar);
      lastStart = nextStart;
      for (var i:int = 0; i < counters.length; i++) {
        nextStart += counters[i];
      }
      // Read off white space
      while (nextStart < end && !row._get(nextStart)) {
        nextStart++;
      }
    } while (decodedChar != '*');
    result.deleteCharAt(result.length - 1); // remove asterisk

    // Should be at least one more black module
    if (nextStart == end || !row._get(nextStart)) {
      throw NotFoundException.getNotFoundInstance();
    }

    if (result.length < 4) {
      // Almost surely a false positive
      throw NotFoundException.getNotFoundInstance();
    }

    checkChecksums(result);
    // Remove checksum digits
    result.setLength(result.length - 2);

    var resultString:String = decodeExtended(result);

    var left:Number =  (start[1] + start[0]) / 2.0;
    var right:Number = (nextStart + lastStart) / 2.0;
    return new Result(
        resultString,
        null,
        [
            new ResultPoint(left, Math.floor(rowNumber as Number)),
            new ResultPoint(right, Math.floor(rowNumber as Number))],
        BarcodeFormat.CODE_93);

  }

  private static function findAsteriskPattern(row:BitArray ):Array {
    var width:int = row.getSize();
    var rowOffset:int = 0;
    while (rowOffset < width) {
      if (row._get(rowOffset)) {
        break;
      }
      rowOffset++;
    }

    var counterPosition:int = 0;
    var counters:Array= new Array(6);
    for(var k:int=0;k<counters.length;k++){ counters[k] = 0; } 
    var patternStart:int = rowOffset;
    var isWhite:Boolean = false;
    var patternLength:int = counters.length;

    for (var i:int = rowOffset; i < width; i++) {
      var pixel:Boolean = row._get(i);
      if (pixel != isWhite) {
        counters[counterPosition]++;
      } else {
        if (counterPosition == patternLength - 1) {
          if (toPattern(counters) == ASTERISK_ENCODING) {
            return [patternStart, i];
          }
          patternStart += counters[0] + counters[1];
          for (var y:int = 2; y < patternLength; y++) {
            counters[y - 2] = counters[y];
          }
          counters[patternLength - 2] = 0;
          counters[patternLength - 1] = 0;
          counterPosition--;
        } else {
          counterPosition++;
        }
        counters[counterPosition] = 1;
        if (isWhite) { isWhite = false; } else { isWhite = true;} //isWhite = !isWhite;
      }
    }
    throw NotFoundException.getNotFoundInstance();
  }

  private static function toPattern(counters:Array):int {
    var max:int = counters.length;
    var sum:int = 0;
    for (var i:int = 0; i < max; i++) {
      sum += counters[i];
    }
    var pattern:int = 0;
    for (i = 0; i < max; i++) {
      var scaledShifted:int = (counters[i] << INTEGER_MATH_SHIFT) * 9 / sum;
      var scaledUnshifted:int = scaledShifted >> INTEGER_MATH_SHIFT;
      if ((scaledShifted & 0xFF) > 0x7F) {
        scaledUnshifted++;
      }
      if (scaledUnshifted < 1 || scaledUnshifted > 4) {
        return -1;
      }
      if ((i & 0x01) == 0) {
        for (var j:int = 0; j < scaledUnshifted; j++) {
          pattern = (pattern << 1) | 0x01;
        }
      } else {
        pattern <<= scaledUnshifted;
      }
    }
    return pattern;
  }

  private static function patternToChar(pattern:int):String {
    for (var i:int = 0; i < CHARACTER_ENCODINGS.length; i++) {
      if (CHARACTER_ENCODINGS[i] == pattern) {
        return ALPHABET[i];
      }
    }
    throw NotFoundException.getNotFoundInstance();
  }

  private static function decodeExtended(encoded:StringBuilder):String {
    var length:int = encoded.length;
    var decoded:StringBuilder  = new StringBuilder(length);
    for (var i:int = 0; i < length; i++) {
      var c:String = encoded.charAt(i);
      if (c >= 'a' && c <= 'd') {
        var next:String = encoded.charAt(i + 1);
        var decodedChar:String = '\0';
        switch (c) {
          case 'd':
            // +A to +Z map to a to z
            if (next >= 'A' && next <= 'Z') {
              decodedChar =  String.fromCharCode(next.charCodeAt(0) + 32);
            } else {
              throw FormatException.getFormatInstance();
            }
            break;
          case 'a':
            // $A to $Z map to control codes SH to SB
            if (next >= 'A' && next <= 'Z') {
              decodedChar = String.fromCharCode(next.charCodeAt(0) - 64);
            } else {
              throw FormatException.getFormatInstance();
            }
            break;
          case 'b':
            // %A to %E map to control codes ESC to US
            if (next >= 'A' && next <= 'E') {
              decodedChar = String.fromCharCode(next.charCodeAt(0) - 38);
            } else if (next >= 'F' && next <= 'W') {
              decodedChar = String.fromCharCode(next.charCodeAt(0) - 11);
            } else {
              throw FormatException.getFormatInstance();
            }
            break;
          case 'c':
            // /A to /O map to ! to , and /Z maps to :
            if (next >= 'A' && next <= 'O') {
              decodedChar = String.fromCharCode(next.charCodeAt(0) - 32);
            } else if (next == 'Z') {
              decodedChar = ':';
            } else {
              throw FormatException.getFormatInstance();
            }
            break;
        }
        decoded.Append(decodedChar);
        // bump up i again since we read two characters
        i++;
      } else {
        decoded.Append(c);
      }
    }
    return decoded.toString();
  }

  

  private static function checkChecksums(result:StringBuilder):void {
 		 var length:int = result.length;
 	   checkOneChecksum(result, length - 2, 20);
   	 checkOneChecksum(result, length - 1, 15);
  }
  
  private static function checkOneChecksum(result:StringBuilder, checkPosition:int, weightMax:int):void
  {
      var weight:int = 1;
	    var total:int = 0;
	    for (var i:int = checkPosition - 1; i >= 0; i--) {
	      total += weight * ALPHABET_STRING.indexOf(result.charAt(i));
	      if (++weight > weightMax) {
	        weight = 1;
	      }
	    }
	    if (result.charAt(checkPosition) != ALPHABET[total % 47]) {
	      throw ChecksumException.getChecksumInstance();
	    }
	  }
}
}
