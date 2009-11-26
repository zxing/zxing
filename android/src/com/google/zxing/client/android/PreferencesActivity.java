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

package com.google.zxing.client.android;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;

/**
 * The main settings activity.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class PreferencesActivity extends PreferenceActivity
    implements OnSharedPreferenceChangeListener {

  static final String KEY_DECODE_1D = "preferences_decode_1D";
  static final String KEY_DECODE_QR = "preferences_decode_QR";
  public static final String KEY_CUSTOM_PRODUCT_SEARCH = "preferences_custom_product_search";

  static final String KEY_PLAY_BEEP = "preferences_play_beep";
  static final String KEY_VIBRATE = "preferences_vibrate";
  static final String KEY_COPY_TO_CLIPBOARD = "preferences_copy_to_clipboard";

  static final String KEY_HELP_VERSION_SHOWN = "preferences_help_version_shown";
  public static final String KEY_NOT_OUR_RESULTS_SHOWN = "preferences_not_out_results_shown";

  private CheckBoxPreference decode1D;
  private CheckBoxPreference decodeQR;

  @Override
  protected void onCreate(Bundle icicle) {
    super.onCreate(icicle);
    addPreferencesFromResource(R.xml.preferences);

    PreferenceScreen preferences = getPreferenceScreen();
    preferences.getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    decode1D = (CheckBoxPreference) preferences.findPreference(KEY_DECODE_1D);
    decodeQR = (CheckBoxPreference) preferences.findPreference(KEY_DECODE_QR);
  }

  // Prevent the user from turning off both decode options
  public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
    if (key.equals(KEY_DECODE_1D)) {
      decodeQR.setEnabled(decode1D.isChecked());
      decodeQR.setChecked(true);
    } else if (key.equals(KEY_DECODE_QR)) {
      decode1D.setEnabled(decodeQR.isChecked());
      decode1D.setChecked(true);
    }
  }
}
