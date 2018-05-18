/*
 * Copyright 2014 ZXing authors
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
package com.google.zxing.aztec.decoder;

import com.google.zxing.FormatException;
import com.google.zxing.ResultPoint;
import com.google.zxing.aztec.AztecDetectorResult;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.DecoderResult;
import org.junit.Test;
import org.junit.Assert;

/**
 * Tests {@link Decoder}.
 */
public final class DecoderTest extends Assert {

  private static final ResultPoint[] NO_POINTS = new ResultPoint[0];
  
  @Test
  public void testAztecResult() throws FormatException {
    BitMatrix matrix = BitMatrix.parse(
        "X X X X X     X X X       X X X     X X X     \n" +
        "X X X     X X X     X X X X     X X X     X X \n" +
        "  X   X X       X   X   X X X X     X     X X \n" +
        "  X   X X     X X     X     X   X       X   X \n" +
        "  X X   X X         X               X X     X \n" +
        "  X X   X X X X X X X X X X X X X X X     X   \n" +
        "  X X X X X                       X   X X X   \n" +
        "  X   X   X   X X X X X X X X X   X X X   X X \n" +
        "  X   X X X   X               X   X X       X \n" +
        "  X X   X X   X   X X X X X   X   X X X X   X \n" +
        "  X X   X X   X   X       X   X   X   X X X   \n" +
        "  X   X   X   X   X   X   X   X   X   X   X   \n" +
        "  X X X   X   X   X       X   X   X X   X X   \n" +
        "  X X X X X   X   X X X X X   X   X X X   X X \n" +
        "X X   X X X   X               X   X   X X   X \n" +
        "  X       X   X X X X X X X X X   X   X     X \n" +
        "  X X   X X                       X X   X X   \n" +
        "  X X X   X X X X X X X X X X X X X X   X X   \n" +
        "X     X     X     X X   X X               X X \n" +
        "X   X X X X X   X X X X X     X   X   X     X \n" +
        "X X X   X X X X           X X X       X     X \n" +
        "X X     X X X     X X X X     X X X     X X   \n" +
        "    X X X     X X X       X X X     X X X X   \n",
        "X ", "  ");
    AztecDetectorResult r = new AztecDetectorResult(matrix, NO_POINTS, false, 30, 2);
    DecoderResult result = new Decoder().decode(r);
    assertEquals("88888TTTTTTTTTTTTTTTTTTTTTTTTTTTTTT", result.getText());
    assertArrayEquals(
        new byte[] {-11, 85, 85, 117, 107, 90, -42, -75, -83, 107,
            90, -42, -75, -83, 107, 90, -42, -75, -83, 107,
            90, -42, -80},
        result.getRawBytes());
    assertEquals(180, result.getNumBits());
  }

  @Test(expected = FormatException.class)
  public void testDecodeTooManyErrors() throws FormatException {
    BitMatrix matrix = BitMatrix.parse(""
        + "X X . X . . . X X . . . X . . X X X . X . X X X X X . \n"
        + "X X . . X X . . . . . X X . . . X X . . . X . X . . X \n"
        + "X . . . X X . . X X X . X X . X X X X . X X . . X . . \n"
        + ". . . . X . X X . . X X . X X . X . X X X X . X . . X \n"
        + "X X X . . X X X X X . . . . . X X . . . X . X . X . X \n"
        + "X X . . . . . . . . X . . . X . X X X . X . . X . . . \n"
        + "X X . . X . . . . . X X . . . . . X . . . . X . . X X \n"
        + ". . . X . X . X . . . . . X X X X X X . . . . . . X X \n"
        + "X . . . X . X X X X X X . . X X X . X . X X X X X X . \n"
        + "X . . X X X . X X X X X X X X X X X X X . . . X . X X \n"
        + ". . . . X X . . . X . . . . . . . X X . . . X X . X . \n"
        + ". . . X X X . . X X . X X X X X . X . . X . . . . . . \n"
        + "X . . . . X . X . X . X . . . X . X . X X . X X . X X \n"
        + "X . X . . X . X . X . X . X . X . X . . . . . X . X X \n"
        + "X . X X X . . X . X . X . . . X . X . X X X . . . X X \n"
        + "X X X X X X X X . X . X X X X X . X . X . X . X X X . \n"
        + ". . . . . . . X . X . . . . . . . X X X X . . . X X X \n"
        + "X X . . X . . X . X X X X X X X X X X X X X . . X . X \n"
        + "X X X . X X X X . . X X X X . . X . . . . X . . X X X \n"
        + ". . . . X . X X X . . . . X X X X . . X X X X . . . . \n"
        + ". . X . . X . X . . . X . X X . X X . X . . . X . X . \n"
        + "X X . . X . . X X X X X X X . . X . X X X X X X X . . \n"
        + "X . X X . . X X . . . . . X . . . . . . X X . X X X . \n"
        + "X . . X X . . X X . X . X . . . . X . X . . X . . X . \n"
        + "X . X . X . . X . X X X X X X X X . X X X X . . X X . \n"
        + "X X X X . . . X . . X X X . X X . . X . . . . X X X . \n"
        + "X X . X . X . . . X . X . . . . X X . X . . X X . . . \n",
        "X ", ". ");
    AztecDetectorResult r = new AztecDetectorResult(matrix, NO_POINTS, true, 16, 4);
    new Decoder().decode(r);
  }

  @Test(expected = FormatException.class)
  public void testDecodeTooManyErrors2() throws FormatException {
    BitMatrix matrix = BitMatrix.parse(""
        + ". X X . . X . X X . . . X . . X X X . . . X X . X X . \n"
        + "X X . X X . . X . . . X X . . . X X . X X X . X . X X \n"
        + ". . . . X . . . X X X . X X . X X X X . X X . . X . . \n"
        + "X . X X . . X . . . X X . X X . X . X X . . . . . X . \n"
        + "X X . X . . X . X X . . . . . X X . . . . . X . . . X \n"
        + "X . . X . . . . . . X . . . X . X X X X X X X . . . X \n"
        + "X . . X X . . X . . X X . . . . . X . . . . . X X X . \n"
        + ". . X X X X . X . . . . . X X X X X X . . . . . . X X \n"
        + "X . . . X . X X X X X X . . X X X . X . X X X X X X . \n"
        + "X . . X X X . X X X X X X X X X X X X X . . . X . X X \n"
        + ". . . . X X . . . X . . . . . . . X X . . . X X . X . \n"
        + ". . . X X X . . X X . X X X X X . X . . X . . . . . . \n"
        + "X . . . . X . X . X . X . . . X . X . X X . X X . X X \n"
        + "X . X . . X . X . X . X . X . X . X . . . . . X . X X \n"
        + "X . X X X . . X . X . X . . . X . X . X X X . . . X X \n"
        + "X X X X X X X X . X . X X X X X . X . X . X . X X X . \n"
        + ". . . . . . . X . X . . . . . . . X X X X . . . X X X \n"
        + "X X . . X . . X . X X X X X X X X X X X X X . . X . X \n"
        + "X X X . X X X X . . X X X X . . X . . . . X . . X X X \n"
        + ". . X X X X X . X . . . . X X X X . . X X X . X . X . \n"
        + ". . X X . X . X . . . X . X X . X X . . . . X X . . . \n"
        + "X . . . X . X . X X X X X X . . X . X X X X X . X . . \n"
        + ". X . . . X X X . . . . . X . . . . . X X X X X . X . \n"
        + "X . . X . X X X X . X . X . . . . X . X X . X . . X . \n"
        + "X . . . X X . X . X X X X X X X X . X X X X . . X X . \n"
        + ". X X X X . . X . . X X X . X X . . X . . . . X X X . \n"
        + "X X . . . X X . . X . X . . . . X X . X . . X . X . X \n",
        "X ", ". ");
    AztecDetectorResult r = new AztecDetectorResult(matrix, NO_POINTS, true, 16, 4);
    new Decoder().decode(r);
  }

  @Test
  public void testRawBytes() {
    boolean[] bool0 = {};
    boolean[] bool1 = { true };
    boolean[] bool7 = { true, false, true, false, true, false, true };
    boolean[] bool8 = { true, false, true, false, true, false, true, false };
    boolean[] bool9 = { true, false, true, false, true, false, true, false,
                        true };
    boolean[] bool16 = { false, true, true, false, false, false, true, true,
                         true, true, false, false, false, false, false, true };
    byte[] byte0 = {};
    byte[] byte1 = { -128 };
    byte[] byte7 = { -86 };
    byte[] byte8 = { -86 };
    byte[] byte9 = { -86, -128 };
    byte[] byte16 = { 99, -63 };

    assertArrayEquals(byte0, Decoder.convertBoolArrayToByteArray(bool0));
    assertArrayEquals(byte1, Decoder.convertBoolArrayToByteArray(bool1));
    assertArrayEquals(byte7, Decoder.convertBoolArrayToByteArray(bool7));
    assertArrayEquals(byte8, Decoder.convertBoolArrayToByteArray(bool8));
    assertArrayEquals(byte9, Decoder.convertBoolArrayToByteArray(bool9));
    assertArrayEquals(byte16, Decoder.convertBoolArrayToByteArray(bool16));
  }
}
