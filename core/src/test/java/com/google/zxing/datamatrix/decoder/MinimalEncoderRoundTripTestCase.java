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
import java.util.List;

/**
 * Checks that the codewords of {@link MinimalEncoder} decode back to the input.
 */
public final class MinimalEncoderRoundTripTestCase extends Assert {

  private static final String MACRO_05 = "[)>\u001E05\u001D";
  private static final String MACRO_06 = "[)>\u001E06\u001D";
  private static final String MACRO_TRAILER = "\u001E\u0004";

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
  public void testFNC1NotEncodedAsLiteralByteInBase256() throws Exception {
    // Extended ASCII around GS used to keep MinimalEncoder in Base 256 and emit literal 0x1D instead of FNC1 (232)
    String input = "\u00e4\u00f6\u00fc\u00e4\u00f6\u00fc\u001d\u00e4\u00f6\u00fc\u00e4\u00f6\u00fc";
    String codewords = assertRoundTrip(input, 29, SymbolShapeHint.FORCE_NONE);
    byte[] bytes = codewords.getBytes(StandardCharsets.ISO_8859_1);
    assertEquals(232, bytes[0] & 0xFF);
    // Verify that the internal GS was encoded as FNC1 (codeword 232), not inside Base 256 byte segments
    List<byte[]> segments = DecodedBitStreamParser.decode(bytes).getByteSegments();
    if (segments != null) {
      for (byte[] segment : segments) {
        for (byte b : segment) {
          assertNotEquals("FNC1 delimiter must not be encoded as literal 0x1D in Base 256", 0x1D, b & 0xFF);
        }
      }
    }
  }

  @Test
  public void testExtendedAscii0x9DNotConflatedWithFNC1InC40AndText() throws Exception {
    // 0x9D = 128 + 29; in C40/Text Upper Shift it must encode as Shift 1 + 29, not FNC1 (Shift 2 + 27)
    assertRoundTrip("ABCDEFGH\u009dIJKLMNOPQR", 29);
    assertRoundTrip("abcdefgh\u009dijklmnopqr", 29);
  }

  @Test
  public void testFNC1WithUtf16BeEci() throws Exception {
    // Multi-byte UTF-16BE characters separated by FNC1 must not be misaligned by the FNC1 separator
    assertRoundTrip("\u4e2d\u6587\u4e2d\u6587\u001d\u4e2d\u6587\u4e2d\u6587", 29);
  }

  @Test
  public void testLeadingGSNotDuplicatedOnRoundTrip() throws Exception {
    String gs1Payload = "010123456789012810ABC\u001d17260101";
    String firstPass = MinimalEncoder.encodeHighLevel(gs1Payload, null, 29, SymbolShapeHint.FORCE_NONE);
    String decoded = DecodedBitStreamParser.decode(firstPass.getBytes(StandardCharsets.ISO_8859_1)).getText();
    assertEquals("\u001d" + gs1Payload, decoded);

    // Re-encoding the decoded string (which already begins with GS) must not emit a duplicate leading FNC1
    String secondPass = MinimalEncoder.encodeHighLevel(decoded, null, 29, SymbolShapeHint.FORCE_NONE);
    byte[] secondBytes = secondPass.getBytes(StandardCharsets.ISO_8859_1);
    assertEquals(232, secondBytes[0] & 0xFF);
    assertNotEquals(232, secondBytes[1] & 0xFF);
    assertEquals(decoded, DecodedBitStreamParser.decode(secondBytes).getText());

    // Single leading GS alone produces a single FNC1 codeword
    String singleGs = MinimalEncoder.encodeHighLevel("\u001d", null, 29, SymbolShapeHint.FORCE_NONE);
    assertEquals("\u001d", DecodedBitStreamParser.decode(singleGs.getBytes(StandardCharsets.ISO_8859_1)).getText());
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
