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
 * Represents a parsed result that encodes contact information, like that in an address book
 * entry.
 *
 * @author Sean Owen
 */
public final class AddressBookParsedResult extends ParsedResult {

  private final String[] names;
  private final String[] nicknames;
  private final String pronunciation;
  private final String[] phoneNumbers;
  private final String[] phoneTypes;
  private final String[] emails;
  private final String[] emailTypes;
  private final String instantMessenger;
  private final String note;
  private final String[] addresses;
  private final String[] addressTypes;
  private final String org;
  private final String birthday;
  private final String title;
  private final String[] urls;
  private final String[] geo;

  public AddressBookParsedResult(String[] names,
                                 String[] phoneNumbers,
                                 String[] phoneTypes,
                                 String[] emails,
                                 String[] emailTypes,
                                 String[] addresses,
                                 String[] addressTypes) {
    this(names,
         null,
         null,
         phoneNumbers,
         phoneTypes,
         emails,
         emailTypes,
         null,
         null,
         addresses,
         addressTypes,
         null,
         null,
         null,
         null,
         null);
  }

  public AddressBookParsedResult(String[] names,
                                 String[] nicknames,
                                 String pronunciation,
                                 String[] phoneNumbers,
                                 String[] phoneTypes,
                                 String[] emails,
                                 String[] emailTypes,
                                 String instantMessenger,
                                 String note,
                                 String[] addresses,
                                 String[] addressTypes,
                                 String org,
                                 String birthday,
                                 String title,
                                 String[] urls,
                                 String[] geo) {
    super(ParsedResultType.ADDRESSBOOK);
    if (phoneNumbers != null && phoneTypes != null && phoneNumbers.length != phoneTypes.length) {
      throw new IllegalArgumentException("Phone numbers and types lengths differ");
    }
    if (emails != null && emailTypes != null && emails.length != emailTypes.length) {
      throw new IllegalArgumentException("Emails and types lengths differ");
    }
    if (addresses != null && addressTypes != null && addresses.length != addressTypes.length) {
      throw new IllegalArgumentException("Addresses and types lengths differ");
    }
    this.names = names;
    this.nicknames = nicknames;
    this.pronunciation = pronunciation;
    this.phoneNumbers = phoneNumbers;
    this.phoneTypes = phoneTypes;
    this.emails = emails;
    this.emailTypes = emailTypes;
    this.instantMessenger = instantMessenger;
    this.note = note;
    this.addresses = addresses;
    this.addressTypes = addressTypes;
    this.org = org;
    this.birthday = birthday;
    this.title = title;
    this.urls = urls;
    this.geo = geo;
  }

  public String[] getNames() {
    return names;
  }

  public String[] getNicknames() {
    return nicknames;
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

  /**
   * @return optional descriptions of the type of each phone number. It could be like "HOME", but,
   *  there is no guaranteed or standard format.
   */
  public String[] getPhoneTypes() {
    return phoneTypes;
  }

  public String[] getEmails() {
    return emails;
  }

  /**
   * @return optional descriptions of the type of each e-mail. It could be like "WORK", but,
   *  there is no guaranteed or standard format.
   */
  public String[] getEmailTypes() {
    return emailTypes;
  }
  
  public String getInstantMessenger() {
    return instantMessenger;
  }

  public String getNote() {
    return note;
  }

  public String[] getAddresses() {
    return addresses;
  }

  /**
   * @return optional descriptions of the type of each e-mail. It could be like "WORK", but,
   *  there is no guaranteed or standard format.
   */
  public String[] getAddressTypes() {
    return addressTypes;
  }

  public String getTitle() {
    return title;
  }

  public String getOrg() {
    return org;
  }

  public String[] getURLs() {
    return urls;
  }

  /**
   * @return birthday formatted as yyyyMMdd (e.g. 19780917)
   */
  public String getBirthday() {
    return birthday;
  }

  /**
   * @return a location as a latitude/longitude pair
   */
  public String[] getGeo() {
    return geo;
  }

  @Override
  public String getDisplayResult() {
    StringBuilder result = new StringBuilder(100);
    maybeAppend(names, result);
    maybeAppend(nicknames, result);
    maybeAppend(pronunciation, result);
    maybeAppend(title, result);
    maybeAppend(org, result);
    maybeAppend(addresses, result);
    maybeAppend(phoneNumbers, result);
    maybeAppend(emails, result);
    maybeAppend(instantMessenger, result);
    maybeAppend(urls, result);
    maybeAppend(birthday, result);
    maybeAppend(geo, result);
    maybeAppend(note, result);
    return result.toString();
  }

}
