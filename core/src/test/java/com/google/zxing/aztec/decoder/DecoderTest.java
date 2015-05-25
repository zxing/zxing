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
import org.junit.Test;

public final class DecoderTest {

  private static final ResultPoint[] NO_POINTS = new ResultPoint[0];

  /**
   * throws
   * <pre>com.google.zxing.FormatException: com.google.zxing.common.reedsolomon.ReedSolomonException: Error locator degree does not match number of roots
   *	at com.google.zxing.common.reedsolomon.ReedSolomonDecoder.findErrorLocations(ReedSolomonDecoder.java:158)
   *	at com.google.zxing.common.reedsolomon.ReedSolomonDecoder.decode(ReedSolomonDecoder.java:77)
   *	at com.google.zxing.aztec.decoder.Decoder.correctBits(Decoder.java:231)
   *	at com.google.zxing.aztec.decoder.Decoder.decode(Decoder.java:77)
   *	at com.google.zxing.aztec.decoder.DecoderTest.testDecodeBug1(DecoderTest.java:66)</pre>
   * @throws FormatException
   */
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

}
