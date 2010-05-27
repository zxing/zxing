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

package com.google.zxing.client.result;

import com.google.zxing.Result;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Vector;

/**
 * Parses contact information formatted according to the VCard (2.1) format. This is not a complete
 * implementation but should parse information as commonly encoded in 2D barcodes.
 *
 * @author Sean Owen
 */
final class VCardResultParser extends ResultParser {

  private VCardResultParser() {
  }

  public static AddressBookParsedResult parse(Result result) {
    // Although we should insist on the raw text ending with "END:VCARD", there's no reason
    // to throw out everything else we parsed just because this was omitted. In fact, Eclair
    // is doing just that, and we can't parse its contacts without this leniency.
    String rawText = result.getText();
    if (rawText == null || !rawText.startsWith("BEGIN:VCARD")) {
      return null;
    }
    String[] names = matchVCardPrefixedField("FN", rawText, true);
    if (names == null) {
      // If no display names found, look for regular name fields and format them
      names = matchVCardPrefixedField("N", rawText, true);
      formatNames(names);
    }
    String[] phoneNumbers = matchVCardPrefixedField("TEL", rawText, true);
    String[] emails = matchVCardPrefixedField("EMAIL", rawText, true);
    String note = matchSingleVCardPrefixedField("NOTE", rawText, false);
    String[] addresses = matchVCardPrefixedField("ADR", rawText, true);
    if (addresses != null) {
      for (int i = 0; i < addresses.length; i++) {
        addresses[i] = formatAddress(addresses[i]);
      }
    }
    String org = matchSingleVCardPrefixedField("ORG", rawText, true);
    String birthday = matchSingleVCardPrefixedField("BDAY", rawText, true);
    if (!isLikeVCardDate(birthday)) {
      birthday = null;
    }
    String title = matchSingleVCardPrefixedField("TITLE", rawText, true);
    String url = matchSingleVCardPrefixedField("URL", rawText, true);
    return new AddressBookParsedResult(names, null, phoneNumbers, emails, note, addresses, org,
        birthday, title, url);
  }

  private static String[] matchVCardPrefixedField(String prefix, String rawText, boolean trim) {
    Vector matches = null;
    int i = 0;
    int max = rawText.length();

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
      i += prefix.length(); // Skip past this prefix we found to start
      if (rawText.charAt(i) != ':' && rawText.charAt(i) != ';') {
        continue;
      }

      int metadataStart = i;
      while (rawText.charAt(i) != ':') { // Skip until a colon
        i++;
      }

      boolean quotedPrintable = false;
      String quotedPrintableCharset = null;
      if (i > metadataStart) {
        // There was something after the tag, before colon
        int j = metadataStart+1;
        while (j <= i) {
          if (rawText.charAt(j) == ';' || rawText.charAt(j) == ':') {
            String metadata = rawText.substring(metadataStart+1, j);
            int equals = metadata.indexOf('=');
            if (equals >= 0) {
              String key = metadata.substring(0, equals);
              String value = metadata.substring(equals+1);
              if (key.equalsIgnoreCase("ENCODING")) {
                if (value.equalsIgnoreCase("QUOTED-PRINTABLE")) {
                  quotedPrintable = true;
                }
              } else if (key.equalsIgnoreCase("CHARSET")) {
                quotedPrintableCharset = value;
              }
            }
            metadataStart = j;
          }
          j++;
        }
      }

      i++; // skip colon

      int matchStart = i; // Found the start of a match here

      while ((i = rawText.indexOf((int) '\n', i)) >= 0) { // Really, end in \r\n
        if (i < rawText.length() - 1 &&           // But if followed by tab or space,
            (rawText.charAt(i+1) == ' ' ||        // this is only a continuation
             rawText.charAt(i+1) == '\t')) {
          i += 2; // Skip \n and continutation whitespace
        } else if (quotedPrintable &&             // If preceded by = in quoted printable
                   (rawText.charAt(i-1) == '=' || // this is a continuation
                    rawText.charAt(i-2) == '=')) {
          i++; // Skip \n
        } else {
          break;
        }
      }

      if (i < 0) {
        // No terminating end character? uh, done. Set i such that loop terminates and break
        i = max;
      } else if (i > matchStart) {
        // found a match
        if (matches == null) {
          matches = new Vector(1); // lazy init
        }
        if (rawText.charAt(i-1) == '\r') {
          i--; // Back up over \r, which really should be there
        }
        String element = rawText.substring(matchStart, i);
        if (trim) {
          element = element.trim();
        }
        if (quotedPrintable) {
          element = decodeQuotedPrintable(element, quotedPrintableCharset);
        } else {
          element = stripContinuationCRLF(element);
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

  private static String stripContinuationCRLF(String value) {
    int length = value.length();
    StringBuffer result = new StringBuffer(length);
    boolean lastWasLF = false;
    for (int i = 0; i < length; i++) {
      if (lastWasLF) {
        lastWasLF = false;
        continue;
      }
      char c = value.charAt(i);
      lastWasLF = false;
      switch (c) {
        case '\n':
          lastWasLF = true;
          break;
        case '\r':
          break;
        default:
          result.append(c);
      }
    }
    return result.toString();
  }

  private static String decodeQuotedPrintable(String value, String charset) {
    int length = value.length();
    StringBuffer result = new StringBuffer(length);
    ByteArrayOutputStream fragmentBuffer = new ByteArrayOutputStream();
    for (int i = 0; i < length; i++) {
      char c = value.charAt(i);
      switch (c) {
        case '\r':
        case '\n':
          break;
        case '=':
          if (i < length - 2) {
            char nextChar = value.charAt(i+1);
            if (nextChar == '\r' || nextChar == '\n') {
              // Ignore, it's just a continuation symbol
            } else {
              char nextNextChar = value.charAt(i+2);
              try {
                int encodedByte = 16 * toHexValue(nextChar) + toHexValue(nextNextChar);
                fragmentBuffer.write(encodedByte);
              } catch (IllegalArgumentException iae) {
                // continue, assume it was incorrectly encoded
              }
              i += 2;
            }
          }
          break;
        default:
          maybeAppendFragment(fragmentBuffer, charset, result);
          result.append(c);
      }
    }
    maybeAppendFragment(fragmentBuffer, charset, result);
    return result.toString();
  }

  private static int toHexValue(char c) {
    if (c >= '0' && c <= '9') {
      return c - '0';
    } else if (c >= 'A' && c <= 'F') {
      return c - 'A' + 10;
    } else if (c >= 'a' && c <= 'f') {
      return c - 'a' + 10;
    }
    throw new IllegalArgumentException();
  }

  private static void maybeAppendFragment(ByteArrayOutputStream fragmentBuffer,
                                          String charset,
                                          StringBuffer result) {
    if (fragmentBuffer.size() > 0) {
      byte[] fragmentBytes = fragmentBuffer.toByteArray();
      String fragment;
      if (charset == null) {
        fragment = new String(fragmentBytes);
      } else {
        try {
          fragment = new String(fragmentBytes, charset);
        } catch (UnsupportedEncodingException e) {
          // Yikes, well try anyway:
          fragment = new String(fragmentBytes);
        }
      }
      fragmentBuffer.reset();
      result.append(fragment);
    }
  }

  static String matchSingleVCardPrefixedField(String prefix, String rawText, boolean trim) {
    String[] values = matchVCardPrefixedField(prefix, rawText, trim);
    return values == null ? null : values[0];
  }

  private static boolean isLikeVCardDate(String value) {
    if (value == null) {
      return true;
    }
    // Not really sure this is true but matches practice
    // Mach YYYYMMDD
    if (isStringOfDigits(value, 8)) {
      return true;
    }
    // or YYYY-MM-DD
    return
        value.length() == 10 &&
        value.charAt(4) == '-' &&
        value.charAt(7) == '-' &&
        isSubstringOfDigits(value, 0, 4) &&
        isSubstringOfDigits(value, 5, 2) &&
        isSubstringOfDigits(value, 8, 2);
  }

  private static String formatAddress(String address) {
    if (address == null) {
      return null;
    }
    int length = address.length();
    StringBuffer newAddress = new StringBuffer(length);
    for (int j = 0; j < length; j++) {
      char c = address.charAt(j);
      if (c == ';') {
        newAddress.append(' ');
      } else {
        newAddress.append(c);
      }
    }
    return newAddress.toString().trim();
  }

  /**
   * Formats name fields of the form "Public;John;Q.;Reverend;III" into a form like
   * "Reverend John Q. Public III".
   *
   * @param names name values to format, in place
   */
  private static void formatNames(String[] names) {
    if (names != null) {
      for (int i = 0; i < names.length; i++) {
        String name = names[i];
        String[] components = new String[5];
        int start = 0;
        int end;
        int componentIndex = 0;
        while ((end = name.indexOf(';', start)) > 0) {
          components[componentIndex] = name.substring(start, end);
          componentIndex++;
          start = end + 1;
        }
        components[componentIndex] = name.substring(start);
        StringBuffer newName = new StringBuffer(100);
        maybeAppendComponent(components, 3, newName);
        maybeAppendComponent(components, 1, newName);
        maybeAppendComponent(components, 2, newName);
        maybeAppendComponent(components, 0, newName);
        maybeAppendComponent(components, 4, newName);
        names[i] = newName.toString().trim();
      }
    }
  }

  private static void maybeAppendComponent(String[] components, int i, StringBuffer newName) {
    if (components[i] != null) {
      newName.append(' ');
      newName.append(components[i]);
    }
  }

}
