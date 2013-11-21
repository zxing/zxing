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
 * Tests {@link TelParsedResult}.
 *
 * @author Sean Owen
 */
public final class TelParsedResultTestCase extends Assert {

  @Test
  public void testTel() {
    doTest("tel:+15551212", "+15551212", null);
    doTest("tel:2125551212", "2125551212", null);
  }

  private static void doTest(String contents, String number, String title) {
    Result fakeResult = new Result(contents, null, null, BarcodeFormat.QR_CODE);
    ParsedResult result = ResultParser.parseResult(fakeResult);
    assertSame(ParsedResultType.TEL, result.getType());
    TelParsedResult telResult = (TelParsedResult) result;
    assertEquals(number, telResult.getNumber());
    assertEquals(title, telResult.getTitle());
    assertEquals("tel:" + number, telResult.getTelURI());
  }

}