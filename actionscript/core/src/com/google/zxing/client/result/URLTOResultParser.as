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
 * Parses the "URLTO" result format, which is of the form "URLTO:[title]:[url]".
 * This seems to be used sometimes, but I am not able to find documentation
 * on its origin or official format?
 *
 * @author Sean Owen
 */
public final class URLTOResultParser {

  public function URLTOResultParser() {
  }

  public static function  parse(result:Result):URIParsedResult {
    var rawText:String = result.getText();
    if (rawText == null || (!Utils.startsWith(rawText,"urlto:") && !Utils.startsWith(rawText,"URLTO:"))) {
      return null;
    }
    var titleEnd:int = rawText.indexOf(':', 6);
    if (titleEnd < 0) {
      return null;
    }
    var title:String = titleEnd <= 6 ? null : rawText.substring(6, titleEnd);
    var uri:String = rawText.substring(titleEnd + 1);
    return new URIParsedResult(uri, title);
  }

}
}