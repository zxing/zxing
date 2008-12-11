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
import android.preference.PreferenceScreen;

public final class PreferencesActivity extends android.preference.PreferenceActivity
    implements OnSharedPreferenceChangeListener {

  static final String KEY_DECODE_1D = "preferences_decode_1D";
  static final String KEY_DECODE_QR = "preferences_decode_QR";

  static final String KEY_PLAY_BEEP = "preferences_play_beep";
  static final String KEY_VIBRATE = "preferences_vibrate";
  static final String KEY_COPY_TO_CLIPBOARD = "preferences_copy_to_clipboard";

  static final String KEY_HELP_VERSION_SHOWN = "preferences_help_version_shown";

  CheckBoxPreference mDecode1D;
  CheckBoxPreference mDecodeQR;

  @Override
  protected void onCreate(Bundle icicle) {
    super.onCreate(icicle);
    addPreferencesFromResource(R.xml.preferences);

    PreferenceScreen preferences = getPreferenceScreen();
    preferences.getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    mDecode1D = (CheckBoxPreference) preferences.findPreference(KEY_DECODE_1D);
    mDecodeQR = (CheckBoxPreference) preferences.findPreference(KEY_DECODE_QR);
  }

  // Prevent the user from turning off both decode options
  public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
    if (key.equals(KEY_DECODE_1D)) {
      mDecodeQR.setEnabled(mDecode1D.isChecked());
      mDecodeQR.setChecked(true);
    } else if (key.equals(KEY_DECODE_QR)) {
      mDecode1D.setEnabled(mDecodeQR.isChecked());
      mDecode1D.setChecked(true);
    }
  }

}
