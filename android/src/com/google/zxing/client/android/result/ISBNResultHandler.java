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

import android.app.Activity;
import com.google.zxing.client.android.R;
import com.google.zxing.client.result.ISBNParsedResult;
import com.google.zxing.client.result.ParsedResult;

public final class ISBNResultHandler extends ResultHandler {

  private static final int[] mButtons = {
      R.string.button_product_search,
      R.string.button_book_search,
      R.string.button_search_book_contents
  };

  public ISBNResultHandler(Activity activity, ParsedResult result) {
    super(activity, result);
  }

  @Override
  public int getButtonCount() {
    return mButtons.length;
  }

  @Override
  public int getButtonText(int index) {
    return mButtons[index];
  }

  @Override
  public void handleButtonPress(int index) {
    ISBNParsedResult isbnResult = (ISBNParsedResult) mResult;
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
    }
  }

  @Override
  public int getDisplayTitle() {
    return R.string.result_isbn;
  }

}
