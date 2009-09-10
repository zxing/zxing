/*
 * Copyright (C) 2008 ZXing authors
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

package com.google.zxing.client.android;

/**
 * The underlying data for a SBC result.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class SearchBookContentsResult {
  private static String query;

  private final String pageNumber;
  private final String snippet;
  private final boolean validSnippet;

  public SearchBookContentsResult(String pageNumber, String snippet, boolean validSnippet) {
    this.pageNumber = pageNumber;
    this.snippet = snippet;
    this.validSnippet = validSnippet;
  }

  public static void setQuery(String query) {
    SearchBookContentsResult.query = query;
  }

  public String getPageNumber() {
    return pageNumber;
  }

  public String getSnippet() {
    return snippet;
  }

  public boolean getValidSnippet() {
    return validSnippet;
  }

  public static String getQuery() {
    return query;
  }
}
