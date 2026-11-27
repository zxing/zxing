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

package com.google.zxing.oned;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.DecodeHintType;
import com.google.zxing.EncodeHintType;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.ResultMetadataType;
import com.google.zxing.common.BitArray;
import com.google.zxing.common.BitMatrix;

import org.junit.Assert;
import org.junit.Test;

import java.util.EnumMap;
import java.util.Map;

public final class Code128ReaderTestCase extends Assert {

  private static final String[][] INPUTS = {{"@C", "?A", "@@"}, {"AB", "As", "A?"}, {"1045", "1142", "1042"}};
  private static final int[] CHECKSUMS = {102, 97, 96};

  @Test
  public void testFunctionCodeChecksumsText() throws Exception {
    for (int i = 0; i < INPUTS.length; i++) {
      for (int j = 0; j < CHECKSUMS.length; j++) {
        Result result = decode(INPUTS[i][j], String.valueOf((char) ('A' + i)), true);
        byte[] raw = result.getRawBytes();
        assertEquals(CHECKSUMS[j], raw[raw.length - 2] & 0xFF);
        assertEquals(INPUTS[i][j], result.getText());
      }
    }
  }

  @Test
  public void testFunctionCodeChecksumsMetadata() throws Exception {
    for (int i = 0; i < INPUTS.length; i++) {
      for (int j = 0; j < CHECKSUMS.length; j++) {
        Result result = decode(INPUTS[i][j], String.valueOf((char) ('A' + i)), false);
        byte[] raw = result.getRawBytes();
        assertEquals(CHECKSUMS[j], raw[raw.length - 2] & 0xFF);
        assertEquals("]C0", result.getResultMetadata().get(ResultMetadataType.SYMBOLOGY_IDENTIFIER));
        assertEquals(Boolean.FALSE, result.getResultMetadata().get(ResultMetadataType.READER_INIT));
      }
    }
  }

  @Test
  public void testGS1WithFunctionCodeChecksum() throws Exception {
    String input = "01012345678901281726010110BATCH070";
    Result result = decode("\u00f1" + input, null, true);
    byte[] raw = result.getRawBytes();
    assertEquals(102, raw[raw.length - 2] & 0xFF);
    assertEquals("]C1" + input, result.getText());
    assertEquals("]C1", result.getResultMetadata().get(ResultMetadataType.SYMBOLOGY_IDENTIFIER));
    assertEquals(Boolean.FALSE, result.getResultMetadata().get(ResultMetadataType.READER_INIT));
  }

  @Test
  public void testFunctionCodesInData() throws Exception {
    Result fnc1 = decode("\u00f1AB", "B", true);
    assertEquals("]C1AB", fnc1.getText());
    assertEquals("]C1", fnc1.getResultMetadata().get(ResultMetadataType.SYMBOLOGY_IDENTIFIER));
    Result fnc2 = decode("\u00f2AB", "B", false);
    assertEquals("AB", fnc2.getText());
    assertEquals("]C4", fnc2.getResultMetadata().get(ResultMetadataType.SYMBOLOGY_IDENTIFIER));
    Result fnc3 = decode("\u00f3AB", "B", false);
    assertEquals("AB", fnc3.getText());
    assertEquals(Boolean.TRUE, fnc3.getResultMetadata().get(ResultMetadataType.READER_INIT));
  }

  @Test
  public void testFunctionCodeChecksumsKeepMetadata() throws Exception {
    Result fnc1Checksum = decode("\u00f3V", "B", false);
    byte[] raw = fnc1Checksum.getRawBytes();
    assertEquals(102, raw[raw.length - 2] & 0xFF);
    assertEquals("V", fnc1Checksum.getText());
    assertEquals("]C0", fnc1Checksum.getResultMetadata().get(ResultMetadataType.SYMBOLOGY_IDENTIFIER));
    assertEquals(Boolean.TRUE, fnc1Checksum.getResultMetadata().get(ResultMetadataType.READER_INIT));
    Result fnc2Checksum = decode("\u00f1@+", "B", true);
    raw = fnc2Checksum.getRawBytes();
    assertEquals(97, raw[raw.length - 2] & 0xFF);
    assertEquals("]C1@+", fnc2Checksum.getText());
    assertEquals("]C1", fnc2Checksum.getResultMetadata().get(ResultMetadataType.SYMBOLOGY_IDENTIFIER));
    assertEquals(Boolean.FALSE, fnc2Checksum.getResultMetadata().get(ResultMetadataType.READER_INIT));
  }

  @Test
  public void testReaderInitializationWithoutText() throws Exception {
    Result result = decode("\u00f3\u00f3", "B", false);
    assertEquals("", result.getText());
    assertEquals(Boolean.TRUE, result.getResultMetadata().get(ResultMetadataType.READER_INIT));
  }

  @Test
  public void testFNC1SeparatorTranslatedWithAndWithoutAssumeGS1() throws Exception {
    String input = "\u00f1010123456789012810ABC\u00f117260101";
    Result withoutHint = decode(input, null, false);
    assertEquals("010123456789012810ABC\u001d17260101", withoutHint.getText());
    assertEquals("]C1", withoutHint.getResultMetadata().get(ResultMetadataType.SYMBOLOGY_IDENTIFIER));

    Result withHint = decode(input, null, true);
    assertEquals("]C1010123456789012810ABC\u001d17260101", withHint.getText());
    assertEquals("]C1", withHint.getResultMetadata().get(ResultMetadataType.SYMBOLOGY_IDENTIFIER));
  }

  @Test
  public void testLeadingFNC1PreservesSymbologyModifierAcrossSubsequentFNC1() throws Exception {
    // Leading FNC1 followed by 1 char and another FNC1 must stay ]C1 (not be overwritten to ]C2)
    Result withoutHint = decode("\u00f1A\u00f1B", "B", false);
    assertEquals("A\u001dB", withoutHint.getText());
    assertEquals("]C1", withoutHint.getResultMetadata().get(ResultMetadataType.SYMBOLOGY_IDENTIFIER));

    Result withHint = decode("\u00f1A\u00f1B", "B", true);
    assertEquals("]C1A\u001dB", withHint.getText());
    assertEquals("]C1", withHint.getResultMetadata().get(ResultMetadataType.SYMBOLOGY_IDENTIFIER));

    // Second-position FNC1 (AIM industry standard) still yields ]C2
    Result aimWithoutHint = decode("A\u00f1B", "B", false);
    assertEquals("AB", aimWithoutHint.getText());
    assertEquals("]C2", aimWithoutHint.getResultMetadata().get(ResultMetadataType.SYMBOLOGY_IDENTIFIER));

    Result aimWithHint = decode("A\u00f1B", "B", true);
    assertEquals("A\u001dB", aimWithHint.getText());
    assertEquals("]C2", aimWithHint.getResultMetadata().get(ResultMetadataType.SYMBOLOGY_IDENTIFIER));
  }

  @Test
  public void testSingleFNC3ReaderInitializationWithNonPrintableChecksum() throws Exception {
    // START_A(103) + 1*FNC3(96) => checksum 96 (FNC3); START_B(104) + 1*FNC3(96) => checksum 97 (FNC2).
    // Both must decode as empty reader-initialization symbols rather than throwing NotFoundException.
    for (String codeSet : new String[] {"A", "B"}) {
      Result result = decode("\u00f3", codeSet, false);
      assertEquals("", result.getText());
      assertEquals("]C0", result.getResultMetadata().get(ResultMetadataType.SYMBOLOGY_IDENTIFIER));
      assertEquals(Boolean.TRUE, result.getResultMetadata().get(ResultMetadataType.READER_INIT));
    }
  }

  @Test
  public void testRejectsZeroPayloadAndMissingChecksumSymbols() throws Exception {
    int[][] invalidSequences = {
        {103, 106},       // START_A, STOP (missing check digit)
        {104, 106},       // START_B, STOP (missing check digit)
        {105, 106},       // START_C, STOP (missing check digit)
        {103, 0, 106},    // START_A, valid check 0, STOP (zero payload)
        {104, 1, 106},    // START_B, valid check 1, STOP (zero payload)
        {105, 2, 106},    // START_C, valid check 2, STOP (zero payload)
    };
    Code128Reader reader = new Code128Reader();
    for (int[] seq : invalidSequences) {
      try {
        reader.decodeRow(0, encodeRawCodes(seq), null);
        fail("Expected NotFoundException for short/empty Code 128 symbol");
      } catch (NotFoundException expected) {
        // expected
      }
    }
  }

  @Test
  public void testRejectsControlOnlyEmptySymbols() throws Exception {
    int[][] controlOnlyPayloads = {
        {105, 101},          // START_C, CODE_A => checksum 0 (printable ' ')
        {104, 101, 98},      // START_B, CODE_A, SHIFT => checksum 92 (printable '|')
        {103, 100},          // START_A, CODE_B => checksum 100 (non-printable)
    };
    for (int[] payload : controlOnlyPayloads) {
      try {
        decodeWithChecksum(false, payload);
        fail("Expected NotFoundException for control-only empty symbol");
      } catch (NotFoundException expected) {
        // expected
      }
    }
  }

  @Test
  public void testControlAndShiftCodewordsBeforeCheckDigit() throws Exception {
    // Trailing CODE_SHIFT followed by function-code check digits (102=FNC1, 97=FNC2, 96=FNC3) and printable check
    Result trailingShiftFnc1 = decodeWithChecksum(true, 104, 8, 98); // '(' + SHIFT, checksum 102
    assertEquals(102, trailingShiftFnc1.getRawBytes()[trailingShiftFnc1.getRawBytes().length - 2] & 0xFF);
    assertEquals("(", trailingShiftFnc1.getText());
    assertEquals("]C0", trailingShiftFnc1.getResultMetadata().get(ResultMetadataType.SYMBOLOGY_IDENTIFIER));
    assertEquals(Boolean.FALSE, trailingShiftFnc1.getResultMetadata().get(ResultMetadataType.READER_INIT));

    Result trailingShiftFnc2 = decodeWithChecksum(false, 104, 3, 98); // '#' + SHIFT, checksum 97
    assertEquals(97, trailingShiftFnc2.getRawBytes()[trailingShiftFnc2.getRawBytes().length - 2] & 0xFF);
    assertEquals("#", trailingShiftFnc2.getText());
    assertEquals("]C0", trailingShiftFnc2.getResultMetadata().get(ResultMetadataType.SYMBOLOGY_IDENTIFIER));

    Result trailingShiftFnc3 = decodeWithChecksum(false, 104, 2, 98); // '"' + SHIFT, checksum 96
    assertEquals(96, trailingShiftFnc3.getRawBytes()[trailingShiftFnc3.getRawBytes().length - 2] & 0xFF);
    assertEquals("\"", trailingShiftFnc3.getText());
    assertEquals(Boolean.FALSE, trailingShiftFnc3.getResultMetadata().get(ResultMetadataType.READER_INIT));

    Result trailingShiftPrintable = decodeWithChecksum(false, 104, 65, 98); // 'a' + SHIFT, checksum 56
    assertEquals("a", trailingShiftPrintable.getText());

    // Trailing CODE_C latch before printable check digit (26 in Code C would emit 2 digits if speculatively decoded)
    Result trailingCodeC = decodeWithChecksum(false, 104, 33, 99); // 'A' + CODE_C, checksum 26
    assertEquals("A", trailingCodeC.getText());

    // Trailing FNC4 before check digit
    Result trailingFnc4 = decodeWithChecksum(false, 104, 33, 100); // 'A' + FNC4_B, checksum 28
    assertEquals("A", trailingFnc4.getText());
  }

  @Test
  public void testAllNonPrintableChecksumsInAllCodeSets() throws Exception {
    for (int startCode = 103; startCode <= 105; startCode++) {
      for (int targetChecksum = 96; targetChecksum <= 102; targetChecksum++) {
        // Choose c2 = 30 and solve (startCode + 1*c1 + 2*30) % 103 == targetChecksum; c1 lands in 34..42.
        int c1 = (targetChecksum - startCode - 60 + 206) % 103;
        int c2 = 30;
        String expected = startCode == 105
            ? String.format("%02d%02d", c1, c2)
            : String.valueOf((char) (' ' + c1)) + (char) (' ' + c2);
        for (boolean assumeGS1 : new boolean[] {false, true}) {
          Result result = decodeWithChecksum(assumeGS1, startCode, c1, c2);
          byte[] raw = result.getRawBytes();
          assertEquals(targetChecksum, raw[raw.length - 2] & 0xFF);
          assertEquals(expected, result.getText());
          assertEquals("]C0", result.getResultMetadata().get(ResultMetadataType.SYMBOLOGY_IDENTIFIER));
          assertEquals(Boolean.FALSE, result.getResultMetadata().get(ResultMetadataType.READER_INIT));
        }
      }
    }
  }

  @Test
  public void testShiftFollowedByCodeSetLatch() throws Exception {
    // START_A, SHIFT, CODE_C, 12 => latch to Code C must not be overwritten by unshift
    Result shiftThenCodeC = decodeWithChecksum(false, 103, 98, 99, 12);
    assertEquals("12", shiftThenCodeC.getText());

    // START_A, SHIFT, CODE_A(101 in Code B), 65 => must stay in Code A (65 => SOH '\u0001')
    Result shiftThenCodeA = decodeWithChecksum(false, 103, 98, 101, 65);
    assertEquals("\u0001", shiftThenCodeA.getText());

    // START_B, SHIFT, CODE_B(100 in Code A), 65 => must stay in Code B (65 => 'a')
    Result shiftThenCodeB = decodeWithChecksum(false, 104, 98, 100, 65);
    assertEquals("a", shiftThenCodeB.getText());
  }

  @Test
  public void testFNC4WithShiftAndCodeC() throws Exception {
    // START_A, FNC4_A(101), SHIFT(98), 'a'(65 in B => 'á'), 'A'(33 in A)
    Result fnc4Shift = decodeWithChecksum(false, 103, 101, 98, 65, 33);
    assertEquals("\u00e1A", fnc4Shift.getText());

    // START_B, FNC4_B, FNC4_B (latch upper), 'a'(65 => 'á'), CODE_C(99), 12, CODE_B(100), 'b'(66 => 'â')
    Result fnc4AcrossCodeC = decodeWithChecksum(false, 104, 100, 100, 65, 99, 12, 100, 66);
    assertEquals("\u00e112\u00e2", fnc4AcrossCodeC.getText());
  }

  private static BitArray encodeRawCodes(int... codes) {
    int quietZone = 10;
    int totalWidth = quietZone * 2;
    for (int code : codes) {
      for (int w : Code128Reader.CODE_PATTERNS[code]) {
        totalWidth += w;
      }
    }
    BitArray row = new BitArray(totalWidth);
    int pos = quietZone;
    for (int code : codes) {
      boolean black = true;
      for (int w : Code128Reader.CODE_PATTERNS[code]) {
        if (black) {
          for (int i = 0; i < w; i++) {
            row.set(pos + i);
          }
        }
        pos += w;
        black = !black;
      }
    }
    return row;
  }

  private static Result decodeWithChecksum(boolean assumeGS1, int... startAndPayload) throws Exception {
    int checksum = startAndPayload[0];
    for (int i = 1; i < startAndPayload.length; i++) {
      checksum = (checksum + i * startAndPayload[i]) % 103;
    }
    int[] full = new int[startAndPayload.length + 2];
    System.arraycopy(startAndPayload, 0, full, 0, startAndPayload.length);
    full[startAndPayload.length] = checksum;
    full[startAndPayload.length + 1] = 106; // CODE_STOP
    Map<DecodeHintType,Object> decodeHints = null;
    if (assumeGS1) {
      decodeHints = new EnumMap<>(DecodeHintType.class);
      decodeHints.put(DecodeHintType.ASSUME_GS1, Boolean.TRUE);
    }
    return new Code128Reader().decodeRow(0, encodeRawCodes(full), decodeHints);
  }

  private static Result decode(String input, String codeSet, boolean assumeGS1) throws Exception {
    Map<EncodeHintType,Object> encodeHints = new EnumMap<>(EncodeHintType.class);
    if (codeSet != null) {
      encodeHints.put(EncodeHintType.FORCE_CODE_SET, codeSet);
    }
    BitMatrix matrix = new Code128Writer().encode(input, BarcodeFormat.CODE_128, 0, 0, encodeHints);
    Map<DecodeHintType,Object> decodeHints = null;
    if (assumeGS1) {
      decodeHints = new EnumMap<>(DecodeHintType.class);
      decodeHints.put(DecodeHintType.ASSUME_GS1, Boolean.TRUE);
    }
    return new Code128Reader().decodeRow(0, matrix.getRow(0, null), decodeHints);
  }
}
