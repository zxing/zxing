/*
 * Copyright 2012 ZXing authors
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

package com.google.zxing.client.android.result.supplement;

import android.widget.TextView;
import com.google.zxing.client.android.HttpHelper;
import com.google.zxing.client.android.history.HistoryManager;
import com.google.zxing.client.result.URIParsedResult;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Retrieves the title of a web page as supplemental info.
 *
 * @author Sean Owen
 */
final class TitleRetriever extends SupplementalInfoRetriever {

  private static final Pattern TITLE_PATTERN = Pattern.compile("<title>([^<]+)");
  private static final int MAX_TITLE_LEN = 100;

  private final String httpUrl;

  TitleRetriever(TextView textView, URIParsedResult result, HistoryManager historyManager) {
    super(textView, historyManager);
    this.httpUrl = result.getURI();
  }

  @Override
  void retrieveSupplementalInfo() {
    CharSequence contents;
    try {
      contents = HttpHelper.downloadViaHttp(httpUrl, HttpHelper.ContentType.HTML, 4096);
    } catch (IOException ioe) {
      // ignore this
      return;
    }
    if (contents != null && contents.length() > 0) {
      Matcher m = TITLE_PATTERN.matcher(contents);
      if (m.find()) {
        String title = m.group(1);
        if (title != null && !title.isEmpty()) {
          if (title.length() > MAX_TITLE_LEN) {
            title = title.substring(0, MAX_TITLE_LEN) + "...";
          }
          append(httpUrl, null, new String[] {title}, httpUrl);
        }
      }
    }
  }

}
