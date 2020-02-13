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

import com.google.zxing.BarcodeFormat;
import com.google.zxing.CoverageTool2000;

import java.util.Collection;
import java.util.Collections;


/**
 * This class renders CodaBar as {@code boolean[]}.
 *
 * @author dsbnatut@gmail.com (Kazuki Nishiura)
 */
public final class CodaBarWriter extends OneDimensionalCodeWriter {

  private static final char[] START_END_CHARS = {'A', 'B', 'C', 'D'};
  private static final char[] ALT_START_END_CHARS = {'T', 'N', '*', 'E'};
  private static final char[] CHARS_WHICH_ARE_TEN_LENGTH_EACH_AFTER_DECODED = {'/', ':', '+', '.'};
  private static final char DEFAULT_GUARD = START_END_CHARS[0];

  @Override
  protected Collection<BarcodeFormat> getSupportedWriteFormats() {
    return Collections.singleton(BarcodeFormat.CODABAR);
  }

  @Override
  public boolean[] encode(String contents) {


    if (contents.length() < 2) {
      CoverageTool2000.setCoverageMatrix(3, 0);
      // Can't have a start/end guard, so tentatively add default guards
      contents = DEFAULT_GUARD + contents + DEFAULT_GUARD;
    } else {
      CoverageTool2000.setCoverageMatrix(3, 1);
      // Verify input and calculate decoded length.
      char firstChar = Character.toUpperCase(contents.charAt(0));
      char lastChar = Character.toUpperCase(contents.charAt(contents.length() - 1));
      boolean startsNormal = CodaBarReader.arrayContains(START_END_CHARS, firstChar);
      boolean endsNormal = CodaBarReader.arrayContains(START_END_CHARS, lastChar);
      boolean startsAlt = CodaBarReader.arrayContains(ALT_START_END_CHARS, firstChar);
      boolean endsAlt = CodaBarReader.arrayContains(ALT_START_END_CHARS, lastChar);
      if (startsNormal) {
        CoverageTool2000.setCoverageMatrix(3, 2);
        if (!endsNormal) {
          CoverageTool2000.setCoverageMatrix(3, 3);
          throw new IllegalArgumentException("Invalid start/end guards: " + contents);
        }
        CoverageTool2000.setCoverageMatrix(3, 4);
        // else already has valid start/end
      } else if (startsAlt) {
        CoverageTool2000.setCoverageMatrix(3, 5);
        if (!endsAlt) {
          CoverageTool2000.setCoverageMatrix(3, 6);
          throw new IllegalArgumentException("Invalid start/end guards: " + contents);
        }
        CoverageTool2000.setCoverageMatrix(3, 7);
        // else already has valid start/end
      } else {
        CoverageTool2000.setCoverageMatrix(3, 8);
        // Doesn't start with a guard
        if (endsNormal || endsAlt) {
          CoverageTool2000.setCoverageMatrix(3, 9);
          throw new IllegalArgumentException("Invalid start/end guards: " + contents);
        }
        CoverageTool2000.setCoverageMatrix(3, 10);
        // else doesn't end with guard either, so add a default
        contents = DEFAULT_GUARD + contents + DEFAULT_GUARD;
      }
    }


    // The start character and the end character are decoded to 10 length each.
    int resultLength = 20;
    for (int i = 1; i < contents.length() - 1; i++) {

      if (Character.isDigit(contents.charAt(i)) || contents.charAt(i) == '-' || contents.charAt(i) == '$') {
        CoverageTool2000.setCoverageMatrix(3, 11);
        resultLength += 9;
      } else if (CodaBarReader.arrayContains(CHARS_WHICH_ARE_TEN_LENGTH_EACH_AFTER_DECODED, contents.charAt(i))) {
        CoverageTool2000.setCoverageMatrix(3, 12);
        resultLength += 10;
      } else {
        CoverageTool2000.setCoverageMatrix(3, 13);
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
        CoverageTool2000.setCoverageMatrix(3, 14);
        // The start/end chars are not in the CodaBarReader.ALPHABET.
        switch (c) {
          case 'T':
            CoverageTool2000.setCoverageMatrix(3, 15);
            c = 'A';
            break;
          case 'N':
            CoverageTool2000.setCoverageMatrix(3, 16);
            c = 'B';
            break;
          case '*':
            CoverageTool2000.setCoverageMatrix(3, 17);
            c = 'C';
            break;
          case 'E':
            CoverageTool2000.setCoverageMatrix(3, 18);
            c = 'D';
            break;
        }
      }
      CoverageTool2000.setCoverageMatrix(3, 19);
      int code = 0;
      for (int i = 0; i < CodaBarReader.ALPHABET.length; i++) {
        // Found any, because I checked above.
        if (c == CodaBarReader.ALPHABET[i]) {
          CoverageTool2000.setCoverageMatrix(3, 20);
          code = CodaBarReader.CHARACTER_ENCODINGS[i];
          break;
        }
        CoverageTool2000.setCoverageMatrix(3, 21);
      }
      boolean color = true;
      int counter = 0;
      int bit = 0;
      while (bit < 7) { // A character consists of 7 digit.
        result[position] = color;
        position++;
        if (((code >> (6 - bit)) & 1) == 0 || counter == 1) {
          CoverageTool2000.setCoverageMatrix(3, 22);
          color = !color; // Flip the color.
          bit++;
          counter = 0;
        } else {
          CoverageTool2000.setCoverageMatrix(3, 23);
          counter++;
        }
      }
      if (index < contents.length() - 1) {
        CoverageTool2000.setCoverageMatrix(3, 24);
        result[position] = false;
        position++;
      }
      CoverageTool2000.setCoverageMatrix(3, 25);
    }
    return result;
  }
}

