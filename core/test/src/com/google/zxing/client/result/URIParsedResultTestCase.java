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
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests {@link URIParsedResult}.
 *
 * @author Sean Owen
 */
public final class URIParsedResultTestCase extends Assert {

  @Test
  public void testBookmarkDocomo() {
    doTest("MEBKM:URL:google.com;;", "http://google.com", null);
    doTest("MEBKM:URL:http://google.com;;", "http://google.com", null);
    doTest("MEBKM:URL:google.com;TITLE:Google;", "http://google.com", "Google");
  }

  @Test
  public void testURI() {
    doTest("google.com", "http://google.com", null);
    doTest("http://google.com", "http://google.com", null);
    doTest("https://google.com", "https://google.com", null);
    doTest("google.com:443", "http://google.com:443", null);
    doTest("https://www.google.com/calendar/hosted/google.com/embed?mode=AGENDA&force_login=true&src=google.com_726f6f6d5f6265707075@resource.calendar.google.com",
           "https://www.google.com/calendar/hosted/google.com/embed?mode=AGENDA&force_login=true&src=google.com_726f6f6d5f6265707075@resource.calendar.google.com",
           null);
    doTest("otpauth://remoteaccess?devaddr=00%a1b2%c3d4&devname=foo&key=bar",
           "otpauth://remoteaccess?devaddr=00%a1b2%c3d4&devname=foo&key=bar",
           null);
    doTest("s3://amazon.com:8123", "s3://amazon.com:8123", null);
    doTest("HTTP://R.BEETAGG.COM/?12345", "http://R.BEETAGG.COM/?12345", null);
  }

  @Test
  public void testNotURI() {
    doTestNotUri("google.c");
    doTestNotUri(".com");
    doTestNotUri(":80/");
    doTestNotUri("ABC,20.3,AB,AD");
  }

  @Test
  public void testURLTO() {
    doTest("urlto::bar.com", "http://bar.com", null);
    doTest("urlto::http://bar.com", "http://bar.com", null);
    doTest("urlto:foo:bar.com", "http://bar.com", "foo");
  }

  @Test
  public void testGarbage() {
    doTestNotUri("Da65cV1g^>%^f0bAbPn1CJB6lV7ZY8hs0Sm:DXU0cd]GyEeWBz8]bUHLB");
    doTestNotUri("DEA\u0003\u0019M\u0006\u0000\b√•\u0000¬áHO\u0000X$\u0001\u0000\u001Fwfc\u0007!√æ¬ì¬ò" +
                 "\u0013\u0013¬æZ{√π√é√ù√ö¬óZ¬ß¬®+y_zb√±k\u00117¬∏\u000E¬Ü√ú\u0000\u0000\u0000\u0000" +
                 "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000" +
                 "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000¬£.ux");
  }

  @Test
  public void testIsPossiblyMalicious() {
    doTestIsPossiblyMalicious("http://google.com", false);
    doTestIsPossiblyMalicious("http://google.com@evil.com", true);
    doTestIsPossiblyMalicious("http://google.com:@evil.com", true);
    doTestIsPossiblyMalicious("google.com:@evil.com", false);
    doTestIsPossiblyMalicious("https://google.com:443", false);
    doTestIsPossiblyMalicious("https://google.com:443/", false);
    doTestIsPossiblyMalicious("https://evil@google.com:443", true);
    doTestIsPossiblyMalicious("http://google.com/foo@bar", false);
    doTestIsPossiblyMalicious("http://google.com/@@", false);
  }

  private static void doTest(String contents, String uri, String title) {
    Result fakeResult = new Result(contents, null, null, BarcodeFormat.QR_CODE);
    ParsedResult result = ResultParser.parseResult(fakeResult);
    assertSame(ParsedResultType.URI, result.getType());
    URIParsedResult uriResult = (URIParsedResult) result;
    assertEquals(uri, uriResult.getURI());
    assertEquals(title, uriResult.getTitle());
  }
  
  private static void doTestNotUri(String text) {
    Result fakeResult = new Result(text, null, null, BarcodeFormat.QR_CODE);
    ParsedResult result = ResultParser.parseResult(fakeResult);
    assertSame(ParsedResultType.TEXT, result.getType());
    assertEquals(text, result.getDisplayResult());
  }

  private static void doTestIsPossiblyMalicious(String uri, boolean expected) {
    URIParsedResult result = new URIParsedResult(uri, null);
    assertEquals(expected, result.isPossiblyMaliciousURI());
  }

}