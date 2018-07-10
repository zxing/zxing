/*
 * Copyright 2016 ZXing authors
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
 * Tests {@link UPCEWriter}.
 */
public final class UPCEWriterTestCase extends Assert {

  @Test
  public void testEncode() throws WriterException {
    doTest("05096893",
           "0000000000010101110010100111000101101011110110111001011101010100000000000");
  }

  @Test
  public void testEncodeSystem1() throws WriterException {
    doTest("12345670",
           "0000000000010100100110111101010001101110010000101001000101010100000000000");
  }

  @Test
  public void testAddChecksumAndEncode() throws WriterException {
    doTest("0509689",
           "0000000000010101110010100111000101101011110110111001011101010100000000000");
  }

  private static void doTest(String content, String encoding) throws WriterException {
    BitMatrix result = new UPCEWriter().encode(content, BarcodeFormat.UPC_E, encoding.length(), 0);
    assertEquals(encoding, BitMatrixTestCase.matrixToString(result));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testEncodeIllegalCharacters() throws WriterException {
    new UPCEWriter().encode("05096abc", BarcodeFormat.UPC_E, 0, 0);
  }
}
