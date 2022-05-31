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
 * Tests {@link SMSParsedResult}.
 *
 * @author Sean Owen
 */
public final class SMSMMSParsedResultTestCase extends Assert {

  @Test
  public void testSMS() {
    doTest("sms:+15551212", "+15551212", null, null, null, "sms:+15551212");
    doTest("sms:+15551212?subject=foo&body=bar", "+15551212", "foo", "bar", null,
           "sms:+15551212?body=bar&subject=foo");
    doTest("sms:+15551212;via=999333", "+15551212", null, null, "999333",
           "sms:+15551212;via=999333");
  }

  @Test
  public void testMMS() {
    doTest("mms:+15551212", "+15551212", null, null, null, "sms:+15551212");
    doTest("mms:+15551212?subject=foo&body=bar", "+15551212", "foo", "bar", null,
           "sms:+15551212?body=bar&subject=foo");
    doTest("mms:+15551212;via=999333", "+15551212", null, null, "999333",
           "sms:+15551212;via=999333");
  }

  private static void doTest(String contents,
                             String number,
                             String subject,
                             String body,
                             String via,
                             String parsedURI) {
    Result fakeResult = new Result(contents, null, null, BarcodeFormat.QR_CODE);
    ParsedResult result = ResultParser.parseResult(fakeResult);
    assertSame(ParsedResultType.SMS, result.getType());
    SMSParsedResult smsResult = (SMSParsedResult) result;
    assertArrayEquals(new String[] { number }, smsResult.getNumbers());
    assertEquals(subject, smsResult.getSubject());
    assertEquals(body, smsResult.getBody());
    assertArrayEquals(new String[] { via }, smsResult.getVias());
    assertEquals(parsedURI, smsResult.getSMSURI());
  }

}
