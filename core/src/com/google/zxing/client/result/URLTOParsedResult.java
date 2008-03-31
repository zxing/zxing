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
 * "URLTO" result format, which is of the form "URLTO:[title]:[url]".
 * This seems to be used sometimes, but I am not able to find documentation
 * on its origin or official format?
 *
 * @author srowen@google.com (Sean Owen)
 */
public final class URLTOParsedResult extends ParsedReaderResult {

  private final String title;
  private final String uri;

  private URLTOParsedResult(String title, String uri) {
    super(ParsedReaderResultType.URLTO);
    this.title = title;
    this.uri = uri;
  }

  public static URLTOParsedResult parse(Result result) {
    String rawText = result.getText();
    if (rawText == null || !rawText.startsWith("URLTO:")) {
      return null;
    }
    int titleEnd = rawText.indexOf(':', 6);
    if (titleEnd < 0) {
      return null;
    }
    String title = rawText.substring(6, titleEnd);
    String uri = rawText.substring(titleEnd + 1);
    return new URLTOParsedResult(title, uri);
  }

  public String getTitle() {
    return title;
  }

  public String getURI() {
    return uri;
  }

  public String getDisplayResult() {
    if (title == null) {
      return uri;
    } else {
      return title + '\n' + uri;
    }
  }

}