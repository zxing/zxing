/*
 * Copyright 2011 ZXing authors
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

import com.google.zxing.common.BitMatrix;

/**
 * This class renders CodaBar as {@link BitMatrix}.
 *
 * @author dsbnatut@gmail.com (Kazuki Nishiura)
 */
public class CodaBarWriter extends OneDimensionalCodeWriter {

  public CodaBarWriter() {
    // Super constructor requires the sum of the left and right margin length.
    // CodaBar spec requires a side margin to be more than ten times wider than narrow space.
    // In this implementation, narrow space has a unit length, so 20 is required minimum.
    super(20);
  }

  /*
   * @see OneDimensionalCodeWriter#encode(java.lang.String)
   */
  @Override
  public byte[] encode(String contents) {

    // Verify input and calculate decoded length.
    if (!CodaBarReader.arrayContains(
        new char[]{'A', 'B', 'C', 'D'}, Character.toUpperCase(contents.charAt(0)))) {
      throw new IllegalArgumentException(
          "Codabar should start with one of the following: 'A', 'B', 'C' or 'D'");
    }
    if (!CodaBarReader.arrayContains(new char[]{'T', 'N', '*', 'E'},
                                     Character.toUpperCase(contents.charAt(contents.length() - 1)))) {
      throw new IllegalArgumentException(
          "Codabar should end with one of the following: 'T', 'N', '*' or 'E'");
    }
    // The start character and the end character are decoded to 10 length each.
    int resultLength = 20;
    char[] charsWhichAreTenLengthEachAfterDecoded = {'/', ':', '+', '.'};
    for (int i = 1; i < contents.length() - 1; i++) {
      if (Character.isDigit(contents.charAt(i)) || contents.charAt(i) == '-'
          || contents.charAt(i) == '$') {
        resultLength += 9;
      } else if (CodaBarReader.arrayContains(
          charsWhichAreTenLengthEachAfterDecoded, contents.charAt(i))) {
        resultLength += 10;
      } else {
        throw new IllegalArgumentException("Cannot encode : '" + contents.charAt(i) + '\'');
      }
    }
    // A blank is placed between each character.
    resultLength += contents.length() - 1;

    byte[] result = new byte[resultLength];
    int position = 0;
    for (int index = 0; index < contents.length(); index++) {
      char c = Character.toUpperCase(contents.charAt(index));
      if (index == contents.length() - 1) {
        // Neither * nor E are in the CodaBarReader.ALPHABET.
        // * is equal to the  c pattern, and e is equal to the d pattern
        if (c == '*') {
          c = 'C';
        } else if (c == 'E') {
          c = 'D';
        }
      }
      int code = 0;
      for (int i = 0; i < CodaBarReader.ALPHABET.length; i++) {
        // Found any, because I checked above.
        if (c == CodaBarReader.ALPHABET[i]) {
          code = CodaBarReader.CHARACTER_ENCODINGS[i];
          break;
        }
      }
      byte color = 1;
      int counter = 0;
      int bit = 0;
      while (bit < 7) { // A character consists of 7 digit.
        result[position] = color;
        position++;
        if (((code >> (6 - bit)) & 1) == 0 || counter == 1) {
          color ^= 1; // Flip the color.
          bit++;
          counter = 0;
        } else {
          counter++;
        }
      }
      if (index < contents.length() - 1) {
        result[position] = 0;
        position++;
      }
    }
    return result;
  }
}

