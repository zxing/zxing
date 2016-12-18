/*
 * Copyright (C) 2009 ZXing authors
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

import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.AdapterView;
import com.google.zxing.client.android.LocaleManager;

import java.util.List;

final class BrowseBookListener implements AdapterView.OnItemClickListener {

  private final SearchBookContentsActivity activity;
  private final List<SearchBookContentsResult> items;

  BrowseBookListener(SearchBookContentsActivity activity, List<SearchBookContentsResult> items) {
    this.activity = activity;
    this.items = items;
  }

  @Override
  public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
    if (position < 1) {
      // Clicked header, ignore it
      return;
    }
    int itemOffset = position - 1;
    if (itemOffset >= items.size()) {
      return;
    }
    String pageId = items.get(itemOffset).getPageId();
    String query = SearchBookContentsResult.getQuery();
    if (LocaleManager.isBookSearchUrl(activity.getISBN()) && !pageId.isEmpty()) {
      String uri = activity.getISBN();
      int equals = uri.indexOf('=');
      String volumeId = uri.substring(equals + 1);
      String readBookURI = "http://books.google." +
          LocaleManager.getBookSearchCountryTLD(activity) +
          "/books?id=" + volumeId + "&pg=" + pageId + "&vq=" + query;
      Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(readBookURI));
      intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);                    
      activity.startActivity(intent);
    }
  }
}
