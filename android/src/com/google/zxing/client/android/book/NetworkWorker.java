/*
 * Copyright (C) 2010 ZXing authors
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

package com.google.zxing.client.android.book;

import android.os.Handler;
import android.os.Message;
import android.util.Log;
import com.google.zxing.client.android.HttpHelper;
import com.google.zxing.client.android.LocaleManager;
import com.google.zxing.client.android.R;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

final class NetworkWorker implements Runnable {

  private static final String TAG = NetworkWorker.class.getSimpleName();

  private final String isbn;
  private final String query;
  private final Handler handler;

  NetworkWorker(String isbn, String query, Handler handler) {
    this.isbn = isbn;
    this.query = query;
    this.handler = handler;
  }

  @Override
  public void run() {
    try {
      // These return a JSON result which describes if and where the query was found. This API may
      // break or disappear at any time in the future. Since this is an API call rather than a
      // website, we don't use LocaleManager to change the TLD.
      String uri;
      if (LocaleManager.isBookSearchUrl(isbn)) {
        int equals = isbn.indexOf('=');
        String volumeId = isbn.substring(equals + 1);
        uri = "http://www.google.com/books?id=" + volumeId + "&jscmd=SearchWithinVolume2&q=" + query;
      } else {
        uri = "http://www.google.com/books?vid=isbn" + isbn + "&jscmd=SearchWithinVolume2&q=" + query;
      }

      try {
        String content = HttpHelper.downloadViaHttp(uri, HttpHelper.ContentType.JSON);
        JSONObject json = new JSONObject(content);
        Message message = Message.obtain(handler, R.id.search_book_contents_succeeded);
        message.obj = json;
        message.sendToTarget();
      } catch (IOException ioe) {
        Message message = Message.obtain(handler, R.id.search_book_contents_failed);
        message.sendToTarget();
      }
    } catch (JSONException je) {
      Log.w(TAG, "Error accessing book search", je);
      Message message = Message.obtain(handler, R.id.search_book_contents_failed);
      message.sendToTarget();
    }
  }

}
