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

import com.google.zxing.common.ByteArray;
import com.google.zxing.WriterException;
import junit.framework.TestCase;

/**
 * @author satorux@google.com (Satoru Takabayashi) - creator
 * @author mysen@google.com (Chris Mysen) - ported from C++
 */
public final class EncoderTestCase extends TestCase {

  public void testGetAlphanumericCode() throws WriterException {
    // The first ten code points are numbers.
    for (int i = 0; i < 10; ++i) {
      assertEquals(i, Encoder.getAlphanumericCode('0' + i));
    }

    // The next 26 code points are capital alphabet letters.
    for (int i = 10; i < 36; ++i) {
      assertEquals(i, Encoder.getAlphanumericCode('A' + i - 10));
    }

    // Others are symbol letters
    assertEquals(36, Encoder.getAlphanumericCode(' '));
    assertEquals(37, Encoder.getAlphanumericCode('$'));
    assertEquals(38, Encoder.getAlphanumericCode('%'));
    assertEquals(39, Encoder.getAlphanumericCode('*'));
    assertEquals(40, Encoder.getAlphanumericCode('+'));
    assertEquals(41, Encoder.getAlphanumericCode('-'));
    assertEquals(42, Encoder.getAlphanumericCode('.'));
    assertEquals(43, Encoder.getAlphanumericCode('/'));
    assertEquals(44, Encoder.getAlphanumericCode(':'));

    // Should return -1 for other letters;
    assertEquals(-1, Encoder.getAlphanumericCode('a'));
    assertEquals(-1, Encoder.getAlphanumericCode('#'));
    assertEquals(-1, Encoder.getAlphanumericCode('\0'));
  }

  public void testChooseMode() throws WriterException {
    // Numeric mode.
    assertEquals(QRCode.MODE_NUMERIC, Encoder.chooseMode(new ByteArray("0")));
    assertEquals(QRCode.MODE_NUMERIC, Encoder.chooseMode(new ByteArray("0123456789")));
    // Alphanumeric mode.
    assertEquals(QRCode.MODE_ALPHANUMERIC, Encoder.chooseMode(new ByteArray("A")));
    assertEquals(QRCode.MODE_ALPHANUMERIC,
        Encoder.chooseMode(new ByteArray("0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ $%*+-./:")));
    // 8-bit byte mode.
    assertEquals(QRCode.MODE_8BIT_BYTE, Encoder.chooseMode(new ByteArray("a")));
    assertEquals(QRCode.MODE_8BIT_BYTE, Encoder.chooseMode(new ByteArray("#")));
    assertEquals(QRCode.MODE_8BIT_BYTE, Encoder.chooseMode(new ByteArray("")));
    // Kanji mode.  We used to use MODE_KANJI for these, but we stopped
    // doing that as we cannot distinguish Shift_JIS from other encodings
    // from data bytes alone.  See also comments in qrcode_encoder.h.

    // AIUE in Hiragana in Shift_JIS
    byte[] dat1 = {0x8,0xa,0x8,0xa,0x8,0xa,0x8,(byte)0xa6};
    assertEquals(QRCode.MODE_8BIT_BYTE, Encoder.chooseMode(new ByteArray(dat1)));

    // Nihon in Kanji in Shift_JIS.
    byte[] dat2 = {0x9,0xf,0x9,0x7b};
    assertEquals(QRCode.MODE_8BIT_BYTE, Encoder.chooseMode(new ByteArray(dat2)));

    // Sou-Utsu-Byou in Kanji in Shift_JIS.
    byte[] dat3 = {0xe,0x4,0x9,0x5,0x9,0x61};
    assertEquals(QRCode.MODE_8BIT_BYTE, Encoder.chooseMode(new ByteArray(dat3)));
  }

  public void testEncode() throws WriterException {
    QRCode qrCode = new QRCode();
    Encoder.encode(new ByteArray("ABCDEF"), QRCode.EC_LEVEL_H, qrCode);
    // The following is a valid QR Code that can be read by cell phones.
    String expected =
      "<<\n" +
      " mode: ALPHANUMERIC\n" +
      " ecLevel: H\n" +
      " version: 1\n" +
      " matrixWidth: 21\n" +
      " maskPattern: 0\n" +
      " numTotalBytes: 26\n" +
      " numDataBytes: 9\n" +
      " numECBytes: 17\n" +
      " numRSBlocks: 1\n" +
      " matrix:\n" +
      " 1 1 1 1 1 1 1 0 1 1 1 1 0 0 1 1 1 1 1 1 1\n" +
      " 1 0 0 0 0 0 1 0 0 1 1 1 0 0 1 0 0 0 0 0 1\n" +
      " 1 0 1 1 1 0 1 0 0 1 0 1 1 0 1 0 1 1 1 0 1\n" +
      " 1 0 1 1 1 0 1 0 1 1 1 0 1 0 1 0 1 1 1 0 1\n" +
      " 1 0 1 1 1 0 1 0 0 1 1 1 0 0 1 0 1 1 1 0 1\n" +
      " 1 0 0 0 0 0 1 0 0 1 0 0 0 0 1 0 0 0 0 0 1\n" +
      " 1 1 1 1 1 1 1 0 1 0 1 0 1 0 1 1 1 1 1 1 1\n" +
      " 0 0 0 0 0 0 0 0 0 0 1 0 1 0 0 0 0 0 0 0 0\n" +
      " 0 0 1 0 1 1 1 0 1 1 0 0 1 1 0 0 0 1 0 0 1\n" +
      " 1 0 1 1 1 0 0 1 0 0 0 1 0 1 0 0 0 0 0 0 0\n" +
      " 0 0 1 1 0 0 1 0 1 0 0 0 1 0 1 0 1 0 1 1 0\n" +
      " 1 1 0 1 0 1 0 1 1 1 0 1 0 1 0 0 0 0 0 1 0\n" +
      " 0 0 1 1 0 1 1 1 1 0 0 0 1 0 1 0 1 1 1 1 0\n" +
      " 0 0 0 0 0 0 0 0 1 0 0 1 1 1 0 1 0 1 0 0 0\n" +
      " 1 1 1 1 1 1 1 0 0 0 1 0 1 0 1 1 0 0 0 0 1\n" +
      " 1 0 0 0 0 0 1 0 1 1 1 1 0 1 0 1 1 1 1 0 1\n" +
      " 1 0 1 1 1 0 1 0 1 0 1 1 0 1 0 1 0 0 0 0 1\n" +
      " 1 0 1 1 1 0 1 0 0 1 1 0 1 1 1 1 0 1 0 1 0\n" +
      " 1 0 1 1 1 0 1 0 1 0 0 0 1 0 1 0 1 1 1 0 1\n" +
      " 1 0 0 0 0 0 1 0 0 1 1 0 1 1 0 1 0 0 0 1 1\n" +
      " 1 1 1 1 1 1 1 0 0 0 0 0 0 0 0 0 1 0 1 0 1\n" +
      ">>\n";
    assertEquals(expected, qrCode.toString());
  }

  public void testAppendModeInfo() throws WriterException {
    BitVector bits = new BitVector();
    Encoder.appendModeInfo(QRCode.MODE_NUMERIC, bits);
    assertEquals("0001", bits.toString());
  }

  public void testAppendLengthInfo() throws WriterException {
    {
      BitVector bits = new BitVector();
      Encoder.appendLengthInfo(1,  // 1 letter (1/1).
						  1,  // version 1.
						  QRCode.MODE_NUMERIC,
						  bits);
      assertEquals("0000000001", bits.toString());  // 10 bits.
    }
    {
      BitVector bits = new BitVector();
      Encoder.appendLengthInfo(2,  // 2 letters (2/1).
						  10,  // version 10.
						  QRCode.MODE_ALPHANUMERIC,
						  bits);
      assertEquals("00000000010", bits.toString());  // 11 bits.
    }
    {
      BitVector bits = new BitVector();
      Encoder.appendLengthInfo(255,  // 255 letter (255/1).
						  27,  // version 27.
						  QRCode.MODE_8BIT_BYTE,
						  bits);
      assertEquals("0000000011111111", bits.toString());  // 16 bits.
    }
    {
      BitVector bits = new BitVector();
      Encoder.appendLengthInfo(1024,  // 512 letters (1024/2).
						  40,  // version 40.
						  QRCode.MODE_KANJI,
						  bits);
      assertEquals("001000000000", bits.toString());  // 12 bits.
    }
  }

  public void testAppendBytes() throws WriterException {
    {
      // Should use appendNumericBytes.
      // 1 = 01 = 0001 in 4 bits.
      BitVector bits = new BitVector();
      Encoder.appendBytes(new ByteArray("1"), QRCode.MODE_NUMERIC, bits);
      assertEquals("0001" , bits.toString());
      // 'A' cannot be encoded in MODE_NUMERIC.
      try {
        Encoder.appendBytes(new ByteArray("A"), QRCode.MODE_NUMERIC, bits);
        fail("Should have thrown exception");
      } catch (WriterException we) {
        // good
      }
    }
    {
      // Should use appendAlphanumericBytes.
      // A = 10 = 0xa = 001010 in 6 bits
      BitVector bits = new BitVector();
      Encoder.appendBytes(new ByteArray("A"), QRCode.MODE_ALPHANUMERIC, bits);
      assertEquals("001010" , bits.toString());
      // Lower letters such as 'a' cannot be encoded in MODE_ALPHANUMERIC.
      try {
        Encoder.appendBytes(new ByteArray("a"), QRCode.MODE_ALPHANUMERIC, bits);
      } catch (WriterException we) {
        // good
      }
    }
    {
      // Should use append8BitBytes.
      // 0x61, 0x62, 0x63
      BitVector bits = new BitVector();
      Encoder.appendBytes(new ByteArray("abc"), QRCode.MODE_8BIT_BYTE, bits);
      assertEquals("01100001" + "01100010" + "01100011", bits.toString());
      // Anything can be encoded in QRCode.MODE_8BIT_BYTE.
      byte[] bytes = {0x00};
      Encoder.appendBytes(new ByteArray(bytes), QRCode.MODE_8BIT_BYTE, bits);
    }
    {
      // Should use appendKanjiBytes.
      // 0x93, 0x5f
      BitVector bits = new BitVector();
      byte[] bytes = {(byte)0x93,0x5f};
      Encoder.appendBytes(new ByteArray(bytes), QRCode.MODE_KANJI, bits);
      assertEquals("0110110011111", bits.toString());
      // ASCII characters can not be encoded in QRCode.MODE_KANJI.

      try {
        Encoder.appendBytes(new ByteArray("a"), QRCode.MODE_KANJI, bits);
      } catch (WriterException we) {
        // good
      }
    }
  }

  public void testInit() {
    // TODO: should be implemented.
  }

  public void testTerminateBits() throws WriterException {
    {
      BitVector v = new BitVector();
      Encoder.terminateBits(0, v);
      assertEquals("", v.toString());
    }
    {
      BitVector v = new BitVector();
      Encoder.terminateBits(1, v);
      assertEquals("00000000", v.toString());
    }
    {
      BitVector v = new BitVector();
      v.appendBits(0, 3);  // Append 000
      Encoder.terminateBits(1, v);
      assertEquals("00000000", v.toString());
    }
    {
      BitVector v = new BitVector();
      v.appendBits(0, 5);  // Append 00000
      Encoder.terminateBits(1, v);
      assertEquals("00000000", v.toString());
    }
    {
      BitVector v = new BitVector();
      v.appendBits(0, 8);  // Append 00000000
      Encoder.terminateBits(1, v);
      assertEquals("00000000", v.toString());
    }
    {
      BitVector v = new BitVector();
      Encoder.terminateBits(2, v);
      assertEquals("0000000011101100", v.toString());
    }
    {
      BitVector v = new BitVector();
      v.appendBits(0, 1);  // Append 0
      Encoder.terminateBits(3, v);
      assertEquals("000000001110110000010001", v.toString());
    }
  }

  public void testGetNumDataBytesAndNumECBytesForBlockID() throws WriterException {
    int[] numDataBytes = new int[1];
    int[] numEcBytes = new int[1];
    // Version 1-H.
    Encoder.getNumDataBytesAndNumECBytesForBlockID(26, 9, 1, 0, numDataBytes, numEcBytes);
    assertEquals(9, numDataBytes[0]);
    assertEquals(17, numEcBytes[0]);

    // Version 3-H.  2 blocks.
    Encoder.getNumDataBytesAndNumECBytesForBlockID(70, 26, 2, 0, numDataBytes, numEcBytes);
    assertEquals(13, numDataBytes[0]);
    assertEquals(22, numEcBytes[0]);
    Encoder.getNumDataBytesAndNumECBytesForBlockID(70, 26, 2, 1, numDataBytes, numEcBytes);
    assertEquals(13, numDataBytes[0]);
    assertEquals(22, numEcBytes[0]);

    // Version 7-H. (4 + 1) blocks.
    Encoder.getNumDataBytesAndNumECBytesForBlockID(196, 66, 5, 0, numDataBytes, numEcBytes);
    assertEquals(13, numDataBytes[0]);
    assertEquals(26, numEcBytes[0]);
    Encoder.getNumDataBytesAndNumECBytesForBlockID(196, 66, 5, 4, numDataBytes, numEcBytes);
    assertEquals(14, numDataBytes[0]);
    assertEquals(26, numEcBytes[0]);

    // Version 40-H. (20 + 61) blocks.
    Encoder.getNumDataBytesAndNumECBytesForBlockID(3706, 1276, 81, 0, numDataBytes, numEcBytes);
    assertEquals(15, numDataBytes[0]);
    assertEquals(30, numEcBytes[0]);
    Encoder.getNumDataBytesAndNumECBytesForBlockID(3706, 1276, 81, 20, numDataBytes, numEcBytes);
    assertEquals(16, numDataBytes[0]);
    assertEquals(30, numEcBytes[0]);
    Encoder.getNumDataBytesAndNumECBytesForBlockID(3706, 1276, 81, 80, numDataBytes, numEcBytes);
    assertEquals(16, numDataBytes[0]);
    assertEquals(30, numEcBytes[0]);
  }

  public void testInterleaveWithECBytes() throws WriterException {
    {
      final byte[] dataBytes = {32, 65, (byte)205, 69, 41, (byte)220, 46, (byte)128, (byte)236};
      BitVector in = new BitVector();
      for (byte dataByte: dataBytes) {
        in.appendBits(dataByte, 8);
      }
      BitVector out = new BitVector();
      Encoder.interleaveWithECBytes(in, 26, 9, 1, out);
      final byte[] expected = {
          // Data bytes.
          32, 65, (byte)205, 69, 41, (byte)220, 46, (byte)128, (byte)236,
          // Error correction bytes.
          42, (byte)159, 74, (byte)221, (byte)244, (byte)169, (byte)239, (byte)150, (byte)138, 70,
          (byte)237, 85, (byte)224, 96, 74, (byte)219, 61,
      };
      assertEquals(expected.length, out.sizeInBytes());
      final byte[] outArray = out.getArray();
      // Can't use Arrays.equals(), because outArray may be longer than out.sizeInBytes()
      for (int x = 0; x < expected.length; x++) {
        assertEquals(expected[x], outArray[x]);
      }
    }
    // Numbers are from http://www.swetake.com/qr/qr8.html
    {
      final byte[] dataBytes = {
          67, 70, 22, 38, 54, 70, 86, 102, 118, (byte)134, (byte)150, (byte)166, (byte)182,
          (byte)198, (byte)214, (byte)230, (byte)247, 7, 23, 39, 55, 71, 87, 103, 119, (byte)135,
          (byte)151, (byte)166, 22, 38, 54, 70, 86, 102, 118, (byte)134, (byte)150, (byte)166,
          (byte)182, (byte)198, (byte)214, (byte)230, (byte)247, 7, 23, 39, 55, 71, 87, 103, 119,
          (byte)135, (byte)151, (byte)160, (byte)236, 17, (byte)236, 17, (byte)236, 17, (byte)236,
          17
      };
      BitVector in = new BitVector();
      for (byte dataByte: dataBytes) {
        in.appendBits(dataByte, 8);
      }
      BitVector out = new BitVector();
      Encoder.interleaveWithECBytes(in, 134, 62, 4, out);
      final byte[] expected = {
          // Data bytes.
          67, (byte)230, 54, 55, 70, (byte)247, 70, 71, 22, 7, 86, 87, 38, 23, 102, 103, 54, 39,
          118, 119, 70, 55, (byte)134, (byte)135, 86, 71, (byte)150, (byte)151, 102, 87, (byte)166,
          (byte)160, 118, 103, (byte)182, (byte)236, (byte)134, 119, (byte)198, 17, (byte)150,
          (byte)135, (byte)214, (byte)236, (byte)166, (byte)151, (byte)230, 17, (byte)182,
          (byte)166, (byte)247, (byte)236, (byte)198, 22, 7, 17, (byte)214, 38, 23, (byte)236, 39,
          17,
          // Error correction bytes.
          (byte)175, (byte)155, (byte)245, (byte)236, 80, (byte)146, 56, 74, (byte)155, (byte)165,
          (byte)133, (byte)142, 64, (byte)183, (byte)132, 13, (byte)178, 54, (byte)132, 108, 45,
          113, 53, 50, (byte)214, 98, (byte)193, (byte)152, (byte)233, (byte)147, 50, 71, 65,
          (byte)190, 82, 51, (byte)209, (byte)199, (byte)171, 54, 12, 112, 57, 113, (byte)155, 117,
          (byte)211, (byte)164, 117, 30, (byte)158, (byte)225, 31, (byte)190, (byte)242, 38,
          (byte)140, 61, (byte)179, (byte)154, (byte)214, (byte)138, (byte)147, 87, 27, 96, 77, 47,
          (byte)187, 49, (byte)156, (byte)214,
      };
      assertEquals(expected.length, out.sizeInBytes());
      final byte[] outArray = out.getArray();
      for (int x = 0; x < expected.length; x++) {
        assertEquals(expected[x], outArray[x]);
      }
    }
  }

  public void testAppendNumericBytes() throws WriterException {
    {
      // 1 = 01 = 0001 in 4 bits.
      BitVector bits = new BitVector();
      Encoder.appendNumericBytes(new ByteArray("1"), bits);
      assertEquals("0001" , bits.toString());
    }
    {
      // 12 = 0xc = 0001100 in 7 bits.
      BitVector bits = new BitVector();
      Encoder.appendNumericBytes(new ByteArray("12"), bits);
      assertEquals("0001100" , bits.toString());
    }
    {
      // 123 = 0x7b = 0001111011 in 10 bits.
      BitVector bits = new BitVector();
      Encoder.appendNumericBytes(new ByteArray("123"), bits);
      assertEquals("0001111011" , bits.toString());
    }
    {
      // 1234 = "123" + "4" = 0001111011 + 0100
      BitVector bits = new BitVector();
      Encoder.appendNumericBytes(new ByteArray("1234"), bits);
      assertEquals("0001111011" + "0100" , bits.toString());
    }
    {
      // Empty.
      BitVector bits = new BitVector();
      Encoder.appendNumericBytes(new ByteArray(""), bits);
      assertEquals("" , bits.toString());
    }
    {
      // Invalid data.
      BitVector bits = new BitVector();
      try {
        Encoder.appendNumericBytes(new ByteArray("abc"), bits);
      } catch (WriterException we) {
        // good
      }
    }
  }

  public void testAppendAlphanumericBytes() throws WriterException {
    {
      // A = 10 = 0xa = 001010 in 6 bits
      BitVector bits = new BitVector();
      Encoder.appendAlphanumericBytes(new ByteArray("A"), bits);
      assertEquals("001010" , bits.toString());
    }
    {
      // AB = 10 * 45 + 11 = 461 = 0x1cd = 00111001101 in 11 bits
      BitVector bits = new BitVector();
      Encoder.appendAlphanumericBytes(new ByteArray("AB"), bits);
      assertEquals("00111001101", bits.toString());
    }
    {
      // ABC = "AB" + "C" = 00111001101 + 001100
      BitVector bits = new BitVector();
      Encoder.appendAlphanumericBytes(new ByteArray("ABC"), bits);
      assertEquals("00111001101" + "001100" , bits.toString());
    }
    {
      // Empty.
      BitVector bits = new BitVector();
      Encoder.appendAlphanumericBytes(new ByteArray(""), bits);
      assertEquals("" , bits.toString());
    }
    {
      // Invalid data.
      BitVector bits = new BitVector();
      try {
        Encoder.appendAlphanumericBytes(new ByteArray("abc"), bits);
      } catch (WriterException we) {
        // good
      }
    }
  }

  public void testAppend8BitBytes() throws WriterException {
    {
      // 0x61, 0x62, 0x63
      BitVector bits = new BitVector();
      Encoder.append8BitBytes(new ByteArray("abc"), bits);
      assertEquals("01100001" + "01100010" + "01100011", bits.toString());
    }
    {
      // Empty.
      BitVector bits = new BitVector();
      Encoder.append8BitBytes(new ByteArray(""), bits);
      assertEquals("", bits.toString());
    }
  }

  // Numbers are from page 21 of JISX0510:2004
  public void testAppendKanjiBytes() throws WriterException {
    {
      BitVector bits = new BitVector();
      byte[] dat1 = {(byte)0x93,0x5f};
      Encoder.appendKanjiBytes(new ByteArray(dat1), bits);
      assertEquals("0110110011111", bits.toString());
      byte[] dat2 = {(byte)0xe4,(byte)0xaa};
      Encoder.appendKanjiBytes(new ByteArray(dat2), bits);
      assertEquals("0110110011111" + "1101010101010", bits.toString());
    }
  }

  // JAVAPORT: Uncomment and fix up with new Reed Solomon objects
//  static boolean ComparePoly(final int[] expected, final int size, final GF_Poly poly) {
//    if (size != poly.degree() + 1) {
//      return false;
//    }
//    for (int i = 0; i < size; ++i) {
//      // "expected" is ordered in a reverse order.  We reverse the coeff
//      // index for comparison.
//      final int coeff = GaloisField.GetField(8).Log(
//          poly.coeff(size - i - 1));
//      if (expected[i] != coeff) {
//        Debug.LOG_ERROR("values don't match at " + i + ": " +
//            expected[i] + " vs. " + coeff);
//        return false;
//      }
//    }
//    return true;
//  }
//
//  // Numbers are from Appendix A of JISX0510 2004 (p.59).
//  public void testGetECPoly() {
//    {
//      final GF_Poly poly = Encoder.GetECPoly(7);
//      final int[] expected = {0, 87, 229, 146, 149, 238, 102, 21};
//      assertTrue(ComparePoly(expected, expected.length, poly));
//    }
//    {
//      final GF_Poly poly = Encoder.GetECPoly(17);
//      final int[] expected = {
//          0, 43, 139, 206, 78, 43, 239, 123, 206, 214, 147, 24, 99, 150,
//          39, 243, 163, 136
//      };
//      assertTrue(ComparePoly(expected, expected.length, poly));
//    }
//    {
//      final GF_Poly poly = Encoder.GetECPoly(34);
//      final int[] expected = {
//          0, 111, 77, 146, 94, 26, 21, 108, 19,
//          105, 94, 113, 193, 86, 140, 163, 125,
//          58,
//          158, 229, 239, 218, 103, 56, 70, 114,
//          61, 183, 129, 167, 13, 98, 62, 129, 51
//      };
//      assertTrue(ComparePoly(expected, expected.length, poly));
//    }
//    {
//      final GF_Poly poly = Encoder.GetECPoly(68);
//      final int[] expected = {
//          0, 247, 159, 223, 33, 224, 93, 77, 70,
//          90, 160, 32, 254, 43, 150, 84, 101,
//          190,
//          205, 133, 52, 60, 202, 165, 220, 203,
//          151, 93, 84, 15, 84, 253, 173, 160,
//          89, 227, 52, 199, 97, 95, 231, 52,
//          177, 41, 125, 137, 241, 166, 225, 118,
//          2, 54,
//          32, 82, 215, 175, 198, 43, 238, 235,
//          27, 101, 184, 127, 3, 5, 8, 163, 238
//      };
//      assertTrue(ComparePoly(expected, expected.length, poly));
//    }
//  }

  // Numbers are from http://www.swetake.com/qr/qr3.html and
  // http://www.swetake.com/qr/qr9.html
  public void testGenerateECBytes() {
    {
      final byte[] dataBytes = {32, 65, (byte)205, 69, 41, (byte)220, 46, (byte)128, (byte)236};
      ByteArray ecBytes = Encoder.generateECBytes(new ByteArray(dataBytes), 17);
      final int[] expected = {
          42, 159, 74, 221, 244, 169, 239, 150, 138, 70, 237, 85, 224, 96, 74, 219, 61
      };
      assertEquals(expected.length, ecBytes.size());
      for (int x = 0; x < expected.length; x++) {
        assertEquals(expected[x], ecBytes.at(x));
      }
    }
    {
      final byte[] dataBytes = {67, 70, 22, 38, 54, 70, 86, 102, 118,
          (byte)134, (byte)150, (byte)166, (byte)182, (byte)198, (byte)214};
      ByteArray ecBytes = Encoder.generateECBytes(new ByteArray(dataBytes), 18);
      final int[] expected = {
          175, 80, 155, 64, 178, 45, 214, 233, 65, 209, 12, 155, 117, 31, 140, 214, 27, 187
      };
      assertEquals(expected.length, ecBytes.size());
      for (int x = 0; x < expected.length; x++) {
        assertEquals(expected[x], ecBytes.at(x));
      }
    }
    {
      // High-order zero cofficient case.
      final byte[] dataBytes = {32, 49, (byte)205, 69, 42, 20, 0, (byte)236, 17};
      ByteArray ecBytes = Encoder.generateECBytes(new ByteArray(dataBytes), 17);
      final int[] expected = {
          0, 3, 130, 179, 194, 0, 55, 211, 110, 79, 98, 72, 170, 96, 211, 137, 213
      };
      assertEquals(expected.length, ecBytes.size());
      for (int x = 0; x < expected.length; x++) {
        assertEquals(expected[x], ecBytes.at(x));
      }
    }
  }

  public void testIsValidKanji() {
    assertTrue(Encoder.isValidKanji(0x82, 0xa0));  // Hiragana "A".
    assertTrue(Encoder.isValidKanji(0x93, 0xfa));  // Nichi in Kanji.
    assertTrue(Encoder.isValidKanji(0x8a, 0xbf));  // Kan in Kanji.
    assertTrue(Encoder.isValidKanji(0xe7, 0x4e));  // Sou in Kanji.
    assertTrue(Encoder.isValidKanji(0xea, 0xa2));  // Haruka in Kanji.

    assertFalse(Encoder.isValidKanji('0', '1'));
    assertFalse(Encoder.isValidKanji(0x82, 0x7f));
    assertFalse(Encoder.isValidKanji(0xa0, 0xa0));
  }

  public void testIsValidKanjiSequence() {
    // AIUEO in Katakana
    byte[] dat1 = {
        (byte)0x83, 0x41, (byte)0x83, 0x43, (byte)0x83, 0x45, (byte)0x83, 0x47, (byte)0x83, 0x49
    };
    assertTrue(Encoder.isValidKanjiSequence(new ByteArray(dat1)));
    // 012345 in multi-byte letters.
    byte[] dat2 = {
        (byte)0x82, 0x4f, (byte)0x82, 0x50, (byte)0x82, 0x51, (byte)0x82, 0x52, (byte)0x82, 0x53,
        (byte)0x82, 0x54
    };
    assertTrue(Encoder.isValidKanjiSequence(new ByteArray(dat2)));
    // Yoroshiku in Kanji.
    byte[] dat3 = {
        (byte)0x96, (byte)0xe9, (byte)0x98, 0x49, (byte)0x8e, (byte)0x80, (byte)0x8b, (byte)0xea
    };
    assertTrue(Encoder.isValidKanjiSequence(new ByteArray(dat3)));
    assertFalse(Encoder.isValidKanjiSequence(new ByteArray("0123")));
    assertFalse(Encoder.isValidKanjiSequence(new ByteArray("ABC")));
  }

  public void testBugInBitVectorNumBytes() throws WriterException {
    // There was a bug in BitVector.sizeInBytes() that caused it to return a
    // smaller-by-one value (ex. 1465 instead of 1466) if the number of bits
    // in the vector is not 8-bit aligned.  In QRCodeEncoder::InitQRCode(),
    // BitVector::sizeInBytes() is used for finding the smallest QR Code
    // version that can fit the given data.  Hence there were corner cases
    // where we chose a wrong QR Code version that cannot fit the given
    // data.  Note that the issue did not occur with MODE_8BIT_BYTE, as the
    // bits in the bit vector are always 8-bit aligned.
    //
    // Before the bug was fixed, the following test didn't pass, because:
    //
    // - MODE_NUMERIC is chosen as all bytes in the data are '0'
    // - The 3518-byte numeric data needs 1466 bytes
    //   - 3518 / 3 * 10 + 7 = 11727 bits = 1465.875 bytes
    //   - 3 numeric bytes are encoded in 10 bits, hence the first
    //     3516 bytes are encoded in 3516 / 3 * 10 = 11720 bits.
    //   - 2 numeric bytes can be encoded in 7 bits, hence the last
    //     2 bytes are encoded in 7 bits.
    // - The version 27 QR Code with the EC level L has 1468 bytes for data.
    //   - 1828 - 360 = 1468
    // - In InitQRCode(), 3 bytes are reserved for a header.  Hence 1465 bytes
    //   (1468 -3) are left for data.
    // - Because of the bug in BitVector::sizeInBytes(), InitQRCode() determines
    //   the given data can fit in 1465 bytes, despite it needs 1466 bytes.
    // - Hence QRCodeEncoder.encode() failed and returned false.
    //   - To be precise, it needs 11727 + 4 (getMode info) + 14 (length info) =
    //     11745 bits = 1468.125 bytes are needed (i.e. cannot fit in 1468
    //     bytes).
    final int arraySize = 3518;
    byte[] dataBytes = new byte[arraySize];
    for (int x = 0; x < arraySize; x++) {
      dataBytes[x] = '0';
    }
    QRCode qrCode = new QRCode();
    Encoder.encode(new ByteArray(dataBytes), QRCode.EC_LEVEL_L, qrCode);
  }
}
