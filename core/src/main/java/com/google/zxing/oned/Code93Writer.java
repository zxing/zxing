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

  @Override
  public boolean[] encode(String contents) {
    int length = contents.length();
    if (length > 80) {
      throw new IllegalArgumentException(
        "Requested contents should be less than 80 digits long, but got " + length);
    }
    //each character is encoded by 9 of 0/1's
    int[] widths = new int[9];

    //length of code + 2 start/stop characters + 2 checksums, each of 9 bits, plus a termination bar
    int codeWidth = (contents.length() + 2 + 2) * 9 + 1;

    //start character (*)
    toIntArray(Code93Reader.CHARACTER_ENCODINGS[47], widths);

    boolean[] result = new boolean[codeWidth];
    int pos = appendPattern(result, 0, widths);

    for (int i = 0; i < length; i++) {
      int indexInString = Code93Reader.ALPHABET_STRING.indexOf(contents.charAt(i));
      toIntArray(Code93Reader.CHARACTER_ENCODINGS[indexInString], widths);
      pos += appendPattern(result, pos, widths);
    }

    //add two checksums
    int check1 = computeChecksumIndex(contents, 20);
    toIntArray(Code93Reader.CHARACTER_ENCODINGS[check1], widths);
    pos += appendPattern(result, pos, widths);

    //append the contents to reflect the first checksum added
    contents += Code93Reader.ALPHABET_STRING.charAt(check1);

    int check2 = computeChecksumIndex(contents, 15);
    toIntArray(Code93Reader.CHARACTER_ENCODINGS[check2], widths);
    pos += appendPattern(result, pos, widths);

    //end character (*)
    toIntArray(Code93Reader.CHARACTER_ENCODINGS[47], widths);
    pos += appendPattern(result, pos, widths);

    //termination bar (single black bar)
    result[pos] = true;

    return result;
  }

  private static void toIntArray(int a, int[] toReturn) {
    for (int i = 0; i < 9; i++) {
      int temp = a & (1 << (8 - i));
      toReturn[i] = temp == 0 ? 0 : 1;
    }
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
    return appendPattern(target, pos, pattern);
  }

  private static int appendPattern(boolean[] target, int pos, int[] pattern) {
    for (int bit : pattern) {
      target[pos++] = bit != 0;
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
}
