/**
 * Copyright 2008 ZXing authors
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

package com.google.zxing.qrcode.encoder;

import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.google.zxing.qrcode.decoder.Mode;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author satorux@google.com (Satoru Takabayashi) - creator
 * @author mysen@google.com (Chris Mysen) - ported from C++
 */
public final class QRCodeTestCase extends Assert {

  @Test
  public void test() {
    QRCode qrCode = new QRCode();
    // Initially the QR Code should be invalid.
    assertFalse(qrCode.isValid());

    // First, test simple setters and getters.
    // We use numbers of version 7-H.
    qrCode.setMode(Mode.BYTE);
    qrCode.setECLevel(ErrorCorrectionLevel.H);
    qrCode.setVersion(7);
    qrCode.setMatrixWidth(45);
    qrCode.setMaskPattern(3);
    qrCode.setNumTotalBytes(196);
    qrCode.setNumDataBytes(66);
    qrCode.setNumECBytes(130);
    qrCode.setNumRSBlocks(5);

    assertSame(Mode.BYTE, qrCode.getMode());
    assertSame(ErrorCorrectionLevel.H, qrCode.getECLevel());
    assertEquals(7, qrCode.getVersion());
    assertEquals(45, qrCode.getMatrixWidth());
    assertEquals(3, qrCode.getMaskPattern());
    assertEquals(196, qrCode.getNumTotalBytes());
    assertEquals(66, qrCode.getNumDataBytes());
    assertEquals(130, qrCode.getNumECBytes());
    assertEquals(5, qrCode.getNumRSBlocks());

    // It still should be invalid.
    assertFalse(qrCode.isValid());

    // Prepare the matrix.
    ByteMatrix matrix = new ByteMatrix(45, 45);
    // Just set bogus zero/one values.
    for (int y = 0; y < 45; ++y) {
      for (int x = 0; x < 45; ++x) {
        matrix.set(x, y, (y + x) % 2);
      }
    }

    // Set the matrix.
    qrCode.setMatrix(matrix);
    assertSame(matrix, qrCode.getMatrix());

    // Finally, it should be valid.
    assertTrue(qrCode.isValid());

    // Make sure "at()" returns the same value.
    for (int y = 0; y < 45; ++y) {
      for (int x = 0; x < 45; ++x) {
        assertEquals((y + x) % 2, qrCode.at(x, y));
      }
    }
  }

  @Test
  public void testToString() {
    {
      QRCode qrCode = new QRCode();
      String expected =
        "<<\n" +
        " mode: null\n" +
        " ecLevel: null\n" +
        " version: -1\n" +
        " matrixWidth: -1\n" +
        " maskPattern: -1\n" +
        " numTotalBytes: -1\n" +
        " numDataBytes: -1\n" +
        " numECBytes: -1\n" +
        " numRSBlocks: -1\n" +
        " matrix: null\n" +
        ">>\n";
      assertEquals(expected, qrCode.toString());
    }
    {
      String expected =
        "<<\n" +
        " mode: BYTE\n" +
        " ecLevel: H\n" +
        " version: 1\n" +
        " matrixWidth: 21\n" +
        " maskPattern: 3\n" +
        " numTotalBytes: 26\n" +
        " numDataBytes: 9\n" +
        " numECBytes: 17\n" +
        " numRSBlocks: 1\n" +
        " matrix:\n" +
        " 0 1 0 1 0 1 0 1 0 1 0 1 0 1 0 1 0 1 0 1 0\n" +
        " 1 0 1 0 1 0 1 0 1 0 1 0 1 0 1 0 1 0 1 0 1\n" +
        " 0 1 0 1 0 1 0 1 0 1 0 1 0 1 0 1 0 1 0 1 0\n" +
        " 1 0 1 0 1 0 1 0 1 0 1 0 1 0 1 0 1 0 1 0 1\n" +
        " 0 1 0 1 0 1 0 1 0 1 0 1 0 1 0 1 0 1 0 1 0\n" +
        " 1 0 1 0 1 0 1 0 1 0 1 0 1 0 1 0 1 0 1 0 1\n" +
        " 0 1 0 1 0 1 0 1 0 1 0 1 0 1 0 1 0 1 0 1 0\n" +
        " 1 0 1 0 1 0 1 0 1 0 1 0 1 0 1 0 1 0 1 0 1\n" +
        " 0 1 0 1 0 1 0 1 0 1 0 1 0 1 0 1 0 1 0 1 0\n" +
        " 1 0 1 0 1 0 1 0 1 0 1 0 1 0 1 0 1 0 1 0 1\n" +
        " 0 1 0 1 0 1 0 1 0 1 0 1 0 1 0 1 0 1 0 1 0\n" +
        " 1 0 1 0 1 0 1 0 1 0 1 0 1 0 1 0 1 0 1 0 1\n" +
        " 0 1 0 1 0 1 0 1 0 1 0 1 0 1 0 1 0 1 0 1 0\n" +
        " 1 0 1 0 1 0 1 0 1 0 1 0 1 0 1 0 1 0 1 0 1\n" +
        " 0 1 0 1 0 1 0 1 0 1 0 1 0 1 0 1 0 1 0 1 0\n" +
        " 1 0 1 0 1 0 1 0 1 0 1 0 1 0 1 0 1 0 1 0 1\n" +
        " 0 1 0 1 0 1 0 1 0 1 0 1 0 1 0 1 0 1 0 1 0\n" +
        " 1 0 1 0 1 0 1 0 1 0 1 0 1 0 1 0 1 0 1 0 1\n" +
        " 0 1 0 1 0 1 0 1 0 1 0 1 0 1 0 1 0 1 0 1 0\n" +
        " 1 0 1 0 1 0 1 0 1 0 1 0 1 0 1 0 1 0 1 0 1\n" +
        " 0 1 0 1 0 1 0 1 0 1 0 1 0 1 0 1 0 1 0 1 0\n" +
        ">>\n";
      QRCode qrCode = new QRCode();
      qrCode.setMode(Mode.BYTE);
      qrCode.setECLevel(ErrorCorrectionLevel.H);
      qrCode.setVersion(1);
      qrCode.setMatrixWidth(21);
      qrCode.setMaskPattern(3);
      qrCode.setNumTotalBytes(26);
      qrCode.setNumDataBytes(9);
      qrCode.setNumECBytes(17);
      qrCode.setNumRSBlocks(1);
      ByteMatrix matrix = new ByteMatrix(21, 21);
      for (int y = 0; y < 21; ++y) {
        for (int x = 0; x < 21; ++x) {
          matrix.set(x, y, (y + x) % 2);
        }
      }
      qrCode.setMatrix(matrix);
      assertTrue(qrCode.isValid());
      assertEquals(expected, qrCode.toString());
    }
  }

  @Test
  public void testIsValidMaskPattern() {
    assertFalse(QRCode.isValidMaskPattern(-1));
    assertTrue(QRCode.isValidMaskPattern(0));
    assertTrue(QRCode.isValidMaskPattern(7));
    assertFalse(QRCode.isValidMaskPattern(8));
  }

}
