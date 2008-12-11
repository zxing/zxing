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
import com.google.zxing.client.result.ParsedResult;
import com.google.zxing.client.result.URIParsedResult;

public final class URIResultHandler extends ResultHandler {

  private static final int[] mButtons = {
      R.string.button_open_browser,
      R.string.button_share_by_email,
      R.string.button_share_by_sms
  };

  public URIResultHandler(Activity activity, ParsedResult result) {
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
    URIParsedResult uriResult = (URIParsedResult) mResult;
    switch (index) {
      case 0:
        openURL(uriResult.getURI());
        break;
      case 1:
        shareByEmail(uriResult.getURI());
        break;
      case 2:
        shareBySMS(uriResult.getURI());
        break;
    }
  }

  @Override
  public int getDisplayTitle() {
    return R.string.result_uri;
  }

}
