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

package com.google.zxing.client.result.optional;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.Result;
import com.google.zxing.client.result.AddressBookParsedResult;

/**
 * <p>Represents a "simple contact" result encoded according to section 4.8 of the
 * MobileTag Reader International Specification.</p>
 *
 * @author Sean Owen
 */
final class MobileTagSimpleContactResultParser extends AbstractMobileTagResultParser {

  public static final String SERVICE_TYPE = "02";

  public static AddressBookParsedResult parse(Result result) {
    if (!result.getBarcodeFormat().equals(BarcodeFormat.DATAMATRIX)) {
      return null;
    }
    String rawText = result.getText();
    if (!rawText.startsWith(SERVICE_TYPE)) {
      return null;
    }

    String[] matches = matchDelimitedFields(rawText.substring(2), 9);
    if (matches == null || !isDigits(matches[7], 8)) {
      return null;
    }
    String fullName = matches[0];
    String telephoneCell = matches[1];
    String telephone = matches[2];
    String email1 = matches[3];
    String email2 = matches[4];
    String address = matches[5];
    String org = matches[6];
    String birthday = matches[7];
    if (!isStringOfDigits(birthday, 8)) {
      return null;
    }
    String title = matches[8];

    return new AddressBookParsedResult(new String[] {fullName},
                                       null,
                                       new String[] {telephoneCell, telephone},
                                       new String[] {email1, email2},
                                       null,
                                       address,
                                       org,
                                       birthday,
                                       title,
                                       null);
  }

}