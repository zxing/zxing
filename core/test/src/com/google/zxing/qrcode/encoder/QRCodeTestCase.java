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

import com.google.zxing.common.ByteMatrix;
import junit.framework.TestCase;

/**
 * @author satorux@google.com (Satoru Takabayashi) - creator
 * @author mysen@google.com (Chris Mysen) - ported from C++
 */
public final class QRCodeTestCase extends TestCase {
  public void test() {
    QRCode qr_code = new QRCode();
    // Initially the QR Code should be invalid.
    assertFalse(qr_code.IsValid());

    // First, test simple setters and getters.
    // We use numbers of version 7-H.
    qr_code.set_mode(QRCode.MODE_8BIT_BYTE);
    qr_code.set_ec_level(QRCode.EC_LEVEL_H);
    qr_code.set_version(7);
    qr_code.set_matrix_width(45);
    qr_code.set_mask_pattern(3);
    qr_code.set_num_total_bytes(196);
    qr_code.set_num_data_bytes(66);
    qr_code.set_num_ec_bytes(130);
    qr_code.set_num_rs_blocks(5);

    assertEquals(QRCode.MODE_8BIT_BYTE, qr_code.mode());
    assertEquals(QRCode.EC_LEVEL_H, qr_code.ec_level());
    assertEquals(7, qr_code.version());
    assertEquals(45, qr_code.matrix_width());
    assertEquals(3, qr_code.mask_pattern());
    assertEquals(196, qr_code.num_total_bytes());
    assertEquals(66, qr_code.num_data_bytes());
    assertEquals(130, qr_code.num_ec_bytes());
    assertEquals(5, qr_code.num_rs_blocks());

    // It still should be invalid.
    assertFalse(qr_code.IsValid());

    // Prepare the matrix.
    ByteMatrix matrix = new ByteMatrix(45, 45);
    // Just set bogus zero/one values.
    for (int y = 0; y < 45; ++y) {
      for (int x = 0; x < 45; ++x) {
        matrix.set(y, x, (y + x) % 2);
      }
    }

    // Set the matrix.
    qr_code.set_matrix(matrix);
    assertEquals(matrix, qr_code.matrix());

    // Finally, it should be valid.
    assertTrue(qr_code.IsValid());

    // Make sure "at()" returns the same value.
    for (int y = 0; y < 45; ++y) {
      for (int x = 0; x < 45; ++x) {
        assertEquals((y + x) % 2, qr_code.at(x, y));
      }
    }
  }

  public void testToString() {
    {
      QRCode qr_code = new QRCode();
      String expected =
	"<<\n" +
	" mode: UNDEFINED\n" +
	" ec_level: UNDEFINED\n" +
	" version: -1\n" +
	" matrix_width: -1\n" +
	" mask_pattern: -1\n" +
	" num_total_bytes_: -1\n" +
	" num_data_bytes: -1\n" +
	" num_ec_bytes: -1\n" +
	" num_rs_blocks: -1\n" +
	" matrix: null\n" +
	">>\n";
      assertEquals(expected, qr_code.toString());
    }
    {
      String expected =
	"<<\n" +
	" mode: 8BIT_BYTE\n" +
	" ec_level: H\n" +
	" version: 1\n" +
	" matrix_width: 21\n" +
	" mask_pattern: 3\n" +
	" num_total_bytes_: 26\n" +
	" num_data_bytes: 9\n" +
	" num_ec_bytes: 17\n" +
	" num_rs_blocks: 1\n" +
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
      QRCode qr_code = new QRCode();
      qr_code.set_mode(QRCode.MODE_8BIT_BYTE);
      qr_code.set_ec_level(QRCode.EC_LEVEL_H);
      qr_code.set_version(1);
      qr_code.set_matrix_width(21);
      qr_code.set_mask_pattern(3);
      qr_code.set_num_total_bytes(26);
      qr_code.set_num_data_bytes(9);
      qr_code.set_num_ec_bytes(17);
      qr_code.set_num_rs_blocks(1);
      ByteMatrix matrix = new ByteMatrix(21, 21);
      for (int y = 0; y < 21; ++y) {
        for (int x = 0; x < 21; ++x) {
          matrix.set(y, x, (y + x) % 2);
        }
      }
      qr_code.set_matrix(matrix);
      assertTrue(qr_code.IsValid());
      assertEquals(expected, qr_code.toString());
    }
  }

  public void testIsValidVersion() {
    assertFalse(QRCode.IsValidVersion(0));
    assertTrue(QRCode.IsValidVersion(1));
    assertTrue(QRCode.IsValidVersion(40));
    assertFalse(QRCode.IsValidVersion(0));
  }

  public void testIsValidECLevel() {
    assertFalse(QRCode.IsValidECLevel(QRCode.EC_LEVEL_UNDEFINED));
    assertTrue(QRCode.IsValidECLevel(QRCode.EC_LEVEL_L));
    assertTrue(QRCode.IsValidECLevel(QRCode.EC_LEVEL_Q));
    assertTrue(QRCode.IsValidECLevel(QRCode.EC_LEVEL_M));
    assertTrue(QRCode.IsValidECLevel(QRCode.EC_LEVEL_H));
    assertFalse(QRCode.IsValidECLevel(QRCode.NUM_EC_LEVELS));
  }

  public void testIsValidMode() {
    assertFalse(QRCode.IsValidMode(QRCode.MODE_UNDEFINED));
    assertTrue(QRCode.IsValidMode(QRCode.MODE_NUMERIC));
    assertTrue(QRCode.IsValidMode(QRCode.MODE_ALPHANUMERIC));
    assertTrue(QRCode.IsValidMode(QRCode.MODE_8BIT_BYTE));
    assertFalse(QRCode.IsValidMode(QRCode.NUM_MODES));
  }

  public void testIsValidMatrixWidth() {
    assertFalse(QRCode.IsValidMatrixWidth(20));
    assertTrue(QRCode.IsValidMatrixWidth(21));
    assertTrue(QRCode.IsValidMatrixWidth(177));
    assertFalse(QRCode.IsValidMatrixWidth(178));
  }

  public void testIsValidMaskPattern() {
    assertFalse(QRCode.IsValidMaskPattern(-1));
    assertTrue(QRCode.IsValidMaskPattern(0));
    assertTrue(QRCode.IsValidMaskPattern(7));
    assertFalse(QRCode.IsValidMaskPattern(8));
  }

  public void testModeToString() {
    assertEquals("UNDEFINED", QRCode.ModeToString(QRCode.MODE_UNDEFINED));
    assertEquals("NUMERIC", QRCode.ModeToString(QRCode.MODE_NUMERIC));
    assertEquals("ALPHANUMERIC", QRCode.ModeToString(QRCode.MODE_ALPHANUMERIC));
    assertEquals("8BIT_BYTE", QRCode.ModeToString(QRCode.MODE_8BIT_BYTE));
    assertEquals("UNKNOWN", QRCode.ModeToString(QRCode.NUM_MODES));
  }

  public void testECLevelToString() {
    assertEquals("UNDEFINED", QRCode.ECLevelToString(QRCode.EC_LEVEL_UNDEFINED));
    assertEquals("L", QRCode.ECLevelToString(QRCode.EC_LEVEL_L));
    assertEquals("M", QRCode.ECLevelToString(QRCode.EC_LEVEL_M));
    assertEquals("Q", QRCode.ECLevelToString(QRCode.EC_LEVEL_Q));
    assertEquals("H", QRCode.ECLevelToString(QRCode.EC_LEVEL_H));
    assertEquals("UNKNOWN", QRCode.ECLevelToString(QRCode.NUM_EC_LEVELS));
  }

  public void testGetModeCode() {
    assertEquals(1, QRCode.GetModeCode(QRCode.MODE_NUMERIC));
    assertEquals(2, QRCode.GetModeCode(QRCode.MODE_ALPHANUMERIC));
    assertEquals(4, QRCode.GetModeCode(QRCode.MODE_8BIT_BYTE));
    assertEquals(8, QRCode.GetModeCode(QRCode.MODE_KANJI));
    assertEquals(-1, QRCode.GetModeCode(QRCode.MODE_UNDEFINED));
  }

  public void testGetECLevelCode() {
    assertEquals(1, QRCode.GetECLevelCode(QRCode.EC_LEVEL_L));
    assertEquals(0, QRCode.GetECLevelCode(QRCode.EC_LEVEL_M));
    assertEquals(3, QRCode.GetECLevelCode(QRCode.EC_LEVEL_Q));
    assertEquals(2, QRCode.GetECLevelCode(QRCode.EC_LEVEL_H));
    assertEquals(-1, QRCode.GetECLevelCode(QRCode.EC_LEVEL_UNDEFINED));
  }
}
