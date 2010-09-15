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

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import java.util.List;

import com.google.zxing.client.android.R;

/**
 * Manufactures list items which represent SBC results.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
final class SearchBookContentsAdapter extends ArrayAdapter<SearchBookContentsResult> {

  SearchBookContentsAdapter(Context context, List<SearchBookContentsResult> items) {
    super(context, R.layout.search_book_contents_list_item, 0, items);
  }

  @Override
  public View getView(int position, View view, ViewGroup viewGroup) {
    SearchBookContentsListItem listItem;

    if (view == null) {
      LayoutInflater factory = LayoutInflater.from(getContext());
      listItem = (SearchBookContentsListItem) factory.inflate(
          R.layout.search_book_contents_list_item, viewGroup, false);
    } else {
      if (view instanceof SearchBookContentsListItem) {
        listItem = (SearchBookContentsListItem) view;
      } else {
        return view;
      }
    }

    SearchBookContentsResult result = getItem(position);
    listItem.set(result);
    return listItem;
  }
}
