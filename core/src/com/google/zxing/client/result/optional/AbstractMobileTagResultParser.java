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

package com.google.zxing.client.result.optional;

import com.google.zxing.client.result.ResultParser;

/**
 * <p>Superclass for classes encapsulating reader results encoded according
 * to the MobileTag Reader International Specification.</p>
 * 
 * @author Sean Owen
 */
abstract class AbstractMobileTagResultParser extends ResultParser {

  public static final int ACTION_DO = 1;
  public static final int ACTION_EDIT = 2;
  public static final int ACTION_SAVE = 4;

  static String[] matchDelimitedFields(String rawText, int maxItems) {
    String[] result = new String[maxItems];
    int item = 0;
    int i = 0;
    int max = rawText.length();
    while (item < maxItems && i < max) {
      int start = i; // Found the start of a match here
      boolean done = false;
      while (!done) {
        i = rawText.indexOf((int) '|', i);
        if (i < 0) {
          // No terminating end character? done. Set i such that loop terminates and break
          i = rawText.length();
          done = true;
        } else if (rawText.charAt(i - 1) == '\\') {
          // semicolon was escaped so continue
          i++;
        } else {
          // found a match
          if (start != i) {
            result[item] = unescapeBackslash(rawText.substring(start, i));
          }
          item++;
          i++;
          done = true;
        }
      }
    }
    if (item < maxItems) {
      return null;
    }
    return result;
  }

  static boolean isDigits(String s, int expectedLength) {
    if (s == null) {
      return true;
    }
    if (s.length() != expectedLength) {
      return false;
    }
    for (int i = 0; i < expectedLength; i++) {
      if (!Character.isDigit(s.charAt(i))) {
        return false;
      }
    }
    return true;
  }

}