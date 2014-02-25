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

package com.google.zxing.client.android.camera;

import android.content.SharedPreferences;
import com.google.zxing.client.android.PreferencesActivity;

/**
 * Enumerates settings of the prefernce controlling the front light.
 */
public enum FrontLightMode {

  /** Always on. */
  ON,
  /** On only when ambient light is low. */
  AUTO,
  /** Always off. */
  OFF;

  private static FrontLightMode parse(String modeString) {
    return modeString == null ? OFF : valueOf(modeString);
  }

  public static FrontLightMode readPref(SharedPreferences sharedPrefs) {
    return parse(sharedPrefs.getString(PreferencesActivity.KEY_FRONT_LIGHT_MODE, OFF.toString()));
  }

}
