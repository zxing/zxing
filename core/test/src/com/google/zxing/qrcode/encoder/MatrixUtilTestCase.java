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

import com.google.zxing.common.ByteMatrix;
import com.google.zxing.WriterException;
import junit.framework.TestCase;

/**
 * @author satorux@google.com (Satoru Takabayashi) - creator
 * @author mysen@google.com (Chris Mysen) - ported from C++
 */
public final class MatrixUtilTestCase extends TestCase {
  public void testtoString() {
    ByteMatrix array = new ByteMatrix(3, 3);
    array.set(0, 0, 0);
    array.set(0, 1, 1);
    array.set(0, 2, 0);
    array.set(1, 0, 1);
    array.set(1, 1, 0);
    array.set(1, 2, 1);
    array.set(2, 0, -1);
    array.set(2, 1, -1);
    array.set(2, 2, -1);
    String expected = " 0 1 0\n" + " 1 0 1\n" + "      \n";
    assertEquals(expected, array.toString());
  }

  public void testClearMatrix() {
    ByteMatrix matrix = new ByteMatrix(2, 2);
    MatrixUtil.ClearMatrix(matrix);
    assertEquals(-1, matrix.get(0, 0));
    assertEquals(-1, matrix.get(0, 1));
    assertEquals(-1, matrix.get(1, 0));
    assertEquals(-1, matrix.get(1, 1));
  }

  public void testEmbedBasicPatterns() throws WriterException {
    {
      // Version 1.
      String expected =
	" 1 1 1 1 1 1 1 0           0 1 1 1 1 1 1 1\n" +
	" 1 0 0 0 0 0 1 0           0 1 0 0 0 0 0 1\n" +
	" 1 0 1 1 1 0 1 0           0 1 0 1 1 1 0 1\n" +
	" 1 0 1 1 1 0 1 0           0 1 0 1 1 1 0 1\n" +
	" 1 0 1 1 1 0 1 0           0 1 0 1 1 1 0 1\n" +
	" 1 0 0 0 0 0 1 0           0 1 0 0 0 0 0 1\n" +
	" 1 1 1 1 1 1 1 0 1 0 1 0 1 0 1 1 1 1 1 1 1\n" +
	" 0 0 0 0 0 0 0 0           0 0 0 0 0 0 0 0\n" +
	"             1                            \n" +
	"             0                            \n" +
	"             1                            \n" +
	"             0                            \n" +
	"             1                            \n" +
	" 0 0 0 0 0 0 0 0 1                        \n" +
	" 1 1 1 1 1 1 1 0                          \n" +
	" 1 0 0 0 0 0 1 0                          \n" +
	" 1 0 1 1 1 0 1 0                          \n" +
	" 1 0 1 1 1 0 1 0                          \n" +
	" 1 0 1 1 1 0 1 0                          \n" +
	" 1 0 0 0 0 0 1 0                          \n" +
	" 1 1 1 1 1 1 1 0                          \n";
      ByteMatrix matrix = new ByteMatrix(21, 21);
      MatrixUtil.ClearMatrix(matrix);
      MatrixUtil.EmbedBasicPatterns(1, matrix);
      assertEquals(expected, matrix.toString());
    }
    {
      // Version 2.  Position adjustment pattern should apppear at right
      // bottom corner.
      String expected =
	" 1 1 1 1 1 1 1 0                   0 1 1 1 1 1 1 1\n" +
	" 1 0 0 0 0 0 1 0                   0 1 0 0 0 0 0 1\n" +
	" 1 0 1 1 1 0 1 0                   0 1 0 1 1 1 0 1\n" +
	" 1 0 1 1 1 0 1 0                   0 1 0 1 1 1 0 1\n" +
	" 1 0 1 1 1 0 1 0                   0 1 0 1 1 1 0 1\n" +
	" 1 0 0 0 0 0 1 0                   0 1 0 0 0 0 0 1\n" +
	" 1 1 1 1 1 1 1 0 1 0 1 0 1 0 1 0 1 0 1 1 1 1 1 1 1\n" +
	" 0 0 0 0 0 0 0 0                   0 0 0 0 0 0 0 0\n" +
	"             1                                    \n" +
	"             0                                    \n" +
	"             1                                    \n" +
	"             0                                    \n" +
	"             1                                    \n" +
	"             0                                    \n" +
	"             1                                    \n" +
	"             0                                    \n" +
	"             1                   1 1 1 1 1        \n" +
	" 0 0 0 0 0 0 0 0 1               1 0 0 0 1        \n" +
	" 1 1 1 1 1 1 1 0                 1 0 1 0 1        \n" +
	" 1 0 0 0 0 0 1 0                 1 0 0 0 1        \n" +
	" 1 0 1 1 1 0 1 0                 1 1 1 1 1        \n" +
	" 1 0 1 1 1 0 1 0                                  \n" +
	" 1 0 1 1 1 0 1 0                                  \n" +
	" 1 0 0 0 0 0 1 0                                  \n" +
	" 1 1 1 1 1 1 1 0                                  \n";
      ByteMatrix matrix = new ByteMatrix(25, 25);
      MatrixUtil.ClearMatrix(matrix);
      MatrixUtil.EmbedBasicPatterns(2, matrix);
      assertEquals(expected, matrix.toString());
    }
  }

  public void testEmbedTypeInfo() throws WriterException {
    // Type info bits = 100000011001110.
    String expected =
      "                 0                        \n" +
      "                 1                        \n" +
      "                 1                        \n" +
      "                 1                        \n" +
      "                 0                        \n" +
      "                 0                        \n" +
      "                                          \n" +
      "                 1                        \n" +
      " 1 0 0 0 0 0   0 1         1 1 0 0 1 1 1 0\n" +
      "                                          \n" +
      "                                          \n" +
      "                                          \n" +
      "                                          \n" +
      "                                          \n" +
      "                 0                        \n" +
      "                 0                        \n" +
      "                 0                        \n" +
      "                 0                        \n" +
      "                 0                        \n" +
      "                 0                        \n" +
      "                 1                        \n";
    ByteMatrix matrix = new ByteMatrix(21, 21);
    MatrixUtil.ClearMatrix(matrix);
    MatrixUtil.EmbedTypeInfo(QRCode.EC_LEVEL_M, 5, matrix);
    assertEquals(expected, matrix.toString());
  }

  public void testEmbedVersionInfo() throws WriterException {
    // Version info bits = 000111 110010 010100
    String expected =
      "                     0 0 1                \n" +
      "                     0 1 0                \n" +
      "                     0 1 0                \n" +
      "                     0 1 1                \n" +
      "                     1 1 1                \n" +
      "                     0 0 0                \n" +
      "                                          \n" +
      "                                          \n" +
      "                                          \n" +
      "                                          \n" +
      " 0 0 0 0 1 0                              \n" +
      " 0 1 1 1 1 0                              \n" +
      " 1 0 0 1 1 0                              \n" +
      "                                          \n" +
      "                                          \n" +
      "                                          \n" +
      "                                          \n" +
      "                                          \n" +
      "                                          \n" +
      "                                          \n" +
      "                                          \n";
    // Actually, version 7 QR Code has 45x45 matrix but we use 21x21 here
    // since 45x45 matrix is too big to depict.
    ByteMatrix matrix = new ByteMatrix(21, 21);
    MatrixUtil.ClearMatrix(matrix);
    MatrixUtil.MaybeEmbedVersionInfo(7, matrix);
    assertEquals(expected, matrix.toString());
  }

  public void testEmbedDataBits() throws WriterException {
    // Cells other than basic patterns should be filled with zero.
    String expected =
      " 1 1 1 1 1 1 1 0 0 0 0 0 0 0 1 1 1 1 1 1 1\n" +
      " 1 0 0 0 0 0 1 0 0 0 0 0 0 0 1 0 0 0 0 0 1\n" +
      " 1 0 1 1 1 0 1 0 0 0 0 0 0 0 1 0 1 1 1 0 1\n" +
      " 1 0 1 1 1 0 1 0 0 0 0 0 0 0 1 0 1 1 1 0 1\n" +
      " 1 0 1 1 1 0 1 0 0 0 0 0 0 0 1 0 1 1 1 0 1\n" +
      " 1 0 0 0 0 0 1 0 0 0 0 0 0 0 1 0 0 0 0 0 1\n" +
      " 1 1 1 1 1 1 1 0 1 0 1 0 1 0 1 1 1 1 1 1 1\n" +
      " 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0\n" +
      " 0 0 0 0 0 0 1 0 0 0 0 0 0 0 0 0 0 0 0 0 0\n" +
      " 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0\n" +
      " 0 0 0 0 0 0 1 0 0 0 0 0 0 0 0 0 0 0 0 0 0\n" +
      " 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0\n" +
      " 0 0 0 0 0 0 1 0 0 0 0 0 0 0 0 0 0 0 0 0 0\n" +
      " 0 0 0 0 0 0 0 0 1 0 0 0 0 0 0 0 0 0 0 0 0\n" +
      " 1 1 1 1 1 1 1 0 0 0 0 0 0 0 0 0 0 0 0 0 0\n" +
      " 1 0 0 0 0 0 1 0 0 0 0 0 0 0 0 0 0 0 0 0 0\n" +
      " 1 0 1 1 1 0 1 0 0 0 0 0 0 0 0 0 0 0 0 0 0\n" +
      " 1 0 1 1 1 0 1 0 0 0 0 0 0 0 0 0 0 0 0 0 0\n" +
      " 1 0 1 1 1 0 1 0 0 0 0 0 0 0 0 0 0 0 0 0 0\n" +
      " 1 0 0 0 0 0 1 0 0 0 0 0 0 0 0 0 0 0 0 0 0\n" +
      " 1 1 1 1 1 1 1 0 0 0 0 0 0 0 0 0 0 0 0 0 0\n";
    BitVector bits = new BitVector();
    ByteMatrix matrix = new ByteMatrix(21, 21);
    MatrixUtil.ClearMatrix(matrix);
    MatrixUtil.EmbedBasicPatterns(1, matrix);
    MatrixUtil.EmbedDataBits(bits, -1, matrix);
    assertEquals(expected, matrix.toString());
  }

  public void testBuildMatrix() throws WriterException {
    // From http://www.swetake.com/qr/qr7.html
    String expected =
      " 1 1 1 1 1 1 1 0 0 1 1 0 0 0 1 1 1 1 1 1 1\n" +
      " 1 0 0 0 0 0 1 0 0 0 0 0 0 0 1 0 0 0 0 0 1\n" +
      " 1 0 1 1 1 0 1 0 0 0 0 1 0 0 1 0 1 1 1 0 1\n" +
      " 1 0 1 1 1 0 1 0 0 1 1 0 0 0 1 0 1 1 1 0 1\n" +
      " 1 0 1 1 1 0 1 0 1 1 0 0 1 0 1 0 1 1 1 0 1\n" +
      " 1 0 0 0 0 0 1 0 0 0 1 1 1 0 1 0 0 0 0 0 1\n" +
      " 1 1 1 1 1 1 1 0 1 0 1 0 1 0 1 1 1 1 1 1 1\n" +
      " 0 0 0 0 0 0 0 0 1 1 0 1 1 0 0 0 0 0 0 0 0\n" +
      " 0 0 1 1 0 0 1 1 1 0 0 1 1 1 1 0 1 0 0 0 0\n" +
      " 1 0 1 0 1 0 0 0 0 0 1 1 1 0 0 1 0 1 1 1 0\n" +
      " 1 1 1 1 0 1 1 0 1 0 1 1 1 0 0 1 1 1 0 1 0\n" +
      " 1 0 1 0 1 1 0 1 1 1 0 0 1 1 1 0 0 1 0 1 0\n" +
      " 0 0 1 0 0 1 1 1 0 0 0 0 0 0 1 0 1 1 1 1 1\n" +
      " 0 0 0 0 0 0 0 0 1 1 0 1 0 0 0 0 0 1 0 1 1\n" +
      " 1 1 1 1 1 1 1 0 1 1 1 1 0 0 0 0 1 0 1 1 0\n" +
      " 1 0 0 0 0 0 1 0 0 0 0 1 0 1 1 1 0 0 0 0 0\n" +
      " 1 0 1 1 1 0 1 0 0 1 0 0 1 1 0 0 1 0 0 1 1\n" +
      " 1 0 1 1 1 0 1 0 1 1 0 1 0 0 0 0 0 1 1 1 0\n" +
      " 1 0 1 1 1 0 1 0 1 1 1 1 0 0 0 0 1 1 1 0 0\n" +
      " 1 0 0 0 0 0 1 0 0 0 0 0 0 0 0 0 1 0 1 0 0\n" +
      " 1 1 1 1 1 1 1 0 0 0 1 1 1 1 1 0 1 0 0 1 0\n";
    char bytes[] = {32, 65, 205, 69, 41, 220, 46, 128, 236,
		    42, 159, 74, 221, 244, 169, 239, 150, 138,
		    70, 237, 85, 224, 96, 74, 219 , 61};
    BitVector bits = new BitVector();
    for (char c: bytes) {
      bits.AppendBits(c, 8);
    }
    ByteMatrix matrix = new ByteMatrix(21, 21);
    MatrixUtil.BuildMatrix(bits,
					      QRCode.EC_LEVEL_H,
					      1,  // Version 1
					      3,  // Mask pattern 3
					      matrix);
  }

  public void testFindMSBSet() {
    assertEquals(0, MatrixUtil.FindMSBSet(0));
    assertEquals(1, MatrixUtil.FindMSBSet(1));
    assertEquals(8, MatrixUtil.FindMSBSet(0x80));
    assertEquals(32, MatrixUtil.FindMSBSet(0x80000000));
  }

  public void testCalculateBCHCode() {
    // Encoding of type information.
    // From Appendix C in JISX0510:2004 (p 65)
    assertEquals(0xdc, MatrixUtil.CalculateBCHCode(5, 0x537));
    // From http://www.swetake.com/qr/qr6.html
    assertEquals(0x1c2, MatrixUtil.CalculateBCHCode(0x13, 0x537));
    // From http://www.swetake.com/qr/qr11.html
    assertEquals(0x214, MatrixUtil.CalculateBCHCode(0x1b, 0x537));

    // Encofing of version information.
    // From Appendix D in JISX0510:2004 (p 68)
    assertEquals(0xc94, MatrixUtil.CalculateBCHCode(7, 0x1f25));
    assertEquals(0x5bc, MatrixUtil.CalculateBCHCode(8, 0x1f25));
    assertEquals(0xa99, MatrixUtil.CalculateBCHCode(9, 0x1f25));
    assertEquals(0x4d3, MatrixUtil.CalculateBCHCode(10, 0x1f25));
    assertEquals(0x9a6, MatrixUtil.CalculateBCHCode(20, 0x1f25));
    assertEquals(0xd75, MatrixUtil.CalculateBCHCode(30, 0x1f25));
    assertEquals(0xc69, MatrixUtil.CalculateBCHCode(40, 0x1f25));
  }

  // We don't test a lot of cases in this function since we've already
  // tested them in TEST(CalculateBCHCode).
  public void testMakeVersionInfoBits() throws WriterException {
    // From Appendix D in JISX0510:2004 (p 68)
    BitVector bits = new BitVector();
    MatrixUtil.MakeVersionInfoBits(7, bits);
    assertEquals("000111110010010100", bits.toString());
  }

  // We don't test a lot of cases in this function since we've already
  // tested them in TEST(CalculateBCHCode).
  public void testMakeTypeInfoInfoBits() throws WriterException {
    // From Appendix C in JISX0510:2004 (p 65)
    BitVector bits = new BitVector();
    MatrixUtil.MakeTypeInfoBits(QRCode.EC_LEVEL_M,
						   5, bits);
    assertEquals("100000011001110", bits.toString());
  }
}
