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

package com.google.zxing.client.result;

/**
 * @author Sean Owen
 */
public final class TelParsedResult extends ParsedResult {

  private final String number;
  private final String telURI;
  private final String title;

  public TelParsedResult(String number, String telURI, String title) {
    super(ParsedResultType.TEL);
    this.number = number;
    this.telURI = telURI;
    this.title = title;
  }

  public String getNumber() {
    return number;
  }

  public String getTelURI() {
    return telURI;
  }

  public String getTitle() {
    return title;
  }

  public String getDisplayResult() {
    StringBuffer result = new StringBuffer(20);
    maybeAppend(number, result);
    maybeAppend(title, result);
    return result.toString();
  }

}