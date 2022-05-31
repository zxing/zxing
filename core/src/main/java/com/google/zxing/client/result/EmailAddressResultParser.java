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
import java.util.regex.Pattern;

/**
 * Represents a result that encodes an e-mail address, either as a plain address
 * like "joe@example.org" or a mailto: URL like "mailto:joe@example.org".
 *
 * @author Sean Owen
 */
public final class EmailAddressResultParser extends ResultParser {

  private static final Pattern COMMA = Pattern.compile(",");

  @Override
  public EmailAddressParsedResult parse(Result result) {
    String rawText = getMassagedText(result);
    if (rawText.startsWith("mailto:") || rawText.startsWith("MAILTO:")) {
      // If it starts with mailto:, assume it is definitely trying to be an email address
      String hostEmail = rawText.substring(7);
      int queryStart = hostEmail.indexOf('?');
      if (queryStart >= 0) {
        hostEmail = hostEmail.substring(0, queryStart);
      }
      try {
        hostEmail = urlDecode(hostEmail);
      } catch (IllegalArgumentException iae) {
        return null;
      }
      String[] tos = null;
      if (!hostEmail.isEmpty()) {
        tos = COMMA.split(hostEmail);
      }
      Map<String,String> nameValues = parseNameValuePairs(rawText);
      String[] ccs = null;
      String[] bccs = null;
      String subject = null;
      String body = null;
      if (nameValues != null) {
        if (tos == null) {
          String tosString = nameValues.get("to");
          if (tosString != null) {
            tos = COMMA.split(tosString);
          }
        }
        String ccString = nameValues.get("cc");
        if (ccString != null) {
          ccs = COMMA.split(ccString);
        }
        String bccString = nameValues.get("bcc");
        if (bccString != null) {
          bccs = COMMA.split(bccString);
        }
        subject = nameValues.get("subject");
        body = nameValues.get("body");
      }
      return new EmailAddressParsedResult(tos, ccs, bccs, subject, body);
    } else {
      if (!EmailDoCoMoResultParser.isBasicallyValidEmailAddress(rawText)) {
        return null;
      }
      return new EmailAddressParsedResult(rawText);
    }
  }

}