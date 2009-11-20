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

/**
 * Represents the type of data encoded by a barcode -- from plain text, to a
 * URI, to an e-mail address, etc.
 *
 * @author Sean Owen
 */
public final class ParsedResultType {

  public static var ADDRESSBOOK:ParsedResultType = new ParsedResultType("ADDRESSBOOK");
  public static var EMAIL_ADDRESS:ParsedResultType = new ParsedResultType("EMAIL_ADDRESS");
  public static var PRODUCT:ParsedResultType = new ParsedResultType("PRODUCT");
  public static var URI:ParsedResultType = new ParsedResultType("URI");
  public static var TEXT:ParsedResultType = new ParsedResultType("TEXT");
  public static var ANDROID_INTENT:ParsedResultType = new ParsedResultType("ANDROID_INTENT");
  public static var GEO:ParsedResultType = new ParsedResultType("GEO");
  public static var TEL:ParsedResultType = new ParsedResultType("TEL");
  public static var SMS:ParsedResultType = new ParsedResultType("SMS");
  public static var CALENDAR:ParsedResultType = new ParsedResultType("CALENDAR");
  // "optional" types
  public static var NDEF_SMART_POSTER:ParsedResultType = new ParsedResultType("NDEF_SMART_POSTER");
  public static var MOBILETAG_RICH_WEB:ParsedResultType = new ParsedResultType("MOBILETAG_RICH_WEB");
  public static var ISBN:ParsedResultType = new ParsedResultType("ISBN");

  private var name:String;

  public function ParsedResultType(name:String) {
    this.name = name;
  }

  public function toString():String {
    return name;
  }

}
}