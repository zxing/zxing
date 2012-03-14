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

package com.google.zxing.client.result;

import com.google.zxing.Result;

import java.util.Map;

/**
 * Represents a result that encodes an e-mail address, either as a plain address
 * like "joe@example.org" or a mailto: URL like "mailto:joe@example.org".
 *
 * @author Sean Owen
 */
public final class EmailAddressResultParser extends ResultParser {

  @Override
  public EmailAddressParsedResult parse(Result result) {
    String rawText = getMassagedText(result);
    String emailAddress;
    if (rawText.startsWith("mailto:") || rawText.startsWith("MAILTO:")) {
      // If it starts with mailto:, assume it is definitely trying to be an email address
      emailAddress = rawText.substring(7);
      int queryStart = emailAddress.indexOf('?');
      if (queryStart >= 0) {
        emailAddress = emailAddress.substring(0, queryStart);
      }
      Map<String,String> nameValues = parseNameValuePairs(rawText);
      String subject = null;
      String body = null;
      if (nameValues != null) {
        if (emailAddress.length() == 0) {
          emailAddress = nameValues.get("to");
        }
        subject = nameValues.get("subject");
        body = nameValues.get("body");
      }
      return new EmailAddressParsedResult(emailAddress, subject, body, rawText);
    } else {
      if (!EmailDoCoMoResultParser.isBasicallyValidEmailAddress(rawText)) {
        return null;
      }
      emailAddress = rawText;
      return new EmailAddressParsedResult(emailAddress, null, null, "mailto:" + emailAddress);
    }
  }

}