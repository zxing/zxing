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
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import com.google.zxing.client.android.R;
import com.google.zxing.client.android.PreferencesActivity;
import com.google.zxing.client.result.ParsedResult;
import com.google.zxing.client.result.ProductParsedResult;

public final class ProductResultHandler extends ResultHandler {

  private static final int[] mButtons = {
      R.string.button_product_search,
      R.string.button_web_search,
      R.string.button_custom_product_search,
  };

  private final String mCustomProductSearch;

  public ProductResultHandler(Activity activity, ParsedResult result) {
    super(activity, result);
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
    mCustomProductSearch = prefs.getString(PreferencesActivity.KEY_CUSTOM_PRODUCT_SEARCH, null);
  }

  @Override
  public int getButtonCount() {
    return mCustomProductSearch != null ? mButtons.length : mButtons.length - 1;
  }

  @Override
  public int getButtonText(int index) {
    return mButtons[index];
  }

  @Override
  public void handleButtonPress(int index) {
    ProductParsedResult productResult = (ProductParsedResult) mResult;
    switch (index) {
      case 0:
        openProductSearch(productResult.getNormalizedProductID());
        break;
      case 1:
        webSearch(productResult.getNormalizedProductID());
        break;
      case 2:
        String url = mCustomProductSearch.replace("%s", productResult.getNormalizedProductID());
        openURL(url);
        break;
    }
  }

  @Override
  public int getDisplayTitle() {
    return R.string.result_product;
  }

}
