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

package com.google.zxing.client.android.result;

import com.google.zxing.Result;
import com.google.zxing.client.android.R;
import com.google.zxing.client.result.ISBNParsedResult;
import com.google.zxing.client.result.ParsedResult;

import android.app.Activity;

/**
 * Handles books encoded by their ISBN values.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class ISBNResultHandler extends ResultHandler {
  private static final int[] buttons = {
      R.string.button_product_search,
      R.string.button_book_search,
      R.string.button_search_book_contents,
      R.string.button_custom_product_search
  };

  public ISBNResultHandler(Activity activity, ParsedResult result, Result rawResult) {
    super(activity, result, rawResult);
  }

  @Override
  public int getButtonCount() {
    return hasCustomProductSearch() ? buttons.length : buttons.length - 1;
  }

  @Override
  public int getButtonText(int index) {
    return buttons[index];
  }

  @Override
  public void handleButtonPress(int index) {
    ISBNParsedResult isbnResult = (ISBNParsedResult) getResult();
    switch (index) {
      case 0:
        openProductSearch(isbnResult.getISBN());
        break;
      case 1:
        openBookSearch(isbnResult.getISBN());
        break;
      case 2:
        searchBookContents(isbnResult.getISBN());
        break;
      case 3:
        openURL(fillInCustomSearchURL(isbnResult.getISBN()));
        break;
    }
  }

  @Override
  public int getDisplayTitle() {
    return R.string.result_isbn;
  }
}
