/*
 * Copyright 2007 Google Inc.
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

package com.google.zxing.client.result;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.Result;
import junit.framework.TestCase;

/**
 * Tests {@link ParsedReaderResult}.
 *
 * @author srowen@google.com (Sean Owen)
 */
public final class ParsedReaderResultTestCase extends TestCase {

  public void testTextType() {
    doTestResult("foo", ParsedReaderResultType.TEXT);
    doTestResult("", ParsedReaderResultType.TEXT);
    doTestResult("This is a test", ParsedReaderResultType.TEXT);
  }

  public void testBookmarkType() {
    doTestResult("MEBKM:URL:google.com;;", ParsedReaderResultType.BOOKMARK);
    doTestResult("MEBKM:URL:google.com;TITLE:Google;;", ParsedReaderResultType.BOOKMARK);
    doTestResult("MEBKM:TITLE:Google;URL:google.com;;", ParsedReaderResultType.BOOKMARK);
    doTestResult("MEBKM:URL:http://google.com;;", ParsedReaderResultType.BOOKMARK);
    doTestResult("MEBKM:URL:HTTPS://google.com;;", ParsedReaderResultType.BOOKMARK);
  }

  public void testURLTOType() {
    doTestResult("URLTO:foo:bar.com", ParsedReaderResultType.URLTO);
    doTestResult("URLTO::bar.com", ParsedReaderResultType.URLTO);
    doTestResult("URLTO::http://bar.com", ParsedReaderResultType.URLTO);
  }

  public void testEmailType() {
    doTestResult("MATMSG:TO:srowen@example.org;;", ParsedReaderResultType.EMAIL);
    doTestResult("MATMSG:TO:srowen@example.org;SUB:Stuff;;", ParsedReaderResultType.EMAIL);
    doTestResult("MATMSG:TO:srowen@example.org;SUB:Stuff;BODY:This is some text;;", ParsedReaderResultType.EMAIL);
    doTestResult("MATMSG:SUB:Stuff;BODY:This is some text;TO:srowen@example.org;;", ParsedReaderResultType.EMAIL);
    doTestResult("TO:srowen@example.org;SUB:Stuff;BODY:This is some text;;", ParsedReaderResultType.TEXT);
  }

  public void testEmailAddressType() {
    doTestResult("srowen@example.org", ParsedReaderResultType.EMAIL_ADDRESS);
    doTestResult("mailto:srowen@example.org", ParsedReaderResultType.EMAIL_ADDRESS);
    doTestResult("srowen@example", ParsedReaderResultType.TEXT);
    doTestResult("srowen", ParsedReaderResultType.TEXT);
    doTestResult("Let's meet @ 2", ParsedReaderResultType.TEXT);
  }

  public void testAddressBookType() {
    doTestResult("MECARD:N:Sean Owen;;", ParsedReaderResultType.ADDRESSBOOK);
    doTestResult("MECARD:TEL:+12125551212;N:Sean Owen;;", ParsedReaderResultType.ADDRESSBOOK);
    doTestResult("MECARD:TEL:+12125551212;N:Sean Owen;URL:google.com;;", ParsedReaderResultType.ADDRESSBOOK);
    doTestResult("TEL:+12125551212;N:Sean Owen;;", ParsedReaderResultType.TEXT);
  }

  public void testAddressBookAUType() {
    doTestResult("MEMORY:\r\n", ParsedReaderResultType.ADDRESSBOOK_AU);
    doTestResult("MEMORY:foo\r\nNAME1:Sean\r\n", ParsedReaderResultType.ADDRESSBOOK_AU);
    doTestResult("TEL1:+12125551212\r\nMEMORY:\r\n", ParsedReaderResultType.ADDRESSBOOK_AU);
  }

  public void testUPC() {
    doTestResult("123456789012", ParsedReaderResultType.UPC, BarcodeFormat.UPC_A);
    doTestResult("1234567890123", ParsedReaderResultType.UPC, BarcodeFormat.UPC_A);
    doTestResult("12345678901", ParsedReaderResultType.TEXT);
  }

  public void testURI() {
    doTestResult("http://google.com", ParsedReaderResultType.URI);
    doTestResult("google.com", ParsedReaderResultType.URI);
    doTestResult("https://google.com", ParsedReaderResultType.URI);
    doTestResult("HTTP://google.com", ParsedReaderResultType.URI);
    doTestResult("http://google.com/foobar", ParsedReaderResultType.URI);
    doTestResult("https://google.com:443/foobar", ParsedReaderResultType.URI);
    doTestResult("google.com:443/foobar", ParsedReaderResultType.URI);
  }

  public void testGeo() {
    doTestResult("geo:1,2", ParsedReaderResultType.GEO);
    doTestResult("geo:1,2,3", ParsedReaderResultType.GEO);
    doTestResult("geo:100.33,-32.3344,3.35", ParsedReaderResultType.GEO);
    doTestResult("geography", ParsedReaderResultType.TEXT);        
  }

  public void testTel() {
    doTestResult("tel:+15551212", ParsedReaderResultType.TEL);
    doTestResult("tel:212 555 1212", ParsedReaderResultType.TEL);
    doTestResult("tel:2125551212", ParsedReaderResultType.TEL);
    doTestResult("telephone", ParsedReaderResultType.TEXT);
  }

  /*
  public void testNDEFText() {
    doTestResult(new byte[] {(byte)0xD1,(byte)0x01,(byte)0x05,(byte)0x54,
                             (byte)0x02,(byte)0x65,(byte)0x6E,(byte)0x68,
                             (byte)0x69},
                 ParsedReaderResultType.NDEF_TEXT);
  }

  public void testNDEFURI() {
    doTestResult(new byte[] {(byte)0xD1,(byte)0x01,(byte)0x08,(byte)0x55,
                             (byte)0x01,(byte)0x6E,(byte)0x66,(byte)0x63,
                             (byte)0x2E,(byte)0x63,(byte)0x6F,(byte)0x6D},
                 ParsedReaderResultType.NDEF_URI);
  }

  public void testNDEFSmartPoster() {
    doTestResult(new byte[] {(byte)0xD1,(byte)0x02,(byte)0x2F,(byte)0x53,
                             (byte)0x70,(byte)0x91,(byte)0x01,(byte)0x0E,
                             (byte)0x55,(byte)0x01,(byte)0x6E,(byte)0x66,
                             (byte)0x63,(byte)0x2D,(byte)0x66,(byte)0x6F,
                             (byte)0x72,(byte)0x75,(byte)0x6D,(byte)0x2E,
                             (byte)0x6F,(byte)0x72,(byte)0x67,(byte)0x11,
                             (byte)0x03,(byte)0x01,(byte)0x61,(byte)0x63,
                             (byte)0x74,(byte)0x00,(byte)0x51,(byte)0x01,
                             (byte)0x12,(byte)0x54,(byte)0x05,(byte)0x65,
                             (byte)0x6E,(byte)0x2D,(byte)0x55,(byte)0x53,
                             (byte)0x48,(byte)0x65,(byte)0x6C,(byte)0x6C,
                             (byte)0x6F,(byte)0x2C,(byte)0x20,(byte)0x77,
                             (byte)0x6F,(byte)0x72,(byte)0x6C,(byte)0x64},
                 ParsedReaderResultType.NDEF_SMART_POSTER);
  }
   */

  private static void doTestResult(String text, ParsedReaderResultType type) {
    doTestResult(text, type, null);
  }

  private static void doTestResult(String text, ParsedReaderResultType type, BarcodeFormat format) {
    Result fakeResult = new Result(text, null, null, format);
    ParsedReaderResult result = ParsedReaderResult.parseReaderResult(fakeResult);
    assertNotNull(result);
    assertEquals(type, result.getType());
  }

  private static void doTestResult(byte[] rawBytes, ParsedReaderResultType type) {
    Result fakeResult = new Result(null, rawBytes, null, null);
    ParsedReaderResult result = ParsedReaderResult.parseReaderResult(fakeResult);
    assertNotNull(result);
    assertEquals(type, result.getType());
  }

}