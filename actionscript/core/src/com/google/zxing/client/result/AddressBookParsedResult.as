package com.google.zxing.client.result
{
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

import com.google.zxing.common.flexdatatypes.StringBuilder;

/**
 * @author Sean Owen
 */
public final class AddressBookParsedResult extends ParsedResult 
{

  private var names:Array;
  private var pronunciation:String;
  private var phoneNumbers:Array;
  private var emails:Array;
  private var note:String;
  private var address:String;
  private var org:String;
  private var birthday:String;
  private var title:String;
  private var url:String;

  public function AddressBookParsedResult(names:Array,
                                 pronunciation:String,
                                 phoneNumbers:Array,
                                 emails:Array,
                                 note:String,
                                 address:String,
                                 org:String,
                                 birthday:String,
                                 title:String,
                                 url:String) {
    super(ParsedResultType.ADDRESSBOOK);
    this.names = names;
    this.pronunciation = pronunciation;
    this.phoneNumbers = phoneNumbers;
    this.emails = emails;
    this.note = note;
    this.address = address;
    this.org = org;
    this.birthday = birthday;
    this.title = title;
    this.url = url;
  }

  public function getNames():Array {
    return names;
  }

  /**
   * In Japanese, the name is written in kanji, which can have multiple readings. Therefore a hint
   * is often provided, called furigana, which spells the name phonetically.
   *
   * @return The pronunciation of the getNames() field, often in hiragana or katakana.
   */
  public function getPronunciation():String {
    return pronunciation;
  }

  public function getPhoneNumbers():Array {
    return phoneNumbers;
  }

  public function getEmails():Array {
    return emails;
  }

  public function getNote():String {
    return note;
  }

  public function getAddress():String {
    return address;
  }

  public function getTitle():String {
    return title;
  }

  public function getOrg():String {
    return org;
  }

  public function getURL():String {
    return url;
  }

  /**
   * @return birthday formatted as yyyyMMdd (e.g. 19780917)
   */
  public function getBirthday():String {
    return birthday;
  }

  public override function getDisplayResult():String {
    var result:StringBuilder = new StringBuilder();
    maybeAppend(names, result);
    maybeAppend(pronunciation, result);
    maybeAppend(title, result);
    maybeAppend(org, result);
    maybeAppend(address, result);
    maybeAppend(phoneNumbers, result);
    maybeAppend(emails, result);
    maybeAppend(url, result);
    maybeAppend(birthday, result);
    maybeAppend(note, result);
    return result.toString();
  }

}

}