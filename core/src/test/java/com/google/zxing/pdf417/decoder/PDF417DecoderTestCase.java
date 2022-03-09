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
import java.nio.charset.Charset;
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
    performEncodeTest('A', new int[] { 3, 4, 5, 6, 4, 4, 5, 5});
  }

  @Test
  public void testNumeric() throws WriterException, FormatException {
    performEncodeTest('1', new int[] { 2, 3, 3, 4, 4, 5, 5, 6, 6, 7, 7, 8, 7, 7, 8, 8, 8, 9, 9, 9, 10, 10});
  }

  @Test
  public void testByte() throws WriterException, FormatException {
    performEncodeTest('\u00c4', new int[] { 3, 4, 5, 6, 7, 7, 8});
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
    performEncodeTest(';', new int[] { 3, 4, 5, 6, 6, 7, 8});
    encodeDecode(";;;;;;;;;;;;;;;;", 17);
  }

  @Test
  public void testUppercaseLowercaseMix2() throws WriterException, FormatException {
    performPermutationTest(new char[] {'A', 'a'}, 10, 8972);
  }

  @Test
  public void testUppercaseNumericMix() throws WriterException, FormatException {
    performPermutationTest(new char[] {'A', '1'}, 14, 192510);
  }

  @Test
  public void testUppercaseMixedMix() throws WriterException, FormatException {
    performPermutationTest(new char[] {'A', '1', ' ', ';'}, 7, 106060);
  }

  @Test
  public void testUppercasePunctuationMix() throws WriterException, FormatException {
    performPermutationTest(new char[] {'A', ';'}, 10, 8967);
  }

  @Test
  public void testUppercaseByteMix() throws WriterException, FormatException {
    performPermutationTest(new char[] {'A', '\u00c4'}, 10, 11222);
  }

  @Test
  public void testLowercaseByteMix() throws WriterException, FormatException {
    performPermutationTest(new char[] {'a', '\u00c4'}, 10, 11233);
  }

  public void testUppercaseLowercaseNumericMix() throws WriterException, FormatException {
    performPermutationTest(new char[] {'A', 'a', '1'}, 7, 15491);
  }

  @Test
  public void testUppercaseLowercasePunctuationMix() throws WriterException, FormatException {
    performPermutationTest(new char[] {'A', 'a', ';'}, 7, 15491);
  }

  @Test
  public void testUppercaseLowercaseByteMix() throws WriterException, FormatException {
    performPermutationTest(new char[] {'A', 'a', '\u00c4'}, 7, 17288);
  }

  @Test
  public void testLowercasePunctuationByteMix() throws WriterException, FormatException {
    performPermutationTest(new char[] {'a', ';', '\u00c4'}, 7, 17427);
  }

  @Test
  public void testUppercaseLowercaseNumericPunctuationMix() throws WriterException, FormatException {
    performPermutationTest(new char[] {'A', 'a', '1', ';'}, 7, 120479);
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
    assertEquals(4190044, total);
  }

  @Test
  public void testECIEnglishHiragana() throws Exception {
    //multi ECI UTF-8, UTF-16 and ISO-8859-1
    performECITest(new char[] {'a', '1', '\u3040'}, new float[] {20f, 1f, 10f}, 105825, 110914);
  }

  @Test
  public void testECIEnglishKatakana() throws Exception {
    //multi ECI UTF-8, UTF-16 and ISO-8859-1
    performECITest(new char[] {'a', '1', '\u30a0'}, new float[] {20f, 1f, 10f}, 109177, 110914);
  }

  @Test
  public void testECIEnglishHalfWidthKatakana() throws Exception {
    //single ECI
    performECITest(new char[] {'a', '1', '\uff80'}, new float[] {20f, 1f, 10f}, 80617, 110914);
  }

  @Test
  public void testECIEnglishChinese() throws Exception {
    //single ECI
    performECITest(new char[] {'a', '1', '\u4e00'}, new float[] {20f, 1f, 10f}, 95797, 110914);
  }

  @Test
  public void testECIGermanCyrillic() throws Exception {
    //single ECI since the German Umlaut is in ISO-8859-1
    performECITest(new char[] {'a', '1', '\u00c4', '\u042f'}, new float[] {20f, 1f, 1f, 10f}, 80755, 96007);
  }

  @Test
  public void testECIEnglishCzechCyrillic1() throws Exception {
    //multi ECI between ISO-8859-2 and ISO-8859-5
    performECITest(new char[] {'a', '1', '\u010c', '\u042f'}, new float[] {10f, 1f, 10f, 10f}, 102824, 124525);
  }

  @Test
  public void testECIEnglishCzechCyrillic2() throws Exception {
    //multi ECI between ISO-8859-2 and ISO-8859-5
    performECITest(new char[] {'a', '1', '\u010c', '\u042f'}, new float[] {40f, 1f, 10f, 10f}, 81321, 88236);
  }

  @Test
  public void testECIEnglishArabicCyrillic() throws Exception {
    //multi ECI between UTF-8 (ISO-8859-6 is excluded in CharacterSetECI) and ISO-8859-5
    performECITest(new char[] {'a', '1', '\u0620', '\u042f'}, new float[] {10f, 1f, 10f, 10f}, 118510, 124525);
  }

  @Test
  public void testBinaryMultiECI() throws Exception {
    //Test the cases described in 5.5.5.3 "ECI and Byte Compaction mode using latch 924 and 901"
    performDecodeTest(new int[] {5, 927, 4, 913, 200}, "\u010c");
    performDecodeTest(new int[] {9, 927, 4, 913, 200, 927, 7, 913, 207}, "\u010c\u042f");
    performDecodeTest(new int[] {9, 927, 4, 901, 200, 927, 7, 901, 207}, "\u010c\u042f");
    performDecodeTest(new int[] {8, 927, 4, 901, 200, 927, 7, 207}, "\u010c\u042f");
    performDecodeTest(new int[] {14, 927, 4, 901, 200, 927, 7, 207, 927, 4, 200, 927, 7, 207},
         "\u010c\u042f\u010c\u042f");
    performDecodeTest(new int[] {16, 927, 4, 924, 336, 432, 197, 51, 300, 927, 7, 348, 231, 311, 858, 567},
        "\u010c\u010c\u010c\u010c\u010c\u010c\u042f\u042f\u042f\u042f\u042f\u042f");
  }

  private static void encodeDecode(String input, int expectedLength) throws WriterException, FormatException {
    assertEquals(expectedLength, encodeDecode(input));
  }

  private static int encodeDecode(String input) throws WriterException, FormatException {
    return encodeDecode(input, null, false, true);
  }

  private static int encodeDecode(String input, Charset charset, boolean autoECI, boolean decode)
      throws WriterException, FormatException {
    String s = PDF417HighLevelEncoderTestAdapter.encodeHighLevel(input, Compaction.AUTO, charset, autoECI);
    if (decode) {
      int[] codewords = new int[s.length() + 1];
      codewords[0] = codewords.length;
      for (int i = 1; i < codewords.length; i++) {
        codewords[i] = s.charAt(i - 1);
      }
      performDecodeTest(codewords, input);
    }
    return s.length() + 1;
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

  private static void performPermutationTest(char[] chars, int length, int expectedTotal) throws WriterException,
      FormatException {
    int endIndex = getEndIndex(length, chars);
    int total = 0;
    for (int i = 0; i < endIndex; i++) {
      total += encodeDecode(generatePermutation(i, length, chars));
    }
    assertEquals(expectedTotal, total);
  }

  private static void performEncodeTest(char c, int[] expectedLengths) throws WriterException, FormatException {
    for (int i = 0; i < expectedLengths.length; i++) {
      StringBuilder sb = new StringBuilder();
      for (int j = 0; j <= i; j++) {
        sb.append(c);
      }
      encodeDecode(sb.toString(), expectedLengths[i]);
    }
  }

  private static void performDecodeTest(int[] codewords, String expectedResult) throws FormatException {
    DecoderResult result = DecodedBitStreamParser.decode(codewords, "0");
    assertEquals(expectedResult, result.getText());
  }

  private static void performECITest(char[] chars,
                               float[] weights,
                               int expectedMinLength,
                               int expectedUTFLength) throws WriterException, FormatException {
    Random random = new Random(0);
    int minLength = 0;
    int utfLength = 0;
    for (int i = 0; i < 1000; i++) {
      String s = generateText(random, 100, chars, weights);
      minLength += encodeDecode(s, null, true, true);
      utfLength += encodeDecode(s, StandardCharsets.UTF_8, false, true);
    }
    assertEquals(expectedMinLength, minLength);
    assertEquals(expectedUTFLength, utfLength);
  }

  private static String generateText(Random random, int maxWidth, char[] chars, float[] weights) {
    StringBuilder result = new StringBuilder();
    final int maxWordWidth = 7;
    float total = 0;
    for (int i = 0; i < weights.length; i++) {
      total += weights[i];
    }
    for (int i = 0; i < weights.length; i++) {
      weights[i] /= total;
    }
    int cnt = 0;
    do {
      float maxValue = 0;
      int maxIndex = 0;
      for (int j = 0; j < weights.length; j++) {
        float value = random.nextFloat() * weights[j];
        if (value > maxValue) {
          maxValue = value;
          maxIndex = j;
        }
      }
      final float wordLength = maxWordWidth * random.nextFloat();
      if (wordLength > 0 && result.length() > 0) {
        result.append(' ');
      }
      for (int j = 0; j < wordLength; j++) {
        char c = chars[maxIndex];
        if (j == 0 && c >= 'a' && c <= 'z' && random.nextBoolean()) {
          c = (char) (c - 'a' + 'A');
        }
        result.append(c);
      }
      if (cnt % 2 != 0 && random.nextBoolean()) {
        result.append('.');
      }
      cnt++;
    } while (result.length() < maxWidth - maxWordWidth);
    return result.toString();
  }
}
