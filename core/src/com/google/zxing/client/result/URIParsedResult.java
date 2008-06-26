/*
 * Copyright 2007 ZXing authors
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
 * @author srowen@google.com (Sean Owen)
 */
public final class URIParsedResult extends ParsedResult {

  private final String uri;
  private final String title;

  public URIParsedResult(String uri, String title) {
    super(ParsedResultType.URI);
    this.uri = uri;
    this.title = title;
  }

  public String getURI() {
    return uri;
  }

  public String getTitle() {
    return title;
  }

  public String getDisplayResult() {
    if (title == null) {
      return uri;
    } else {
      return title + '\n' + uri;
    }
  }


}