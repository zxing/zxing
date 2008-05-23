/*
 * Copyright 2007 Google Inc.
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
 * Represents a result that encodes an e-mail address, either as a plain address
 * like "joe@example.org" or a mailto: URL like "mailto:joe@example.org".
 *
 * @author srowen@google.com (Sean Owen)
 */
public final class EmailAddressParsedResult extends AbstractDoCoMoParsedResult {

  private final String emailAddress;
  private final String subject;
  private final String body;
  private final String mailtoURI;

  private EmailAddressParsedResult(String emailAddress, String subject, String body, String mailtoURI) {
    super(ParsedReaderResultType.EMAIL_ADDRESS);
    this.emailAddress = emailAddress;
    this.subject = subject;
    this.body = body;
    this.mailtoURI = mailtoURI;
  }

  public static EmailAddressParsedResult parse(Result result) {
    String rawText = result.getText();
    if (rawText == null) {
      return null;
    }
    String emailAddress;
    if (rawText.startsWith("mailto:")) {
      // If it starts with mailto:, assume it is definitely trying to be an email address
      emailAddress = rawText.substring(7);
      int queryStart = emailAddress.indexOf('?');
      if (queryStart >= 0) {
        emailAddress = emailAddress.substring(0, queryStart);
      }
      Hashtable nameValues = parseNameValuePairs(rawText);
      if (emailAddress.length() == 0) {
        emailAddress = (String) nameValues.get("to");
      }
      String subject = (String) nameValues.get("subject");
      String body = (String) nameValues.get("body");
      return new EmailAddressParsedResult(emailAddress, subject, body, rawText);
    } else {
      if (!EmailDoCoMoParsedResult.isBasicallyValidEmailAddress(rawText)) {
        return null;
      }
      emailAddress = rawText;
      return new EmailAddressParsedResult(emailAddress, null, null, "mailto:" + emailAddress);
    }
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

  public String getDisplayResult() {
    return emailAddress;
  }

}