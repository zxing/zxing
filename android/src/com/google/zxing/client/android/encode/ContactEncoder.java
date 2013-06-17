/*
 * Copyright (C) 2011 ZXing authors
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

package com.google.zxing.client.android.encode;

import java.util.Collection;
import java.util.HashSet;

/**
 * Implementations encode according to some scheme for encoding contact information, like VCard or
 * MECARD.
 *
 * @author Sean Owen
 */
abstract class ContactEncoder {

  /**
   * @return first, the best effort encoding of all data in the appropriate format; second, a
   *   display-appropriate version of the contact information
   */
  abstract String[] encode(Iterable<String> names,
                           String organization,
                           Iterable<String> addresses,
                           Iterable<String> phones,
                           Iterable<String> emails,
                           Iterable<String> urls,
                           String note);

  /**
   * @return null if s is null or empty, or result of s.trim() otherwise
   */
  static String trim(String s) {
    if (s == null) {
      return null;
    }
    String result = s.trim();
    return result.isEmpty() ? null : result;
  }

  static void doAppend(StringBuilder newContents,
                             StringBuilder newDisplayContents,
                             String prefix,
                             String value,
                             Formatter fieldFormatter,
                             char terminator) {
    String trimmed = trim(value);
    if (trimmed != null) {
      newContents.append(prefix).append(':').append(fieldFormatter.format(trimmed)).append(terminator);
      newDisplayContents.append(trimmed).append('\n');
    }
  }

  static void doAppendUpToUnique(StringBuilder newContents,
                                 StringBuilder newDisplayContents,
                                 String prefix,
                                 Iterable<String> values,
                                 int max,
                                 Formatter formatter,
                                 Formatter fieldFormatter,
                                 char terminator) {
    if (values == null) {
      return;
    }
    int count = 0;
    Collection<String> uniques = new HashSet<String>(2);
    for (String value : values) {
      String trimmed = trim(value);
      if (trimmed != null && !trimmed.isEmpty() && !uniques.contains(trimmed)) {
        newContents.append(prefix).append(':').append(fieldFormatter.format(trimmed)).append(terminator);
        String display = formatter == null ? trimmed : formatter.format(trimmed);
        newDisplayContents.append(display).append('\n');
        if (++count == max) {
          break;
        }
        uniques.add(trimmed);
      }
    }
  }

}
