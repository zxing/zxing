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
    assertRoundTrip("abcdef\u0000\u0001");
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

  @Test
  public void testFNC1InsideC40AndText() throws Exception {
    // A FNC1 inside C40 or Text data was written in the Shift 1 set, where 27 decodes as ESC instead of FNC1
    assertRoundTrip("ABCDEFGH\u001dIJKLMNOPQR", 29);
    assertRoundTrip("abcdefgh\u001dijklmnopqr", 29);
  }

  @Test
  public void testEdifactEndWithThreeCodewordsLeft() throws Exception {
    // A final full EDIFACT triple followed by three unused codewords needs the unlatch character
    assertRoundTrip("?;DF%%*J$8,,0T?'", SymbolShapeHint.FORCE_NONE);
    assertRoundTrip(".IL5\"W\\(LM", SymbolShapeHint.FORCE_SQUARE);
    assertRoundTrip("ZK^IRPZR", SymbolShapeHint.FORCE_RECTANGLE);
  }

  @Test
  public void testEdifactEndWithFewerThanThreeCodewordsLeft() throws Exception {
    // A final full EDIFACT triple with zero, one or two unused codewords returns to ASCII without an unlatch
    String edifact8 = "[@@]^^\\[";
    String edifact12 = "]^^\\[[@@]^^\\";
    String edifact16 = "\\[[@@]^^\\[[@@]^^";
    assertEdifactEnd(edifact12, SymbolShapeHint.FORCE_NONE, 10, 10); // 0 left
    assertEdifactEnd(edifact12, SymbolShapeHint.FORCE_RECTANGLE, 10, 10); // 0 left
    assertEdifactEnd(edifact8, SymbolShapeHint.FORCE_NONE, 7, 8); // 1 left
    assertEdifactEnd(edifact12, SymbolShapeHint.FORCE_SQUARE, 10, 12); // 2 left
    assertEdifactEnd(MACRO_05 + edifact8 + MACRO_TRAILER, SymbolShapeHint.FORCE_NONE, 8, 8); // 0 left
    assertEdifactEnd(MACRO_05 + edifact12 + MACRO_TRAILER, SymbolShapeHint.FORCE_NONE, 11, 12); // 1 left
    assertEdifactEnd(MACRO_05 + edifact16 + MACRO_TRAILER, SymbolShapeHint.FORCE_NONE, 14, 16); // 2 left
  }

  @Test
  public void testMacroPrefixCountsTowardsCapacity() throws Exception {
    // The macro codeword was left out when deciding how the data ends in the symbol
    assertRoundTrip(MACRO_05 + ";]IL/&VI*$@\\+H" + MACRO_TRAILER, SymbolShapeHint.FORCE_NONE);
    assertRoundTrip(MACRO_05 + "YY9NI&FO?;=H!G" + MACRO_TRAILER, SymbolShapeHint.FORCE_RECTANGLE);
    assertRoundTrip(MACRO_06 + ">,\"]+HA@D" + MACRO_TRAILER, SymbolShapeHint.FORCE_SQUARE);
    assertRoundTrip(MACRO_06 + "W7?$/R9'ZY/IHR(HX" + MACRO_TRAILER, SymbolShapeHint.FORCE_SQUARE);
    String edifact40 = "[@@]^^\\[[@@]^^\\[[@@]^^\\[[@@]^^\\[[@@]^^\\[";
    assertEdifactEnd(MACRO_06 + edifact40 + MACRO_TRAILER, SymbolShapeHint.FORCE_NONE, 32, 32); // 0 left
  }

  private static final String MACRO_05 = "[)>\u001E05\u001D";
  private static final String MACRO_06 = "[)>\u001E06\u001D";
  private static final String MACRO_TRAILER = "\u001E\u0004";

  /**
   * Checks that input encoded entirely in EDIFACT ends with a full triple at {@code dataCodewords}, followed by
   * padding and not by the unlatch character, in a symbol of {@code symbolCodewords} codewords.
   */
  private static void assertEdifactEnd(String input, SymbolShapeHint shape, int dataCodewords, int symbolCodewords)
      throws Exception {
    String codewords = assertRoundTrip(input, -1, shape);
    assertEquals(symbolCodewords, codewords.length());
    if (dataCodewords < symbolCodewords) {
      assertEquals(129, codewords.charAt(dataCodewords)); // pad, not unlatch (124)
    }
  }

  private static void assertRoundTrip(String input) throws Exception {
    assertRoundTrip(input, -1);
  }

  private static void assertRoundTrip(String input, int fnc1) throws Exception {
    assertRoundTrip(input, fnc1, SymbolShapeHint.FORCE_NONE);
  }

  private static void assertRoundTrip(String input, SymbolShapeHint shape) throws Exception {
    assertRoundTrip(input, -1, shape);
  }

  private static String assertRoundTrip(String input, int fnc1, SymbolShapeHint shape) throws Exception {
    String codewords = MinimalEncoder.encodeHighLevel(input, null, fnc1, shape);
    String decoded = DecodedBitStreamParser.decode(codewords.getBytes(StandardCharsets.ISO_8859_1)).getText();
    // A leading FNC1 is returned as a group separator
    assertEquals(fnc1 > 0 ? "\u001d" + input : input, decoded);
    return codewords;
  }
}
