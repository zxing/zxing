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
 * @author Sean Owen
 */
public final class NDEFSmartPosterParsedResult extends ParsedResult {

  public static final int ACTION_UNSPECIFIED = -1;
  public static final int ACTION_DO = 0;
  public static final int ACTION_SAVE = 1;
  public static final int ACTION_OPEN = 2;

  private final String title;
  private final String uri;
  private final int action;

  NDEFSmartPosterParsedResult(int action, String uri, String title) {
    super(ParsedResultType.NDEF_SMART_POSTER);
    this.action = action;
    this.uri = uri;
    this.title = title;
  }

  public String getTitle() {
    return title;
  }

  public String getURI() {
    return uri;
  }

  public int getAction() {
    return action;
  }

  public String getDisplayResult() {
    if (title == null) {
      return uri;
    } else {
      return title + '\n' + uri;
    }
  }

}