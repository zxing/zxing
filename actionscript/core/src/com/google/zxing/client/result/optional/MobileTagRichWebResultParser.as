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
import com.google.zxing.common.flexdatatypes.Utils;
import com.google.zxing.Result;
import com.google.zxing.BarcodeFormat;

/**
 * <p>Represents a "rich web" result encoded according to section 5 of the
 * MobileTag Reader International Specification.</p>
 *
 * @author Sean Owen
 */
public final class MobileTagRichWebResultParser extends AbstractMobileTagResultParser {

  public static var SERVICE_TYPE:String = "54";
  private static var DEFAULT_ACTION:int = AbstractMobileTagResultParser.ACTION_DO;

  public static function parse(result:Result,tagserver:String):MobileTagRichWebParsedResult {
    if (MobileTagRichWebParsedResult.TAGSERVER_URI_PREFIX == null) {
      return null;
    }
    if (result.getBarcodeFormat() != BarcodeFormat.DATAMATRIX) {
      return null;
    }
    var rawText:String = result.getText();
    if (!Utils.startsWith(rawText,SERVICE_TYPE)) {
      return null;
    }

    var length:int = rawText.length;
    if (!isDigits(rawText, length)) {
      return null;
    }
    var action:int;
    var id:String;
    if (length == 15) {
      action = DEFAULT_ACTION;
      id = rawText.substring(0, 2) + action + rawText.substring(2);
    } else if (length == 16) {
      action = rawText.charCodeAt(2) - ('0').charCodeAt(0);
      id = rawText;
    } else {
      return null;
    }

    return new MobileTagRichWebParsedResult(id, action,tagserver);
  }

}}