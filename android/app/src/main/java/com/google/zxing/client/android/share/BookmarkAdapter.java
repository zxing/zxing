/*
 * Copyright (C) 2011 ZXing authors
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

package com.google.zxing.client.android.share;

import java.util.List;

import com.google.zxing.client.android.R;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * A custom adapter designed to fetch bookmarks from a cursor. Before Honeycomb we used
 * SimpleCursorAdapter, but it assumes the existence of an _id column, and the bookmark schema was
 * rewritten for HC without one. This caused the app to crash, hence this new class, which is
 * forwards and backwards compatible.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
final class BookmarkAdapter extends BaseAdapter {

  private final Context context;
  private final List<String[]> titleURLs;

  BookmarkAdapter(Context context, List<String[]> titleURLs) {
    this.context = context;
    this.titleURLs = titleURLs;
  }

  @Override
  public int getCount() {
    return titleURLs.size();
  }

  @Override
  public Object getItem(int index) {
    return titleURLs.get(index);
  }

  @Override
  public long getItemId(int index) {
    return index;
  }

  @Override
  public View getView(int index, View view, ViewGroup viewGroup) {
    View layout;
    if (view instanceof LinearLayout) {
      layout = view;
    } else {
      LayoutInflater factory = LayoutInflater.from(context);
      layout = factory.inflate(R.layout.bookmark_picker_list_item, viewGroup, false);
    }
    String[] titleURL = titleURLs.get(index);
    ((TextView) layout.findViewById(R.id.bookmark_title)).setText(titleURL[0]);
    ((TextView) layout.findViewById(R.id.bookmark_url)).setText(titleURL[1]);
    return layout;
  }
}
