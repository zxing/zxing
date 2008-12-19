/*
 * Copyright 2007 ZXing authors
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
 * Tests {@link ParsedResult}.
 *
 * @author Sean Owen
 */
public final class ParsedReaderResultTestCase extends TestCase {

  public void testTextType() {
    doTestResult("foo", ParsedResultType.TEXT);
    doTestResult("", ParsedResultType.TEXT);
    doTestResult("This is a test", ParsedResultType.TEXT);
  }

  public void testBookmarkType() {
    doTestResult("MEBKM:URL:google.com;;", ParsedResultType.URI);
    doTestResult("MEBKM:URL:google.com;TITLE:Google;;", ParsedResultType.URI);
    doTestResult("MEBKM:TITLE:Google;URL:google.com;;", ParsedResultType.URI);
    doTestResult("MEBKM:URL:http://google.com;;", ParsedResultType.URI);
    doTestResult("MEBKM:URL:HTTPS://google.com;;", ParsedResultType.URI);
  }

  public void testURLTOType() {
    doTestResult("urlto:foo:bar.com", ParsedResultType.URI);
    doTestResult("URLTO:foo:bar.com", ParsedResultType.URI);
    doTestResult("URLTO::bar.com", ParsedResultType.URI);
    doTestResult("URLTO::http://bar.com", ParsedResultType.URI);
  }

  public void testEmailType() {
    doTestResult("MATMSG:TO:srowen@example.org;;", ParsedResultType.EMAIL_ADDRESS);
    doTestResult("MATMSG:TO:srowen@example.org;SUB:Stuff;;", ParsedResultType.EMAIL_ADDRESS);
    doTestResult("MATMSG:TO:srowen@example.org;SUB:Stuff;BODY:This is some text;;", ParsedResultType.EMAIL_ADDRESS);
    doTestResult("MATMSG:SUB:Stuff;BODY:This is some text;TO:srowen@example.org;;", ParsedResultType.EMAIL_ADDRESS);
    doTestResult("TO:srowen@example.org;SUB:Stuff;BODY:This is some text;;", ParsedResultType.TEXT);
  }

  public void testEmailAddressType() {
    doTestResult("srowen@example.org", ParsedResultType.EMAIL_ADDRESS);
    doTestResult("mailto:srowen@example.org", ParsedResultType.EMAIL_ADDRESS);
    doTestResult("MAILTO:srowen@example.org", ParsedResultType.EMAIL_ADDRESS);
    doTestResult("srowen@example", ParsedResultType.TEXT);
    doTestResult("srowen", ParsedResultType.TEXT);
    doTestResult("Let's meet @ 2", ParsedResultType.TEXT);
  }

  public void testAddressBookType() {
    doTestResult("MECARD:N:Sean Owen;;", ParsedResultType.ADDRESSBOOK);
    doTestResult("MECARD:TEL:+12125551212;N:Sean Owen;;", ParsedResultType.ADDRESSBOOK);
    doTestResult("MECARD:TEL:+12125551212;N:Sean Owen;URL:google.com;;", ParsedResultType.ADDRESSBOOK);
    doTestResult("N:Sean Owen;TEL:+12125551212;;", ParsedResultType.TEXT);
  }

  public void testAddressBookAUType() {
    doTestResult("MEMORY:\r\n", ParsedResultType.ADDRESSBOOK);
    doTestResult("MEMORY:foo\r\nNAME1:Sean\r\n", ParsedResultType.ADDRESSBOOK);
    doTestResult("TEL1:+12125551212\r\nMEMORY:\r\n", ParsedResultType.ADDRESSBOOK);
  }

  public void testUPC() {
    doTestResult("123456789012", ParsedResultType.PRODUCT, BarcodeFormat.UPC_A);
    doTestResult("1234567890123", ParsedResultType.PRODUCT, BarcodeFormat.UPC_A);
    doTestResult("12345678901", ParsedResultType.TEXT);
  }

  public void testEAN() {
    doTestResult("00393157", ParsedResultType.PRODUCT, BarcodeFormat.EAN_8);
    doTestResult("00393158", ParsedResultType.TEXT);
    doTestResult("5051140178499", ParsedResultType.PRODUCT, BarcodeFormat.EAN_13);
    doTestResult("5051140178490", ParsedResultType.TEXT);
  }

  public void testISBN() {
    doTestResult("9784567890123", ParsedResultType.ISBN, BarcodeFormat.EAN_13);
    doTestResult("9794567890123", ParsedResultType.ISBN, BarcodeFormat.EAN_13);
    doTestResult("97845678901", ParsedResultType.TEXT);
    doTestResult("97945678901", ParsedResultType.TEXT);
  }

  public void testURI() {
    doTestResult("http://google.com", ParsedResultType.URI);
    doTestResult("google.com", ParsedResultType.URI);
    doTestResult("https://google.com", ParsedResultType.URI);
    doTestResult("HTTP://google.com", ParsedResultType.URI);
    doTestResult("http://google.com/foobar", ParsedResultType.URI);
    doTestResult("https://google.com:443/foobar", ParsedResultType.URI);
    doTestResult("google.com:443/foobar", ParsedResultType.URI);
  }

  public void testGeo() {
    doTestResult("geo:1,2", ParsedResultType.GEO);
    doTestResult("GEO:1,2", ParsedResultType.GEO);
    doTestResult("geo:1,2,3", ParsedResultType.GEO);
    doTestResult("geo:100.33,-32.3344,3.35", ParsedResultType.GEO);
    doTestResult("geography", ParsedResultType.TEXT);
  }

  public void testTel() {
    doTestResult("tel:+15551212", ParsedResultType.TEL);
    doTestResult("TEL:+15551212", ParsedResultType.TEL);
    doTestResult("tel:212 555 1212", ParsedResultType.TEL);
    doTestResult("tel:2125551212", ParsedResultType.TEL);
    doTestResult("tel:212-555-1212", ParsedResultType.TEL);
    doTestResult("telephone", ParsedResultType.TEXT);
  }

  public void testVCard() {
    doTestResult("BEGIN:VCARD\r\nEND:VCARD", ParsedResultType.ADDRESSBOOK);
    doTestResult("BEGIN:VCARD\r\nN:Owen;Sean\r\nEND:VCARD", ParsedResultType.ADDRESSBOOK);
    doTestResult("BEGIN:VCARD\r\nVERSION:2.1\r\nN:Owen;Sean\r\nEND:VCARD", ParsedResultType.ADDRESSBOOK);
    doTestResult("BEGIN:VCARD\r\nADR;HOME:123 Main St\r\nVERSION:2.1\r\nN:Owen;Sean\r\n" +
                 "END:VCARD", ParsedResultType.ADDRESSBOOK);    
    doTestResult("BEGIN:VCARD", ParsedResultType.URI); // yeah we end up guessing "URI" here
  }

  public void testVEvent() {
    // UTC times
    doTestResult("BEGIN:VCALENDAR\r\nBEGIN:VEVENT\r\nSUMMARY:foo\r\nDTSTART:20080504T123456Z\r\n" +
                 "DTEND:20080505T234555Z\r\nEND:VEVENT\r\nEND:VCALENDAR", ParsedResultType.CALENDAR);
    doTestResult("BEGIN:VEVENT\r\nSUMMARY:foo\r\nDTSTART:20080504T123456Z\r\n" +
                 "DTEND:20080505T234555Z\r\nEND:VEVENT", ParsedResultType.CALENDAR);
    // Local times
    doTestResult("BEGIN:VEVENT\r\nSUMMARY:foo\r\nDTSTART:20080504T123456\r\n" +
        "DTEND:20080505T234555\r\nEND:VEVENT", ParsedResultType.CALENDAR);
    // Date only (all day event)
    doTestResult("BEGIN:VEVENT\r\nSUMMARY:foo\r\nDTSTART:20080504\r\n" +
        "DTEND:20080505\r\nEND:VEVENT", ParsedResultType.CALENDAR);
    // Start time only
    doTestResult("BEGIN:VEVENT\r\nSUMMARY:foo\r\nDTSTART:20080504T123456Z\r\nEND:VEVENT",
        ParsedResultType.CALENDAR);
    doTestResult("BEGIN:VEVENT\r\nSUMMARY:foo\r\nDTSTART:20080504T123456\r\nEND:VEVENT",
        ParsedResultType.CALENDAR);
    doTestResult("BEGIN:VEVENT\r\nSUMMARY:foo\r\nDTSTART:20080504\r\nEND:VEVENT",
        ParsedResultType.CALENDAR);
    doTestResult("BEGIN:VEVENT\r\nDTEND:20080505T\r\nEND:VEVENT", ParsedResultType.TEXT);
    // Make sure illegal entries without newlines don't crash
    doTestResult("BEGIN:VEVENTSUMMARY:EventDTSTART:20081030T122030ZDTEND:20081030T132030ZEND:VEVENT",
        ParsedResultType.URI);
    doTestResult("BEGIN:VEVENT", ParsedResultType.URI); // See above note on why this is URI
  }

  public void testSMS() {
    doTestResult("sms:+15551212", ParsedResultType.SMS);
    doTestResult("SMS:+15551212", ParsedResultType.SMS);
    doTestResult("SMSTO:+15551212", ParsedResultType.SMS);
    doTestResult("smsto:+15551212", ParsedResultType.SMS);
    doTestResult("sms:+15551212;via=999333", ParsedResultType.SMS);
    doTestResult("sms:+15551212?subject=foo&body=bar", ParsedResultType.SMS);
    doTestResult("sms:+15551212:subject", ParsedResultType.SMS);
    // Need to handle question mark in the subject
    doTestResult("sms:+15551212:What's up?", ParsedResultType.SMS);
    doTestResult("sms:212-555-1212:Here's a longer message. Should be fine.", ParsedResultType.SMS);
  }

  public void testMMS() {
    doTestResult("mms:+15551212", ParsedResultType.SMS);
    doTestResult("MMS:+15551212", ParsedResultType.SMS);
    doTestResult("MMSTO:+15551212", ParsedResultType.SMS);
    doTestResult("mmsto:+15551212", ParsedResultType.SMS);
    doTestResult("mms:+15551212;via=999333", ParsedResultType.SMS);
    doTestResult("mms:+15551212?subject=foo&body=bar", ParsedResultType.SMS);
    doTestResult("mms:+15551212:subject", ParsedResultType.SMS);
    doTestResult("mms:+15551212:What's up?", ParsedResultType.SMS);
    doTestResult("mms:212-555-1212:Here's a longer message. Should be fine.", ParsedResultType.SMS);
  }

  /*
  public void testNDEFText() {
    doTestResult(new byte[] {(byte)0xD1,(byte)0x01,(byte)0x05,(byte)0x54,
                             (byte)0x02,(byte)0x65,(byte)0x6E,(byte)0x68,
                             (byte)0x69},
                 ParsedResultType.TEXT);
  }

  public void testNDEFURI() {
    doTestResult(new byte[] {(byte)0xD1,(byte)0x01,(byte)0x08,(byte)0x55,
                             (byte)0x01,(byte)0x6E,(byte)0x66,(byte)0x63,
                             (byte)0x2E,(byte)0x63,(byte)0x6F,(byte)0x6D},
                 ParsedResultType.URI);
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
                 ParsedResultType.NDEF_SMART_POSTER);
  }
   */

  private static void doTestResult(String text, ParsedResultType type) {
    doTestResult(text, type, null);
  }

  private static void doTestResult(String text, ParsedResultType type, BarcodeFormat format) {
    Result fakeResult = new Result(text, null, null, format);
    ParsedResult result = ResultParser.parseResult(fakeResult);
    assertNotNull(result);
    assertEquals(type, result.getType());
  }

  /*
  private static void doTestResult(byte[] rawBytes, ParsedResultType type) {
    Result fakeResult = new Result(null, rawBytes, null, null);
    ParsedResult result = ResultParser.parseResult(fakeResult);
    assertNotNull(result);
    assertEquals(type, result.getType());
  }
   */

}