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

package com.google.zxing.client.android.book;

import android.app.Activity;
import android.content.Intent;
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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import com.google.zxing.client.android.LocaleManager;
import com.google.zxing.client.android.R;
import com.google.zxing.client.android.Intents;

/**
 * Uses Google Book Search to find a word or phrase in the requested book.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class SearchBookContentsActivity extends Activity {

  private static final String TAG = SearchBookContentsActivity.class.getSimpleName();

  private static final Pattern TAG_PATTERN = Pattern.compile("\\<.*?\\>");
  private static final Pattern LT_ENTITY_PATTERN = Pattern.compile("&lt;");
  private static final Pattern GT_ENTITY_PATTERN = Pattern.compile("&gt;");
  private static final Pattern QUOTE_ENTITY_PATTERN = Pattern.compile("&#39;");
  private static final Pattern QUOT_ENTITY_PATTERN = Pattern.compile("&quot;");

  private Thread networkThread;
  private String isbn;
  private EditText queryTextView;
  private Button queryButton;
  private ListView resultListView;
  private TextView headerView;

  private final Handler handler = new Handler() {
    @Override
    public void handleMessage(Message message) {
      switch (message.what) {
        case R.id.search_book_contents_succeeded:
          handleSearchResults((JSONObject) message.obj);
          resetForNewQuery();
          break;
        case R.id.search_book_contents_failed:
          resetForNewQuery();
          headerView.setText(R.string.msg_sbc_failed);
          break;
      }
    }
  };

  private final Button.OnClickListener buttonListener = new Button.OnClickListener() {
    @Override
    public void onClick(View view) {
      launchSearch();
    }
  };

  private final View.OnKeyListener keyListener = new View.OnKeyListener() {
    @Override
    public boolean onKey(View view, int keyCode, KeyEvent event) {
      if (keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN) {
        launchSearch();
        return true;
      }
      return false;
    }
  };

  String getISBN() {
    return isbn;
  }

  @Override
  public void onCreate(Bundle icicle) {
    super.onCreate(icicle);

    // Make sure that expired cookies are removed on launch.
    CookieSyncManager.createInstance(this);
    CookieManager.getInstance().removeExpiredCookie();

    Intent intent = getIntent();
    if (intent == null || !intent.getAction().equals(Intents.SearchBookContents.ACTION)) {
      finish();
      return;
    }

    isbn = intent.getStringExtra(Intents.SearchBookContents.ISBN);
    if (LocaleManager.isBookSearchUrl(isbn)) {
      setTitle(getString(R.string.sbc_name));
    } else {
      setTitle(getString(R.string.sbc_name) + ": ISBN " + isbn);
    }

    setContentView(R.layout.search_book_contents);
    queryTextView = (EditText) findViewById(R.id.query_text_view);

    String initialQuery = intent.getStringExtra(Intents.SearchBookContents.QUERY);
    if (initialQuery != null && initialQuery.length() > 0) {
      // Populate the search box but don't trigger the search
      queryTextView.setText(initialQuery);
    }
    queryTextView.setOnKeyListener(keyListener);

    queryButton = (Button) findViewById(R.id.query_button);
    queryButton.setOnClickListener(buttonListener);

    resultListView = (ListView) findViewById(R.id.result_list_view);
    LayoutInflater factory = LayoutInflater.from(this);
    headerView = (TextView) factory.inflate(R.layout.search_book_contents_header,
        resultListView, false);
    resultListView.addHeaderView(headerView);
  }

  @Override
  protected void onResume() {
    super.onResume();
    queryTextView.selectAll();
  }

  private void resetForNewQuery() {
    networkThread = null;
    queryTextView.setEnabled(true);
    queryTextView.selectAll();
    queryButton.setEnabled(true);
  }

  private void launchSearch() {
    if (networkThread == null) {
      String query = queryTextView.getText().toString();
      if (query != null && query.length() > 0) {
        networkThread = new Thread(new NetworkWorker(isbn, query, handler));
        networkThread.start();
        headerView.setText(R.string.msg_sbc_searching_book);
        resultListView.setAdapter(null);
        queryTextView.setEnabled(false);
        queryButton.setEnabled(false);
      }
    }
  }

  // Currently there is no way to distinguish between a query which had no results and a book
  // which is not searchable - both return zero results.
  private void handleSearchResults(JSONObject json) {
    try {
      int count = json.getInt("number_of_results");
      headerView.setText("Found " + (count == 1 ? "1 result" : count + " results"));
      if (count > 0) {
        JSONArray results = json.getJSONArray("search_results");
        SearchBookContentsResult.setQuery(queryTextView.getText().toString());
        List<SearchBookContentsResult> items = new ArrayList<SearchBookContentsResult>(count);
        for (int x = 0; x < count; x++) {
          items.add(parseResult(results.getJSONObject(x)));
        }
        resultListView.setOnItemClickListener(new BrowseBookListener(this, items));
        resultListView.setAdapter(new SearchBookContentsAdapter(this, items));
      } else {
        String searchable = json.optString("searchable");
        if ("false".equals(searchable)) {
          headerView.setText(R.string.msg_sbc_book_not_searchable);
        }
        resultListView.setAdapter(null);
      }
    } catch (JSONException e) {
      Log.w(TAG, "Bad JSON from book search", e);
      resultListView.setAdapter(null);
      headerView.setText(R.string.msg_sbc_failed);
    }
  }

  // Available fields: page_id, page_number, page_url, snippet_text
  private SearchBookContentsResult parseResult(JSONObject json) {
    try {
      String pageId = json.getString("page_id");
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
        snippet = TAG_PATTERN.matcher(snippet).replaceAll("");
        snippet = LT_ENTITY_PATTERN.matcher(snippet).replaceAll("<");
        snippet = GT_ENTITY_PATTERN.matcher(snippet).replaceAll(">");
        snippet = QUOTE_ENTITY_PATTERN.matcher(snippet).replaceAll("'");
        snippet = QUOT_ENTITY_PATTERN.matcher(snippet).replaceAll("\"");
      } else {
        snippet = '(' + getString(R.string.msg_sbc_snippet_unavailable) + ')';
        valid = false;
      }
      return new SearchBookContentsResult(pageId, pageNumber, snippet, valid);
    } catch (JSONException e) {
      // Never seen in the wild, just being complete.
      return new SearchBookContentsResult(getString(R.string.msg_sbc_no_page_returned), "", "", false);
    }
  }

}
