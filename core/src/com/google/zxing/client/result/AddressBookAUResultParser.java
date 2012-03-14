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

import java.util.ArrayList;
import java.util.List;

/**
 * Implements KDDI AU's address book format. See
 * <a href="http://www.au.kddi.com/ezfactory/tec/two_dimensions/index.html">
 * http://www.au.kddi.com/ezfactory/tec/two_dimensions/index.html</a>.
 * (Thanks to Yuzo for translating!)
 *
 * @author Sean Owen
 */
public final class AddressBookAUResultParser extends ResultParser {

  @Override
  public AddressBookParsedResult parse(Result result) {
    String rawText = getMassagedText(result);
    // MEMORY is mandatory; seems like a decent indicator, as does end-of-record separator CR/LF
    if (!rawText.contains("MEMORY") || !rawText.contains("\r\n")) {
      return null;
    }

    // NAME1 and NAME2 have specific uses, namely written name and pronunciation, respectively.
    // Therefore we treat them specially instead of as an array of names.
    String name = matchSinglePrefixedField("NAME1:", rawText, '\r', true);
    String pronunciation = matchSinglePrefixedField("NAME2:", rawText, '\r', true);

    String[] phoneNumbers = matchMultipleValuePrefix("TEL", 3, rawText, true);
    String[] emails = matchMultipleValuePrefix("MAIL", 3, rawText, true);
    String note = matchSinglePrefixedField("MEMORY:", rawText, '\r', false);
    String address = matchSinglePrefixedField("ADD:", rawText, '\r', true);
    String[] addresses = address == null ? null : new String[] {address};
    return new AddressBookParsedResult(maybeWrap(name),
                                       pronunciation,
                                       phoneNumbers,
                                       null,
                                       emails,
                                       null,
                                       null,
                                       note,
                                       addresses,
                                       null,
                                       null,
                                       null,
                                       null,
                                       null);
  }

  private static String[] matchMultipleValuePrefix(String prefix,
                                                   int max,
                                                   String rawText,
                                                   boolean trim) {
    List<String> values = null;
    for (int i = 1; i <= max; i++) {
      String value = matchSinglePrefixedField(prefix + i + ':', rawText, '\r', trim);
      if (value == null) {
        break;
      }
      if (values == null) {
        values = new ArrayList<String>(max); // lazy init
      }
      values.add(value);
    }
    if (values == null) {
      return null;
    }
    return values.toArray(new String[values.size()]);
  }

}
