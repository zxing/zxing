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

import com.google.zxing.client.result.ParsedResult;
import com.google.zxing.client.result.ParsedResultType;

/**
 * @author srowen@google.com (Sean Owen)
 */
public final class MobileTagRichWebParsedResult extends ParsedResult {

  // Example: "http://www.tagserver.com/script.asp?id="
  static final String TAGSERVER_URI_PREFIX = System.getProperty("zxing.mobiletag.tagserver");

  private final String id;
  private final int action;

  MobileTagRichWebParsedResult(String id, int action) {
    super(ParsedResultType.MOBILETAG_RICH_WEB);
    this.id = id;
    this.action = action;
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