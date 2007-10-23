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

import java.util.ArrayList;
import java.util.List;

/**
 * See
 * <a href="http://www.nttdocomo.co.jp/english/service/imode/make/content/barcode/about/s2.html">
 * DoCoMo's documentation</a> about the result types represented by subclasses of this class.
 *
 * @author srowen@google.com (Sean Owen)
 */
abstract class AbstractDoCoMoResult extends ParsedReaderResult {

  AbstractDoCoMoResult(ParsedReaderResultType type) {
    super(type);
  }

  // This could as well be implemented with java.util.regex. It was already implemented partially
  // to run in a J2ME enviroment, where this unavailable.

  static String[] matchPrefixedField(String prefix, String rawText) {
    List<String> matches = null;
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
        i = rawText.indexOf((int) ';', i);
        if (i < 0) {
          // No terminating semicolon? uh, done. Set i such that loop terminates and break
          i = rawText.length();
          done = true;
        } else if (rawText.charAt(i-1) == '\\') {
          // semicolon was escaped so continue
          i++;
        } else {
          // found a match
          if (matches == null) {
            matches = new ArrayList<String>(3); // lazy init
          }
          matches.add(unescape(rawText.substring(start, i)));
          i++;
          done = true;
        }
      }
    }
    if (matches == null) {
      return null;
    }
    return matches.toArray(new String[matches.size()]);
  }

  static String matchSinglePrefixedField(String prefix, String rawText) {
    String[] matches = matchPrefixedField(prefix, rawText);
    return matches == null ? null : matches[0];
  }

  static String[] matchRequiredPrefixedField(String prefix, String rawText) {
    String[] result = matchPrefixedField(prefix, rawText);
    if (result == null) {
      throw new IllegalArgumentException("Did not match prefix " + prefix);
    }
    return result;
  }

  private static String unescape(String escaped) {
    if (escaped != null) {
      int backslash = escaped.indexOf((int) '\\');
      if (backslash >= 0) {
        int max = escaped.length();
        StringBuilder unescaped = new StringBuilder(max - 1);
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

  static void maybeAppend(String value, StringBuilder result) {
    if (value != null) {
      result.append('\n');
      result.append(value);
    }
  }

}