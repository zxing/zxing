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

package com.google.zxing.datamatrix.decoder;

import com.google.zxing.datamatrix.encoder.MinimalEncoder;
import com.google.zxing.datamatrix.encoder.SymbolShapeHint;
import org.junit.Assert;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

/**
 * Checks that the codewords of {@link MinimalEncoder} decode back to the input.
 */
public final class MinimalEncoderRoundTripTestCase extends Assert {

  @Test
  public void testEdifactAtEndOfDataIsUnlatched() throws Exception {
    // Used to end with a full EDIFACT triple followed by padding, which was then read as EDIFACT data
    assertRoundTrip("I9PS4$TP$M:WSLF BH-052/VBS$C7N");
    assertRoundTrip("; %$-[#^@+,'\" !$");
    assertRoundTrip("][-DEDWI3SVTUW77");
  }

  @Test
  public void testTextShift1() throws Exception {
    // \u0001 to \u0003 were written one value too low in Text encodation
    assertRoundTrip("mibm\u0001");
    assertRoundTrip("hstsymnr\r\n\u0001\n");
    assertRoundTrip("abcdef\u0002\u0003");
  }

  @Test
  public void testECIAfterEdifact() throws Exception {
    // The ECI used to be written right after a full EDIFACT triple, without returning to ASCII first
    assertRoundTrip("kybdtcdo/&!(&/$! ;>:\";\u0411\u0411\u4e2d\u0411");
  }

  @Test
  public void testFNC1PrefixCountsTowardsCapacity() throws Exception {
    // The leading FNC1 was left out when deciding how the data ends in the symbol
    assertRoundTrip("SPORE", 29);
    assertRoundTrip("zqjuvdhzzahwvjn", 29);
    assertRoundTrip("u\\>;^@]nyxtib", 29);
  }

  private static void assertRoundTrip(String input) throws Exception {
    assertRoundTrip(input, -1);
  }

  private static void assertRoundTrip(String input, int fnc1) throws Exception {
    String codewords = MinimalEncoder.encodeHighLevel(input, null, fnc1, SymbolShapeHint.FORCE_NONE);
    String decoded = DecodedBitStreamParser.decode(codewords.getBytes(StandardCharsets.ISO_8859_1)).getText();
    // A leading FNC1 is returned as a group separator
    assertEquals(fnc1 > 0 ? "\u001d" + input : input, decoded);
  }
}
