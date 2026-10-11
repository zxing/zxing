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
import com.google.zxing.Result;
import com.google.zxing.ResultMetadataType;
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
