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

import com.google.zxing.Result;

import java.util.Hashtable;

/**
 * <p>Abstract class representing the result of decoding a barcode, as more than
 * a String -- as some type of structured data. This might be a subclass which represents
 * a URL, or an e-mail address. {@link #parseReaderResult(Result)} will turn a raw
 * decoded string into the most appropriate type of structured representation.</p>
 *
 * <p>Thanks to Jeff Griffin for proposing rewrite of these classes that relies less
 * on exception-based mechanisms during parsing.</p>
 *
 * @author srowen@google.com (Sean Owen)
 */
public abstract class ParsedReaderResult {

  private final ParsedReaderResultType type;

  protected ParsedReaderResult(ParsedReaderResultType type) {
    this.type = type;
  }

  public ParsedReaderResultType getType() {
    return type;
  }

  public abstract String getDisplayResult();

  public static ParsedReaderResult parseReaderResult(Result theResult) {
    // This is a bit messy, but given limited options in MIDP / CLDC, this may well be the simplest
    // way to go about this. For example, we have no reflection available, really.
    // Order is important here.
    ParsedReaderResult result;
    if ((result = BookmarkDoCoMoParsedResult.parse(theResult)) != null) {
      return result;
    } else if ((result = AddressBookDoCoMoParsedResult.parse(theResult)) != null) {
      return result;
    } else if ((result = EmailDoCoMoParsedResult.parse(theResult)) != null) {
      return result;
    } else if ((result = EmailAddressParsedResult.parse(theResult)) != null) {
      return result;
    } else if ((result = AddressBookAUParsedResult.parse(theResult)) != null) {
      return result;
    } else if ((result = TelParsedResult.parse(theResult)) != null) {
      return result;
    } else if ((result = SMSParsedResult.parse(theResult)) != null) {
      return result;
    } else if ((result = SMSTOParsedResult.parse(theResult)) != null) {
      return result;
    } else if ((result = GeoParsedResult.parse(theResult)) != null) {
      return result;
    } else if ((result = URLTOParsedResult.parse(theResult)) != null) {
      return result;
    } else if ((result = URIParsedResult.parse(theResult)) != null) {
      return result;
    } else if ((result = UPCParsedResult.parse(theResult)) != null) {
      return result;
    }
    return TextParsedResult.parse(theResult);
  }

  public String toString() {
    return getDisplayResult();
  }

  protected static void maybeAppend(String value, StringBuffer result) {
    if (value != null) {
      result.append('\n');
      result.append(value);
    }
  }

  protected static String unescapeBackslash(String escaped) {
    if (escaped != null) {
      int backslash = escaped.indexOf((int) '\\');
      if (backslash >= 0) {
        int max = escaped.length();
        StringBuffer unescaped = new StringBuffer(max - 1);
        unescaped.append(escaped.toCharArray(), 0, backslash);
        boolean nextIsEscaped = false;
        for (int i = backslash; i < max; i++) {
          char c = escaped.charAt(i);
          if (nextIsEscaped || c != '\\') {
            unescaped.append(c);
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

  protected static String urlDecode(String escaped) {

    // No we can't use java.net.URLDecoder here. JavaME doesn't have it.
    if (escaped == null) {
      return null;
    }
    char[] escapedArray = escaped.toCharArray();

    int first = findFirstEscape(escapedArray);
    if (first < 0) {
      return escaped;
    }

    int max = escapedArray.length;
    // final length is at most 2 less than original due to at least 1 unescaping
    StringBuffer unescaped = new StringBuffer(max - 2);
    // Can append everything up to first escape character
    unescaped.append(escapedArray, 0, first);

    for (int i = first; i < max; i++) {
      char c = escapedArray[i];
      if (c == '+') {
        // + is translated directly into a space
        unescaped.append(' ');
      } else if (c == '%') {
        // Are there even two more chars? if not we will just copy the escaped sequence and be done
        if (i >= max - 2) {
          unescaped.append('%'); // append that % and move on
        } else {
          int firstDigitValue = parseHexDigit(escapedArray[++i]);
          int secondDigitValue = parseHexDigit(escapedArray[++i]);
          if (firstDigitValue < 0 || secondDigitValue < 0) {
            // bad digit, just move on
            unescaped.append('%');
            unescaped.append(escapedArray[i-1]);
            unescaped.append(escapedArray[i]);
          }
          unescaped.append((char) ((firstDigitValue << 4) + secondDigitValue));
        }
      } else {
        unescaped.append(c);
      }
    }
    return unescaped.toString();
  }

  private static int findFirstEscape(char[] escapedArray) {
    int max = escapedArray.length;
    for (int i = 0; i < max; i++) {
      char c = escapedArray[i];
      if (c == '+' || c == '%') {
        return i;
      }
    }
    return -1;
  }

  private static int parseHexDigit(char c) {
    if (c >= 'a') {
      if (c <= 'f') {
        return 10 + (c - 'a');
      }
    } else if (c >= 'A') {
      if (c <= 'F') {
        return 10 + (c - 'A');
      }
    } else if (c >= '0') {
      if (c <= '9') {
        return c - '0';
      }
    }
    return -1;
  }

  protected static Hashtable parseNameValuePairs(String uri) {
    int paramStart = uri.indexOf('?');
    if (paramStart < 0) {
      return null;
    }
    Hashtable result = new Hashtable(3);
    paramStart++;
    int paramEnd;
    while ((paramEnd = uri.indexOf('&', paramStart)) >= 0) {
      appendKeyValue(uri, paramStart, paramEnd, result);
      paramStart = paramEnd + 1;
    }
    appendKeyValue(uri, paramStart, uri.length(), result);
    return result;
  }

  private static void appendKeyValue(String uri, int paramStart, int paramEnd, Hashtable result) {
    int separator = uri.indexOf('=', paramStart);
    if (separator >= 0) {
      // key = value
      String key = uri.substring(paramStart, separator);
      String value = uri.substring(separator + 1, paramEnd);
      value = urlDecode(value);
      result.put(key, value);
    } else {
      // key, no value
      String key = uri.substring(paramStart, paramEnd);
      result.put(key, null);
    }
  }

}
