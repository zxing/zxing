/*
 * Copyright 2015 ZXing authors
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
 * This object renders a CODE93 code as a BitMatrix
 */
public class Code93Writer extends OneDimensionalCodeWriter {
  @Override
  public BitMatrix encode(String contents,
                          BarcodeFormat format,
                          int width,
                          int height,
                          Map<EncodeHintType,?> hints) throws WriterException {
    if (format != BarcodeFormat.CODE_93) {
      throw new IllegalArgumentException("Can only encode CODE_93, but got " + format);
    }
    return super.encode(contents, format, width, height, hints);
  }

  /**
   * @param contents barcode contents to encode. It should not be encoded for extended characters.
   * @return a {@code boolean[]} of horizontal pixels (false = white, true = black)
   */
  @Override
  public boolean[] encode(String contents) {
    contents = convertToExtended(contents);
    int length = contents.length();
    if (length > 80) {
      throw new IllegalArgumentException(
        "Requested contents should be less than 80 digits long after converting to extended encoding, but got " + length);
    }

    //length of code + 2 start/stop characters + 2 checksums, each of 9 bits, plus a termination bar
    int codeWidth = (contents.length() + 2 + 2) * 9 + 1;

    boolean[] result = new boolean[codeWidth];

    //start character (*)
    int pos = appendPattern(result, 0, Code93Reader.ASTERISK_ENCODING);

    for (int i = 0; i < length; i++) {
      int indexInString = Code93Reader.ALPHABET_STRING.indexOf(contents.charAt(i));
      pos += appendPattern(result, pos, Code93Reader.CHARACTER_ENCODINGS[indexInString]);
    }

    //add two checksums
    int check1 = computeChecksumIndex(contents, 20);
    pos += appendPattern(result, pos, Code93Reader.CHARACTER_ENCODINGS[check1]);

    //append the contents to reflect the first checksum added
    contents += Code93Reader.ALPHABET_STRING.charAt(check1);

    int check2 = computeChecksumIndex(contents, 15);
    pos += appendPattern(result, pos, Code93Reader.CHARACTER_ENCODINGS[check2]);

    //end character (*)
    pos += appendPattern(result, pos, Code93Reader.ASTERISK_ENCODING);

    //termination bar (single black bar)
    result[pos] = true;

    return result;
  }

  /**
   * @param target output to append to
   * @param pos start position
   * @param pattern pattern to append
   * @param startColor unused
   * @return 9
   * @deprecated without replacement; intended as an internal-only method
   */
  @Deprecated
  protected static int appendPattern(boolean[] target, int pos, int[] pattern, boolean startColor) {
    for (int bit : pattern) {
      target[pos++] = bit != 0;
    }
    return 9;
  }

  private static int appendPattern(boolean[] target, int pos, int a) {
    for (int i = 0; i < 9; i++) {
      int temp = a & (1 << (8 - i));
      target[pos + i] = temp != 0;
    }
    return 9;
  }

  private static int computeChecksumIndex(String contents, int maxWeight) {
    int weight = 1;
    int total = 0;

    for (int i = contents.length() - 1; i >= 0; i--) {
      int indexInString = Code93Reader.ALPHABET_STRING.indexOf(contents.charAt(i));
      total += indexInString * weight;
      if (++weight > maxWeight) {
        weight = 1;
      }
    }
    return total % 47;
  }

  static String convertToExtended(String contents) {
    int length = contents.length();
    StringBuilder extendedContent = new StringBuilder(length * 2);
    for (int i = 0; i < length; i++) {
      char character = contents.charAt(i);
      // ($)=a, (%)=b, (/)=c, (+)=d. see Code93Reader.ALPHABET_STRING
      if (character == 0) {
        // NUL: (%)U
        extendedContent.append("bU");
      } else if (character <= 26) {
        // SOH - SUB: ($)A - ($)Z
        extendedContent.append('a');
        extendedContent.append((char) ('A' + character - 1));
      } else if (character <= 31) {
        // ESC - US: (%)A - (%)E
        extendedContent.append('b');
        extendedContent.append((char) ('A' + character - 27));
      } else if (character == ' ' || character == '$' || character == '%' || character == '+') {
        // space $ % +
        extendedContent.append(character);
      } else if (character <= ',') {
        // ! " # & ' ( ) * ,: (/)A - (/)L
        extendedContent.append('c');
        extendedContent.append((char) ('A' + character - '!'));
      } else if (character <= '9') {
        extendedContent.append(character);
      } else if (character == ':') {
        // :: (/)Z
        extendedContent.append("cZ");
      } else if (character <= '?') {
        // ; - ?: (%)F - (%)J
        extendedContent.append('b');
        extendedContent.append((char) ('F' + character - ';'));
      } else if (character == '@') {
        // @: (%)V
        extendedContent.append("bV");
      } else if (character <= 'Z') {
        // A - Z
        extendedContent.append(character);
      } else if (character <= '_') {
        // [ - _: (%)K - (%)O
        extendedContent.append('b');
        extendedContent.append((char) ('K' + character - '['));
      } else if (character == '`') {
        // `: (%)W
        extendedContent.append("bW");
      } else if (character <= 'z') {
        // a - z: (*)A - (*)Z
        extendedContent.append('d');
        extendedContent.append((char) ('A' + character - 'a'));
      } else if (character <= 127) {
        // { - DEL: (%)P - (%)T
        extendedContent.append('b');
        extendedContent.append((char) ('P' + character - '{'));
      } else {
        throw new IllegalArgumentException(
          "Requested content contains a non-encodable character: '" + character + "'");
      }
    }
    return extendedContent.toString();
  }

}
