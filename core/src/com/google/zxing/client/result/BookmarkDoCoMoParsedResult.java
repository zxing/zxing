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
 * @author srowen@google.com (Sean Owen)
 */
public final class BookmarkDoCoMoParsedResult extends AbstractDoCoMoParsedResult {

  private final String title;
  private final String uri;

  private BookmarkDoCoMoParsedResult(String title, String uri) {
    super(ParsedReaderResultType.BOOKMARK);
    this.title = title;
    this.uri = uri;
  }

  public static BookmarkDoCoMoParsedResult parse(Result result) {
    String rawText = result.getText();
    if (rawText == null || !rawText.startsWith("MEBKM:")) {
      return null;
    }
    String title = matchSinglePrefixedField("TITLE:", rawText);
    String[] rawUri = matchPrefixedField("URL:", rawText);
    if (rawUri == null) {
      return null;
    }
    String uri = rawUri[0];
    if (!URIParsedResult.isBasicallyValidURI(uri)) {
      return null;
    }
    return new BookmarkDoCoMoParsedResult(title, uri);
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