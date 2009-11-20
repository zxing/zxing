package com.google.zxing.client.result.optional
{
	
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
	import com.google.zxing.client.result.URIParsedResult;
	import com.google.zxing.common.flexdatatypes.Utils;
	import com.google.zxing.Result;
	import com.google.zxing.BarcodeFormat;

/**
 * <p>Represents a "simple web" result encoded according to section 4.11 of the
 * MobileTag Reader International Specification.</p>
 *
 * @author Sean Owen
 */
public final class MobileTagSimpleWebResultParser extends AbstractMobileTagResultParser {

  public static var SERVICE_TYPE:String = "04";
  private static var URI_PREFIXES:Array = [
      null,
      "http://",
      "http://www.",
      "https://",
      "https://www.",
      "rtsp://",
  ];

  public static function parse(result:Result):URIParsedResult {
    if (result.getBarcodeFormat() != BarcodeFormat.DATAMATRIX) {
      return null;
    }
    var rawText:String = result.getText();
    if (!Utils.startsWith(rawText,SERVICE_TYPE)) {
      return null;
    }

    var matches:Array = matchDelimitedFields(rawText.substring(2), 2);
    if (matches == null) {
      return null;
    }
    var uri:String = matches[0];
    var title:String = matches[1];

    var maybePrefixChar:String = uri.charAt(2);
    if (maybePrefixChar >= '0' && maybePrefixChar <= '9') {
      var prefixIndex:int = (maybePrefixChar).charCodeAt(0) - ('0').charCodeAt(0);
      // Note that '0' is reserved
      if (prefixIndex >= 1 && prefixIndex < URI_PREFIXES.length) {
        uri = URI_PREFIXES[prefixIndex] + uri.substring(1);
      } else {
        uri = uri.substring(1);
      }
    }

    return new URIParsedResult(uri, title);
  }

}
}