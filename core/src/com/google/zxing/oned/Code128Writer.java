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

package com.google.zxing.oned;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

/**
 * This object renders a CODE128 code as a {@link BitMatrix}.
 * 
 * @author erik.barbara@gmail.com (Erik Barbara)
 */
public final class Code128Writer extends UPCEANWriter {

  private static final int CODE_START_B = 104;
  private static final int CODE_START_C = 105;
  private static final int CODE_CODE_B = 100;
  private static final int CODE_CODE_C = 99;
  private static final int CODE_STOP = 106;

  public BitMatrix encode(String contents,
                          BarcodeFormat format,
                          int width,
                          int height,
                          Hashtable hints) throws WriterException {
    if (format != BarcodeFormat.CODE_128) {
      throw new IllegalArgumentException("Can only encode CODE_128, but got " + format);
    }
    return super.encode(contents, format, width, height, hints);
  }

  public byte[] encode(String contents) {
    int length = contents.length();
    // Check length
    if (length < 1 || length > 80) {
      throw new IllegalArgumentException(
          "Contents length should be between 1 and 80 characters, but got " + length);
    }
    // Check content
    for (int i = 0; i < length; i++) {
      char c = contents.charAt(i);
      if (c < ' ' || c > '~') {
        throw new IllegalArgumentException("Contents should only contain characters between ' ' and '~'");
      }
    }
    
    Vector patterns = new Vector(); // temporary storage for patterns
    int checkSum = 0;
    int checkWeight = 1;
    int codeSet = 0; // selected code (CODE_CODE_B or CODE_CODE_C)
    int position = 0; // position in contents
    
    while (position < length) {
      //Select code to use
      int requiredDigitCount = codeSet == CODE_CODE_C ? 2 : 4;
      int newCodeSet;
      if (length - position >= requiredDigitCount && isDigits(contents, position, requiredDigitCount)) {
        newCodeSet = CODE_CODE_C;
      } else {
        newCodeSet = CODE_CODE_B;
      }
      
      //Get the pattern index
      int patternIndex;
      if (newCodeSet == codeSet) {
        // Encode the current character
        if (codeSet == CODE_CODE_B) {
          patternIndex = contents.charAt(position) - ' ';
          position += 1;
        } else { // CODE_CODE_C
          patternIndex = Integer.parseInt(contents.substring(position, position + 2));
          position += 2;
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
    int codeWidth = 0;
    Enumeration patternEnumeration = patterns.elements();
    while (patternEnumeration.hasMoreElements()) {
      int[] pattern = (int[]) patternEnumeration.nextElement();
      for (int i = 0; i < pattern.length; i++) {
        codeWidth += pattern[i];
      }
    }
    
    // Compute result
    byte[] result = new byte[codeWidth];
    patternEnumeration = patterns.elements();
    int pos = 0;
    while (patternEnumeration.hasMoreElements()) {
      int[] pattern = (int[]) patternEnumeration.nextElement();
      pos += appendPattern(result, pos, pattern, 1);
    }
    
    return result;
  }

  private static boolean isDigits(String value, int start, int length) {
    int end = start + length;
    for (int i = start; i < end; i++) {
      char c = value.charAt(i);
      if (c < '0' || c > '9') {
        return false;
      }
    }
    return true;
  }

}
