/*
 * Copyright 2010 ZXing authors
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
 * @author Sean Owen
 */
public final class ParsedResultType {

  public static final ParsedResultType ADDRESSBOOK = new ParsedResultType("ADDRESSBOOK");
  public static final ParsedResultType EMAIL_ADDRESS = new ParsedResultType("EMAIL_ADDRESS");
  public static final ParsedResultType PRODUCT = new ParsedResultType("PRODUCT");
  public static final ParsedResultType URI = new ParsedResultType("URI");
  public static final ParsedResultType TEXT = new ParsedResultType("TEXT");
  public static final ParsedResultType ANDROID_INTENT = new ParsedResultType("ANDROID_INTENT");
  public static final ParsedResultType GEO = new ParsedResultType("GEO");
  public static final ParsedResultType TEL = new ParsedResultType("TEL");
  public static final ParsedResultType SMS = new ParsedResultType("SMS");
  public static final ParsedResultType CALENDAR = new ParsedResultType("CALENDAR");
  public static final ParsedResultType WIFI = new ParsedResultType("WIFI");
  // "optional" types
  public static final ParsedResultType NDEF_SMART_POSTER = new ParsedResultType("NDEF_SMART_POSTER");
  public static final ParsedResultType MOBILETAG_RICH_WEB = new ParsedResultType("MOBILETAG_RICH_WEB");
  public static final ParsedResultType ISBN = new ParsedResultType("ISBN");

  private final String name;

  private ParsedResultType(String name) {
    this.name = name;
  }

  public String toString() {
    return name;
  }

}
