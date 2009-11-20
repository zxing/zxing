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
import com.google.zxing.client.result.AddressBookParsedResult;
import com.google.zxing.common.flexdatatypes.Utils;
/**
 * <p>Represents a "simple contact" result encoded according to section 4.8 of the
 * MobileTag Reader International Specification.</p>
 *
 * @author Sean Owen
 */
public final class MobileTagSimpleContactResultParser extends AbstractMobileTagResultParser {

  public static var SERVICE_TYPE:String = "02";

  public static function parse(result:Result):AddressBookParsedResult {
    if (result.getBarcodeFormat() != BarcodeFormat.DATAMATRIX) {
      return null;
    }
    var rawText:String  = result.getText();
    if (!Utils.startsWith(rawText,SERVICE_TYPE)) {
      return null;
    }

    var matches:Array = matchDelimitedFields(rawText.substring(2), 9);
    if (matches == null || !isDigits(matches[7], 8)) {
      return null;
    }
    var fullName:String = matches[0];
    var telephoneCell:String = matches[1];
    var telephone:String = matches[2];
    var email1:String = matches[3];
    var email2:String = matches[4];
    var address:String = matches[5];
    var org:String = matches[6];
    var birthday:String = matches[7];
    if (!isStringOfDigits(birthday, 8)) {
      return null;
    }
    var title:String = matches[8];

    return new AddressBookParsedResult([fullName],
                                       null,
                                       [telephoneCell, telephone],
                                       [email1, email2],
                                       null,
                                       address,
                                       org,
                                       birthday,
                                       title,
                                       null);
  }

}
}