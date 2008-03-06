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

/**
 * Implements the "MECARD" address book entry format.
 *
 * Supported keys: N, TEL, EMAIL, NOTE, ADR Unsupported keys: SOUND, TEL-AV, BDAY, URL, NICKNAME
 *
 * Except for TEL, multiple values for keys are also not supported;
 * the first one found takes precedence.
 *
 * @author srowen@google.com (Sean Owen)
 */
public final class AddressBookDoCoMoResult extends AbstractDoCoMoResult {

  private final String name;
  private final String[] phoneNumbers;
  private final String email;
  private final String note;
  private final String address;

  private AddressBookDoCoMoResult(String name, String[] phoneNumbers, String email, String note, String address) {
    super(ParsedReaderResultType.ADDRESSBOOK);
    this.name = name;
    this.phoneNumbers = phoneNumbers;
    this.email = email;
    this.note = note;
    this.address = address;
  }

  public static AddressBookDoCoMoResult parse(String rawText) {
    if (!rawText.startsWith("MECARD:")) {
      return null;
    }
    String[] rawName = matchPrefixedField("N:", rawText);
    if (rawName == null) {
      return null;
    }
    String name = parseName(rawName[0]);
    String[] phoneNumbers = matchPrefixedField("TEL:", rawText);
    String email = matchSinglePrefixedField("EMAIL:", rawText);
    String note = matchSinglePrefixedField("NOTE:", rawText);
    String address = matchSinglePrefixedField("ADR:", rawText);
    return new AddressBookDoCoMoResult(name, phoneNumbers, email, note, address);
  }

  public String getName() {
    return name;
  }

  public String[] getPhoneNumbers() {
    return phoneNumbers;
  }

  public String getEmail() {
    return email;
  }

  public String getNote() {
    return note;
  }

  public String getAddress() {
    return address;
  }

  public String getDisplayResult() {
    StringBuffer result = new StringBuffer(name);
    maybeAppend(email, result);
    maybeAppend(address, result);
    maybeAppend(phoneNumbers, result);
    maybeAppend(note, result);
    return result.toString();
  }

  private static String parseName(String name) {
    int comma = name.indexOf((int) ',');
    if (comma >= 0) {
      // Format may be last,first; switch it around
      return name.substring(comma + 1) + ' ' + name.substring(0, comma);
    }
    return name;
  }

}