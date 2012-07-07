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

/**
 * <p>Abstract class representing the result of decoding a barcode, as more than
 * a String -- as some type of structured data. This might be a subclass which represents
 * a URL, or an e-mail address. {@link ResultParser#parseResult(Result)} will turn a raw
 * decoded string into the most appropriate type of structured representation.</p>
 *
 * <p>Thanks to Jeff Griffin for proposing rewrite of these classes that relies less
 * on exception-based mechanisms during parsing.</p>
 *
 * @author Sean Owen
 */
public abstract class ParsedResult {

  private final ParsedResultType type;

  protected ParsedResult(ParsedResultType type) {
    this.type = type;
  }

  public final ParsedResultType getType() {
    return type;
  }

  public abstract String getDisplayResult();

  @Override
  public final String toString() {
    return getDisplayResult();
  }

  public static void maybeAppend(String value, StringBuilder result) {
    if (value != null && value.length() > 0) {
      // Don't add a newline before the first value
      if (result.length() > 0) {
        result.append('\n');
      }
      result.append(value);
    }
  }

  public static void maybeAppend(String[] value, StringBuilder result) {
    if (value != null) {
      for (String s : value) {
        if (s != null && s.length() > 0) {
          if (result.length() > 0) {
            result.append('\n');
          }
          result.append(s);
        }
      }
    }
  }

}
