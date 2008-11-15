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
 * A simple result type encapsulating a string that has no further
 * interpretation.
 * 
 * @author Sean Owen
 */
public final class TextParsedResult extends ParsedResult {

  private final String text;
  private final String language;

  public TextParsedResult(String text, String language) {
    super(ParsedResultType.TEXT);
    this.text = text;
    this.language = language;
  }

  public String getText() {
    return text;
  }

  public String getLanguage() {
    return language;
  }

  public String getDisplayResult() {
    return text;
  }

}
