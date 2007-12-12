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

/**
 * Represents a result that encodes an e-mail address, either as a plain address
 * like "joe@example.org" or a mailto: URL like "mailto:joe@example.org".
 *
 * @author srowen@google.com (Sean Owen)
 */
public final class EmailAddressResult extends AbstractDoCoMoResult {

  private final String emailAddress;

  public EmailAddressResult(String rawText) {
    super(ParsedReaderResultType.EMAIL_ADDRESS);
    if (rawText.startsWith("mailto:")) {
      // If it starts with mailto:, assume it is definitely trying to be an email address
      emailAddress = rawText.substring(7);
    } else {
      if (!EmailDoCoMoResult.isBasicallyValidEmailAddress(rawText)) {
        throw new IllegalArgumentException("Invalid email address: " + rawText);
      }
      emailAddress = rawText;
    }
  }

  public String getEmailAddress() {
    return emailAddress;
  }

  public String getDisplayResult() {
    return emailAddress;
  }

}