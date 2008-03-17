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
 * Represents a result that encodes an e-mail address, either as a plain address
 * like "joe@example.org" or a mailto: URL like "mailto:joe@example.org".
 *
 * @author srowen@google.com (Sean Owen)
 */
public final class EmailAddressParsedResult extends AbstractDoCoMoParsedResult {

  private final String emailAddress;

  private EmailAddressParsedResult(String emailAddress) {
    super(ParsedReaderResultType.EMAIL_ADDRESS);
    this.emailAddress = emailAddress;
  }

  public static EmailAddressParsedResult parse(Result result) {
    String rawText = result.getText();
    String emailAddress;
    if (rawText.startsWith("mailto:")) {
      // If it starts with mailto:, assume it is definitely trying to be an email address
      emailAddress = rawText.substring(7);
    } else {
      if (!EmailDoCoMoParsedResult.isBasicallyValidEmailAddress(rawText)) {
        return null;
      }
      emailAddress = rawText;
    }
    return new EmailAddressParsedResult(emailAddress);
  }

  public String getEmailAddress() {
    return emailAddress;
  }

  public String getDisplayResult() {
    return emailAddress;
  }

}