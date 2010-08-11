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

import java.util.Hashtable;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

/**
 * This object renders a ITF code as a {@link BitMatrix}.
 * 
 * @author erik.barbara@gmail.com (Erik Barbara)
 */
public final class ITFWriter extends UPCEANWriter {

  public BitMatrix encode(String contents,
                          BarcodeFormat format,
                          int width,
                          int height,
                          Hashtable hints) throws WriterException {
    if (format != BarcodeFormat.ITF) {
      throw new IllegalArgumentException("Can only encode ITF, but got " + format);
    }

    return super.encode(contents, format, width, height, hints);
  }

  public byte[] encode(String contents) {
    int length = contents.length();
    if (length > 80) {
      throw new IllegalArgumentException(
          "Requested contents should be less than 80 digits long, but got " + length);
    }
    byte[] result = new byte[9 + 9 * length];
    int[] start = {1, 1, 1, 1};
    int pos = appendPattern(result, 0, start, 1);
    for (int i = 0; i < length; i += 2) {
      int one = Character.digit(contents.charAt(i), 10);
      int two = Character.digit(contents.charAt(i+1), 10);
      int[] encoding = new int[18];
      for (int j = 0; j < 5; j++) {
        encoding[(j << 1)] = ITFReader.PATTERNS[one][j];
        encoding[(j << 1) + 1] = ITFReader.PATTERNS[two][j];
      }
      pos += appendPattern(result, pos, encoding, 1);
    }
    int[] end = {3, 1, 1};
    pos += appendPattern(result, pos, end, 1);

    return result;
  }

}