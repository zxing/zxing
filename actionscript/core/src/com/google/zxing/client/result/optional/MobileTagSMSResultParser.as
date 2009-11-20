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

import com.google.zxing.BarcodeFormat;
import com.google.zxing.Result;
import com.google.zxing.client.result.SMSParsedResult;
import com.google.zxing.common.flexdatatypes.Utils;

/**
 * <p>Represents a "SMS" result encoded according to section 4.6 of the
 * MobileTag Reader International Specification.</p>
 *
 * @author Sean Owen
 */
public final class MobileTagSMSResultParser extends AbstractMobileTagResultParser {

  public static var SERVICE_TYPE:String = "03";

  public static function parse(result:Result):SMSParsedResult {
    if (result.getBarcodeFormat() != BarcodeFormat.DATAMATRIX) {
      return null;
    }
    var rawText:String = result.getText();
    if (!Utils.startsWith(rawText,SERVICE_TYPE)) {
      return null;
    }

    var matches:Array = matchDelimitedFields(rawText.substring(2), 3);
    if (matches == null) {
      return null;
    }
    var _to:String = matches[0];
    var body:String = matches[1];
    var title:String = matches[2];

    return new SMSParsedResult("sms:" + _to, _to, null, null, body, title);
  }

}
}