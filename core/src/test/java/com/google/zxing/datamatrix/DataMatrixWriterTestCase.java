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

package com.google.zxing.datamatrix;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.EncodeHintType;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.ResultMetadataType;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.datamatrix.encoder.HighLevelEncoder;
import com.google.zxing.datamatrix.encoder.SymbolShapeHint;
import com.google.zxing.oned.Code128Writer;
import com.google.zxing.qrcode.QRCodeWriter;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * @author satorux@google.com (Satoru Takabayashi) - creator
 * @author dswitkin@google.com (Daniel Switkin) - ported and expanded from C++
 */
public final class DataMatrixWriterTestCase extends Assert {

  @Test
  public void testDataMatrixImageWriter() {

    Map<EncodeHintType,Object> hints = new EnumMap<>(EncodeHintType.class);
    hints.put(EncodeHintType.DATA_MATRIX_SHAPE, SymbolShapeHint.FORCE_SQUARE);

    int bigEnough = 64;
    DataMatrixWriter writer = new DataMatrixWriter();
    BitMatrix matrix = writer.encode("Hello Google", BarcodeFormat.DATA_MATRIX, bigEnough, bigEnough, hints);
    assertNotNull(matrix);
    assertTrue(bigEnough >= matrix.getWidth());
    assertTrue(bigEnough >= matrix.getHeight());
  }

  @Test
  public void testDataMatrixWriter() {

    Map<EncodeHintType,Object> hints = new EnumMap<>(EncodeHintType.class);
    hints.put(EncodeHintType.DATA_MATRIX_SHAPE, SymbolShapeHint.FORCE_SQUARE);

    int bigEnough = 14;
    DataMatrixWriter writer = new DataMatrixWriter();
    BitMatrix matrix = writer.encode("Hello Me", BarcodeFormat.DATA_MATRIX, bigEnough, bigEnough, hints);
    assertNotNull(matrix);
    assertEquals(bigEnough, matrix.getWidth());
    assertEquals(bigEnough, matrix.getHeight());
  }

  @Test
  public void testDataMatrixTooSmall() {
    // The DataMatrix will not fit in this size, so the matrix should come back bigger
    int tooSmall = 8;
    DataMatrixWriter writer = new DataMatrixWriter();
    BitMatrix matrix = writer.encode("http://www.google.com/", BarcodeFormat.DATA_MATRIX, tooSmall, tooSmall, null);
    assertNotNull(matrix);
    assertTrue(tooSmall < matrix.getWidth());
    assertTrue(tooSmall < matrix.getHeight());
  }

  @Test
  public void testGS1DataMatrixStandardAndCompactRoundTrip() throws Exception {
    String gs1Data = "010123456789012810LOT42\u001d17260101";
    Map<DecodeHintType,Object> gs1DecodeHints = Collections.singletonMap(DecodeHintType.ASSUME_GS1, Boolean.TRUE);

    for (boolean compact : new boolean[] {false, true}) {
      Map<EncodeHintType,Object> encodeHints = new EnumMap<>(EncodeHintType.class);
      encodeHints.put(EncodeHintType.GS1_FORMAT, Boolean.TRUE);
      if (compact) {
        encodeHints.put(EncodeHintType.DATA_MATRIX_COMPACT, Boolean.TRUE);
      }
      BitMatrix matrix = new DataMatrixWriter().encode(gs1Data, BarcodeFormat.DATA_MATRIX, 0, 0, encodeHints);
      BinaryBitmap bitmap = toBitmap(matrix);

      Result withoutHint = new DataMatrixReader().decode(bitmap);
      assertEquals("\u001d" + gs1Data, withoutHint.getText());
      assertEquals("]d2", withoutHint.getResultMetadata().get(ResultMetadataType.SYMBOLOGY_IDENTIFIER));

      Result withHint = new DataMatrixReader().decode(bitmap, gs1DecodeHints);
      assertEquals("]d2" + gs1Data, withHint.getText());
      assertEquals("]d2", withHint.getResultMetadata().get(ResultMetadataType.SYMBOLOGY_IDENTIFIER));

      // Re-encoding the decoded "\u001d..." text with GS1_FORMAT must not duplicate the leading FNC1
      BitMatrix roundTripped = new DataMatrixWriter().encode(
          withoutHint.getText(), BarcodeFormat.DATA_MATRIX, 0, 0, encodeHints);
      Result roundTrippedResult = new DataMatrixReader().decode(toBitmap(roundTripped), gs1DecodeHints);
      assertEquals("]d2" + gs1Data, roundTrippedResult.getText());
      assertEquals("]d2", roundTrippedResult.getResultMetadata().get(ResultMetadataType.SYMBOLOGY_IDENTIFIER));
    }
  }

  @Test
  public void testGS1DataMatrixStandardModesAndForceC40() throws Exception {
    Map<DecodeHintType,Object> gs1DecodeHints = Collections.singletonMap(DecodeHintType.ASSUME_GS1, Boolean.TRUE);

    // Standard mode with Force C40, Text, and Base 256 transitions across GS
    String[] payloads = {
        "ABCDEFGH\u001dIJKLMNOPQR",
        "abcdefgh\u001dijklmnopqr",
        "\u00e4\u00f6\u00fc\u00e4\u00f6\u00fc\u001d\u00e4\u00f6\u00fc\u00e4\u00f6\u00fc",
        "ABCDEFGH\u009dIJKLMNOPQR",
        "\u001d"
    };
    for (boolean forceC40 : new boolean[] {false, true}) {
      for (String payload : payloads) {
        Map<EncodeHintType,Object> encodeHints = new EnumMap<>(EncodeHintType.class);
        encodeHints.put(EncodeHintType.GS1_FORMAT, Boolean.TRUE);
        if (forceC40) {
          encodeHints.put(EncodeHintType.FORCE_C40, Boolean.TRUE);
        }
        BitMatrix matrix = new DataMatrixWriter().encode(payload, BarcodeFormat.DATA_MATRIX, 0, 0, encodeHints);
        Result withHint = new DataMatrixReader().decode(toBitmap(matrix), gs1DecodeHints);
        String expectedBody = payload.startsWith("\u001d") ? payload.substring(1) : payload;
        assertEquals("]d2" + expectedBody, withHint.getText());
        assertEquals("]d2", withHint.getResultMetadata().get(ResultMetadataType.SYMBOLOGY_IDENTIFIER));
      }
    }

    // Verify HighLevelEncoder emits FNC1 codeword 232 (not literal ASCII 30 or Base256 0x1D)
    String encodedBase256 = HighLevelEncoder.encodeHighLevel(
        "\u00e4\u00f6\u00fc\u00e4\u00f6\u00fc\u001d\u00e4\u00f6\u00fc\u00e4\u00f6\u00fc",
        SymbolShapeHint.FORCE_NONE, null, null, false, true);
    assertEquals(232, encodedBase256.charAt(0));
    assertTrue("Expected internal FNC1 codeword 232 after Base 256 segment",
        encodedBase256.indexOf((char) 232, 1) > 0);
  }

  @Test
  public void testNonGS1DataMatrixWithLiteralGSNotMarkedAsGS1() throws Exception {
    String literalGsData = "\u001d0101234567890128\u001d17260101";
    Map<DecodeHintType,Object> gs1DecodeHints = Collections.singletonMap(DecodeHintType.ASSUME_GS1, Boolean.TRUE);

    for (boolean compact : new boolean[] {false, true}) {
      Map<EncodeHintType,Object> encodeHints = new EnumMap<>(EncodeHintType.class);
      if (compact) {
        encodeHints.put(EncodeHintType.DATA_MATRIX_COMPACT, Boolean.TRUE);
      }
      BitMatrix matrix = new DataMatrixWriter().encode(literalGsData, BarcodeFormat.DATA_MATRIX, 0, 0, encodeHints);
      Result result = new DataMatrixReader().decode(toBitmap(matrix), gs1DecodeHints);
      assertEquals(literalGsData, result.getText());
      assertEquals("]d1", result.getResultMetadata().get(ResultMetadataType.SYMBOLOGY_IDENTIFIER));
    }
  }

  @Test
  public void testCrossFormatAssumeGS1Consistency() throws Exception {
    String gs1Payload = "010123456789012810LOT42\u001d17260101";
    Map<DecodeHintType,Object> gs1DecodeHints = Collections.singletonMap(DecodeHintType.ASSUME_GS1, Boolean.TRUE);
    MultiFormatReader reader = new MultiFormatReader();

    // GS1-128
    BitMatrix code128 = new Code128Writer().encode(
        "\u00f1010123456789012810LOT42\u00f117260101", BarcodeFormat.CODE_128, 0, 10);
    Result c128Result = reader.decode(toBitmap(code128), gs1DecodeHints);
    assertEquals("]C1" + gs1Payload, c128Result.getText());
    assertEquals("]C1", c128Result.getResultMetadata().get(ResultMetadataType.SYMBOLOGY_IDENTIFIER));

    // GS1 DataMatrix (standard and compact)
    Map<EncodeHintType,Object> dmHints = new EnumMap<>(EncodeHintType.class);
    dmHints.put(EncodeHintType.GS1_FORMAT, Boolean.TRUE);
    Result dmStd = reader.decode(
        toBitmap(new DataMatrixWriter().encode(gs1Payload, BarcodeFormat.DATA_MATRIX, 0, 0, dmHints)), gs1DecodeHints);
    assertEquals("]d2" + gs1Payload, dmStd.getText());
    assertEquals("]d2", dmStd.getResultMetadata().get(ResultMetadataType.SYMBOLOGY_IDENTIFIER));

    dmHints.put(EncodeHintType.DATA_MATRIX_COMPACT, Boolean.TRUE);
    Result dmCompact = reader.decode(
        toBitmap(new DataMatrixWriter().encode(gs1Payload, BarcodeFormat.DATA_MATRIX, 0, 0, dmHints)), gs1DecodeHints);
    assertEquals("]d2" + gs1Payload, dmCompact.getText());
    assertEquals("]d2", dmCompact.getResultMetadata().get(ResultMetadataType.SYMBOLOGY_IDENTIFIER));

    // GS1 QR Code (Alphanumeric with '%' as FNC1 separator, standard and compact)
    Map<EncodeHintType,Object> qrHints = new EnumMap<>(EncodeHintType.class);
    qrHints.put(EncodeHintType.GS1_FORMAT, Boolean.TRUE);
    String qrGs1Alnum = "010123456789012810LOT42%17260101";
    Result qrStd = reader.decode(
        toBitmap(new QRCodeWriter().encode(qrGs1Alnum, BarcodeFormat.QR_CODE, 0, 0, qrHints)), gs1DecodeHints);
    assertEquals("]Q3" + gs1Payload, qrStd.getText());
    assertEquals("]Q3", qrStd.getResultMetadata().get(ResultMetadataType.SYMBOLOGY_IDENTIFIER));

    qrHints.put(EncodeHintType.QR_COMPACT, Boolean.TRUE);
    Result qrCompact = reader.decode(
        toBitmap(new QRCodeWriter().encode(qrGs1Alnum, BarcodeFormat.QR_CODE, 0, 0, qrHints)), gs1DecodeHints);
    assertEquals("]Q3" + gs1Payload, qrCompact.getText());
    assertEquals("]Q3", qrCompact.getResultMetadata().get(ResultMetadataType.SYMBOLOGY_IDENTIFIER));
  }

  private static BinaryBitmap toBitmap(BitMatrix matrix) {
    int quietZone = 4;
    int scale = 4;
    int width = (matrix.getWidth() + 2 * quietZone) * scale;
    int height = (matrix.getHeight() + 2 * quietZone) * scale;
    byte[] pixels = new byte[width * height];
    Arrays.fill(pixels, (byte) 0xFF);
    for (int y = 0; y < matrix.getHeight(); y++) {
      for (int x = 0; x < matrix.getWidth(); x++) {
        if (matrix.get(x, y)) {
          for (int dy = 0; dy < scale; dy++) {
            int row = ((y + quietZone) * scale + dy) * width;
            for (int dx = 0; dx < scale; dx++) {
              pixels[row + (x + quietZone) * scale + dx] = 0;
            }
          }
        }
      }
    }
    LuminanceSource source = new PlanarYUVLuminanceSource(pixels, width, height, 0, 0, width, height, false);
    return new BinaryBitmap(new HybridBinarizer(source));
  }

}
