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

package com.google.zxing.client.result;

import com.google.zxing.Result;

/**
 * Implements the "BIZCARD" address book entry format, though this has been
 * largely reverse-engineered from examples observed in the wild -- still
 * looking for a definitive reference.
 *
 * @author srowen@google.com (Sean Owen)
 */
public final class BizcardResultParser extends AbstractDoCoMoResultParser {

  // Yes, we extend AbstractDoCoMoResultParser since the format is very much
  // like the DoCoMo MECARD format, but this is not technically one of 
  // DoCoMo's proposed formats

  public static AddressBookParsedResult parse(Result result) {
    String rawText = result.getText();
    if (rawText == null || !rawText.startsWith("BIZCARD:")) {
      return null;
    }
    String firstName = matchSinglePrefixedField("N:", rawText);
    String lastName = matchSinglePrefixedField("X:", rawText);
    String fullName = buildName(firstName, lastName);
    String title = matchSinglePrefixedField("T:", rawText);
    String org = matchSinglePrefixedField("C:", rawText);
    String address = matchSinglePrefixedField("A:", rawText);
    String phoneNumber1 = matchSinglePrefixedField("B:", rawText);
    String phoneNumber2 = matchSinglePrefixedField("F:", rawText);
    String email = matchSinglePrefixedField("E:", rawText);

    return new AddressBookParsedResult(maybeWrap(fullName),
                                       buildPhoneNumbers(phoneNumber1, phoneNumber2),
                                       maybeWrap(email),
                                       null,
                                       address,
                                       org,
                                       null,
                                       title);
  }

  private static String[] buildPhoneNumbers(String number1, String number2) {
    if (number1 == null) {
      return maybeWrap(number2);
    } else {
      return number2 == null ? new String[] { number1 } : new String[] { number1, number2 };
    }
  }

  private static String buildName(String firstName, String lastName) {
    if (firstName == null) {
      return lastName;
    } else {
      return lastName == null ? firstName : firstName + ' ' + lastName;
    }
  }

}
