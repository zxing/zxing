/*
 * Copyright 2008 Google Inc.
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

package com.google.zxing.client.android;

import android.content.Intent;
import com.google.zxing.client.result.ParsedReaderResult;
import com.google.zxing.client.result.ParsedReaderResultType;

import java.net.URISyntaxException;

/**
 * A {@link ParsedReaderResult} derived from a URI that encodes an Android
 * {@link Intent}, and which should presumably trigger that intent on Android.
 *
 * @author srowen@google.com (Sean Owen)
 */
public final class AndroidIntentParsedResult extends ParsedReaderResult {

  private final Intent intent;

  private AndroidIntentParsedResult(Intent intent) {
    super(ParsedReaderResultType.ANDROID_INTENT);
    this.intent = intent;
  }

  public static AndroidIntentParsedResult parse(String rawText) {
    try {
      return new AndroidIntentParsedResult(Intent.getIntent(rawText));
    } catch (URISyntaxException urise) {
      return null;
    }
  }

  public Intent getIntent() {
    return intent;
  }

  @Override
  public String getDisplayResult() {
    return intent.toString();
  }

}