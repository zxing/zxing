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
 * <p>Represents a "MMS" result encoded according to section 4.7 of the
 * MobileTag Reader International Specification.</p>
 *
 * @author srowen@google.com (Sean Owen)
 */
public final class MobileTagMMSParsedResult extends AbstractMobileTagParsedResult {

  public static final String SERVICE_TYPE = "05";

  private final String to;
  private final String subject;
  private final String body;
  private final String title;

  private MobileTagMMSParsedResult(String to, String subject, String body, String title) {
    super(ParsedReaderResultType.MOBILETAG_MMS);
    this.to = to;
    this.subject = subject;
    this.body = body;
    this.title = title;
  }

  public static MobileTagMMSParsedResult parse(Result result) {
    if (!result.getBarcodeFormat().equals(BarcodeFormat.DATAMATRIX)) {
      return null;
    }
    String rawText = result.getText();
    if (!rawText.startsWith(SERVICE_TYPE)) {
      return null;
    }

    String[] matches = matchDelimitedFields(rawText.substring(2), 4);
    if (matches == null) {
      return null;
    }
    String to = matches[0];
    String subject = matches[1];
    String body = matches[2];
    String title = matches[3];

    return new MobileTagMMSParsedResult(to, subject, body, title);
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

  public String getTitle() {
    return title;
  }

  public String getDisplayResult() {
    StringBuffer result = new StringBuffer(to);
    maybeAppend(subject, result);
    maybeAppend(title, result);
    maybeAppend(body, result);
    return result.toString();
  }

}