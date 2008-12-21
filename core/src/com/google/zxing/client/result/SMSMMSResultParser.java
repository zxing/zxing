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

import java.util.Hashtable;

/**
 * <p>Parses an "sms:" URI result, which specifies a number to SMS and optional
 * "via" number. See <a href="http://gbiv.com/protocols/uri/drafts/draft-antti-gsm-sms-url-04.txt">
 * the IETF draft</a> on this.</p>
 *
 * <p>This actually also parses URIs starting with "mms:", "smsto:", "mmsto:", "SMSTO:", and
 * "MMSTO:", and treats them all the same way, and effectively converts them to an "sms:" URI
 * for purposes of forwarding to the platform.</p>
 *
 * @author Sean Owen
 */
final class SMSMMSResultParser extends ResultParser {

  private SMSMMSResultParser() {
  }

  public static SMSParsedResult parse(Result result) {
    String rawText = result.getText();
    if (rawText == null) {
      return null;
    }
    int prefixLength;
    if (rawText.startsWith("sms:") || rawText.startsWith("SMS:") ||
        rawText.startsWith("mms:") || rawText.startsWith("MMS:")) {
      prefixLength = 4;
    } else if (rawText.startsWith("smsto:") || rawText.startsWith("SMSTO:") ||
               rawText.startsWith("mmsto:") || rawText.startsWith("MMSTO:")) {
      prefixLength = 6;
    } else {
      return null;
    }

    // Check up front if this is a URI syntax string with query arguments
    Hashtable nameValuePairs = parseNameValuePairs(rawText);
    String subject = null;
    String body = null;
    boolean querySyntax = false;
    if (nameValuePairs != null && !nameValuePairs.isEmpty()) {
      subject = (String) nameValuePairs.get("subject");
      body = (String) nameValuePairs.get("body");
      querySyntax = true;
    }

    // Drop sms, query portion
    int queryStart = rawText.indexOf('?', prefixLength);
    String smsURIWithoutQuery;
    // If it's not query syntax, the question mark is part of the subject or message
    if (queryStart < 0 || !querySyntax) {
      smsURIWithoutQuery = rawText.substring(prefixLength);
    } else {
      smsURIWithoutQuery = rawText.substring(prefixLength, queryStart);
    }
    int numberEnd = smsURIWithoutQuery.indexOf(';');
    String number;
    String via;
    if (numberEnd < 0) {
      number = smsURIWithoutQuery;
      via = null;
    } else {
      number = smsURIWithoutQuery.substring(0, numberEnd);
      String maybeVia = smsURIWithoutQuery.substring(numberEnd + 1);
      if (maybeVia.startsWith("via=")) {
        via = maybeVia.substring(4);
      } else {
        via = null;
      }
    }

    // Thanks to dominik.wild for suggesting this enhancement to support
    // smsto:number:body URIs
    if (body == null) {
      int bodyStart = number.indexOf(':');
      if (bodyStart >= 0) {
        body = number.substring(bodyStart + 1);
        number = number.substring(0, bodyStart);
      }
    }
    return new SMSParsedResult("sms:" + number, number, via, subject, body, null);
  }

}