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

import com.google.zxing.client.android.R;

import android.content.Context;
import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Locale;

/**
 * A list item which displays the page number and snippet of this search result.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class SearchBookContentsListItem extends LinearLayout {
  private TextView pageNumberView;
  private TextView snippetView;

  SearchBookContentsListItem(Context context) {
    super(context);
  }

  public SearchBookContentsListItem(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  @Override
  protected void onFinishInflate() {
    super.onFinishInflate();
    pageNumberView = (TextView) findViewById(R.id.page_number_view);
    snippetView = (TextView) findViewById(R.id.snippet_view);
  }

  public void set(SearchBookContentsResult result) {
    pageNumberView.setText(result.getPageNumber());
    String snippet = result.getSnippet();
    if (snippet.isEmpty()) {
      snippetView.setText("");
    } else {
      if (result.getValidSnippet()) {
        String lowerQuery = SearchBookContentsResult.getQuery().toLowerCase(Locale.getDefault());
        String lowerSnippet = snippet.toLowerCase(Locale.getDefault());
        Spannable styledSnippet = new SpannableString(snippet);
        StyleSpan boldSpan = new StyleSpan(Typeface.BOLD);
        int queryLength = lowerQuery.length();
        int offset = 0;
        while (true) {
          int pos = lowerSnippet.indexOf(lowerQuery, offset);
          if (pos < 0) {
            break;
          }
          styledSnippet.setSpan(boldSpan, pos, pos + queryLength, 0);
          offset = pos + queryLength;
        }
        snippetView.setText(styledSnippet);
      } else {
        // This may be an error message, so don't try to bold the query terms within it
        snippetView.setText(snippet);
      }
    }
  }
}
