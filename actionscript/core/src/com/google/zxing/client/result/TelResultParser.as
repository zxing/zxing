package com.google.zxing.client.result
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


import com.google.zxing.Result;
import com.google.zxing.common.flexdatatypes.Utils;

/**
 * Parses a "tel:" URI result, which specifies a phone number.
 *
 * @author Sean Owen
 */
public final class TelResultParser extends ResultParser {

  public function TelResultParser() {
  }

  public static function parse(result:Result):TelParsedResult {
    var rawText:String = result.getText();
    if (rawText == null || (!Utils.startsWith(rawText,"tel:") && !Utils.startsWith(rawText,"TEL:"))) {
      return null;
    }
    // Normalize "TEL:" to "tel:"
    var telURI:String = Utils.startsWith(rawText,"TEL:") ? "tel:" + rawText.substring(4) : rawText;
    // Drop tel, query portion
    var queryStart:int = rawText.indexOf('?', 4);
    var number:String = queryStart < 0 ? rawText.substring(4) : rawText.substring(4, queryStart);
    return new TelParsedResult(number, telURI, null);
  }

}}