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

import java.util.Vector;

import com.google.zxing.client.android.wifi.WifiActivity.NetworkType;

/**
 * Everything we could get from the barcode is to be here
 * @author Vikram Aggarwal
 *
 */
class NetworkSetting {
  // The ancillary network setting from the barcode
  private NetworkType mNetworkType;
  // The password this ssid has
  private String mPassword;
  // The ssid we read from the barcode
  private String mSsid;

  static String[] toStringArray(Vector<String> strings) {
    int size = strings.size();
    String[] result = new String[size];
    for (int j = 0; j < size; j++) {
      result[j] = (String) strings.elementAt(j);
    }
    return result;
  }
  /**
   * Create a new NetworkSetting object.
   * @param ssid: The SSID
   * @param password: Password for the setting, blank if unsecured network
   * @param networkType: WPA for WPA/WPA2, or WEP for WEP or unsecured
   */
  public NetworkSetting(String ssid, String password, NetworkType networkType){
    mSsid = ssid;
    mPassword = password;
    mNetworkType = networkType;
  }

  public NetworkType getNetworkType() {
    return mNetworkType;
  }
  public String getPassword() {
    return mPassword;
  }

  public String getSsid() {
    return mSsid;
  }
}