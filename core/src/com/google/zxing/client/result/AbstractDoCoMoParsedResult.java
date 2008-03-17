/*
 * Copyright 2007 Google Inc.
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

import java.util.Vector;

/**
 * <p>See
 * <a href="http://www.nttdocomo.co.jp/english/service/imode/make/content/barcode/about/s2.html">
 * DoCoMo's documentation</a> about the result types represented by subclasses of this class.</p>
 *
 * <p>Thanks to Jeff Griffin for proposing rewrite of these classes that relies less
 * on exception-based mechanisms during parsing.</p>
 *
 * @author srowen@google.com (Sean Owen)
 */
abstract class AbstractDoCoMoParsedResult extends ParsedReaderResult {

  AbstractDoCoMoParsedResult(ParsedReaderResultType type) {
    super(type);
  }

  // This could as well be implemented with java.util.regex. It was already implemented partially
  // to run in a J2ME enviroment, where this unavailable.

  static String[] matchPrefixedField(String prefix, String rawText) {
    return matchPrefixedField(prefix, rawText, ';');
  }

  static String[] matchPrefixedField(String prefix, String rawText, char endChar) {
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
          matches.addElement(unescape(rawText.substring(start, i)));
          i++;
          done = true;
        }
      }
    }
    if (matches == null || matches.isEmpty()) {
      return null;
    }
    int size = matches.size();
    String[] result = new String[size];
    for (int j = 0; j < size; j++) {
      result[j] = (String) matches.elementAt(j);
    }
    return result;
  }

  static String matchSinglePrefixedField(String prefix, String rawText) {
    return matchSinglePrefixedField(prefix, rawText, ';');
  }

  static String matchSinglePrefixedField(String prefix, String rawText, char endChar) {
    String[] matches = matchPrefixedField(prefix, rawText, endChar);
    return matches == null ? null : matches[0];
  }

  private static String unescape(String escaped) {
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

  static void maybeAppend(String value, StringBuffer result) {
    if (value != null) {
      result.append('\n');
      result.append(value);
    }
  }

  static void maybeAppend(String[] value, StringBuffer result) {
    if (value != null) {
      for (int i = 0; i < value.length; i++) {
        result.append('\n');
        result.append(value[i]);
      }
    }
  }

}