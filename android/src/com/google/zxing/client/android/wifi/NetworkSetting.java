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

import com.google.zxing.client.android.wifi.WifiActivity.NetworkType;

/**
 * Everything we could get from the barcode is to be here
 *
 * @author Vikram Aggarwal
 */
final class NetworkSetting {

  /** The ancillary network setting from the barcode */
  private final NetworkType networkType;
  /** The password this ssid has */
  private final String password;
  /** The ssid we read from the barcode */
  private final String ssid;

  /**
   * Create a new NetworkSetting object.
   * @param ssid: The SSID
   * @param password: Password for the setting, blank if unsecured network
   * @param networkType: WPA for WPA/WPA2, or WEP for WEP or unsecured
   */
  NetworkSetting(String ssid, String password, NetworkType networkType){
    this.ssid = ssid;
    this.password = password;
    this.networkType = networkType;
  }

  NetworkType getNetworkType() {
    return networkType;
  }

  String getPassword() {
    return password;
  }

  String getSsid() {
    return ssid;
  }

}