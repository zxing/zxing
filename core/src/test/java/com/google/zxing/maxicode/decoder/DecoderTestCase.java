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
import com.google.zxing.common.DecoderResult;

import java.util.Locale;

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
  public void testStructuredCarrierMessageLocaleIndependent() throws FormatException {
    // Mode 2 prepends a numeric postcode, country code and service class. These must decode to
    // Latin digits regardless of the JVM default locale (e.g. Arabic-Indic digits otherwise).
    byte[] datawords = new byte[94];
    Locale defaultLocale = Locale.getDefault();
    String expected;
    try {
      Locale.setDefault(Locale.ENGLISH);
      expected = DecodedBitStreamParser.decode(datawords, 2).getText();
      Locale.setDefault(new Locale("ar", "EG"));
      DecoderResult result = DecodedBitStreamParser.decode(datawords, 2);
      assertEquals(expected, result.getText());
      assertTrue(result.getText().startsWith("0"));
    } finally {
      Locale.setDefault(defaultLocale);
    }
  }

  @Test
  public void testTruncatedStructuredCarrierMessageHeader() {
    // A mode 2/3 secondary message that starts with the structured append header "[)>RS01GS"
    // but is shorter than the 9 characters the postcode/country/service splice assumes must be
    // rejected as a format error, not insert past the end of the buffer.
    byte[] datawords = new byte[94];
    // SHIFTB '[' ')' SHIFTB '>' RS '0' '1' GS -> decodes to exactly "[)>01" (7 chars)
    int[] message = {59, 42, 41, 59, 40, 30, 48, 49, 29};
    for (int i = 0; i < message.length; i++) {
      datawords[10 + i] = (byte) message[i];
    }
    for (int i = 10 + message.length; i < datawords.length; i++) {
      datawords[i] = 33; // PAD, trimmed off the decoded message
    }
    try {
      DecodedBitStreamParser.decode(datawords, 2);
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
