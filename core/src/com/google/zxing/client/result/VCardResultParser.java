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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses contact information formatted according to the VCard (2.1) format. This is not a complete
 * implementation but should parse information as commonly encoded in 2D barcodes.
 *
 * @author Sean Owen
 */
public final class VCardResultParser extends ResultParser {

  private static final Pattern BEGIN_VCARD = Pattern.compile("BEGIN:VCARD", Pattern.CASE_INSENSITIVE);
  private static final Pattern VCARD_LIKE_DATE = Pattern.compile("\\d{4}-?\\d{2}-?\\d{2}");
  private static final Pattern CR_LF_SPACE_TAB = Pattern.compile("\r\n[ \t]");
  private static final Pattern NEWLINE_ESCAPE = Pattern.compile("\\\\[nN]");
  private static final Pattern VCARD_ESCAPES = Pattern.compile("\\\\([,;\\\\])");
  private static final Pattern EQUALS = Pattern.compile("=");
  private static final Pattern SEMICOLON = Pattern.compile(";");
  private static final Pattern UNESCAPED_SEMICOLONS = Pattern.compile("(?<!\\\\);+");

  @Override
  public AddressBookParsedResult parse(Result result) {
    // Although we should insist on the raw text ending with "END:VCARD", there's no reason
    // to throw out everything else we parsed just because this was omitted. In fact, Eclair
    // is doing just that, and we can't parse its contacts without this leniency.
    String rawText = getMassagedText(result);
    Matcher m = BEGIN_VCARD.matcher(rawText);
    if (!m.find() || m.start() != 0) {
      return null;
    }
    List<List<String>> names = matchVCardPrefixedField("FN", rawText, true, false);
    if (names == null) {
      // If no display names found, look for regular name fields and format them
      names = matchVCardPrefixedField("N", rawText, true, false);
      formatNames(names);
    }
    List<List<String>> phoneNumbers = matchVCardPrefixedField("TEL", rawText, true, false);
    List<List<String>> emails = matchVCardPrefixedField("EMAIL", rawText, true, false);
    List<String> note = matchSingleVCardPrefixedField("NOTE", rawText, false, false);
    List<List<String>> addresses = matchVCardPrefixedField("ADR", rawText, true, true);
    List<String> org = matchSingleVCardPrefixedField("ORG", rawText, true, true);
    List<String> birthday = matchSingleVCardPrefixedField("BDAY", rawText, true, false);
    if (birthday != null && !isLikeVCardDate(birthday.get(0))) {
      birthday = null;
    }
    List<String> title = matchSingleVCardPrefixedField("TITLE", rawText, true, false);
    List<String> url = matchSingleVCardPrefixedField("URL", rawText, true, false);
    List<String> instantMessenger = matchSingleVCardPrefixedField("IMPP", rawText, true, false);
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

  static List<List<String>> matchVCardPrefixedField(CharSequence prefix,
                                                    String rawText,
                                                    boolean trim,
                                                    boolean parseFieldDivider) {
    List<List<String>> matches = null;
    int i = 0;
    int max = rawText.length();

    while (i < max) {

      // At start or after newline, match prefix, followed by optional metadata 
      // (led by ;) ultimately ending in colon
      Matcher matcher = Pattern.compile("(?:^|\n)" + prefix + "(?:;([^:]*))?:",
                                        Pattern.CASE_INSENSITIVE).matcher(rawText);
      if (i > 0) {
        i--; // Find from i-1 not i since looking at the preceding character
      }
      if (!matcher.find(i)) {
        break;
      }
      i = matcher.end(0); // group 0 = whole pattern; end(0) is past final colon

      String metadataString = matcher.group(1); // group 1 = metadata substring
      List<String> metadata = null;
      boolean quotedPrintable = false;
      String quotedPrintableCharset = null;
      if (metadataString != null) {
        for (String metadatum : SEMICOLON.split(metadataString)) {
          if (metadata == null) {
            metadata = new ArrayList<String>(1);
          }
          metadata.add(metadatum);
          String[] metadatumTokens = EQUALS.split(metadatum, 2);
          if (metadatumTokens.length > 1) {
            String key = metadatumTokens[0];
            String value = metadatumTokens[1];
            if ("ENCODING".equalsIgnoreCase(key) && "QUOTED-PRINTABLE".equalsIgnoreCase(value)) {
              quotedPrintable = true;
            } else if ("CHARSET".equalsIgnoreCase(key)) {
              quotedPrintableCharset = value;
            }
          }
        }
      }

      int matchStart = i; // Found the start of a match here

      while ((i = rawText.indexOf((int) '\n', i)) >= 0) { // Really, end in \r\n
        if (i < rawText.length() - 1 &&           // But if followed by tab or space,
            (rawText.charAt(i+1) == ' ' ||        // this is only a continuation
             rawText.charAt(i+1) == '\t')) {
          i += 2; // Skip \n and continutation whitespace
        } else if (quotedPrintable &&             // If preceded by = in quoted printable
                   ((i >= 1 && rawText.charAt(i-1) == '=') || // this is a continuation
                    (i >= 2 && rawText.charAt(i-2) == '='))) {
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
          matches = new ArrayList<List<String>>(1); // lazy init
        }
        if (i >= 1 && rawText.charAt(i-1) == '\r') {
          i--; // Back up over \r, which really should be there
        }
        String element = rawText.substring(matchStart, i);
        if (trim) {
          element = element.trim();
        }
        if (quotedPrintable) {
          element = decodeQuotedPrintable(element, quotedPrintableCharset);
          if (parseFieldDivider) {
            element = UNESCAPED_SEMICOLONS.matcher(element).replaceAll("\n").trim();
          }
        } else {
          if (parseFieldDivider) {
            element = UNESCAPED_SEMICOLONS.matcher(element).replaceAll("\n").trim();
          }
          element = CR_LF_SPACE_TAB.matcher(element).replaceAll("");
          element = NEWLINE_ESCAPE.matcher(element).replaceAll("\n");
          element = VCARD_ESCAPES.matcher(element).replaceAll("$1");
        }
        if (metadata == null) {
          List<String> match = new ArrayList<String>(1);
          match.add(element);
          matches.add(match);
        } else {
          metadata.add(0, element);
          matches.add(metadata);
        }
        i++;
      } else {
        i++;
      }

    }

    return matches;
  }

  private static String decodeQuotedPrintable(CharSequence value, String charset) {
    int length = value.length();
    StringBuilder result = new StringBuilder(length);
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
            if (nextChar != '\r' && nextChar != '\n') {
              char nextNextChar = value.charAt(i+2);
              int firstDigit = parseHexDigit(nextChar);
              int secondDigit = parseHexDigit(nextNextChar);
              if (firstDigit >= 0 && secondDigit >= 0) {
                fragmentBuffer.write((firstDigit << 4) + secondDigit);
              } // else ignore it, assume it was incorrectly encoded
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

  private static void maybeAppendFragment(ByteArrayOutputStream fragmentBuffer,
                                          String charset,
                                          StringBuilder result) {
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

  static List<String> matchSingleVCardPrefixedField(CharSequence prefix,
                                                    String rawText,
                                                    boolean trim,
                                                    boolean parseFieldDivider) {
    List<List<String>> values = matchVCardPrefixedField(prefix, rawText, trim, parseFieldDivider);
    return values == null || values.isEmpty() ? null : values.get(0);
  }
  
  private static String toPrimaryValue(List<String> list) {
    return list == null || list.isEmpty() ? null : list.get(0);
  }
  
  private static String[] toPrimaryValues(Collection<List<String>> lists) {
    if (lists == null || lists.isEmpty()) {
      return null;
    }
    List<String> result = new ArrayList<String>(lists.size());
    for (List<String> list : lists) {
      result.add(list.get(0));
    }
    return result.toArray(new String[lists.size()]);
  }
  
  private static String[] toTypes(Collection<List<String>> lists) {
    if (lists == null || lists.isEmpty()) {
      return null;
    }
    List<String> result = new ArrayList<String>(lists.size());
    for (List<String> list : lists) {
      String type = null;
      for (int i = 1; i < list.size(); i++) {
        String metadatum = list.get(i);
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
      result.add(type);
    }
    return result.toArray(new String[lists.size()]);
  }

  private static boolean isLikeVCardDate(CharSequence value) {
    return value == null || VCARD_LIKE_DATE.matcher(value).matches();
  }

  /**
   * Formats name fields of the form "Public;John;Q.;Reverend;III" into a form like
   * "Reverend John Q. Public III".
   *
   * @param names name values to format, in place
   */
  private static void formatNames(Iterable<List<String>> names) {
    if (names != null) {
      for (List<String> list : names) {
        String name = list.get(0);
        String[] components = new String[5];
        int start = 0;
        int end;
        int componentIndex = 0;
        while (componentIndex < components.length - 1 && (end = name.indexOf(';', start)) > 0) {
          components[componentIndex] = name.substring(start, end);
          componentIndex++;
          start = end + 1;
        }
        components[componentIndex] = name.substring(start);
        StringBuilder newName = new StringBuilder(100);
        maybeAppendComponent(components, 3, newName);
        maybeAppendComponent(components, 1, newName);
        maybeAppendComponent(components, 2, newName);
        maybeAppendComponent(components, 0, newName);
        maybeAppendComponent(components, 4, newName);
        list.set(0, newName.toString().trim());
      }
    }
  }

  private static void maybeAppendComponent(String[] components, int i, StringBuilder newName) {
    if (components[i] != null) {
      newName.append(' ');
      newName.append(components[i]);
    }
  }

}
