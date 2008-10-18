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

import android.content.Intent;
import com.google.zxing.client.result.ParsedResult;
import com.google.zxing.client.result.ParsedResultType;

import java.net.URISyntaxException;

/**
 * A {@link com.google.zxing.client.result.ParsedResult} derived from a URI that encodes an Android
 * {@link Intent}, and which should presumably trigger that intent on Android.
 */
public final class AndroidIntentParsedResult extends ParsedResult {

  private final Intent mIntent;

  private AndroidIntentParsedResult(Intent intent) {
    super(ParsedResultType.ANDROID_INTENT);
    mIntent = intent;
  }

  public static AndroidIntentParsedResult parse(String rawText) {
    try {
      return new AndroidIntentParsedResult(Intent.getIntent(rawText));
    } catch (URISyntaxException urise) {
      return null;
    } catch (IllegalArgumentException iae) {
      return null;
    }
  }

  public Intent getIntent() {
    return mIntent;
  }

  @Override
  public String getDisplayResult() {
    return mIntent.toString();
  }

}
