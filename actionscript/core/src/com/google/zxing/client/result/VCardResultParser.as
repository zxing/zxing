package com.google.zxing.client.result
{
	/*
 * Copyright 2008 ZXing authors
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

import com.google.zxing.Result;
import com.google.zxing.common.flexdatatypes.ArrayList;
import com.google.zxing.common.flexdatatypes.StringBuilder;
import com.google.zxing.common.flexdatatypes.Utils;
import mx.utils.StringUtil;

/**
 * Parses contact information formatted according to the VCard (2.1) format. This is not a complete
 * implementation but should parse information as commonly encoded in 2D barcodes.
 *
 * @author Sean Owen
 */
public final class VCardResultParser extends ResultParser {

  public function VCardResultParser() {
  }

  public static function parse(result:Result):AddressBookParsedResult {
    var rawText:String = result.getText();
    if (rawText == null || (Utils.startsWith(rawText,"BEGIN:VCARD")) || !Utils.endsWith(rawText,"END:VCARD")) {
      return null;
    }
    var names:Array = matchVCardPrefixedField("FN", rawText, true);
    if (names == null) {
      // If no display names found, look for regular name fields and format them
      names = matchVCardPrefixedField("N", rawText, true);
      formatNames(names);
    }
    var phoneNumbers:Array = matchVCardPrefixedField("TEL", rawText, true);
    var emails:Array = matchVCardPrefixedField("EMAIL", rawText, true);
    var note:String = matchSingleVCardPrefixedField("NOTE", rawText, false);
    var address:String = matchSingleVCardPrefixedField("ADR", rawText, true);
    address = formatAddress(address);
    var org:String = matchSingleVCardPrefixedField("ORG", rawText, true);
    var birthday:String = matchSingleVCardPrefixedField("BDAY", rawText, true);
    if (birthday != null && !isStringOfDigits(birthday, 8)) {
      return null;
    }
    var title:String = matchSingleVCardPrefixedField("TITLE", rawText, true);
    var url:String = matchSingleVCardPrefixedField("URL", rawText, true);
    return new AddressBookParsedResult(names, null, phoneNumbers, emails, note, address, org,
        birthday, title, url);
  }

  private static function matchVCardPrefixedField(prefix:String, rawText:String, trim:Boolean):Array {
    var matches:ArrayList = null;
    var i:int = 0;
    var max:int = rawText.length;
    while (i < max) {
      i = rawText.indexOf(prefix, i);
      if (i < 0) {
        break;
      }
      if (i > 0 && rawText.charAt(i - 1) != '\n') {
        // then this didn't start a new token, we matched in the middle of something
        i++;
        continue;
      }
      i += prefix.length; // Skip past this prefix we found to start
      if (rawText.charAt(i) != ':' && rawText.charAt(i) != ';') {
        continue;
      }
      while (rawText.charAt(i) != ':') { // Skip until a colon
        i++;
      }
      i++; // skip colon
      var start:int = i; // Found the start of a match here
      i = rawText.indexOf('\n', i); // Really, ends in \r\n
      if (i < 0) {
        // No terminating end character? uh, done. Set i such that loop terminates and break
        i = max;
      } else if (i > start) {
        // found a match
        if (matches == null) {
          matches = new ArrayList(3); // lazy init
        }
        var element:String = rawText.substring(start, i);
        if (trim) {
          element = StringUtil.trim(element);
        }
        matches.addElement(element);
        i++;
      } else {
        i++;
      }
    }
    if (matches == null || matches.isEmpty()) {
      return null;
    }
    return toStringArray(matches);
  }

  public static function matchSingleVCardPrefixedField(prefix:String , rawText:String , trim:Boolean):String {
    var values:Array = matchVCardPrefixedField(prefix, rawText, trim);
    return values == null ? null : values[0];
  }

  private static function formatAddress(address:String):String {
    if (address == null) {
      return null;
    }
    var length:int = address.length;
    var newAddress:StringBuilder = new StringBuilder(length);
    for (var j:int = 0; j < length; j++) {
      var c:String = address.charAt(j);
      if (c == ';') {
        newAddress.Append(' ');
      } else {
        newAddress.Append(c);
      }
    }
    return StringUtil.trim(newAddress.toString());
  }

  /**
   * Formats name fields of the form "Public;John;Q.;Reverend;III" into a form like
   * "Reverend John Q. Public III".
   *
   * @param names name values to format, in place
   */
  private static function formatNames(names:Array):void {
    if (names != null) {
      for (var i:int = 0; i < names.length; i++) {
        var name:String = names[i];
        var components:Array = new Array(5);
        var start:int = 0;
        var end:int;
        var componentIndex:int = 0;
        while ((end = name.indexOf(';', start)) > 0) {
          components[componentIndex] = name.substring(start, end);
          componentIndex++;
          start = end + 1;
        }
        components[componentIndex] = name.substring(start);
        var newName:StringBuilder = new StringBuilder();
        maybeAppendComponent(components, 3, newName);
        maybeAppendComponent(components, 1, newName);
        maybeAppendComponent(components, 2, newName);
        maybeAppendComponent(components, 0, newName);
        maybeAppendComponent(components, 4, newName);
        names[i] = StringUtil.trim(newName.toString());
      }
    }
  }

  private static function maybeAppendComponent(components:Array,i:int, newName:StringBuilder):void {
    if (components[i] != null) {
      newName.Append(' ');
      newName.Append(components[i]);
    }
  }

}

}