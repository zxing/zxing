/*
 * Copyright 2024 ZXing authors
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

package com.google.zxing.maxicode.decoder;

import com.google.zxing.ChecksumException;
import com.google.zxing.FormatException;
import com.google.zxing.common.BitMatrix;

import org.junit.Test;
import org.junit.Assert;

/**
 * Tests {@link Decoder}.
 */
public final class DecoderTestCase extends Assert {

  @Test
  public void testOversizedMatrix() throws ChecksumException {
    // A matrix larger than the 30x33 MaxiCode size must be rejected as a format error,
    // not indexed past the fixed bit-number table.
    BitMatrix bits = new BitMatrix(40, 40);
    for (int y = 0; y < 40; y++) {
      for (int x = 0; x < 40; x++) {
        if (((x * 7 + y * 13) & 1) == 0) {
          bits.set(x, y);
        }
      }
    }
    try {
      new Decoder().decode(bits);
      fail("Expected FormatException");
    } catch (FormatException expected) {
      // good
    }
  }

  @Test
  public void testWrongDimensions() throws ChecksumException {
    for (int[] wh : new int[][] {{31, 33}, {30, 34}, {29, 33}, {30, 32}}) {
      try {
        new Decoder().decode(new BitMatrix(wh[0], wh[1]));
        fail("Expected FormatException for " + wh[0] + "x" + wh[1]);
      } catch (FormatException expected) {
        // good
      }
    }
  }

}
