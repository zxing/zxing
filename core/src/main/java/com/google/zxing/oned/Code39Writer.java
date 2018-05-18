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

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

import java.util.Map;

/**
 * This object renders a CODE39 code as a {@link BitMatrix}.
 *
 * @author erik.barbara@gmail.com (Erik Barbara)
 */
public final class Code39Writer extends OneDimensionalCodeWriter {

  @Override
  public BitMatrix encode(String contents,
                          BarcodeFormat format,
                          int width,
                          int height,
                          Map<EncodeHintType,?> hints) throws WriterException {
    if (format != BarcodeFormat.CODE_39) {
      throw new IllegalArgumentException("Can only encode CODE_39, but got " + format);
    }
    return super.encode(contents, format, width, height, hints);
  }

  @Override
  public boolean[] encode(String contents) {
    int length = contents.length();
    if (length > 80) {
      throw new IllegalArgumentException(
          "Requested contents should be less than 80 digits long, but got " + length);
    }

    for (int i = 0; i < length; i++) {
      int indexInString = Code39Reader.ALPHABET_STRING.indexOf(contents.charAt(i));
      if (indexInString < 0) {
        contents = tryToConvertToExtendedMode(contents);
        length = contents.length();
        if (length > 80) {
          throw new IllegalArgumentException(
              "Requested contents should be less than 80 digits long, but got " + length + " (extended full ASCII mode)");
        }
        break;
      }
    }

    int[] widths = new int[9];
    int codeWidth = 24 + 1 + length;
    for (int i = 0; i < length; i++) {
      int indexInString = Code39Reader.ALPHABET_STRING.indexOf(contents.charAt(i));
      toIntArray(Code39Reader.CHARACTER_ENCODINGS[indexInString], widths);
      for (int width : widths) {
        codeWidth += width;
      }
    }
    boolean[] result = new boolean[codeWidth];
    toIntArray(Code39Reader.ASTERISK_ENCODING, widths);
    int pos = appendPattern(result, 0, widths, true);
    int[] narrowWhite = {1};
    pos += appendPattern(result, pos, narrowWhite, false);
    //append next character to byte matrix
    for (int i = 0; i < length; i++) {
      int indexInString = Code39Reader.ALPHABET_STRING.indexOf(contents.charAt(i));
      toIntArray(Code39Reader.CHARACTER_ENCODINGS[indexInString], widths);
      pos += appendPattern(result, pos, widths, true);
      pos += appendPattern(result, pos, narrowWhite, false);
    }
    toIntArray(Code39Reader.ASTERISK_ENCODING, widths);
    appendPattern(result, pos, widths, true);
    return result;
  }

  private static void toIntArray(int a, int[] toReturn) {
    for (int i = 0; i < 9; i++) {
      int temp = a & (1 << (8 - i));
      toReturn[i] = temp == 0 ? 1 : 2;
    }
  }

  private static String tryToConvertToExtendedMode(String contents) {
     int length = contents.length();
     StringBuilder extendedContent = new StringBuilder();
     for (int i = 0; i < length; i++) {
       char character = contents.charAt(i);
       switch (character) {
         case '\u0000':
           extendedContent.append("%U");
           break;
         case ' ':
         case '-':
         case '.':
           extendedContent.append(character);
           break;
         case '@':
           extendedContent.append("%V");
           break;
         case '`':
           extendedContent.append("%W");
           break;
         default:
           if (character <= 26) {
             extendedContent.append('$');
             extendedContent.append((char) ('A' + (character - 1)));
           } else if (character < ' ') {
             extendedContent.append('%');
             extendedContent.append((char) ('A' + (character - 27)));
           } else if (character <= ',' || character == '/' || character == ':') {
             extendedContent.append('/');
             extendedContent.append((char) ('A' + (character - 33)));
           } else if (character <= '9') {
             extendedContent.append((char) ('0' + (character - 48)));
           } else if (character <= '?') {
             extendedContent.append('%');
             extendedContent.append((char) ('F' + (character - 59)));
           } else if (character <= 'Z') {
             extendedContent.append((char) ('A' + (character - 65)));
           } else if (character <= '_') {
             extendedContent.append('%');
             extendedContent.append((char) ('K' + (character - 91)));
           } else if (character <= 'z') {
             extendedContent.append('+');
             extendedContent.append((char) ('A' + (character - 97)));
           } else if (character <= 127) {
             extendedContent.append('%');
             extendedContent.append((char) ('P' + (character - 123)));
           } else {
             throw new IllegalArgumentException("Requested content contains a non-encodable character: '" + contents.charAt(i) + "'");
           }
           break;
       }
    }

    return extendedContent.toString();
  }

}
