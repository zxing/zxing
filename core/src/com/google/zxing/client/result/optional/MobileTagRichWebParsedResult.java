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

package com.google.zxing.client.result.optional;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.Result;
import com.google.zxing.client.result.ParsedReaderResultType;

/**
 * <p>Represents a "rich web" result encoded according to section 5 of the
 * MobileTag Reader International Specification.</p>
 *
 * @author srowen@google.com (Sean Owen)
 */
public final class MobileTagRichWebParsedResult extends AbstractMobileTagParsedResult {

  public static final String SERVICE_TYPE = "54";

  private static final int DEFAULT_ACTION = ACTION_DO;
  // Example: "http://www.tagserver.com/script.asp?id="
  private static final String TAGSERVER_URI_PREFIX = System.getProperty("zxing.mobiletag.tagserver");

  private final String id;
  private final int action;

  private MobileTagRichWebParsedResult(String id, int action) {
    super(ParsedReaderResultType.MOBILETAG_RICH_WEB);
    this.id = id;
    this.action = action;
  }

  public static MobileTagRichWebParsedResult parse(Result result) {
    if (TAGSERVER_URI_PREFIX == null) {
      return null;
    }
    if (!result.getBarcodeFormat().equals(BarcodeFormat.DATAMATRIX)) {
      return null;
    }
    String rawText = result.getText();
    if (!rawText.startsWith(SERVICE_TYPE)) {
      return null;
    }

    int length = rawText.length();
    if (!isDigits(rawText, length)) {
      return null;
    }
    int action;
    String id;
    if (length == 15) {
      action = DEFAULT_ACTION;
      id = rawText.substring(0, 2) + action + rawText.substring(2);
    } else if (length == 16) {
      action = rawText.charAt(2) - '0';
      id = rawText;
    } else {
      return null;
    }

    return new MobileTagRichWebParsedResult(id, action);
  }

  public static String getTagserverURIPrefix() {
    return TAGSERVER_URI_PREFIX;
  }

  public String getId() {
    return id;
  }

  public int getAction() {
    return action;
  }

  public String getTagserverURI() {
    return TAGSERVER_URI_PREFIX + id;
  }

  public String getDisplayResult() {
    return id;
  }

}