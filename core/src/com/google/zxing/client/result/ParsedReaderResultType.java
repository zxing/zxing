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
 * Represents the type of data encoded by a barcode -- from plain text, to a
 * URI, to an e-mail address, etc.
 *
 * @author srowen@google.com (Sean Owen)
 */
public final class ParsedReaderResultType {

  public static final ParsedReaderResultType BOOKMARK = new ParsedReaderResultType("BOOKMARK");
  public static final ParsedReaderResultType URLTO = new ParsedReaderResultType("URLTO");
  public static final ParsedReaderResultType ADDRESSBOOK = new ParsedReaderResultType("ADDRESSBOOK");
  public static final ParsedReaderResultType ADDRESSBOOK_AU = new ParsedReaderResultType("ADDRESSBOOK_AU");  
  public static final ParsedReaderResultType EMAIL = new ParsedReaderResultType("EMAIL");
  public static final ParsedReaderResultType EMAIL_ADDRESS = new ParsedReaderResultType("EMAIL_ADDRESS");
  public static final ParsedReaderResultType UPC = new ParsedReaderResultType("UPC");
  public static final ParsedReaderResultType URI = new ParsedReaderResultType("URI");
  public static final ParsedReaderResultType TEXT = new ParsedReaderResultType("TEXT");
  public static final ParsedReaderResultType ANDROID_INTENT = new ParsedReaderResultType("ANDROID_INTENT"); 
  public static final ParsedReaderResultType GEO = new ParsedReaderResultType("GEO");
  public static final ParsedReaderResultType TEL = new ParsedReaderResultType("TEL");
  public static final ParsedReaderResultType SMS = new ParsedReaderResultType("SMS");
  public static final ParsedReaderResultType SMSTO = new ParsedReaderResultType("SMSTO");
  
  // "optional" types
  public static final ParsedReaderResultType NDEF_TEXT = new ParsedReaderResultType("NDEF_TEXT");
  public static final ParsedReaderResultType NDEF_URI = new ParsedReaderResultType("NDEF_URI");
  public static final ParsedReaderResultType NDEF_SMART_POSTER = new ParsedReaderResultType("NDEF_SMART_POSTER");
  public static final ParsedReaderResultType MOBILETAG_TEL = new ParsedReaderResultType("MOBILETAG_TEL");
  public static final ParsedReaderResultType MOBILETAG_SMS = new ParsedReaderResultType("MOBILETAG_SMS");
  public static final ParsedReaderResultType MOBILETAG_MMS = new ParsedReaderResultType("MOBILETAG_MMS");    
  public static final ParsedReaderResultType MOBILETAG_SIMPLE_WEB = new ParsedReaderResultType("MOBILETAG_SIMPLE_WEB");  
  public static final ParsedReaderResultType MOBILETAG_SIMPLE_CONTACT =
      new ParsedReaderResultType("MOBILETAG_SIMPLE_CONTACT");
  public static final ParsedReaderResultType MOBILETAG_SIMPLE_CALENDAR =
      new ParsedReaderResultType("MOBILETAG_SIMPLE_CALENDAR");
  public static final ParsedReaderResultType MOBILETAG_RICH_WEB = new ParsedReaderResultType("MOBILETAG_RICH_WEB");

  private final String name;

  private ParsedReaderResultType(String name) {
    this.name = name;
  }

  public String toString() {
    return name;
  }

}
