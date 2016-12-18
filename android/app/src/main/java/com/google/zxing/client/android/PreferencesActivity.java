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

import android.app.Activity;
import android.os.Bundle;

/**
 * The main settings activity.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 * @author Sean Owen
 */
public final class PreferencesActivity extends Activity {

  public static final String KEY_DECODE_1D_PRODUCT = "preferences_decode_1D_product";
  public static final String KEY_DECODE_1D_INDUSTRIAL = "preferences_decode_1D_industrial";
  public static final String KEY_DECODE_QR = "preferences_decode_QR";
  public static final String KEY_DECODE_DATA_MATRIX = "preferences_decode_Data_Matrix";
  public static final String KEY_DECODE_AZTEC = "preferences_decode_Aztec";
  public static final String KEY_DECODE_PDF417 = "preferences_decode_PDF417";

  public static final String KEY_CUSTOM_PRODUCT_SEARCH = "preferences_custom_product_search";

  public static final String KEY_PLAY_BEEP = "preferences_play_beep";
  public static final String KEY_VIBRATE = "preferences_vibrate";
  public static final String KEY_COPY_TO_CLIPBOARD = "preferences_copy_to_clipboard";
  public static final String KEY_FRONT_LIGHT_MODE = "preferences_front_light_mode";
  public static final String KEY_BULK_MODE = "preferences_bulk_mode";
  public static final String KEY_REMEMBER_DUPLICATES = "preferences_remember_duplicates";
  public static final String KEY_ENABLE_HISTORY = "preferences_history";
  public static final String KEY_SUPPLEMENTAL = "preferences_supplemental";
  public static final String KEY_AUTO_FOCUS = "preferences_auto_focus";
  public static final String KEY_INVERT_SCAN = "preferences_invert_scan";  
  public static final String KEY_SEARCH_COUNTRY = "preferences_search_country";
  public static final String KEY_DISABLE_AUTO_ORIENTATION = "preferences_orientation";

  public static final String KEY_DISABLE_CONTINUOUS_FOCUS = "preferences_disable_continuous_focus";
  public static final String KEY_DISABLE_EXPOSURE = "preferences_disable_exposure";
  public static final String KEY_DISABLE_METERING = "preferences_disable_metering";
  public static final String KEY_DISABLE_BARCODE_SCENE_MODE = "preferences_disable_barcode_scene_mode";
  public static final String KEY_AUTO_OPEN_WEB = "preferences_auto_open_web";

  @Override
  protected void onCreate(Bundle icicle) {
    super.onCreate(icicle);
    getFragmentManager().beginTransaction().replace(android.R.id.content, new PreferencesFragment()).commit();
  }

  // Apparently this will be necessary when targeting API 19+:
  /*
  @Override
  protected boolean isValidFragment(String fragmentName) {
    return true;
  }
   */

}
