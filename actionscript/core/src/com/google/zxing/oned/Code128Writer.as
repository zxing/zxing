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
import com.google.zxing.common.flexdatatypes.HashTable;
import com.google.zxing.common.flexdatatypes.IllegalArgumentException;
import com.google.zxing.common.flexdatatypes.ArrayList;

/**
 * This object renders a CODE128 code as a {@link BitMatrix}.
 * 
 * @author erik.barbara@gmail.com (Erik Barbara)
 */
public final class Code128Writer extends UPCEANWriter {

  private static var CODE_START_B:int = 104;
  private static var CODE_START_C:int = 105;
  private static var CODE_CODE_B:int = 100;
  private static var CODE_CODE_C:int = 99;
  private static var CODE_STOP:int = 106;

  // Dummy characters used to specify control characters in input
  private static var ESCAPE_FNC_1:String = '\u00f1';
  private static var ESCAPE_FNC_2:String = '\u00f2';
  private static var ESCAPE_FNC_3:String = '\u00f3';
  private static var ESCAPE_FNC_4:String = '\u00f4';

  private static var CODE_FNC_1:int = 102;   // Code A, Code B, Code C
  private static var CODE_FNC_2:int = 97;    // Code A, Code B
  private static var CODE_FNC_3:int = 96;    // Code A, Code B
  private static var CODE_FNC_4_B:int = 100; // Code B

  public override function  encode(contents:String ,
                          format:BarcodeFormat = null,
                          width:int = 0,
                          height:int = 0,
                          hints:HashTable = null):Object // either BitMatrix or String 
  {
  	if (format != null)
  	{
    	if (format != BarcodeFormat.CODE_128) {
      	throw new IllegalArgumentException("Can only encode CODE_128, but got " + format);
    	}
    	//returns a BitMatrix
   		return super.encode(contents, format, width, height, hints);
  	}
	// this part will return a string
    var length:int = contents.length;
    // Check length
    if (length < 1 || length > 80) {throw new IllegalArgumentException("Contents length should be between 1 and 80 characters, but got " + length);
    }
    // Check content
    for (var i:int = 0; i < length; i++) {
      var c:String = contents.charAt(i);
      if (c < ' ' || c > '~') {
        switch (c) {
          case ESCAPE_FNC_1:
          case ESCAPE_FNC_2:
          case ESCAPE_FNC_3:
          case ESCAPE_FNC_4:
            break;
          default:
            throw new IllegalArgumentException("Bad character in input: " + c);
        }
      }
    }
    
    var patterns:ArrayList = new ArrayList(); // temporary storage for patterns
    var checkSum:int = 0;
    var checkWeight:int = 1;
    var codeSet:int = 0; // selected code (CODE_CODE_B or CODE_CODE_C)
    var position:int = 0; // position in contents
    
    while (position < length) {
      //Select code to use
      var requiredDigitCount:int = codeSet == CODE_CODE_C ? 2 : 4;
      var newCodeSet:int;
      if (isDigits(contents, position, requiredDigitCount)) {
        newCodeSet = CODE_CODE_C;
      } else {
        newCodeSet = CODE_CODE_B;
      }
      
      //Get the pattern index
      var patternIndex:int;
      if (newCodeSet == codeSet) {
        // Encode the current character
        if (codeSet == CODE_CODE_B) {
          patternIndex = contents.charCodeAt(position) - (' ').charCodeAt(0);
          position += 1;
        } else { // CODE_CODE_C
          switch (contents.charAt(position)) {
            case ESCAPE_FNC_1:
              patternIndex = CODE_FNC_1;
              position++;
              break;
            case ESCAPE_FNC_2:
              patternIndex = CODE_FNC_2;
              position++;
              break;
            case ESCAPE_FNC_3:
              patternIndex = CODE_FNC_3;
              position++;
              break;
            case ESCAPE_FNC_4:
              patternIndex = CODE_FNC_4_B; // FIXME if this ever outputs Code A
              position++;
              break;
            default:
              patternIndex = parseInt(contents.substring(position, position + 2));
              position += 2;
              break;
          }
        }
      } else {
        // Should we change the current code?
        // Do we have a code set?
        if (codeSet == 0) {
          // No, we don't have a code set
          if (newCodeSet == CODE_CODE_B) {
            patternIndex = CODE_START_B;
          } else {
            // CODE_CODE_C
            patternIndex = CODE_START_C;
          }
        } else {
          // Yes, we have a code set
          patternIndex = newCodeSet;
        }
        codeSet = newCodeSet;
      }
      
      // Get the pattern
      patterns.addElement(Code128Reader.CODE_PATTERNS[patternIndex]);
      
      // Compute checksum
      checkSum += patternIndex * checkWeight;
      if (position != 0) {
        checkWeight++;
      }
    }
    
    // Compute and append checksum
    checkSum %= 103;
    patterns.addElement(Code128Reader.CODE_PATTERNS[checkSum]);
    
    // Append stop code
    patterns.addElement(Code128Reader.CODE_PATTERNS[CODE_STOP]);
    
    // Compute code width
    var codeWidth:int = 0;
    for (var j:int=0;j<patterns.length;j++)
    {
      var pattern:Array = patterns.getObjectByIndex(j) as Array;
      for (i = 0; i < pattern.length; i++) 
      {
        codeWidth += pattern[i];
      }
    } 
        
    // Compute result
    var result:Array = new Array(codeWidth);
    var pos:int = 0;
    for (var jk:int=0;jk<patterns.length;jk++)
    {
      pos += appendPattern(result, pos, patterns.getObjectByIndex(jk)  as Array, 1);
    }
    
    return result;
  }

  private static function isDigits(value:String, start:int, length:int):Boolean {
    var end:int = start + length;
    var last:int = value.length;
    for (var i:int = start; i < end && i < last; i++) {
      var c:String = value.charAt(i);
      if (c < '0' || c > '9') {
        if (c != ESCAPE_FNC_1) {
          return false;
        }
        end++; // ignore FNC_1
      }
    }
    return end <= last; // end > last if we've run out of string
  }
}
}
