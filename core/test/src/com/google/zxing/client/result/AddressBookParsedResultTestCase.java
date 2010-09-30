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

import java.util.Arrays;

/**
 * Tests {@link AddressBookParsedResult}.
 *
 * @author Sean Owen
 */
public final class AddressBookParsedResultTestCase extends Assert {

  @Test
  public void testAddressBookDocomo() {
    doTest("MECARD:N:Sean Owen;;", null, new String[] {"Sean Owen"}, null, null, null, null, null, null, null, null);
    doTest("MECARD:NOTE:ZXing Team;N:Sean Owen;URL:google.com;EMAIL:srowen@example.org;;",
        null, new String[] {"Sean Owen"}, null, null, new String[] {"srowen@example.org"}, null, null,
        "google.com", null, "ZXing Team");
  }

  @Test
  public void testAddressBookAU() {
    doTest("MEMORY:foo\r\nNAME1:Sean\r\nTEL1:+12125551212\r\n",
        null, new String[] {"Sean"}, null, null, null, new String[] {"+12125551212"}, null, null, null, "foo");
  }

  @Test
  public void testVCard() {
    doTest("BEGIN:VCARD\r\nADR;HOME:123 Main St\r\nVERSION:2.1\r\nN:Owen;Sean\r\nEND:VCARD",
           null, new String[] {"Sean Owen"}, null, new String[] {"123 Main St"}, null, null, null, null, null, null);
  }

  @Test
  public void testBizcard() {
    doTest("BIZCARD:N:Sean;X:Owen;C:Google;A:123 Main St;M:+12125551212;E:srowen@example.org;",
        null, new String[] {"Sean Owen"}, null, new String[] {"123 Main St"}, new String[] {"srowen@example.org"},
        new String[] {"+12125551212"}, "Google", null, null, null);
  }

  @Test
  public void testSeveralAddresses() {
    doTest("MECARD:N:Foo Bar;ORG:Company;TEL:5555555555;EMAIL:foo.bar@xyz.com;ADR:City, 10001;" +
           "ADR:City, 10001;NOTE:This is the memo.;;",
           null, new String[] {"Foo Bar"}, null, new String[] {"City, 10001", "City, 10001"},
           new String[] {"foo.bar@xyz.com"},
           new String[] {"5555555555" }, "Company", null, null, "This is the memo.");
  }

  @Test
  public void testQuotedPrintable() {
    doTest("BEGIN:VCARD\r\nADR;HOME;CHARSET=UTF-8;ENCODING=QUOTED-PRINTABLE:;;" +
           "=35=38=20=4C=79=6E=62=72=6F=6F=6B=0D=0A=43=\r\n" +
           "=4F=20=36=39=39=\r\n" +
           "=32=36;;;\r\nEND:VCARD",
           null, null, null, new String[] {"58 Lynbrook\r\nCO 69926"},
           null, null, null, null, null, null);
  }

  private static void doTest(String contents,
                             String title,
                             String[] names,
                             String pronunciation,
                             String[] addresses,
                             String[] emails,
                             String[] phoneNumbers,
                             String org,
                             String url,
                             String birthday,
                             String note) {
    Result fakeResult = new Result(contents, null, null, BarcodeFormat.QR_CODE);
    ParsedResult result = ResultParser.parseResult(fakeResult);
    assertSame(ParsedResultType.ADDRESSBOOK, result.getType());
    AddressBookParsedResult addressResult = (AddressBookParsedResult) result;
    assertEquals(title, addressResult.getTitle());
    assertTrue(Arrays.equals(names, addressResult.getNames()));
    assertEquals(pronunciation, addressResult.getPronunciation());
    assertTrue(Arrays.equals(addresses, addressResult.getAddresses()));
    assertTrue(Arrays.equals(emails, addressResult.getEmails()));
    assertTrue(Arrays.equals(phoneNumbers, addressResult.getPhoneNumbers()));
    assertEquals(org, addressResult.getOrg());
    assertEquals(url, addressResult.getURL());
    assertEquals(birthday, addressResult.getBirthday());
    assertEquals(note, addressResult.getNote());
  }

}