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
 * "URLTO" result format, which is of the form "URLTO:[title]:[url]".
 * This seems to be used sometimes, but I am not able to find documentation
 * on its origin or official format?
 *
 * @author srowen@google.com (Sean Owen)
 */
public final class URLTOResult extends ParsedReaderResult {

  private final String title;
  private final String uri;

  public URLTOResult(String rawText) {
    super(ParsedReaderResultType.URLTO);
    if (!rawText.startsWith("URLTO:")) {
      throw new IllegalArgumentException("Does not begin with URLTO");
    }
    int titleEnd = rawText.indexOf(':', 6);
    title = rawText.substring(6, titleEnd);
    uri = rawText.substring(titleEnd + 1);
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