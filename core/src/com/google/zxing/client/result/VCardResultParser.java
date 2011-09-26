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
    if (!rawText.startsWith("BEGIN:VCARD")) {
      return null;
    }
    Vector names = matchVCardPrefixedField("FN", rawText, true);
    if (names == null) {
      // If no display names found, look for regular name fields and format them
      names = matchVCardPrefixedField("N", rawText, true);
      formatNames(names);
    }
    Vector phoneNumbers = matchVCardPrefixedField("TEL", rawText, true);
    Vector emails = matchVCardPrefixedField("EMAIL", rawText, true);
    Vector note = matchSingleVCardPrefixedField("NOTE", rawText, false);
    Vector addresses = matchVCardPrefixedField("ADR", rawText, true);
    if (addresses != null) {
      for (int i = 0; i < addresses.size(); i++) {
        Vector list = (Vector) addresses.elementAt(i);
        list.setElementAt(formatAddress((String) list.elementAt(0)), 0);
      }
    }
    Vector org = matchSingleVCardPrefixedField("ORG", rawText, true);
    Vector birthday = matchSingleVCardPrefixedField("BDAY", rawText, true);
    if (birthday != null && !isLikeVCardDate((String) birthday.elementAt(0))) {
      birthday = null;
    }
    Vector title = matchSingleVCardPrefixedField("TITLE", rawText, true);
    Vector url = matchSingleVCardPrefixedField("URL", rawText, true);
    Vector instantMessenger = matchSingleVCardPrefixedField("IMPP", rawText, true);
    return new AddressBookParsedResult(toPrimaryValues(names), 
                                       null, 
                                       toPrimaryValues(phoneNumbers), 
                                       toTypes(phoneNumbers),
                                       toPrimaryValues(emails),
                                       toTypes(emails),
                                       toPrimaryValue(instantMessenger),
                                       toPrimaryValue(note),
                                       toPrimaryValues(addresses),
                                       toTypes(addresses),
                                       toPrimaryValue(org),
                                       toPrimaryValue(birthday),
                                       toPrimaryValue(title),
                                       toPrimaryValue(url));
  }

  private static Vector matchVCardPrefixedField(String prefix, 
                                                String rawText,
                                                boolean trim) {
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

      Vector metadata = null;
      boolean quotedPrintable = false;
      String quotedPrintableCharset = null;
      if (i > metadataStart) {
        // There was something after the tag, before colon
        for (int j = metadataStart + 1; j <= i; j++) {
          char c = rawText.charAt(j);
          if (c == ';' || c == ':') {
            String metadatum = rawText.substring(metadataStart+1, j);
            if (metadata == null) {
              metadata = new Vector(1);
            }
            metadata.addElement(metadatum);
            int equals = metadatum.indexOf('=');
            if (equals >= 0) {
              String key = metadatum.substring(0, equals);
              String value = metadatum.substring(equals+1);
              if ("ENCODING".equalsIgnoreCase(key)) {
                if ("QUOTED-PRINTABLE".equalsIgnoreCase(value)) {
                  quotedPrintable = true;
                }
              } else if ("CHARSET".equalsIgnoreCase(key)) {
                quotedPrintableCharset = value;
              }
            }
            metadataStart = j;
          }
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
        if (metadata == null) {
          Vector match = new Vector(1);
          match.addElement(element);
          matches.addElement(match);
        } else {
          metadata.insertElementAt(element, 0);
          matches.addElement(metadata);
        }
        i++;
      } else {
        i++;
      }

    }

    if (matches == null || matches.isEmpty()) {
      return null;
    }
    return matches;
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
    }
    if (c >= 'A' && c <= 'F') {
      return c - 'A' + 10;
    }
    if (c >= 'a' && c <= 'f') {
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

  static Vector matchSingleVCardPrefixedField(String prefix, 
                                              String rawText,
                                              boolean trim) {
    Vector values = matchVCardPrefixedField(prefix, rawText, trim);
    return values == null || values.isEmpty() ? null : (Vector) values.elementAt(0);
  }
  
  private static String toPrimaryValue(Vector list) {
    return list == null || list.isEmpty() ? null : (String) list.elementAt(0);
  }
  
  private static String[] toPrimaryValues(Vector lists) {
    if (lists == null || lists.isEmpty()) {
      return null;
    }
    Vector result = new Vector(lists.size());
    for (int i = 0; i < lists.size(); i++) {
      Vector list = (Vector) lists.elementAt(i);
      result.addElement(list.elementAt(0));
    }
    return toStringArray(result);
  }
  
  private static String[] toTypes(Vector lists) {
    if (lists == null || lists.isEmpty()) {
      return null;
    }
    Vector result = new Vector(lists.size());
    for (int j = 0; j < lists.size(); j++) {
      Vector list = (Vector) lists.elementAt(j);
      String type = null;
      for (int i = 1; i < list.size(); i++) {
        String metadatum = (String) list.elementAt(i);
        int equals = metadatum.indexOf('=');
        if (equals < 0) {
          // take the whole thing as a usable label
          type = metadatum;
          break;
        }
        if ("TYPE".equalsIgnoreCase(metadatum.substring(0, equals))) {
          type = metadatum.substring(equals + 1);
          break;
        }
      }
      result.addElement(type);
    }
    return toStringArray(result);
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
  private static void formatNames(Vector names) {
    if (names != null) {
      for (int i = 0; i < names.size(); i++) {
        Vector list = (Vector) names.elementAt(i);
        String name = (String) list.elementAt(0);
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
        list.setElementAt(newName.toString().trim(), 0);
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
