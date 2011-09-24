/*
 * Copyright (C) 2011 ZXing authors
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

import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.text.TextUtils;

import java.util.List;
import java.util.regex.Pattern;

/**
 * @author Vikram Aggarwal
 * @author Sean Owen
 */
public final class WifiConfigManager {

  private static final Pattern HEX_DIGITS_64 = Pattern.compile("[0-9A-Fa-f]{64}");
  private static final Pattern HEX_DIGITS = Pattern.compile("[0-9A-Fa-f]+");

  private WifiConfigManager() {
  }

  public static void configure(WifiManager wifiManager, String ssid, String password, String networkTypeString) {
    // If the SSID is empty, throw an error and return
    if (ssid == null || ssid.length() == 0) {
      throw new IllegalArgumentException();
    }
    NetworkType networkType;
    if (password == null || password.length() == 0 || networkTypeString == null) {
      networkType = NetworkType.NO_PASSWORD;
    } else {
      networkType = NetworkType.forIntentValue(networkTypeString);
    }

    // Start WiFi, otherwise nothing will work
    if (!wifiManager.isWifiEnabled()) {
      wifiManager.setWifiEnabled(true);
    }

    switch (networkType) {
      case WEP:
        changeNetworkWEP(wifiManager, ssid, password);
        break;
      case WPA:
        changeNetworkWPA(wifiManager, ssid, password);
        break;
      case NO_PASSWORD:
        changeNetworkUnEncrypted(wifiManager, ssid);
        break;
    }
  }

  /**
   * Update the network: either create a new network or modify an existing network
   * @param config the new network configuration
   * @return network ID of the connected network.
   */
  private static void updateNetwork(WifiManager wifiManager, WifiConfiguration config) {
    Integer foundNetworkID = findNetworkInExistingConfig(wifiManager, config.SSID);
    if (foundNetworkID != null) {
      wifiManager.removeNetwork(foundNetworkID);
      wifiManager.saveConfiguration();
    }
    int networkId = wifiManager.addNetwork(config);
    if (networkId >= 0) {
      // Try to disable the current network and start a new one.
      if (wifiManager.enableNetwork(networkId, true)) {
        wifiManager.reassociate();
      }
    }
  }

  private static WifiConfiguration changeNetworkCommon(String ssid) {
    WifiConfiguration config = new WifiConfiguration();
    config.allowedAuthAlgorithms.clear();
    config.allowedGroupCiphers.clear();
    config.allowedKeyManagement.clear();
    config.allowedPairwiseCiphers.clear();
    config.allowedProtocols.clear();
    // Android API insists that an ascii SSID must be quoted to be correctly handled.
    config.SSID = convertToQuotedString(ssid);
    return config;
  }

  // Adding a WEP network
  private static void changeNetworkWEP(WifiManager wifiManager, String ssid, String password) {
    WifiConfiguration config = changeNetworkCommon(ssid);
    if (isHexWepKey(password)) {
      config.wepKeys[0] = password;
    } else {
      config.wepKeys[0] = convertToQuotedString(password);
    }
    config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
    config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
    config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
    config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
    config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
    config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
    config.wepTxKeyIndex = 0;
    updateNetwork(wifiManager, config);
  }

  // Adding a WPA or WPA2 network
  private static void changeNetworkWPA(WifiManager wifiManager, String ssid, String password) {
    WifiConfiguration config = changeNetworkCommon(ssid);
    // Hex passwords that are 64 bits long are not to be quoted.
    if (HEX_DIGITS_64.matcher(password).matches()) {
      config.preSharedKey = password;
    } else {
      config.preSharedKey = convertToQuotedString(password);
    }
    config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
    // For WPA
    config.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
    // For WPA2
    config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
    config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
    config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
    config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
    config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
    config.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
    updateNetwork(wifiManager, config);
  }

  // Adding an open, unsecured network
  private static void changeNetworkUnEncrypted(WifiManager wifiManager, String ssid) {
    WifiConfiguration config = changeNetworkCommon(ssid);
    config.wepKeys[0] = "";
    config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
    config.wepTxKeyIndex = 0;
    updateNetwork(wifiManager, config);
  }

  private static Integer findNetworkInExistingConfig(WifiManager wifiManager, String ssid) {
    List<WifiConfiguration> existingConfigs = wifiManager.getConfiguredNetworks();
    for (WifiConfiguration existingConfig : existingConfigs) {
      if (existingConfig.SSID.equals(ssid)) {
        return existingConfig.networkId;
      }
    }
    return null;
  }

  /**
   * Encloses the incoming string inside double quotes, if it isn't already quoted.
   * @param string the input string
   * @return a quoted string, of the form "input".  If the input string is null, it returns null
   * as well.
   */
  private static String convertToQuotedString(String string) {
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
   * @return true if the input string is indeed hex or empty.  False if the input string is non-hex
   * or null.
   */
  private static boolean isHexWepKey(CharSequence wepKey) {
    if (wepKey == null) {
      return false;
    }
    int length = wepKey.length();
    // WEP-40, WEP-104, and some vendors using 256-bit WEP (WEP-232?)
    return (length == 10 || length == 26 || length == 58) && HEX_DIGITS.matcher(wepKey).matches();
  }

}
