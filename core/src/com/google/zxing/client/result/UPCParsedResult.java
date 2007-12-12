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

/**
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class UPCParsedResult extends ParsedReaderResult {

  private final String upc;

  public UPCParsedResult(String rawText) {
    super(ParsedReaderResultType.UPC);
    int length = rawText.length();
    if (length != 12 && length != 13) {
      throw new IllegalArgumentException("Wrong number of digits for UPC");
    }
    for (int x = 0; x < length; x++) {
      char c = rawText.charAt(x);
      if (c < '0' || c > '9') {
        throw new IllegalArgumentException("Invalid character found in UPC");
      }
    }
    upc = rawText;
  }

  public String getUPC() {
    return upc;
  }

  public String getDisplayResult() {
    return upc;
  }

}
