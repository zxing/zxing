/*
 * Copyright (C) 2012 ZXing authors
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

package com.google.zxing.client.android.pref;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.preference.Preference;
import android.util.AttributeSet;

/**
 * A dummy pref that launches Play to the BS+ page.
 *
 * @author Sean Owen
 */
public final class BSPlusPreference extends Preference {

  private static final String MARKET_URL = "market://details?id=com.srowen.bs.android";

  public BSPlusPreference(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    configureClickListener();
  }

  public BSPlusPreference(Context context, AttributeSet attrs) {
    super(context, attrs);
    configureClickListener();
  }

  public BSPlusPreference(Context context) {
    super(context);
    configureClickListener();
  }

  private void configureClickListener() {
    setOnPreferenceClickListener(new OnPreferenceClickListener() {
      @Override
      public boolean onPreferenceClick(Preference preference) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(MARKET_URL));
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        getContext().startActivity(intent);
        return true;
      }
    });
  }

}
