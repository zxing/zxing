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

/**
 * @author Sean Owen
 */
public final class SMSParsedResult extends ParsedResult {

  private final String smsURI;
  private final String number;
  private final String via;
  private final String subject;
  private final String body;
  private final String title;

  public SMSParsedResult(String smsURI, String number, String via, String subject, String body, String title) {
    super(ParsedResultType.SMS);
    this.smsURI = smsURI;
    this.number = number;
    this.via = via;
    this.subject = subject;
    this.body = body;
    this.title = title;
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

  public String getSubject() {
    return subject;
  }

  public String getBody() {
    return body;
  }

  public String getTitle() {
    return title;
  }

  public String getDisplayResult() {
    StringBuffer result = new StringBuffer(100);
    maybeAppend(number, result);
    maybeAppend(via, result);
    maybeAppend(subject, result);
    maybeAppend(body, result);
    maybeAppend(title, result);
    return result.toString();
  }

}