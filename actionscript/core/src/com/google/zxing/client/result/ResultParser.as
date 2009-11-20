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


import com.google.zxing.Result;
import com.google.zxing.common.flexdatatypes.HashTable;
import com.google.zxing.common.flexdatatypes.ArrayList;
import com.google.zxing.common.flexdatatypes.StringBuilder;
import com.google.zxing.common.flexdatatypes.Utils;
import mx.utils.StringUtil;

/**
 * <p>Abstract class representing the result of decoding a barcode, as more than
 * a String -- as some type of structured data. This might be a subclass which represents
 * a URL, or an e-mail address. {@link #parseResult(com.google.zxing.Result)} will turn a raw
 * decoded string into the most appropriate type of structured representation.</p>
 *
 * <p>Thanks to Jeff Griffin for proposing rewrite of these classes that relies less
 * on exception-based mechanisms during parsing.</p>
 *
 * @author Sean Owen
 */
public class ResultParser {

  public static function parseResult(theResult:Result):ParsedResult {
    // This is a bit messy, but given limited options in MIDP / CLDC, this may well be the simplest
    // way to go about this. For example, we have no reflection available, really.
    // Order is important here.
    var result:ParsedResult;
    if ((result = BookmarkDoCoMoResultParser.parse(theResult)) != null) {
      return result;
    } else if ((result = AddressBookDocomoResultParser.parse(theResult)) != null) {
      return result;
    } else if ((result = EmailDoCoMoResultParser.parse(theResult)) != null) {
      return result;
    } else if ((result = AddressBookDocomoResultParser.parse(theResult)) != null) {
      return result;
    } else if ((result = VCardResultParser.parse(theResult)) != null) {
      return result;
    } else if ((result = BizcardResultParser.parse(theResult)) != null) {
      return result;
    } else if ((result = VEventResultParser.parse(theResult)) != null) {
      return result;
    } else if ((result = EmailAddressResultParser.parse(theResult)) != null) {
      return result;
    } else if ((result = TelResultParser.parse(theResult)) != null) {
      return result;
    } else if ((result = SMSMMSResultParser.parse(theResult)) != null) {
      return result;
    } else if ((result = GeoResultParser.parse(theResult)) != null) {
      return result;
    } else if ((result = URLTOResultParser.parse(theResult)) != null) {
      return result;
    } else if ((result = URIResultParser.parse(theResult)) != null) {
      return result;
    } else if ((result = ISBNResultParser.parse(theResult)) != null) {
      // We depend on ISBN parsing coming before UPC, as it is a subset.
      return result;
    } else if ((result = ProductResultParser.parse(theResult)) != null) {
      return result;
    }
    return new TextParsedResult(theResult.getText(), null);
  }

  protected static function maybeAppend(value:String, result:StringBuilder):void {
    if (value != null) {
      result.Append('\n');
      result.Append(value);
    }
  }

  protected static function maybeWrap(value:String ):Array {
    return value == null ? null : [ value ];
  }

  protected static function unescapeBackslash(escaped:String):String {
    if (escaped != null) {
      var backslash:int = escaped.indexOf('\\');
      if (backslash >= 0) {
        var max:int = escaped.length;
        var unescaped:StringBuilder = new StringBuilder(max - 1);
        unescaped.Append(escaped.split(""), 0, backslash);
        var nextIsEscaped:Boolean = false;
        for (var i:int = backslash; i < max; i++) {
          var c:String = escaped.charAt(i);
          if (nextIsEscaped || c != '\\') {
            unescaped.Append(c);
            nextIsEscaped = false;
          } else {
            nextIsEscaped = true;
          }
        }
        return unescaped.toString();
      }
    }
    return escaped;
  }

  public static function urlDecode(escaped:String):String {

    // No we can't use java.net.URLDecoder here. JavaME doesn't have it.
    if (escaped == null) {
      return null;
    }
    var escapedArray:Array = escaped.split("");

    var first:int = findFirstEscape(escapedArray);
    if (first < 0) {
      return escaped;
    }

    var max:int = escapedArray.length;
    // final length is at most 2 less than original due to at least 1 unescaping
    var unescaped:StringBuilder = new StringBuilder(max - 2);
    // Can append everything up to first escape character
    unescaped.Append(escapedArray, 0, first);

    for (var i:int = first; i < max; i++) {
      var c:String = escapedArray[i];
      if (c == '+') {
        // + is translated directly into a space
        unescaped.Append(' ');
      } else if (c == '%') {
        // Are there even two more chars? if not we will just copy the escaped sequence and be done
        if (i >= max - 2) {
          unescaped.Append('%'); // append that % and move on
        } else {
          var firstDigitValue:int = parseHexDigit(escapedArray[++i]);
          var secondDigitValue:int = parseHexDigit(escapedArray[++i]);
          if (firstDigitValue < 0 || secondDigitValue < 0) {
            // bad digit, just move on
            unescaped.Append('%');
            unescaped.Append(escapedArray[i-1]);
            unescaped.Append(escapedArray[i]);
          }
          unescaped.Append(String.fromCharCode((firstDigitValue << 4) + secondDigitValue));
        }
      } else {
        unescaped.Append(c);
      }
    }
    return unescaped.toString();
  }

  private static function findFirstEscape(escapedArray:Array):int {
    var max:int = escapedArray.length;
    for (var i:int = 0; i < max; i++) {
      var c:String = escapedArray[i];
      if (c == '+' || c == '%') {
        return i;
      }
    }
    return -1;
  }

  private static function parseHexDigit(c:String):int {
    if (c.charCodeAt(0) >= ('a').charCodeAt(0)) {
      if (c.charCodeAt(0) <= ('f').charCodeAt(0)) {
        return 10 + (c.charCodeAt(0) - ('a').charCodeAt(0));
      }
    } else if (c.charCodeAt(0) >= ('A').charCodeAt(0)) {
      if (c.charCodeAt(0) <= ('F').charCodeAt(0)) {
        return 10 + (c.charCodeAt(0) - ('A').charCodeAt(0));
      }
    } else if (c.charCodeAt(0) >= ('0').charCodeAt(0)) {
      if (c.charCodeAt(0) <= ('9').charCodeAt(0)) {
        return c.charCodeAt(0) - ('0').charCodeAt(0);
      }
    }
    return -1;
  }

  protected static function isStringOfDigits(value:String,length:int):Boolean {
    if (value == null) {
      return false;
    }
    var stringLength:int = value.length;
    if (length != stringLength) {
      return false;
    }
    for (var i:int = 0; i < length; i++) {
      var c:String = value.charAt(i);
      if (c < '0' || c > '9') {
        return false;
      }
    }
    return true;
  }

  public static function parseNameValuePairs(uri:String):HashTable {
    var paramStart:int = uri.indexOf('?');
    if (paramStart < 0) {
      return null;
    }
    var result:HashTable = new HashTable(3);
    paramStart++;
    var paramEnd:int;
    while ((paramEnd = uri.indexOf('&', paramStart)) >= 0) {
      appendKeyValue(uri, paramStart, paramEnd, result);
      paramStart = paramEnd + 1;
    }
    appendKeyValue(uri, paramStart, uri.length, result);
    return result;
  }

  private static function appendKeyValue(uri:String , paramStart:int , paramEnd:int, result:HashTable):void {
    var separator:int = uri.indexOf('=', paramStart);
    if (separator >= 0) {
      // key = value
      var key:String = uri.substring(paramStart, separator);
      var value:String = uri.substring(separator + 1, paramEnd);
      value = urlDecode(value);
      result._put(key, value);
    }
    // Can't put key, null into a hashtable
  }

  public static function matchPrefixedField(prefix:String, rawText:String, endChar:String, trim:Boolean):Array {
    var matches:ArrayList = null;
    var i:int = 0;
    var max:int = rawText.length;
    while (i < max) {
      i = rawText.indexOf(prefix, i);
      if (i < 0) {
        break;
      }
      i += prefix.length; // Skip past this prefix we found to start
      var start:int = i; // Found the start of a match here
      var done:Boolean = false;
      while (!done) {
        i = rawText.indexOf( endChar, i);
        if (i < 0) {
          // No terminating end character? uh, done. Set i such that loop terminates and break
          i = rawText.length;
          done = true;
        } else if (rawText.charAt(i - 1) == '\\') {
          // semicolon was escaped so continue
          i++;
        } else {
          // found a match
          if (matches == null) {
            matches = new ArrayList(3); // lazy init
          }
          var element:String = unescapeBackslash(rawText.substring(start, i));
          if (trim) {
            element = StringUtil.trim(element);
          }
          matches.addElement(element);
          i++;
          done = true;
        }
      }
    }
    if (matches == null || matches.isEmpty()) {
      return null;
    }
    return toStringArray(matches);
  }

  public static function matchSinglePrefixedField(prefix:String, rawText:String, endChar:String, trim:Boolean ):String 
  {
    var matches:Array = matchPrefixedField(prefix, rawText, endChar, trim);
    return matches == null ? null : matches[0];
  }

  public static function toStringArray(strings:ArrayList):Array {
    var size:int = strings.size();
    var result:Array = new Array(size);
    for (var j:int = 0; j < size; j++) {
      result[j] = String(strings.elementAt(j));
    }
    return result;
  }

}

}