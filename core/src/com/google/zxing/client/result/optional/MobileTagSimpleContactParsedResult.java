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
import com.google.zxing.client.result.ParsedReaderResultType;

/**
 * <p>Represents a "simple contact" result encoded according to section 4.8 of the
 * MobileTag Reader International Specification.</p>
 *
 * @author srowen@google.com (Sean Owen)
 */
public final class MobileTagSimpleContactParsedResult extends AbstractMobileTagParsedResult {

  public static final String SERVICE_TYPE = "02";

  private final String fullName;
  private final String telephoneCell;
  private final String telephone;
  private final String email1;
  private final String email2;
  private final String address;
  private final String org;
  private final String birthday;
  private final String title;

  private MobileTagSimpleContactParsedResult(String fullName,
                                             String telephoneCell,
                                             String telephone,
                                             String email1,
                                             String email2,
                                             String address,
                                             String org,
                                             String birthday,
                                             String title) {
    super(ParsedReaderResultType.MOBILETAG_SIMPLE_CONTACT);
    this.fullName = fullName;
    this.telephoneCell = telephoneCell;
    this.telephone = telephone;
    this.email1 = email1;
    this.email2 = email2;
    this.address = address;
    this.org = org;
    this.birthday = birthday;
    this.title = title;
  }

  public static MobileTagSimpleContactParsedResult parse(Result result) {
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
    String title = matches[8];

    return new MobileTagSimpleContactParsedResult(fullName,
                                                  telephoneCell,
                                                  telephone,
                                                  email1,
                                                  email2,
                                                  address,
                                                  org,
                                                  birthday,
                                                  title);
  }


  public String getFullName() {
    return fullName;
  }

  public String getTelephoneCell() {
    return telephoneCell;
  }

  public String getTelephone() {
    return telephone;
  }

  public String getEmail1() {
    return email1;
  }

  public String getEmail2() {
    return email2;
  }

  public String getAddress() {
    return address;
  }

  public String getOrg() {
    return org;
  }

  public String getBirthday() {
    return birthday;
  }

  public String getTitle() {
    return title;
  }

  public String getDisplayResult() {
    StringBuffer result = new StringBuffer(fullName);
    maybeAppend(telephoneCell, result);
    maybeAppend(telephone, result);
    maybeAppend(email1, result);
    maybeAppend(email2, result);
    maybeAppend(address, result);
    maybeAppend(org, result);
    maybeAppend(birthday, result);
    maybeAppend(title, result);
    return result.toString();
  }

}