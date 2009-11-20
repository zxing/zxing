package com.google.zxing.client.result
{
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

import com.google.zxing.Result;
import com.google.zxing.common.flexdatatypes.Utils;

/**
 * Tries to parse results that are a URI of some kind.
 * 
 * @author Sean Owen
 */
public final class URIResultParser extends ResultParser {

  public function URIResultParser() {
  }

  public static function parse(result:Result):URIParsedResult {
    var rawText:String = result.getText();
    // We specifically handle the odd "URL" scheme here for simplicity
    if (rawText != null && Utils.startsWith(rawText,"URL:")) {
      rawText = rawText.substring(4);
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
  public static function  isBasicallyValidURI(uri:String):Boolean {

    if (uri == null || uri.indexOf(' ') >= 0 || uri.indexOf('\n') >= 0) {
      return false;
    }
    var period:int = uri.indexOf('.');
    // Look for period in a domain but followed by at least a two-char TLD
    return period < uri.length - 2 && (period >= 0 || uri.indexOf(':') >= 0);
  }

}
}