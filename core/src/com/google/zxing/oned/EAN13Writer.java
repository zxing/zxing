/*
 * Copyright 2009 ZXing authors
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
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

import java.util.Hashtable;


/**
 * This object renders an EAN13 code as a {@link BitMatrix}.
 *
 * @author aripollak@gmail.com (Ari Pollak)
 */
public final class EAN13Writer extends UPCEANWriter {

  private static final int codeWidth = 3 + // start guard
      (7 * 6) + // left bars
      5 + // middle guard
      (7 * 6) + // right bars
      3; // end guard

  public BitMatrix encode(String contents, BarcodeFormat format, int width, int height,
      Hashtable hints) throws WriterException {
    if (format != BarcodeFormat.EAN_13) {
      throw new IllegalArgumentException("Can only encode EAN_13, but got " + format);
    }

    return super.encode(contents, format, width, height, hints);
  }

  public byte[] encode(String contents) {
    if (contents.length() != 13) {
      throw new IllegalArgumentException(
          "Requested contents should be 13 digits long, but got " + contents.length());
    }

    int firstDigit = Integer.parseInt(contents.substring(0, 1));
    int parities = EAN13Reader.FIRST_DIGIT_ENCODINGS[firstDigit];
    byte[] result = new byte[codeWidth];
    int pos = 0;

    pos += appendPattern(result, pos, UPCEANReader.START_END_PATTERN, 1);

    // See {@link #EAN13Reader} for a description of how the first digit & left bars are encoded
    for (int i = 1; i <= 6; i++) {
      int digit = Integer.parseInt(contents.substring(i, i + 1));
      if ((parities >> (6 - i) & 1) == 1) {
        digit += 10;
      }
      pos += appendPattern(result, pos, UPCEANReader.L_AND_G_PATTERNS[digit], 0);
    }

    pos += appendPattern(result, pos, UPCEANReader.MIDDLE_PATTERN, 0);

    for (int i = 7; i <= 12; i++) {
      int digit = Integer.parseInt(contents.substring(i, i + 1));
      pos += appendPattern(result, pos, UPCEANReader.L_PATTERNS[digit], 1);
    }
    pos += appendPattern(result, pos, UPCEANReader.START_END_PATTERN, 1);

    return result;
  }

}
