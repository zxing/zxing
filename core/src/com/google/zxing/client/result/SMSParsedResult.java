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

  private final String[] numbers;
  private final String[] vias;
  private final String subject;
  private final String body;

  public SMSParsedResult(String number,
                         String via,
                         String subject,
                         String body) {
    super(ParsedResultType.SMS);
    this.numbers = new String[] {number};
    this.vias = new String[] {via};
    this.subject = subject;
    this.body = body;
  }

  public SMSParsedResult(String[] numbers,
                         String[] vias,
                         String subject,
                         String body) {
    super(ParsedResultType.SMS);
    this.numbers = numbers;
    this.vias = vias;
    this.subject = subject;
    this.body = body;
  }

  public String getSMSURI() {
    StringBuilder result = new StringBuilder();
    result.append("sms:");
    boolean first = true;
    for (int i = 0; i < numbers.length; i++) {
      if (first) {
        first = false;
      } else {
        result.append(',');
      }
      result.append(numbers[i]);
      if (vias != null && vias[i] != null) {
        result.append(";via=");
        result.append(vias[i]);
      }
    }
    boolean hasBody = body != null;
    boolean hasSubject = subject != null;
    if (hasBody || hasSubject) {
      result.append('?');
      if (hasBody) {
        result.append("body=");
        result.append(body);
      }
      if (hasSubject) {
        if (hasBody) {
          result.append('&');
        }
        result.append("subject=");
        result.append(subject);
      }
    }
    return result.toString();
  }

  public String[] getNumbers() {
    return numbers;
  }

  public String[] getVias() {
    return vias;
  }

  public String getSubject() {
    return subject;
  }

  public String getBody() {
    return body;
  }

  @Override
  public String getDisplayResult() {
    StringBuilder result = new StringBuilder(100);
    maybeAppend(numbers, result);
    maybeAppend(subject, result);
    maybeAppend(body, result);
    return result.toString();
  }

}