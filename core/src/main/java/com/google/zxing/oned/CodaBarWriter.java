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

import java.util.Arrays;

/**
 * This class renders CodaBar as {@code boolean[]}.
 *
 * @author dsbnatut@gmail.com (Kazuki Nishiura)
 */
public final class CodaBarWriter extends OneDimensionalCodeWriter {

  private static final char[] START_END_CHARS = {'A', 'B', 'C', 'D'};
  private static final char[] ALT_START_END_CHARS = {'T', 'N', '*', 'E'};
  private static final char[] CHARS_WHICH_ARE_TEN_LENGTH_EACH_AFTER_DECODED = {'/', ':', '+', '.'};

  @Override
  public boolean[] encode(String contents) {

    if (contents.length() < 2) {
      throw new IllegalArgumentException("Codabar should start/end with start/stop symbols");
    }
    // Verify input and calculate decoded length.
    char firstChar = Character.toUpperCase(contents.charAt(0));
    char lastChar = Character.toUpperCase(contents.charAt(contents.length() - 1));
    boolean startsEndsNormal = 
        CodaBarReader.arrayContains(START_END_CHARS, firstChar) && 
        CodaBarReader.arrayContains(START_END_CHARS, lastChar);
    boolean startsEndsAlt = 
        CodaBarReader.arrayContains(ALT_START_END_CHARS, firstChar) && 
        CodaBarReader.arrayContains(ALT_START_END_CHARS, lastChar);
    if (!(startsEndsNormal || startsEndsAlt)) {
      throw new IllegalArgumentException(
          "Codabar should start/end with " + Arrays.toString(START_END_CHARS) + 
          ", or start/end with " + Arrays.toString(ALT_START_END_CHARS));
    }

    // The start character and the end character are decoded to 10 length each.
    int resultLength = 20;
    for (int i = 1; i < contents.length() - 1; i++) {
      if (Character.isDigit(contents.charAt(i)) || contents.charAt(i) == '-' || contents.charAt(i) == '$') {
        resultLength += 9;
      } else if (CodaBarReader.arrayContains(CHARS_WHICH_ARE_TEN_LENGTH_EACH_AFTER_DECODED, contents.charAt(i))) {
        resultLength += 10;
      } else {
        throw new IllegalArgumentException("Cannot encode : '" + contents.charAt(i) + '\'');
      }
    }
    // A blank is placed between each character.
    resultLength += contents.length() - 1;

    boolean[] result = new boolean[resultLength];
    int position = 0;
    for (int index = 0; index < contents.length(); index++) {
      char c = Character.toUpperCase(contents.charAt(index));
      if (index == 0 || index == contents.length() - 1) {
        // The start/end chars are not in the CodaBarReader.ALPHABET.
        switch (c) {
          case 'T':
            c = 'A';
            break;
          case 'N':
            c = 'B';
            break;
          case '*':
            c = 'C';
            break;
          case 'E':
            c = 'D';
            break;
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
      boolean color = true;
      int counter = 0;
      int bit = 0;
      while (bit < 7) { // A character consists of 7 digit.
        result[position] = color;
        position++;
        if (((code >> (6 - bit)) & 1) == 0 || counter == 1) {
          color = !color; // Flip the color.
          bit++;
          counter = 0;
        } else {
          counter++;
        }
      }
      if (index < contents.length() - 1) {
        result[position] = false;
        position++;
      }
    }
    return result;
  }
}

