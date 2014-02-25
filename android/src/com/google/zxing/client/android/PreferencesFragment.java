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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;

public final class PreferencesFragment 
    extends PreferenceFragment 
    implements SharedPreferences.OnSharedPreferenceChangeListener {

  private CheckBoxPreference[] checkBoxPrefs;
  
  @Override
  public void onCreate(Bundle icicle) {
    super.onCreate(icicle);
    addPreferencesFromResource(R.xml.preferences);
    
    PreferenceScreen preferences = getPreferenceScreen();
    preferences.getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    checkBoxPrefs = findDecodePrefs(preferences,
                                    PreferencesActivity.KEY_DECODE_1D_PRODUCT,
                                    PreferencesActivity.KEY_DECODE_1D_INDUSTRIAL,
                                    PreferencesActivity.KEY_DECODE_QR,
                                    PreferencesActivity.KEY_DECODE_DATA_MATRIX,
                                    PreferencesActivity.KEY_DECODE_AZTEC,
                                    PreferencesActivity.KEY_DECODE_PDF417);
    disableLastCheckedPref();

    EditTextPreference customProductSearch = (EditTextPreference)
        preferences.findPreference(PreferencesActivity.KEY_CUSTOM_PRODUCT_SEARCH);
    customProductSearch.setOnPreferenceChangeListener(new CustomSearchURLValidator());
  }

  private static CheckBoxPreference[] findDecodePrefs(PreferenceScreen preferences, String... keys) {
    CheckBoxPreference[] prefs = new CheckBoxPreference[keys.length];
    for (int i = 0; i < keys.length; i++) {
      prefs[i] = (CheckBoxPreference) preferences.findPreference(keys[i]);
    }
    return prefs;
  }
  
  @Override
  public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
    disableLastCheckedPref();
  }

  private void disableLastCheckedPref() {
    Collection<CheckBoxPreference> checked = new ArrayList<>(checkBoxPrefs.length);
    for (CheckBoxPreference pref : checkBoxPrefs) {
      if (pref.isChecked()) {
        checked.add(pref);
      }
    }
    boolean disable = checked.size() <= 1;
    for (CheckBoxPreference pref : checkBoxPrefs) {
      pref.setEnabled(!(disable && checked.contains(pref)));
    }
  }

  private class CustomSearchURLValidator implements Preference.OnPreferenceChangeListener {
    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
      if (!isValid(newValue)) {
        AlertDialog.Builder builder =
            new AlertDialog.Builder(PreferencesFragment.this.getActivity());
        builder.setTitle(R.string.msg_error);
        builder.setMessage(R.string.msg_invalid_value);
        builder.setCancelable(true);
        builder.show();
        return false;
      }
      return true;
    }

    private boolean isValid(Object newValue) {
      // Allow empty/null value
      if (newValue == null) {
        return true;
      }
      String valueString = newValue.toString();
      if (valueString.isEmpty()) {
        return true;
      }
      // Require a scheme otherwise:
      try {
        URI uri = new URI(valueString);
        return uri.getScheme() != null;
      } catch (URISyntaxException use) {
        return false;
      }
    }
  }

}
