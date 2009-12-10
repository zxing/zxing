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

/**
 * @author Sean Owen
 */
public final class AddressBookParsedResult extends ParsedResult {

  private final String[] names;
  private final String pronunciation;
  private final String[] phoneNumbers;
  private final String[] emails;
  private final String note;
  private final String[] addresses;
  private final String org;
  private final String birthday;
  private final String title;
  private final String url;

  public AddressBookParsedResult(String[] names,
                                 String pronunciation,
                                 String[] phoneNumbers,
                                 String[] emails,
                                 String note,
                                 String[] addresses,
                                 String org,
                                 String birthday,
                                 String title,
                                 String url) {
    super(ParsedResultType.ADDRESSBOOK);
    this.names = names;
    this.pronunciation = pronunciation;
    this.phoneNumbers = phoneNumbers;
    this.emails = emails;
    this.note = note;
    this.addresses = addresses;
    this.org = org;
    this.birthday = birthday;
    this.title = title;
    this.url = url;
  }

  public String[] getNames() {
    return names;
  }

  /**
   * In Japanese, the name is written in kanji, which can have multiple readings. Therefore a hint
   * is often provided, called furigana, which spells the name phonetically.
   *
   * @return The pronunciation of the getNames() field, often in hiragana or katakana.
   */
  public String getPronunciation() {
    return pronunciation;
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

  public String[] getAddresses() {
    return addresses;
  }

  public String getTitle() {
    return title;
  }

  public String getOrg() {
    return org;
  }

  public String getURL() {
    return url;
  }

  /**
   * @return birthday formatted as yyyyMMdd (e.g. 19780917)
   */
  public String getBirthday() {
    return birthday;
  }

  public String getDisplayResult() {
    StringBuffer result = new StringBuffer(100);
    maybeAppend(names, result);
    maybeAppend(pronunciation, result);
    maybeAppend(title, result);
    maybeAppend(org, result);
    maybeAppend(addresses, result);
    maybeAppend(phoneNumbers, result);
    maybeAppend(emails, result);
    maybeAppend(url, result);
    maybeAppend(birthday, result);
    maybeAppend(note, result);
    return result.toString();
  }

}
