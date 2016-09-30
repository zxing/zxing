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

public final class UPCEWriterTestCase extends Assert {

  @Test
  public void testEncode() throws WriterException {
    String testStr = "0000000000010101110010100111000101101011110110111001011101010100000000000";
    BitMatrix result = new UPCEWriter().encode("05096893", BarcodeFormat.UPC_E, testStr.length(), 0);
    assertEquals(testStr, BitMatrixTestCase.matrixToString(result));
  }

  @Test
  public void testAddChecksumAndEncode() throws WriterException {
    String testStr = "0000000000010101110010100111000101101011110110111001011101010100000000000";
    BitMatrix result = new UPCEWriter().encode("0509689", BarcodeFormat.UPC_E, testStr.length(), 0);
    assertEquals(testStr, BitMatrixTestCase.matrixToString(result));
  }

}
