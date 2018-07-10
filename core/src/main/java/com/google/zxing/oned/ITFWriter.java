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
 * This object renders a ITF code as a {@link BitMatrix}.
 *
 * @author erik.barbara@gmail.com (Erik Barbara)
 */
public final class ITFWriter extends OneDimensionalCodeWriter {

  private static final int[] START_PATTERN = {1, 1, 1, 1};
  private static final int[] END_PATTERN = {3, 1, 1};

  private static final int W = 3; // Pixel width of a 3x wide line
  private static final int N = 1; // Pixed width of a narrow line

  // See ITFReader.PATTERNS

  private static final int[][] PATTERNS = {
      {N, N, W, W, N}, // 0
      {W, N, N, N, W}, // 1
      {N, W, N, N, W}, // 2
      {W, W, N, N, N}, // 3
      {N, N, W, N, W}, // 4
      {W, N, W, N, N}, // 5
      {N, W, W, N, N}, // 6
      {N, N, N, W, W}, // 7
      {W, N, N, W, N}, // 8
      {N, W, N, W, N}  // 9
  };

  @Override
  public BitMatrix encode(String contents,
                          BarcodeFormat format,
                          int width,
                          int height,
                          Map<EncodeHintType,?> hints) throws WriterException {
    if (format != BarcodeFormat.ITF) {
      throw new IllegalArgumentException("Can only encode ITF, but got " + format);
    }

    return super.encode(contents, format, width, height, hints);
  }

  @Override
  public boolean[] encode(String contents) {
    int length = contents.length();
    if (length % 2 != 0) {
      throw new IllegalArgumentException("The length of the input should be even");
    }
    if (length > 80) {
      throw new IllegalArgumentException(
          "Requested contents should be less than 80 digits long, but got " + length);
    }

    checkNumeric(contents);

    boolean[] result = new boolean[9 + 9 * length];
    int pos = appendPattern(result, 0, START_PATTERN, true);
    for (int i = 0; i < length; i += 2) {
      int one = Character.digit(contents.charAt(i), 10);
      int two = Character.digit(contents.charAt(i + 1), 10);
      int[] encoding = new int[10];
      for (int j = 0; j < 5; j++) {
        encoding[2 * j] = PATTERNS[one][j];
        encoding[2 * j + 1] = PATTERNS[two][j];
      }
      pos += appendPattern(result, pos, encoding, true);
    }
    appendPattern(result, pos, END_PATTERN, true);

    return result;
  }

}
