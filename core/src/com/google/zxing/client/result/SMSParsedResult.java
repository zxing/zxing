/*
 * Copyright 2008 Google Inc.
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
 * Represents a "sms:" URI result, which specifies a number to SMS and optional
 * "via" number. See <a href="http://gbiv.com/protocols/uri/drafts/draft-antti-gsm-sms-url-04.txt">
 * the IETF draft</a> on this.
 *
 * @author srowen@google.com (Sean Owen)
 */
public final class SMSParsedResult extends ParsedReaderResult {

  private final String smsURI;
  private final String number;
  private final String via;

  private SMSParsedResult(String smsURI, String number, String via) {
    super(ParsedReaderResultType.SMS);
    this.smsURI = smsURI;
    this.number = number;
    this.via = via;
  }

  public static SMSParsedResult parse(Result result) {
    String rawText = result.getText();
    if (rawText == null || !rawText.startsWith("sms:")) {
      return null;
    }
    // Drop sms, query portion
    int queryStart = rawText.indexOf('?', 4);
    String smsURIWithoutQuery;
    if (queryStart < 0) {
      smsURIWithoutQuery = rawText.substring(4);
    } else {
      smsURIWithoutQuery = rawText.substring(4, queryStart);
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
    return new SMSParsedResult(rawText, number, via);
  }

  public String getSMSURI() {
    return smsURI;
  }

  public String getNumber() {
    return number;
  }

  public String getVia() {
    return via;
  }

  public String getDisplayResult() {
    return number;
  }

}