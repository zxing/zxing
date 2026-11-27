/*
 * Copyright 2026 ZXing authors
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

package com.google.zxing.qrcode.decoder;

import com.google.zxing.ChecksumException;
import com.google.zxing.EncodeHintType;
import com.google.zxing.FormatException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.DecoderResult;
import com.google.zxing.qrcode.encoder.ByteMatrix;
import com.google.zxing.qrcode.encoder.Encoder;
import com.google.zxing.qrcode.encoder.QRCode;

import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.Map;

/**
 * Tests {@link Decoder}, including ISO/IEC 18004 misdecode-protection reserves on small QR versions.
 */
public final class DecoderTestCase extends Assert {

  @Test
  public void testMisdecodeProtectionSmallVersions() throws Exception {
    // Version 1-L: d=7, p=3 -> max 2 errors (not 3)
    assertErrorCorrectionLimit(1, ErrorCorrectionLevel.L, 2);
    // Version 1-M: d=10, p=2 -> max 4 errors (not 5)
    assertErrorCorrectionLimit(1, ErrorCorrectionLevel.M, 4);
    // Version 1-Q: d=13, p=1 -> max 6 errors
    assertErrorCorrectionLimit(1, ErrorCorrectionLevel.Q, 6);
    // Version 1-H: d=17, p=1 -> max 8 errors
    assertErrorCorrectionLimit(1, ErrorCorrectionLevel.H, 8);
    // Version 2-L: d=10, p=2 -> max 4 errors (not 5)
    assertErrorCorrectionLimit(2, ErrorCorrectionLevel.L, 4);
    // Version 3-L: d=15, p=1 -> max 7 errors
    assertErrorCorrectionLimit(3, ErrorCorrectionLevel.L, 7);
  }

  @Test(expected = FormatException.class)
  public void testDimensionExceedingVersion40Rejected() throws Exception {
    new Decoder().decode(new BitMatrix(181, 181));
  }

  private static void assertErrorCorrectionLimit(int versionNumber,
                                                 ErrorCorrectionLevel ecLevel,
                                                 int maxErrors) throws Exception {
    String message = "HELLO";
    Map<EncodeHintType, Object> hints = Collections.singletonMap(EncodeHintType.QR_VERSION, versionNumber);
    QRCode qrCode = Encoder.encode(message, ecLevel, hints);
    assertEquals(versionNumber, qrCode.getVersion().getVersionNumber());

    Decoder decoder = new Decoder();

    // Corrupting maxErrors distinct codewords must decode cleanly.
    BitMatrix atLimit = toBitMatrix(qrCode.getMatrix());
    corruptDistinctCodewords(atLimit, qrCode.getVersion(), maxErrors);
    DecoderResult result = decoder.decode(atLimit);
    assertEquals(message, result.getText());
    assertEquals(Integer.valueOf(maxErrors), result.getErrorsCorrected());

    // Corrupting maxErrors + 1 distinct codewords must be rejected per ISO/IEC 18004 Table 9.
    BitMatrix overLimit = toBitMatrix(qrCode.getMatrix());
    corruptDistinctCodewords(overLimit, qrCode.getVersion(), maxErrors + 1);
    try {
      decoder.decode(overLimit);
      fail("Expected ChecksumException for V" + versionNumber + '-' + ecLevel +
          " with " + (maxErrors + 1) + " codeword errors");
    } catch (ChecksumException expected) {
      // expected
    }
  }

  private static BitMatrix toBitMatrix(ByteMatrix byteMatrix) {
    int width = byteMatrix.getWidth();
    int height = byteMatrix.getHeight();
    BitMatrix bitMatrix = new BitMatrix(width, height);
    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        if (byteMatrix.get(x, y) != 0) {
          bitMatrix.set(x, y);
        }
      }
    }
    return bitMatrix;
  }

  /**
   * Flips the first bit of the first {@code count} codewords in the QR code bit matrix.
   */
  private static void corruptDistinctCodewords(BitMatrix bitMatrix, Version version, int count) {
    int dimension = bitMatrix.getHeight();
    BitMatrix functionPattern = version.buildFunctionPattern();
    boolean readingUp = true;
    int bitsRead = 0;
    int corrupted = 0;
    for (int j = dimension - 1; j > 0 && corrupted < count; j -= 2) {
      if (j == 6) {
        j--;
      }
      for (int step = 0; step < dimension && corrupted < count; step++) {
        int i = readingUp ? dimension - 1 - step : step;
        for (int col = 0; col < 2 && corrupted < count; col++) {
          if (!functionPattern.get(j - col, i)) {
            if (bitsRead == 0) {
              bitMatrix.flip(j - col, i);
              corrupted++;
            }
            bitsRead = (bitsRead + 1) % 8;
          }
        }
      }
      readingUp ^= true;
    }
    assertEquals(count, corrupted);
  }

}
