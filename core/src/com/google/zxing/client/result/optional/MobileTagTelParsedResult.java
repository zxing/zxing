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

package com.google.zxing.client.result.optional;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.Result;
import com.google.zxing.client.result.ParsedReaderResultType;

/**
 * <p>Represents a "TEL" result encoded according to section 4.4 of the
 * MobileTag Reader International Specification.</p>
 * 
 * @author srowen@google.com (Sean Owen)
 */
public final class MobileTagTelParsedResult extends AbstractMobileTagParsedResult {

  public static final String SERVICE_TYPE = "01";

  private final String number;
  private final String title;

  private MobileTagTelParsedResult(String number, String title) {
    super(ParsedReaderResultType.MOBILETAG_TEL);
    this.number = number;
    this.title = title;
  }

  public static MobileTagTelParsedResult parse(Result result) {
    if (!result.getBarcodeFormat().equals(BarcodeFormat.DATAMATRIX)) {
      return null;
    }
    String rawText = result.getText();
    if (!rawText.startsWith(SERVICE_TYPE)) {
      return null;
    }

    String[] matches = matchDelimitedFields(rawText.substring(2), 2);
    if (matches == null) {
      return null;
    }
    String number = matches[0];
    String title = matches[1];

    return new MobileTagTelParsedResult(number, title);
  }

  public String getNumber() {
    return number;
  }

  public String getTitle() {
    return title;
  }

  public String getDisplayResult() {
    if (title == null) {
      return number;
    } else {
      return title + '\n' + number;
    }
  }

}