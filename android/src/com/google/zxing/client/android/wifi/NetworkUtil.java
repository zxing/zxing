/*
 * Copyright (C) 2010 ZXing authors
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

package com.google.zxing.client.android.wifi;

import java.util.regex.Pattern;

import android.text.TextUtils;

/**
 * Try with:
 * http://chart.apis.google.com/chart?cht=qr&chs=240x240&chl=WIFI:S:linksys;P:mypass;T:WPA;;
 *
 * TODO(vikrama): Test with binary ssid or password.
 * 
 * @author Vikram Aggarwal
 */
final class NetworkUtil {

  private static final Pattern HEX_DIGITS = Pattern.compile("[0-9A-Fa-f]+");

  private NetworkUtil() {
  }

  /**
   * Encloses the incoming string inside double quotes, if it isn't already quoted.
   * @param string: the input string
   * @return a quoted string, of the form "input".  If the input string is null, it returns null as well.
   */
  static String convertToQuotedString(String string) {
    if (string == null){
      return null;
    }
    if (TextUtils.isEmpty(string)) {
      return "";
    }
    int lastPos = string.length() - 1;
    if (lastPos < 0 || (string.charAt(0) == '"' && string.charAt(lastPos) == '"')) {
      return string;
    }
    return '\"' + string + '\"';
  }

  /**
   * Check if wepKey is a valid hexadecimal string.
   * @param wepKey the input to be checked
   * @return true if the input string is indeed hex or empty.  False if the input string is non-hex or null.
   */
  static boolean isHexWepKey(CharSequence wepKey) {
    if (wepKey == null) {
      return false;
    }
    int length = wepKey.length();
    // WEP-40, WEP-104, and some vendors using 256-bit WEP (WEP-232?)
    return (length == 10 || length == 26 || length == 58) && HEX_DIGITS.matcher(wepKey).matches();
  }

}