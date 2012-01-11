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

import com.google.zxing.Result;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tries to parse results that are a URI of some kind.
 * 
 * @author Sean Owen
 */
public final class URIResultParser extends ResultParser {

  private static final String PATTERN_END = 
      "(:\\d{1,5})?" + // maybe port
      "(/|\\?|$)"; // query, path or nothing
  private static final Pattern URL_WITH_PROTOCOL_PATTERN = Pattern.compile(
      "[a-zA-Z0-9]{2,}://" + // protocol
      "[a-zA-Z0-9\\-]+(\\.[a-zA-Z0-9\\-]+)*" + // host name elements
      PATTERN_END);
  private static final Pattern URL_WITHOUT_PROTOCOL_PATTERN = Pattern.compile(
      "([a-zA-Z0-9\\-]+\\.)+[a-zA-Z0-9\\-]{2,}" + // host name elements
      PATTERN_END);

  @Override
  public URIParsedResult parse(Result result) {
    String rawText = result.getText();
    // We specifically handle the odd "URL" scheme here for simplicity
    if (rawText.startsWith("URL:")) {
      rawText = rawText.substring(4);
    }
    rawText = rawText.trim();
    return isBasicallyValidURI(rawText) ? new URIParsedResult(rawText, null) : null;
  }

  static boolean isBasicallyValidURI(CharSequence uri) {
    Matcher m = URL_WITH_PROTOCOL_PATTERN.matcher(uri);
    if (m.find() && m.start() == 0) { // match at start only
      return true;
    }
    m = URL_WITHOUT_PROTOCOL_PATTERN.matcher(uri);
    return m.find() && m.start() == 0;
  }

}