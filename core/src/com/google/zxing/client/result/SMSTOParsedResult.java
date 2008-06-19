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

/**
 * Represents a "SMSTO:" result, which specifies a number to SMS.
 *
 * @author srowen@google.com (Sean Owen)
 */
public final class SMSTOParsedResult extends ParsedReaderResult {

  private final String number;

  private SMSTOParsedResult(String number) {
    super(ParsedReaderResultType.SMSTO);
    this.number = number;
  }

  public static SMSTOParsedResult parse(Result result) {
    String rawText = result.getText();
    if (rawText == null || !rawText.startsWith("SMSTO:")) {
      return null;
    }
    String number = rawText.substring(6);
    return new SMSTOParsedResult(number);
  }

  public String getNumber() {
    return number;
  }

  public String getDisplayResult() {
    return number;
  }

  public String getSMSURI() {
    return "sms:" + number;
  }

}