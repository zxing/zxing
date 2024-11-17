/*
 * Copyright (C) 2014 ZXing authors
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

package com.google.zxing.pdf417.encoder;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.Writer;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.pdf417.PDF417Writer;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests {@link PDF417HighLevelEncoder}.
 */
public final class PDF417EncoderTestCase extends Assert {

  private static final String PDF417PFX = "\u039f\u001A\u0385";

  @Test
  public void testEncodeAuto() throws Exception {
    String input = "ABCD";
    assertEquals(PDF417PFX + input, checkEncodeAutoWithSpecialChars(input, Compaction.AUTO));
  }
  
  @Test
  public void testEncodeAutoWithSpecialChars() throws Exception {
    // Just check if this does not throw an exception
    checkEncodeAutoWithSpecialChars("1%§s ?aG$", Compaction.AUTO);
    checkEncodeAutoWithSpecialChars("日本語", Compaction.AUTO);
    checkEncodeAutoWithSpecialChars("₸ 5555", Compaction.AUTO);
    checkEncodeAutoWithSpecialChars("€ 123,45", Compaction.AUTO);
    checkEncodeAutoWithSpecialChars("€ 123,45", Compaction.BYTE);
    checkEncodeAutoWithSpecialChars("123,45", Compaction.TEXT);

    // Greek alphabet
    Charset cp437 = Charset.forName("IBM437");
    assertNotNull(cp437);
    byte[] cp437Array = {(byte) 224,(byte) 225,(byte) 226,(byte) 227,(byte) 228}; //αßΓπΣ
    String greek = new String(cp437Array, cp437);
    assertEquals("αßΓπΣ", greek);
    checkEncodeAutoWithSpecialChars(greek, Compaction.AUTO);
    checkEncodeAutoWithSpecialChars(greek, Compaction.BYTE);
    PDF417HighLevelEncoder.encodeHighLevel(greek, Compaction.AUTO, cp437, true);
    PDF417HighLevelEncoder.encodeHighLevel(greek, Compaction.AUTO, cp437, false);
    
    try {
      // detect when a TEXT Compaction is applied to a non text input
      checkEncodeAutoWithSpecialChars("€ 123,45", Compaction.TEXT);
    } catch (WriterException e) {
      assertNotNull(e.getMessage());
      assertTrue(e.getMessage().contains("8364"));
      assertTrue(e.getMessage().contains("Compaction.TEXT"));
      assertTrue(e.getMessage().contains("Compaction.AUTO"));
    }
    
    try {
      // detect when a TEXT Compaction is applied to a non text input
      String input = "Hello! " + (char) 128;
      checkEncodeAutoWithSpecialChars(input, Compaction.TEXT);
    } catch (WriterException e) {
      assertNotNull(e.getMessage());
      assertTrue(e.getMessage().contains("128"));
      assertTrue(e.getMessage().contains("Compaction.TEXT"));
      assertTrue(e.getMessage().contains("Compaction.AUTO"));
    }

    try {
      // detect when a TEXT Compaction is applied to a non text input
      // https://github.com/zxing/zxing/issues/1761
      String content = "€ 123,45";
      Map<EncodeHintType, Object> hints = new HashMap<>();
      hints.put(EncodeHintType.ERROR_CORRECTION, 4);
      hints.put(EncodeHintType.PDF417_DIMENSIONS, new Dimensions(7, 7, 1, 300));
      hints.put(EncodeHintType.MARGIN, 0);
      hints.put(EncodeHintType.CHARACTER_SET, "ISO-8859-15");
      hints.put(EncodeHintType.PDF417_COMPACTION, Compaction.TEXT);
      
      (new MultiFormatWriter()).encode(content, BarcodeFormat.PDF_417, 200, 100, hints);
    } catch (WriterException e) {
      assertNotNull(e.getMessage());
      assertTrue(e.getMessage().contains("8364"));
      assertTrue(e.getMessage().contains("Compaction.TEXT"));
      assertTrue(e.getMessage().contains("Compaction.AUTO"));
    }
  }
  
  public String checkEncodeAutoWithSpecialChars(String input, Compaction compaction) throws Exception {
    return PDF417HighLevelEncoder.encodeHighLevel(input, compaction, StandardCharsets.UTF_8, false);
  }

  @Test
  public void testCheckCharset() throws Exception {
    String input = "Hello!";
    String errorMessage = UUID.randomUUID().toString();
    
    // no exception
    PDF417HighLevelEncoder.checkCharset(input,255,errorMessage);
    PDF417HighLevelEncoder.checkCharset(input,1255,errorMessage);
    PDF417HighLevelEncoder.checkCharset(input,111,errorMessage);
    
    try {
      // should throw an exception for character 'o' because it exceeds upper limit 110
      PDF417HighLevelEncoder.checkCharset(input,110,errorMessage);
    } catch (WriterException e) {
      assertNotNull(e.getMessage());
      assertTrue(e.getMessage().contains("111"));
      assertTrue(e.getMessage().contains(errorMessage));
    }
  }
  
  @Test
  public void testEncodeIso88591WithSpecialChars() throws Exception {
    // Just check if this does not throw an exception
    PDF417HighLevelEncoder.encodeHighLevel("asdfg§asd", Compaction.AUTO, StandardCharsets.ISO_8859_1, false);
  }

  @Test
  public void testEncodeText() throws Exception {
    String encoded = PDF417HighLevelEncoder.encodeHighLevel(
        "ABCD", Compaction.TEXT, StandardCharsets.UTF_8, false);
    assertEquals("Ο\u001A\u0001?", encoded);
  }

  @Test
  public void testEncodeNumeric() throws Exception {
    String encoded = PDF417HighLevelEncoder.encodeHighLevel(
        "1234", Compaction.NUMERIC, StandardCharsets.UTF_8, false);
    assertEquals("\u039f\u001A\u0386\f\u01b2", encoded);
  }

  @Test
  public void testEncodeByte() throws Exception {
    String encoded = PDF417HighLevelEncoder.encodeHighLevel(
        "abcd", Compaction.BYTE, StandardCharsets.UTF_8, false);
    assertEquals("\u039f\u001A\u0385abcd", encoded);
  }

  @Test(expected = WriterException.class)
  public void testEncodeEmptyString() throws Exception {
    PDF417HighLevelEncoder.encodeHighLevel("", Compaction.AUTO, null, false);
  }

  @Test
  public void testDimensions() throws Exception {
    // test https://github.com/zxing/zxing/issues/1831
    String input = "0000000001000000022200000003330444400888888881010101010";
    testDimensions(input, new Dimensions(1,  30,  7, 10));
    testDimensions(input, new Dimensions(1,  40,  1,  7));
    testDimensions(input, new Dimensions(10, 30,  1,  5));
    testDimensions(input, new Dimensions(1,   3,  1, 15));
    testDimensions(input, new Dimensions(5,  30,  7,  7));
    testDimensions(input, new Dimensions(12, 12,  1, 17));
    testDimensions(input, new Dimensions(1,  30,  7,  7));
  }

  public static void testDimensions(String input, Dimensions dimensions) throws Exception {
    int sourceCodeWords = 20;
    int errorCorrectionCodeWords = 8;
    
    int[] calculated = PDF417.determineDimensions(dimensions.getMinCols(), dimensions.getMaxCols(), 
          dimensions.getMinRows(), dimensions.getMaxRows(),
          sourceCodeWords, errorCorrectionCodeWords);
    
    assertNotNull(calculated);
    assertEquals(2,calculated.length);
    assertTrue(dimensions.getMinCols() <= calculated[0]);
    assertTrue(dimensions.getMaxCols() >= calculated[0]);
    assertTrue(dimensions.getMinRows() <= calculated[1]);
    assertTrue(dimensions.getMaxRows() >= calculated[1]);
    assertNotNull(generatePDF417BitMatrix(input, 371, null, dimensions));
  }

  public static BitMatrix generatePDF417BitMatrix(final String barcodeText, final int width, 
        final Integer heightRequested, final Dimensions dimensions) throws WriterException {
    final Writer barcodeWriter = new PDF417Writer();
    final int height = heightRequested == null ? width / 4 : heightRequested;
    Map<EncodeHintType, Object> hints = new HashMap<>();
    hints.put(EncodeHintType.MARGIN, 0);
    hints.put(EncodeHintType.PDF417_DIMENSIONS, dimensions);
    return barcodeWriter.encode(barcodeText, BarcodeFormat.PDF_417, width, height, hints);
  }
}
