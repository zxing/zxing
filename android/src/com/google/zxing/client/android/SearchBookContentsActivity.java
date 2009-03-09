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

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpUriRequest;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public final class SearchBookContentsActivity extends Activity {

  private static final String TAG = "SearchBookContents";
  private static final String USER_AGENT = "ZXing/1.3 (Android)";

  private NetworkThread mNetworkThread;
  private String mISBN;
  private EditText mQueryTextView;
  private Button mQueryButton;
  private ListView mResultListView;
  private TextView mHeaderView;

  @Override
  public void onCreate(Bundle icicle) {
    super.onCreate(icicle);

    // Make sure that expired cookies are removed on launch.
    CookieSyncManager.createInstance(this);
    CookieManager.getInstance().removeExpiredCookie();

    Intent intent = getIntent();
    if (intent == null || (!intent.getAction().equals(Intents.SearchBookContents.ACTION) &&
        !intent.getAction().equals(Intents.SearchBookContents.DEPRECATED_ACTION))) {
      finish();
      return;
    }

    mISBN = intent.getStringExtra(Intents.SearchBookContents.ISBN);
    setTitle(getString(R.string.sbc_name) + ": ISBN " + mISBN);

    setContentView(R.layout.search_book_contents);
    mQueryTextView = (EditText) findViewById(R.id.query_text_view);

    String initialQuery = intent.getStringExtra(Intents.SearchBookContents.QUERY);
    if (initialQuery != null && initialQuery.length() > 0) {
      // Populate the search box but don't trigger the search
      mQueryTextView.setText(initialQuery);
    }
    mQueryTextView.setOnKeyListener(mKeyListener);

    mQueryButton = (Button) findViewById(R.id.query_button);
    mQueryButton.setOnClickListener(mButtonListener);

    mResultListView = (ListView) findViewById(R.id.result_list_view);
    LayoutInflater factory = LayoutInflater.from(this);
    mHeaderView = (TextView) factory.inflate(R.layout.search_book_contents_header,
        mResultListView, false);
    mResultListView.addHeaderView(mHeaderView);
  }

  @Override
  protected void onResume() {
    super.onResume();
    mQueryTextView.selectAll();
  }

  @Override
  public void onConfigurationChanged(Configuration config) {
    // Do nothing, this is to prevent the activity from being restarted when the keyboard opens.
    super.onConfigurationChanged(config);
  }

  public final Handler mHandler = new Handler() {
    @Override
    public void handleMessage(Message message) {
      switch (message.what) {
        case R.id.search_book_contents_succeeded:
          handleSearchResults((JSONObject) message.obj);
          resetForNewQuery();
          break;
        case R.id.search_book_contents_failed:
          resetForNewQuery();
          mHeaderView.setText(R.string.msg_sbc_failed);
          break;
      }
    }
  };

  private void resetForNewQuery() {
    mNetworkThread = null;
    mQueryTextView.setEnabled(true);
    mQueryTextView.selectAll();
    mQueryButton.setEnabled(true);
  }

  private final Button.OnClickListener mButtonListener = new Button.OnClickListener() {
    public void onClick(View view) {
      launchSearch();
    }
  };

  private final View.OnKeyListener mKeyListener = new View.OnKeyListener() {
    public boolean onKey(View view, int keyCode, KeyEvent event) {
      if (keyCode == KeyEvent.KEYCODE_ENTER) {
        launchSearch();
        return true;
      }
      return false;
    }
  };

  private void launchSearch() {
    if (mNetworkThread == null) {
      String query = mQueryTextView.getText().toString();
      if (query != null && query.length() > 0) {
        mNetworkThread = new NetworkThread(mISBN, query, mHandler);
        mNetworkThread.start();
        mHeaderView.setText(R.string.msg_sbc_searching_book);
        mResultListView.setAdapter(null);
        mQueryTextView.setEnabled(false);
        mQueryButton.setEnabled(false);
      }
    }
  }

  // Currently there is no way to distinguish between a query which had no results and a book
  // which is not searchable - both return zero results.
  private void handleSearchResults(JSONObject json) {
    try {
      int count = json.getInt("number_of_results");
      mHeaderView.setText("Found " + ((count == 1) ? "1 result" : count + " results"));
      if (count > 0) {
        JSONArray results = json.getJSONArray("search_results");
        SearchBookContentsResult.setQuery(mQueryTextView.getText().toString());
        List<SearchBookContentsResult> items = new ArrayList<SearchBookContentsResult>(count);
        for (int x = 0; x < count; x++) {
          items.add(parseResult(results.getJSONObject(x)));
        }
        mResultListView.setAdapter(new SearchBookContentsAdapter(this, items));
      } else {
        String searchable = json.optString("searchable");
        if (searchable != null && searchable.equals("false")) {
          mHeaderView.setText(R.string.msg_sbc_book_not_searchable);
        }
        mResultListView.setAdapter(null);
      }
    } catch (JSONException e) {
      Log.e(TAG, e.toString());
      mResultListView.setAdapter(null);
      mHeaderView.setText(R.string.msg_sbc_failed);
    }
  }

  // Available fields: page_number, page_id, page_url, snippet_text
  private SearchBookContentsResult parseResult(JSONObject json) {
    try {
      String pageNumber = json.getString("page_number");
      if (pageNumber.length() > 0) {
        pageNumber = getString(R.string.msg_sbc_page) + ' ' + pageNumber;
      } else {
        // This can happen for text on the jacket, and possibly other reasons.
        pageNumber = getString(R.string.msg_sbc_unknown_page);
      }

      // Remove all HTML tags and encoded characters. Ideally the server would do this.
      String snippet = json.optString("snippet_text");
      boolean valid = true;
      if (snippet.length() > 0) {
        snippet = snippet.replaceAll("\\<.*?\\>", "");
        snippet = snippet.replaceAll("&lt;", "<");
        snippet = snippet.replaceAll("&gt;", ">");
        snippet = snippet.replaceAll("&#39;", "'");
        snippet = snippet.replaceAll("&quot;", "\"");
      } else {
        snippet = '(' + getString(R.string.msg_sbc_snippet_unavailable) + ')';
        valid = false;
      }
      return new SearchBookContentsResult(pageNumber, snippet, valid);
    } catch (JSONException e) {
      // Never seen in the wild, just being complete.
      return new SearchBookContentsResult(getString(R.string.msg_sbc_no_page_returned), "", false);
    }
  }

  private static final class NetworkThread extends Thread {

    private final String mISBN;
    private final String mQuery;
    private final Handler mHandler;

    NetworkThread(String isbn, String query, Handler handler) {
      mISBN = isbn;
      mQuery = query;
      mHandler = handler;
    }

    @Override
    public void run() {
      AndroidHttpClient client = null;
      try {
        // These return a JSON result which describes if and where the query was found. This API may
        // break or disappear at any time in the future. Since this is an API call rather than a
        // website, we don't use LocaleManager to change the TLD.
        URI uri = new URI("http", null, "www.google.com", -1, "/books", "vid=isbn" + mISBN +
            "&jscmd=SearchWithinVolume2&q=" + mQuery, null);
        HttpUriRequest get = new HttpGet(uri);
        get.setHeader("cookie", getCookie(uri.toString()));
        client = AndroidHttpClient.newInstance(USER_AGENT);
        HttpResponse response = client.execute(get);
        if (response.getStatusLine().getStatusCode() == 200) {
          HttpEntity entity = response.getEntity();
          ByteArrayOutputStream jsonHolder = new ByteArrayOutputStream();
          entity.writeTo(jsonHolder);
          jsonHolder.flush();
          JSONObject json = new JSONObject(jsonHolder.toString(getEncoding(entity)));
          jsonHolder.close();

          Message message = Message.obtain(mHandler, R.id.search_book_contents_succeeded);
          message.obj = json;
          message.sendToTarget();
        } else {
          Log.e(TAG, "HTTP returned " + response.getStatusLine().getStatusCode() + " for " + uri);
          Message message = Message.obtain(mHandler, R.id.search_book_contents_failed);
          message.sendToTarget();
        }
      } catch (Exception e) {
        Log.e(TAG, e.toString());
        Message message = Message.obtain(mHandler, R.id.search_book_contents_failed);
        message.sendToTarget();
      } finally {
        if (client != null) {
          client.close();
        }
      }
    }

    // Book Search requires a cookie to work, which we store persistently. If the cookie does
    // not exist, this could be the first search or it has expired. Either way, do a quick HEAD
    // request to fetch it, save it via the CookieSyncManager to flash, then return it.
    private String getCookie(String url) {
      String cookie = CookieManager.getInstance().getCookie(url);
      if (cookie == null || cookie.length() == 0) {
        Log.v(TAG, "Book Search cookie was missing or expired");
        HttpUriRequest head = new HttpHead(url);
        AndroidHttpClient client = AndroidHttpClient.newInstance(USER_AGENT);
        try {
          HttpResponse response = client.execute(head);
          if (response.getStatusLine().getStatusCode() == 200) {
            Header[] cookies = response.getHeaders("set-cookie");
            for (int x = 0; x < cookies.length; x++) {
              CookieManager.getInstance().setCookie(url, cookies[x].getValue());
            }
            CookieSyncManager.getInstance().sync();
            cookie = CookieManager.getInstance().getCookie(url);
          }
        } catch (IOException e) {
          Log.e(TAG, e.toString());
        }
        client.close();
      }
      return cookie;
    }

    private static String getEncoding(HttpEntity entity) {
      // FIXME: The server is returning ISO-8859-1 but the content is actually windows-1252.
      // Once Jeff fixes the HTTP response, remove this hardcoded value and go back to getting
      // the encoding dynamically.
      return "windows-1252";
//            HeaderElement[] elements = entity.getContentType().getElements();
//            if (elements != null && elements.length > 0) {
//                String encoding = elements[0].getParameterByName("charset").getValue();
//                if (encoding != null && encoding.length() > 0) {
//                    return encoding;
//                }
//            }
//            return "UTF-8";
    }
  }

}
