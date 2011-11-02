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

/**
 * @author Sean Owen
 */
public final class EmailAddressParsedResult extends ParsedResult {

  private final String emailAddress;
  private final String subject;
  private final String body;
  private final String mailtoURI;

  EmailAddressParsedResult(String emailAddress,
                           String subject,
                           String body,
                           String mailtoURI) {
    super(ParsedResultType.EMAIL_ADDRESS);
    this.emailAddress = emailAddress;
    this.subject = subject;
    this.body = body;
    this.mailtoURI = mailtoURI;
  }

  public String getEmailAddress() {
    return emailAddress;
  }

  public String getSubject() {
    return subject;
  }

  public String getBody() {
    return body;
  }

  public String getMailtoURI() {
    return mailtoURI;
  }

  @Override
  public String getDisplayResult() {
    StringBuilder result = new StringBuilder(30);
    maybeAppend(emailAddress, result);
    maybeAppend(subject, result);
    maybeAppend(body, result);
    return result.toString();
  }

}