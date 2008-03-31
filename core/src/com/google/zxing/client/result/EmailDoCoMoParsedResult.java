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

/**
 * Implements the "MATMSG" email message entry format.
 *
 * Supported keys: TO, SUB, BODY
 *
 * @author srowen@google.com (Sean Owen)
 */
public final class EmailDoCoMoParsedResult extends AbstractDoCoMoParsedResult {

  private final String to;
  private final String subject;
  private final String body;

  private EmailDoCoMoParsedResult(String to, String subject, String body) {
    super(ParsedReaderResultType.EMAIL);
    this.to = to;
    this.subject = subject;
    this.body = body;
  }

  public static EmailDoCoMoParsedResult parse(Result result) {
    String rawText = result.getText();
    if (rawText == null || !rawText.startsWith("MATMSG:")) {
      return null;
    }
    String[] rawTo = matchPrefixedField("TO:", rawText);
    if (rawTo == null) {
      return null;
    }
    String to = rawTo[0];
    if (!isBasicallyValidEmailAddress(to)) {
      return null;
    }
    String subject = matchSinglePrefixedField("SUB:", rawText);
    String body = matchSinglePrefixedField("BODY:", rawText);
    return new EmailDoCoMoParsedResult(to, subject, body);
  }

  public String getTo() {
    return to;
  }

  public String getSubject() {
    return subject;
  }

  public String getBody() {
    return body;
  }

  public String getDisplayResult() {
    StringBuffer result = new StringBuffer(to);
    maybeAppend(subject, result);
    maybeAppend(body, result);
    return result.toString();
  }

  /**
   * This implements only the most basic checking for an email address's validity -- that it contains
   * an '@' and a '.' somewhere after that, and that it contains no space.
   * We want to generally be lenient here since this class is only intended to encapsulate what's
   * in a barcode, not "judge" it.
   */
  static boolean isBasicallyValidEmailAddress(String email) {
    if (email == null) {
      return false;
    }
    int atIndex = email.indexOf('@');
    return atIndex >= 0 && email.indexOf('.') > atIndex && email.indexOf(' ') < 0;
  }

}