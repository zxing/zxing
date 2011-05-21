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
 * Tests {@link WifiParsedResult}.
 *
 * @author Vikram Aggarwal
 */
public final class WifiParsedResultTestCase extends Assert {

  @Test
  public void testNoPassword() {
    doTest("WIFI:S:NoPassword;P:;T:;;", "NoPassword", "", "");
    doTest("WIFI:S:No Password;P:;T:;;", "No Password", "", "");
  }

  @Test
  public void testWep() {
    doTest("WIFI:S:TenChars;P:0123456789;T:WEP;;", "TenChars", "0123456789", "WEP");
    doTest("WIFI:S:TenChars;P:abcde56789;T:WEP;;", "TenChars", "abcde56789", "WEP");
    // Non hex should not fail at this level
    doTest("WIFI:S:TenChars;P:hellothere;T:WEP;;", "TenChars", "hellothere", "WEP");

    // Escaped semicolons
    doTest("WIFI:S:Ten\\;\\;Chars;P:0123456789;T:WEP;;", "Ten;;Chars", "0123456789", "WEP");
    // Escaped colons
    doTest("WIFI:S:Ten\\:\\:Chars;P:0123456789;T:WEP;;", "Ten::Chars", "0123456789", "WEP");

    // TODO(vikrama) Need a test for SB as well.
  }

  /**
   * Put in checks for the length of the password for wep.
   */
  @Test
  public void testWpa() {
    doTest("WIFI:S:TenChars;P:wow;T:WPA;;", "TenChars", "wow", "WPA");
    doTest("WIFI:S:TenChars;P:space is silent;T:WPA;;", "TenChars", "space is silent", "WPA");
    doTest("WIFI:S:TenChars;P:hellothere;T:WEP;;", "TenChars", "hellothere", "WEP");

    // Escaped semicolons
    doTest("WIFI:S:TenChars;P:hello\\;there;T:WEP;;", "TenChars", "hello;there", "WEP");
    // Escaped colons
    doTest("WIFI:S:TenChars;P:hello\\:there;T:WEP;;", "TenChars", "hello:there", "WEP");
  }

  /**
   * Given the string contents for the barcode, check that it matches our expectations
   */
  private static void doTest(String contents,
                             String ssid,
                             String password,
                             String type) {
    Result fakeResult = new Result(contents, null, null, BarcodeFormat.QR_CODE);
    ParsedResult result = ResultParser.parseResult(fakeResult);

    // Ensure it is a wifi code
    assertSame(ParsedResultType.WIFI, result.getType());
    WifiParsedResult wifiResult = (WifiParsedResult) result;

    assertEquals(ssid, wifiResult.getSsid());
    assertEquals(password, wifiResult.getPassword());
    assertEquals(type, wifiResult.getNetworkEncryption());
  }
}