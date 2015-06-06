/*
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
import com.google.zxing.qrcode.decoder.Version;
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

    // First, test simple setters and getters.
    // We use numbers of version 7-H.
    qrCode.setMode(Mode.BYTE);
    qrCode.setECLevel(ErrorCorrectionLevel.H);
    qrCode.setVersion(Version.getVersionForNumber(7));
    qrCode.setMaskPattern(3);

    assertSame(Mode.BYTE, qrCode.getMode());
    assertSame(ErrorCorrectionLevel.H, qrCode.getECLevel());
    assertEquals(7, qrCode.getVersion().getVersionNumber());
    assertEquals(3, qrCode.getMaskPattern());

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
  }

  @Test
  public void testToString1() {
    QRCode qrCode = new QRCode();
    String expected =
      "<<\n" +
      " mode: null\n" +
      " ecLevel: null\n" +
      " version: null\n" +
      " maskPattern: -1\n" +
      " matrix: null\n" +
      ">>\n";
    assertEquals(expected, qrCode.toString());
  }

  @Test
  public void testToString2() {
    QRCode qrCode = new QRCode();
    qrCode.setMode(Mode.BYTE);
    qrCode.setECLevel(ErrorCorrectionLevel.H);
    qrCode.setVersion(Version.getVersionForNumber(1));
    qrCode.setMaskPattern(3);
    ByteMatrix matrix = new ByteMatrix(21, 21);
    for (int y = 0; y < 21; ++y) {
      for (int x = 0; x < 21; ++x) {
        matrix.set(x, y, (y + x) % 2);
      }
    }
    qrCode.setMatrix(matrix);
    String expected = "<<\n" +
        " mode: BYTE\n" +
        " ecLevel: H\n" +
        " version: 1\n" +
        " maskPattern: 3\n" +
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
    assertEquals(expected, qrCode.toString());
  }

  @Test
  public void testIsValidMaskPattern() {
    assertFalse(QRCode.isValidMaskPattern(-1));
    assertTrue(QRCode.isValidMaskPattern(0));
    assertTrue(QRCode.isValidMaskPattern(7));
    assertFalse(QRCode.isValidMaskPattern(8));
  }

}
