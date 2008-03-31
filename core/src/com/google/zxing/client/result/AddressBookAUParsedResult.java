/*
 * Copyright 2008 Google Inc.
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

import com.google.zxing.Result;

import java.util.Vector;

/**
 * Implements KDDI AU's address book format. See
 * <a href="http://www.au.kddi.com/ezfactory/tec/two_dimensions/index.html">
 * http://www.au.kddi.com/ezfactory/tec/two_dimensions/index.html</a>.
 * (Thanks to Yuzo for translating!)
 *
 * @author srowen@google.com (Sean Owen)
 */
public final class AddressBookAUParsedResult extends ParsedReaderResult {

  private final String[] names;
  private final String[] phoneNumbers;
  private final String[] emails;
  private final String note;
  private final String address;

  private AddressBookAUParsedResult(String[] names, String[] phoneNumbers, String[] emails, String note, String address) {
    super(ParsedReaderResultType.ADDRESSBOOK_AU);
    this.names = names;
    this.phoneNumbers = phoneNumbers;
    this.emails = emails;
    this.note = note;
    this.address = address;
  }

  public static AddressBookAUParsedResult parse(Result result) {
    String rawText = result.getText();
    // MEMORY is mandatory; seems like a decent indicator, as does end-of-record separator CR/LF
    if (rawText == null || rawText.indexOf("MEMORY") < 0 || rawText.indexOf("\r\n") < 0) {
      return null;
    }
    String[] names = matchMultipleValuePrefix("NAME", 2, rawText);
    String[] phoneNumbers = matchMultipleValuePrefix("TEL", 3, rawText);
    String[] emails = matchMultipleValuePrefix("MAIL", 3, rawText);
    String note = AbstractDoCoMoParsedResult.matchSinglePrefixedField("MEMORY", rawText, '\r');
    String address = AbstractDoCoMoParsedResult.matchSinglePrefixedField("ADD", rawText, '\r');
    return new AddressBookAUParsedResult(names, phoneNumbers, emails, note, address);
  }

  private static String[] matchMultipleValuePrefix(String prefix, int max, String rawText) {
    Vector values = null;
    for (int i = 1; i <= max; i++) {
      String value = AbstractDoCoMoParsedResult.matchSinglePrefixedField(prefix + i, rawText, '\r');
      if (value == null) {
        break;
      }
      if (values == null) {
        values = new Vector(max); // lazy init
      }
      values.addElement(value);
    }
    if (values == null) {
      return null;
    }
    String[] result = new String[values.size()];
    for (int i = 0; i < result.length; i++) {
      result[i] = (String) values.elementAt(i);
    }
    return result;
  }

  public String[] getNames() {
    return names;
  }

  public String[] getPhoneNumbers() {
    return phoneNumbers;
  }

  public String[] getEmails() {
    return emails;
  }

  public String getNote() {
    return note;
  }

  public String getAddress() {
    return address;
  }

  public String getDisplayResult() {
    StringBuffer result = new StringBuffer();
    AbstractDoCoMoParsedResult.maybeAppend(names, result);
    AbstractDoCoMoParsedResult.maybeAppend(emails, result);
    AbstractDoCoMoParsedResult.maybeAppend(address, result);
    AbstractDoCoMoParsedResult.maybeAppend(phoneNumbers, result);
    AbstractDoCoMoParsedResult.maybeAppend(note, result);
    return result.toString();
  }

}