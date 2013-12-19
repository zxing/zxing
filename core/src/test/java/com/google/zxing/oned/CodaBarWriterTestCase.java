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
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author dsbnatut@gmail.com (Kazuki Nishiura)
 * @author Sean Owen
 */
public final class CodaBarWriterTestCase extends Assert {

  @Test
  public void testEncode() throws WriterException {
    doTest("B515-3/B",
           "00000" +
           "1001001011" + "0110101001" + "0101011001" + "0110101001" + "0101001101" +
           "0110010101" + "01101101011" + "01001001011" +
           "00000");
  }

  @Test
  public void testEncode2() throws WriterException {
    doTest("T123T",
           "00000" +
           "1011001001" + "0101011001" + "0101001011" + "0110010101" + "01011001001" +
           "00000");
  }

  @Test
  public void testAltStartEnd() throws WriterException {
    assertEquals(encode("T123456789-$T"), encode("A123456789-$A"));
  }

  private static void doTest(String input, CharSequence expected) throws WriterException {
    BitMatrix result = encode(input);
    StringBuilder actual = new StringBuilder(result.getWidth());
    for (int i = 0; i < result.getWidth(); i++) {
      actual.append(result.get(i, 0) ? '1' : '0');
    }
    assertEquals(expected, actual.toString());
  }

  private static BitMatrix encode(String input) throws WriterException {
    return new CodaBarWriter().encode(input, BarcodeFormat.CODABAR, 0, 0);
  }

}
