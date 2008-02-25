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

  public void testUPC() {
    doTestResult("123456789012", ParsedReaderResultType.UPC);
    doTestResult("1234567890123", ParsedReaderResultType.UPC);
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

  private static void doTestResult(String text, ParsedReaderResultType type) {
    ParsedReaderResult result = ParsedReaderResult.parseReaderResult(text);
    assertNotNull(result);
    assertEquals(type, result.getType());
  }

}