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
      while (rawText.charAt(i) != ':') { // Skip until a colon
        i++;
      }
      i++; // skip colon
      int start = i; // Found the start of a match here
      i = rawText.indexOf((int) '\n', i); // Really, ends in \r\n
      if (i < 0) {
        // No terminating end character? uh, done. Set i such that loop terminates and break
        i = max;
      } else if (i > start) {
        // found a match
        if (matches == null) {
          matches = new Vector(3); // lazy init
        }
        String element = rawText.substring(start, i);
        if (trim) {
          element = element.trim();
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
