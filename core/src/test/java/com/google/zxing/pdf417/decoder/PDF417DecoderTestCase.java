/*
 * Copyright 2009 ZXing authors
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

package com.google.zxing.pdf417.decoder;

import com.google.zxing.FormatException;
import com.google.zxing.WriterException;
import com.google.zxing.pdf417.PDF417ResultMetadata;
import com.google.zxing.common.DecoderResult;
import com.google.zxing.pdf417.encoder.Compaction;
import com.google.zxing.pdf417.encoder.PDF417HighLevelEncoderTestAdapter;

import org.junit.Assert;
import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.util.Random;

/**
 * Tests {@link DecodedBitStreamParser}.
 */
public class PDF417DecoderTestCase extends Assert {

  /**
   * Tests the first sample given in ISO/IEC 15438:2015(E) - Annex H.4
   */
  @Test
  public void testStandardSample1() throws FormatException {
    PDF417ResultMetadata resultMetadata = new PDF417ResultMetadata();
    int[] sampleCodes = {20, 928, 111, 100, 17, 53, 923, 1, 111, 104, 923, 3, 64, 416, 34, 923, 4, 258, 446, 67,
      // we should never reach these
      1000, 1000, 1000};

    DecodedBitStreamParser.decodeMacroBlock(sampleCodes, 2, resultMetadata);

    assertEquals(0, resultMetadata.getSegmentIndex());
    assertEquals("017053", resultMetadata.getFileId());
    assertFalse(resultMetadata.isLastSegment());
    assertEquals(4, resultMetadata.getSegmentCount());
    assertEquals("CEN BE", resultMetadata.getSender());
    assertEquals("ISO CH", resultMetadata.getAddressee());

    @SuppressWarnings("deprecation")
    int[] optionalData = resultMetadata.getOptionalData();
    assertEquals("first element of optional array should be the first field identifier", 1, optionalData[0]);
    assertEquals("last element of optional array should be the last codeword of the last field",
        67, optionalData[optionalData.length - 1]);
  }


  /**
   * Tests the second given in ISO/IEC 15438:2015(E) - Annex H.4
   */
  @Test
  public void testStandardSample2() throws FormatException {
    PDF417ResultMetadata resultMetadata = new PDF417ResultMetadata();
    int[] sampleCodes = {11, 928, 111, 103, 17, 53, 923, 1, 111, 104, 922,
      // we should never reach these
      1000, 1000, 1000};

    DecodedBitStreamParser.decodeMacroBlock(sampleCodes, 2, resultMetadata);

    assertEquals(3, resultMetadata.getSegmentIndex());
    assertEquals("017053", resultMetadata.getFileId());
    assertTrue(resultMetadata.isLastSegment());
    assertEquals(4, resultMetadata.getSegmentCount());
    assertNull(resultMetadata.getAddressee());
    assertNull(resultMetadata.getSender());

    @SuppressWarnings("deprecation")
    int[] optionalData = resultMetadata.getOptionalData();
    assertEquals("first element of optional array should be the first field identifier", 1, optionalData[0]);
    assertEquals("last element of optional array should be the last codeword of the last field",
        104, optionalData[optionalData.length - 1]);
  }


  /**
   * Tests the example given in ISO/IEC 15438:2015(E) - Annex H.6
   */
  @Test
  public void testStandardSample3() throws FormatException {
    PDF417ResultMetadata resultMetadata = new PDF417ResultMetadata();
    int[] sampleCodes = {7, 928, 111, 100, 100, 200, 300,
      0}; // Final dummy ECC codeword required to avoid ArrayIndexOutOfBounds

    DecodedBitStreamParser.decodeMacroBlock(sampleCodes, 2, resultMetadata);

    assertEquals(0, resultMetadata.getSegmentIndex());
    assertEquals("100200300", resultMetadata.getFileId());
    assertFalse(resultMetadata.isLastSegment());
    assertEquals(-1, resultMetadata.getSegmentCount());
    assertNull(resultMetadata.getAddressee());
    assertNull(resultMetadata.getSender());
    assertNull(resultMetadata.getOptionalData());

    // Check that symbol containing no data except Macro is accepted (see note in Annex H.2)
    DecoderResult decoderResult = DecodedBitStreamParser.decode(sampleCodes, "0");
    assertEquals("", decoderResult.getText());
    assertNotNull(decoderResult.getOther());
  }

  @Test
  public void testSampleWithFilename() throws FormatException {
    int[] sampleCodes = {23, 477, 928, 111, 100, 0, 252, 21, 86, 923, 0, 815, 251, 133, 12, 148, 537, 593,
        599, 923, 1, 111, 102, 98, 311, 355, 522, 920, 779, 40, 628, 33, 749, 267, 506, 213, 928, 465, 248,
        493, 72, 780, 699, 780, 493, 755, 84, 198, 628, 368, 156, 198, 809, 19, 113};
    PDF417ResultMetadata resultMetadata = new PDF417ResultMetadata();

    DecodedBitStreamParser.decodeMacroBlock(sampleCodes, 3, resultMetadata);

    assertEquals(0, resultMetadata.getSegmentIndex());
    assertEquals("000252021086", resultMetadata.getFileId());
    assertFalse(resultMetadata.isLastSegment());
    assertEquals(2, resultMetadata.getSegmentCount());
    assertNull(resultMetadata.getAddressee());
    assertNull(resultMetadata.getSender());
    assertEquals("filename.txt", resultMetadata.getFileName());
  }

  @Test
  public void testSampleWithNumericValues() throws FormatException {
    int[] sampleCodes = {25, 477, 928, 111, 100, 0, 252, 21, 86, 923, 2, 2, 0, 1, 0, 0, 0, 923, 5, 130, 923,
        6, 1, 500, 13, 0};
    PDF417ResultMetadata resultMetadata = new PDF417ResultMetadata();

    DecodedBitStreamParser.decodeMacroBlock(sampleCodes, 3, resultMetadata);

    assertEquals(0, resultMetadata.getSegmentIndex());
    assertEquals("000252021086", resultMetadata.getFileId());
    assertFalse(resultMetadata.isLastSegment());

    assertEquals(180980729000000L, resultMetadata.getTimestamp());
    assertEquals(30, resultMetadata.getFileSize());
    assertEquals(260013, resultMetadata.getChecksum());
  }

  @Test
  public void testSampleWithMacroTerminatorOnly() throws FormatException {
    int[] sampleCodes = {7, 477, 928, 222, 198, 0, 922};
    PDF417ResultMetadata resultMetadata = new PDF417ResultMetadata();

    DecodedBitStreamParser.decodeMacroBlock(sampleCodes, 3, resultMetadata);

    assertEquals(99998, resultMetadata.getSegmentIndex());
    assertEquals("000", resultMetadata.getFileId());
    assertTrue(resultMetadata.isLastSegment());
    assertEquals(-1, resultMetadata.getSegmentCount());
    assertNull(resultMetadata.getOptionalData());
  }

  @Test(expected = FormatException.class)
  public void testSampleWithBadSequenceIndexMacro() throws FormatException {
    int[] sampleCodes = {3, 928, 222, 0};
    PDF417ResultMetadata resultMetadata = new PDF417ResultMetadata();
    DecodedBitStreamParser.decodeMacroBlock(sampleCodes, 2, resultMetadata);
  }

  @Test(expected = FormatException.class)
  public void testSampleWithNoFileIdMacro() throws FormatException {
    int[] sampleCodes = {4, 928, 222, 198, 0};
    PDF417ResultMetadata resultMetadata = new PDF417ResultMetadata();
    DecodedBitStreamParser.decodeMacroBlock(sampleCodes, 2, resultMetadata);
  }

  @Test(expected = FormatException.class)
  public void testSampleWithNoDataNoMacro() throws FormatException {
    int[] sampleCodes = {3, 899, 899, 0};
    DecodedBitStreamParser.decode(sampleCodes, "0");
  }

  @Test
  public void testUppercase() throws WriterException, FormatException {
    //encodeDecode("", 0);
    encodeDecode("A", 3);
    encodeDecode("AB", 4);
    encodeDecode("ABC", 5);
    encodeDecode("ABCD", 6);
    encodeDecode("ABCDE", 4);
    encodeDecode("ABCDEF", 4);
    encodeDecode("ABCDEFG", 5);
    encodeDecode("ABCDEFGH", 5);
  }

  @Test
  public void testNumeric() throws WriterException, FormatException {
    encodeDecode("1", 2);
    encodeDecode("12", 3);
    encodeDecode("123", 3);
    encodeDecode("1234", 4);
    encodeDecode("12345", 4);
    encodeDecode("123456", 5);
    encodeDecode("1234567", 5);
    encodeDecode("12345678", 6);
    encodeDecode("123456789", 6);
    encodeDecode("1234567890", 7);
    encodeDecode("12345678901", 7);
    encodeDecode("123456789012", 8);
    encodeDecode("1234567890123", 7);
    encodeDecode("12345678901234", 7);
    encodeDecode("123456789012345", 8);
    encodeDecode("1234567890123456", 8);
    encodeDecode("12345678901234567", 8);
    encodeDecode("123456789012345678", 9);
    encodeDecode("1234567890123456789", 9);
    encodeDecode("12345678901234567890", 9);
    encodeDecode("123456789012345678901", 10);
    encodeDecode("1234567890123456789012", 10);
  }

  @Test
  public void testByte() throws WriterException, FormatException {
    encodeDecode("\u00c4", 3);
    encodeDecode("\u00c4\u00c4", 4);
    encodeDecode("\u00c4\u00c4\u00c4", 5);
    encodeDecode("\u00c4\u00c4\u00c4\u00c4", 6);
    encodeDecode("\u00c4\u00c4\u00c4\u00c4\u00c4", 7);
    encodeDecode("\u00c4\u00c4\u00c4\u00c4\u00c4\u00c4", 7);
    encodeDecode("\u00c4\u00c4\u00c4\u00c4\u00c4\u00c4\u00c4", 8);
  }

  @Test
  public void testUppercaseLowercaseMix1() throws WriterException, FormatException {
    encodeDecode("aA", 4);
    encodeDecode("aAa", 5);
    encodeDecode("Aa", 4);
    encodeDecode("Aaa", 5);
    encodeDecode("AaA", 5);
    encodeDecode("AaaA", 6);
    encodeDecode("Aaaa", 6);
    encodeDecode("AaAaA", 5);
    encodeDecode("AaaAaaA", 6);
    encodeDecode("AaaAAaaA", 7);
  }

  @Test
  public void testPunctuation() throws WriterException, FormatException {
    encodeDecode(";", 3);
    encodeDecode(";;", 4);
    encodeDecode(";;;", 5);
    encodeDecode(";;;;", 6);
    encodeDecode(";;;;;", 6);
    encodeDecode(";;;;;;", 7);
    encodeDecode(";;;;;;;", 8);
    encodeDecode(";;;;;;;;", 9);
    encodeDecode(";;;;;;;;;;;;;;;;", 17);
  }

  @Test
  public void testUppercaseLowercaseMix2() throws WriterException, FormatException {
    performPermutationTest(new char[] {'A','a'},10,8972);
  }

  @Test
  public void testUppercaseNumericMix() throws WriterException, FormatException {
    performPermutationTest(new char[] {'A','1'},14,192510);
  }

  @Test
  public void testUppercaseMixedMix() throws WriterException, FormatException {
    performPermutationTest(new char[] {'A','1',' ',';'},7,106060);
  }

  @Test
  public void testUppercasePunctuationMix() throws WriterException, FormatException {
    performPermutationTest(new char[] {'A',';'},10,8967);
  }

  @Test
  public void testUppercaseByteMix() throws WriterException, FormatException {
    performPermutationTest(new char[] {'A','\u00c4'},10,11222);
  }

  @Test
  public void testLowercaseByteMix() throws WriterException, FormatException {
    performPermutationTest(new char[] {'a','\u00c4'},10,11233);
  }

  public void testUppercaseLowercaseNumericMix() throws WriterException, FormatException {
    performPermutationTest(new char[] {'A','a','1'},7,15491);
  }

  @Test
  public void testUppercaseLowercasePunctuationMix() throws WriterException, FormatException {
    performPermutationTest(new char[] {'A','a',';'},7,15491);
  }

  @Test
  public void testUppercaseLowercaseByteMix() throws WriterException, FormatException {
    performPermutationTest(new char[] {'A','a','\u00c4'},7,17288);
  }

  @Test
  public void testLowercasePunctuationByteMix() throws WriterException, FormatException {
    performPermutationTest(new char[] {'a',';','\u00c4'},7,17427);
  }

  @Test
  public void testUppercaseLowercaseNumericPunctuationMix() throws WriterException, FormatException {
    performPermutationTest(new char[] {'A','a','1',';'},7,120479);
  }

  @Test
  public void testBinaryData() throws WriterException, FormatException {
    byte[] bytes = new byte[500];
    Random random = new Random(0);
    int total = 0;
    for (int i = 0; i < 10000; i++) {
      random.nextBytes(bytes);
      total += encodeDecode(new String(bytes, StandardCharsets.ISO_8859_1));
    }
    assertEquals(4190044,total); 
  }

  private static void encodeDecode(String input,int expectedLength) throws WriterException, FormatException {
    assertEquals(expectedLength, encodeDecode(input));
  }

  private static int encodeDecode(String input) throws WriterException, FormatException {
    String s = PDF417HighLevelEncoderTestAdapter.encodeHighLevel(input, Compaction.AUTO, null);
    int[] codewords = new int[s.length() + 1];
    codewords[0] = codewords.length;
    for (int i = 1; i < codewords.length; i++) {
      codewords[i] = s.charAt(i - 1);
    }
    DecoderResult result = DecodedBitStreamParser.decode(codewords, "0");

    assertEquals(input, result.getText());
    return codewords.length;
  }

  private static int getEndIndex(int length, char[] chars) {
    double decimalLength = Math.log10(chars.length);
    return (int) Math.ceil(Math.pow(10, decimalLength * length));
  }

  private static String generatePermutation(int index, int length, char[] chars) {
    int N = chars.length;
    String baseNNumber = Integer.toString(index, N);
    while (baseNNumber.length() < length) {
      baseNNumber = "0" + baseNNumber;
    }
    String prefix = "";
    for (int i = 0; i < baseNNumber.length(); i++) {
      prefix += chars[baseNNumber.charAt(i) - '0'];
    }
    return prefix;
  }

  private static void performPermutationTest(char[] chars,int length,int expectedTotal) throws WriterException,
      FormatException {
    int endIndex = getEndIndex(length, chars);
    int total = 0;
    for (int i = 0; i < endIndex; i++) {
      total += encodeDecode(generatePermutation(i, length, chars));
    }
    assertEquals(expectedTotal,total);
  }

}
