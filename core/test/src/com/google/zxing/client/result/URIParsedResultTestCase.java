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

import junit.framework.TestCase;
import com.google.zxing.Result;
import com.google.zxing.BarcodeFormat;

/**
 * Tests {@link URIParsedResult}.
 *
 * @author Sean Owen
 */
public final class URIParsedResultTestCase extends TestCase {

  public void testBookmarkDocomo() {
    doTest("MEBKM:URL:google.com;;", "http://google.com", null);
    doTest("MEBKM:URL:http://google.com;;", "http://google.com", null);    
    doTest("MEBKM:URL:google.com;TITLE:Google;", "http://google.com", "Google");
  }

  public void testURI() {
    doTest("google.com", "http://google.com", null);
    doTest("http://google.com", "http://google.com", null);
    doTest("https://google.com", "https://google.com", null);
    doTest("google.com:443", "http://google.com:443", null);
  }

  public void testURLTO() {
    doTest("urlto::bar.com", "http://bar.com", null);
    doTest("urlto::http://bar.com", "http://bar.com", null);    
    doTest("urlto:foo:bar.com", "http://bar.com", "foo");
  }

  public void testIsPossiblyMalicious() {
    doTestIsPossiblyMalicious("http://google.com", false);
    doTestIsPossiblyMalicious("http://google.com@evil.com", true);
    doTestIsPossiblyMalicious("http://google.com:@evil.com", true);
    doTestIsPossiblyMalicious("google.com:@evil.com", true);
    doTestIsPossiblyMalicious("https://google.com:443", false);
    doTestIsPossiblyMalicious("http://google.com/foo@bar", false);    
  }

  private static void doTest(String contents, String uri, String title) {
    Result fakeResult = new Result(contents, null, null, BarcodeFormat.QR_CODE);
    ParsedResult result = ResultParser.parseResult(fakeResult);
    assertEquals(ParsedResultType.URI, result.getType());
    URIParsedResult uriResult = (URIParsedResult) result;
    assertEquals(uri, uriResult.getURI());
    assertEquals(title, uriResult.getTitle());
  }

  private static void doTestIsPossiblyMalicious(String uri, boolean expected) {
    URIParsedResult result = new URIParsedResult(uri, null);
    assertEquals(expected, result.isPossiblyMaliciousURI());
  }

}