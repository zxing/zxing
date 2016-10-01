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
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

import com.google.zxing.common.BitMatrixTestCase;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author qwandor@google.com (Andrew Walbran)
 */
public final class UPCAWriterTestCase extends Assert {

  @Test
  public void testEncode() throws WriterException {
    String testStr = "00001010100011011011101100010001011010111101111010101011100101110100100111011001101101100101110010100000";
    BitMatrix result = new UPCAWriter().encode("485963095124", BarcodeFormat.UPC_A, testStr.length(), 0);
    assertEquals(testStr, BitMatrixTestCase.matrixToString(result));
  }

  @Test
  public void testAddChecksumAndEncode() throws WriterException {
    String testStr = "00001010011001001001101111010100011011000101011110101010001001001000111010011100101100110110110010100000";
    BitMatrix result = new UPCAWriter().encode("12345678901", BarcodeFormat.UPC_A, testStr.length(), 0);
    assertEquals(testStr, BitMatrixTestCase.matrixToString(result));
  }

}
