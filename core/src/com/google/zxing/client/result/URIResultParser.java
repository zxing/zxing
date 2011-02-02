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
 * Tries to parse results that are a URI of some kind.
 * 
 * @author Sean Owen
 */
final class URIResultParser extends ResultParser {

  private URIResultParser() {
  }

  public static URIParsedResult parse(Result result) {
    String rawText = result.getText();
    // We specifically handle the odd "URL" scheme here for simplicity
    if (rawText != null && rawText.startsWith("URL:")) {
      rawText = rawText.substring(4);
    }
    if (rawText != null) {
      rawText = rawText.trim();
    }
    if (!isBasicallyValidURI(rawText)) {
      return null;
    }
    return new URIParsedResult(rawText, null);
  }

  /**
   * Determines whether a string is not obviously not a URI. This implements crude checks; this class does not
   * intend to strictly check URIs as its only function is to represent what is in a barcode, but, it does
   * need to know when a string is obviously not a URI.
   */
  static boolean isBasicallyValidURI(String uri) {
    if (uri == null || uri.indexOf(' ') >= 0 || uri.indexOf('\n') >= 0) {
      return false;
    }
    // Look for period in a domain but followed by at least a two-char TLD
    // Forget strings that don't have a valid-looking protocol
    int period = uri.indexOf('.');
    if (period >= uri.length() - 2) {
      return false;
    }
    int colon = uri.indexOf(':');
    if (period < 0 && colon < 0) {
      return false;
    }
    if (colon >= 0) {
      if (period < 0 || period > colon) {
        // colon ends the protocol
        for (int i = 0; i < colon; i++) {
          char c = uri.charAt(i);
          if ((c < 'a' || c > 'z') && (c < 'A' || c > 'Z')) {
            return false;
          }
        }
      } else {
        // colon starts the port; crudely look for at least two numbers
        if (colon >= uri.length() - 2) {
          return false;
        }
        for (int i = colon + 1; i < colon + 3; i++) {
          char c = uri.charAt(i);
          if (c < '0' || c > '9') {
            return false;
          }
        }
      }
    }
    return true;
  }

}