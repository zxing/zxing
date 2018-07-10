/*
 * Copyright 2017 ZXing authors
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
import com.google.zxing.common.BitMatrixTestCase;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests {@link ITFWriter}.
 */
public final class ITFWriterTestCase extends Assert {

  @Test
  public void testEncode() throws WriterException {
    doTest("00123456789012",
           "0000010101010111000111000101110100010101110001110111010001010001110100011" +
           "100010101000101011100011101011101000111000101110100010101110001110100000");
  }

  private static void doTest(String input, CharSequence expected) throws WriterException {
    BitMatrix result = new ITFWriter().encode(input, BarcodeFormat.ITF, 0, 0);
    assertEquals(expected, BitMatrixTestCase.matrixToString(result));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testEncodeIllegalCharacters() throws WriterException {
    new ITFWriter().encode("00123456789abc", BarcodeFormat.ITF, 0, 0);
  }

}
