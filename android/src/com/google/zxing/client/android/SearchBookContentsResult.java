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

public final class SearchBookContentsResult {

  private static String sQuery;

  private final String mPageNumber;
  private final String mSnippet;
  private final boolean mValidSnippet;

  public SearchBookContentsResult(String pageNumber, String snippet, boolean validSnippet) {
    mPageNumber = pageNumber;
    mSnippet = snippet;
    mValidSnippet = validSnippet;
  }

  public static void setQuery(String query) {
    sQuery = query;
  }

  public String getPageNumber() {
    return mPageNumber;
  }

  public String getSnippet() {
    return mSnippet;
  }

  public boolean getValidSnippet() {
    return mValidSnippet;
  }

  public static String getQuery() {
    return sQuery;
  }

}
