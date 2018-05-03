/*
 * Copyright 2012 ZXing authors
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

package com.google.zxing.common;

import org.junit.Assert;
import org.junit.Test;

import com.google.zxing.DecodeHintType;

import java.io.IOException;
import java.nio.charset.Charset;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;;

public final class StringUtilsTestCase extends Assert {

  @Test
  public void testShortShiftJIS_1() {
    // ÈáëÈ≠ö
    doTest(new byte[] { (byte) 0x8b, (byte) 0xe0, (byte) 0x8b, (byte) 0x9b, }, "SJIS");
  }

  @Test
  public void testShortISO88591_1() {
    // b√•d
    doTest(new byte[] { (byte) 0x62, (byte) 0xe5, (byte) 0x64, }, "ISO-8859-1");
  }

  @Test
  public void testMixedShiftJIS_1() {
    // Hello Èáë!
    doTest(new byte[] { (byte) 0x48, (byte) 0x65, (byte) 0x6c, (byte) 0x6c, (byte) 0x6f,
                        (byte) 0x20, (byte) 0x8b, (byte) 0xe0, (byte) 0x21, },
           "SJIS");
  }
  
  @Test
  public void testBigUTF8_1() throws IOException {
	// Test file utf8.txt under blackbox/encodedtexts
	  byte[] file = Files.readAllBytes(Paths.get("src/test/resources/blackbox/encodedtexts/utf8.txt"));
	  doTest(file, "UTF8");
  }
  
  @Test
  public void testBigISO88591_1() throws IOException {
	// Test file iso.txt under blackbox/encodedtexts
	  byte[] file = Files.readAllBytes(Paths.get("src/test/resources/blackbox/encodedtexts/iso.txt"));
	  doTest(file, "ISO-8859-1");
  }
  
  @Test
  public void testBigShiftJIS_1() throws IOException {
	// Test file sjis.txt under blackbox/encodedtexts
	  byte[] file = Files.readAllBytes(Paths.get("src/test/resources/blackbox/encodedtexts/sjis.txt"));
	  doTest(file, "SJIS");
  }

  @Test
  public void testNotSupported_1() throws IOException {
	// Test file sjis.txt under blackbox/encodedtexts
	  byte[] file1 = Files.readAllBytes(Paths.get("src/test/resources/blackbox/encodedtexts/ibm852.txt"));
	  byte[] file2 = Files.readAllBytes(Paths.get("src/test/resources/blackbox/encodedtexts/windows1250.txt"));
	  doTest(file1, Charset.defaultCharset().name());
	  doTest(file2, Charset.defaultCharset().name());
  }
  
  @Test
  public void testWithHint_1() {
	// Test file sjis.txt under blackbox/encodedtexts
	  byte[] file = {};
	  Charset charset = Charset.forName("SJIS");
	  Map<DecodeHintType,String> hints = new HashMap<DecodeHintType, String>();
	  hints.put(DecodeHintType.CHARACTER_SET, charset.name());
	  doTest(file, charset.name(), hints);
  }
  
  private static void doTest(byte[] bytes, String charsetName, Map<DecodeHintType,?> hints) {
	    Charset charset = Charset.forName(charsetName);
	    String guessedName = StringUtils.guessEncoding(bytes, null);
	    Charset guessedEncoding = Charset.forName(guessedName);
	    assertEquals(charset, guessedEncoding);
  }
  
  private static void doTest(byte[] bytes, String charsetName) {
    Charset charset = Charset.forName(charsetName);
    String guessedName = StringUtils.guessEncoding(bytes, null);
    Charset guessedEncoding = Charset.forName(guessedName);
    assertEquals(charset, guessedEncoding);
  }

  /**
   * Utility for printing out a string in given encoding as a Java statement, since it's better
   * to write that into the Java source file rather than risk character encoding issues in the 
   * source file itself.
   *
   * @param args command line arguments
   */
  public static void main(String[] args) {
    String text = args[0];
    Charset charset = Charset.forName(args[1]);
    StringBuilder declaration = new StringBuilder();
    declaration.append("new byte[] { ");
    for (byte b : text.getBytes(charset)) {
      declaration.append("(byte) 0x");
      declaration.append(Integer.toHexString(b & 0xFF));
      declaration.append(", ");
    }
    declaration.append('}');
    System.out.println(declaration);
  }

}
