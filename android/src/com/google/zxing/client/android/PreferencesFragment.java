/*
 * Copyright (C) 2013 ZXing authors
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

import java.util.ArrayList;
import java.util.Collection;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;

public final class PreferencesFragment 
    extends PreferenceFragment 
    implements SharedPreferences.OnSharedPreferenceChangeListener {
  
  private CheckBoxPreference decode1D;
  private CheckBoxPreference decodeQR;
  private CheckBoxPreference decodeDataMatrix;
  
  @Override
  public void onCreate(Bundle icicle) {
    super.onCreate(icicle);
    addPreferencesFromResource(R.xml.preferences);
    
    PreferenceScreen preferences = getPreferenceScreen();
    preferences.getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    decode1D = (CheckBoxPreference) preferences.findPreference(PreferencesActivity.KEY_DECODE_1D);
    decodeQR = (CheckBoxPreference) preferences.findPreference(PreferencesActivity.KEY_DECODE_QR);
    decodeDataMatrix = (CheckBoxPreference) preferences.findPreference(PreferencesActivity.KEY_DECODE_DATA_MATRIX);
    disableLastCheckedPref();
  }
  
  @Override
  public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
    disableLastCheckedPref();
  }

  private void disableLastCheckedPref() {
    Collection<CheckBoxPreference> checked = new ArrayList<CheckBoxPreference>(3);
    if (decode1D.isChecked()) {
      checked.add(decode1D);
    }
    if (decodeQR.isChecked()) {
      checked.add(decodeQR);
    }
    if (decodeDataMatrix.isChecked()) {
      checked.add(decodeDataMatrix);
    }
    boolean disable = checked.size() < 2;
    CheckBoxPreference[] checkBoxPreferences = {decode1D, decodeQR, decodeDataMatrix};
    for (CheckBoxPreference pref : checkBoxPreferences) {
      pref.setEnabled(!(disable && checked.contains(pref)));
    }
  }
}
