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
import java.util.Vector;

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
public abstract class ResultParser {

  public static ParsedResult parseResult(Result theResult) {
    // This is a bit messy, but given limited options in MIDP / CLDC, this may well be the simplest
    // way to go about this. For example, we have no reflection available, really.
    // Order is important here.
    ParsedResult result;
    if ((result = BookmarkDoCoMoResultParser.parse(theResult)) != null) {
      return result;
    } else if ((result = AddressBookDoCoMoResultParser.parse(theResult)) != null) {
      return result;
    } else if ((result = EmailDoCoMoResultParser.parse(theResult)) != null) {
      return result;
    } else if ((result = AddressBookAUResultParser.parse(theResult)) != null) {
      return result;
    } else if ((result = VCardResultParser.parse(theResult)) != null) {
      return result;
    } else if ((result = BizcardResultParser.parse(theResult)) != null) {
      return result;
    } else if ((result = VEventResultParser.parse(theResult)) != null) {
      return result;
    } else if ((result = EmailAddressResultParser.parse(theResult)) != null) {
      return result;
    } else if ((result = SMTPResultParser.parse(theResult)) != null) {
      return result;
    } else if ((result = TelResultParser.parse(theResult)) != null) {
      return result;
    } else if ((result = SMSMMSResultParser.parse(theResult)) != null) {
      return result;
    } else if ((result = SMSTOMMSTOResultParser.parse(theResult)) != null) {
      return result;
    } else if ((result = GeoResultParser.parse(theResult)) != null) {
      return result;
    } else if ((result = WifiResultParser.parse(theResult)) != null) {
      return result;
    } else if ((result = URLTOResultParser.parse(theResult)) != null) {
      return result;
    } else if ((result = URIResultParser.parse(theResult)) != null) {
      // URI is a catch-all for protocol: contents that we don't handle explicitly above.
      return result;
    } else if ((result = ISBNResultParser.parse(theResult)) != null) {
      // We depend on ISBN parsing coming before UPC, as it is a subset.
      return result;
    } else if ((result = ProductResultParser.parse(theResult)) != null) {
      return result;
    } else if ((result = ExpandedProductResultParser.parse(theResult)) != null) {
      return result;
    }
    return new TextParsedResult(theResult.getText(), null);
  }

  protected static void maybeAppend(String value, StringBuffer result) {
    if (value != null) {
      result.append('\n');
      result.append(value);
    }
  }

  protected static void maybeAppend(String[] value, StringBuffer result) {
    if (value != null) {
      for (int i = 0; i < value.length; i++) {
        result.append('\n');
        result.append(value[i]);
      }
    }
  }

  protected static String[] maybeWrap(String value) {
    return value == null ? null : new String[] { value };
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

  private static String urlDecode(String escaped) {
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
    // Final length is at most 2 less than original due to at least 1 unescaping.
    StringBuffer unescaped = new StringBuffer(max - 2);
    // Can append everything up to first escape character
    unescaped.append(escapedArray, 0, first);

    for (int i = first; i < max; i++) {
      char c = escapedArray[i];
      switch (c) {
        case '+':
          // + is translated directly into a space
          unescaped.append(' ');
          break;
        case '%':
          // Are there even two more chars? If not we'll just copy the escaped sequence and be done.
          if (i >= max - 2) {
            unescaped.append('%'); // append that % and move on
          } else {
            int firstDigitValue = parseHexDigit(escapedArray[++i]);
            int secondDigitValue = parseHexDigit(escapedArray[++i]);
            if (firstDigitValue < 0 || secondDigitValue < 0) {
              // Bad digit, just move on.
              unescaped.append('%');
              unescaped.append(escapedArray[i - 1]);
              unescaped.append(escapedArray[i]);
            }
            unescaped.append((char) ((firstDigitValue << 4) + secondDigitValue));
          }
          break;
        default:
          unescaped.append(c);
          break;
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

  protected static boolean isStringOfDigits(String value, int length) {
    if (value == null) {
      return false;
    }
    int stringLength = value.length();
    if (length != stringLength) {
      return false;
    }
    for (int i = 0; i < length; i++) {
      char c = value.charAt(i);
      if (c < '0' || c > '9') {
        return false;
      }
    }
    return true;
  }

  protected static boolean isSubstringOfDigits(String value, int offset, int length) {
    if (value == null) {
      return false;
    }
    int stringLength = value.length();
    int max = offset + length;
    if (stringLength < max) {
      return false;
    }
    for (int i = offset; i < max; i++) {
      char c = value.charAt(i);
      if (c < '0' || c > '9') {
        return false;
      }
    }
    return true;
  }

  static Hashtable parseNameValuePairs(String uri) {
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
    }
    // Can't put key, null into a hashtable
  }

  static String[] matchPrefixedField(String prefix, String rawText, char endChar, boolean trim) {
    Vector matches = null;
    int i = 0;
    int max = rawText.length();
    while (i < max) {
      i = rawText.indexOf(prefix, i);
      if (i < 0) {
        break;
      }
      i += prefix.length(); // Skip past this prefix we found to start
      int start = i; // Found the start of a match here
      boolean done = false;
      while (!done) {
        i = rawText.indexOf((int) endChar, i);
        if (i < 0) {
          // No terminating end character? uh, done. Set i such that loop terminates and break
          i = rawText.length();
          done = true;
        } else if (rawText.charAt(i - 1) == '\\') {
          // semicolon was escaped so continue
          i++;
        } else {
          // found a match
          if (matches == null) {
            matches = new Vector(3); // lazy init
          }
          String element = unescapeBackslash(rawText.substring(start, i));
          if (trim) {
            element = element.trim();
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

  static String matchSinglePrefixedField(String prefix, String rawText, char endChar, boolean trim) {
    String[] matches = matchPrefixedField(prefix, rawText, endChar, trim);
    return matches == null ? null : matches[0];
  }

  static String[] toStringArray(Vector strings) {
    int size = strings.size();
    String[] result = new String[size];
    for (int j = 0; j < size; j++) {
      result[j] = (String) strings.elementAt(j);
    }
    return result;
  }

}
