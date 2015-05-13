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
import android.os.AsyncTask;
import android.util.Log;

import java.util.regex.Pattern;

import com.google.zxing.client.result.WifiParsedResult;

/**
 * @author Vikram Aggarwal
 * @author Sean Owen
 */
public final class WifiConfigManager extends AsyncTask<WifiParsedResult,Object,Object> {

  private static final String TAG = WifiConfigManager.class.getSimpleName();

  private static final Pattern HEX_DIGITS = Pattern.compile("[0-9A-Fa-f]+");

  private final WifiManager wifiManager;

  public WifiConfigManager(WifiManager wifiManager) {
    this.wifiManager = wifiManager;
  }

  @Override
  protected Object doInBackground(WifiParsedResult... args) {
    WifiParsedResult theWifiResult = args[0];
    // Start WiFi, otherwise nothing will work
    if (!wifiManager.isWifiEnabled()) {
      Log.i(TAG, "Enabling wi-fi...");
      if (wifiManager.setWifiEnabled(true)) {
        Log.i(TAG, "Wi-fi enabled");
      } else {
        Log.w(TAG, "Wi-fi could not be enabled!");
        return null;
      }
      // This happens very quickly, but need to wait for it to enable. A little busy wait?
      int count = 0;
      while (!wifiManager.isWifiEnabled()) {
        if (count >= 10) {
          Log.i(TAG, "Took too long to enable wi-fi, quitting");
          return null;
        }
        Log.i(TAG, "Still waiting for wi-fi to enable...");
        try {
          Thread.sleep(1000L);
        } catch (InterruptedException ie) {
          // continue
        }
        count++;
      }
    }
    String networkTypeString = theWifiResult.getNetworkEncryption();
    NetworkType networkType;
    try {
      networkType = NetworkType.forIntentValue(networkTypeString);
    } catch (IllegalArgumentException ignored) {
      Log.w(TAG, "Bad network type; see NetworkType values: " + networkTypeString);
      return null;
    }
    if (networkType == NetworkType.NO_PASSWORD) {
      changeNetworkUnEncrypted(wifiManager, theWifiResult);
    } else {
      String password = theWifiResult.getPassword();
      if (password != null && !password.isEmpty()) {
        if (networkType == NetworkType.WEP) {
          changeNetworkWEP(wifiManager, theWifiResult);
        } else if (networkType == NetworkType.WPA) {
          changeNetworkWPA(wifiManager, theWifiResult);
        }
      }
    }
    return null;
  }

  /**
   * Update the network: either create a new network or modify an existing network
   * @param config the new network configuration
   */
  private static void updateNetwork(WifiManager wifiManager, WifiConfiguration config) {
    Integer foundNetworkID = findNetworkInExistingConfig(wifiManager, config.SSID);
    if (foundNetworkID != null) {
      Log.i(TAG, "Removing old configuration for network " + config.SSID);
      wifiManager.removeNetwork(foundNetworkID);
      wifiManager.saveConfiguration();
    }
    int networkId = wifiManager.addNetwork(config);
    if (networkId >= 0) {
      // Try to disable the current network and start a new one.
      if (wifiManager.enableNetwork(networkId, true)) {
        Log.i(TAG, "Associating to network " + config.SSID);
        wifiManager.saveConfiguration();
      } else {
        Log.w(TAG, "Failed to enable network " + config.SSID);
      }
    } else {
      Log.w(TAG, "Unable to add network " + config.SSID);
    }
  }

  private static WifiConfiguration changeNetworkCommon(WifiParsedResult wifiResult) {
    WifiConfiguration config = new WifiConfiguration();
    config.allowedAuthAlgorithms.clear();
    config.allowedGroupCiphers.clear();
    config.allowedKeyManagement.clear();
    config.allowedPairwiseCiphers.clear();
    config.allowedProtocols.clear();
    // Android API insists that an ascii SSID must be quoted to be correctly handled.
    config.SSID = quoteNonHex(wifiResult.getSsid());
    config.hiddenSSID = wifiResult.isHidden();
    return config;
  }

  // Adding a WEP network
  private static void changeNetworkWEP(WifiManager wifiManager, WifiParsedResult wifiResult) {
    WifiConfiguration config = changeNetworkCommon(wifiResult);
    config.wepKeys[0] = quoteNonHex(wifiResult.getPassword(), 10, 26, 58);
    config.wepTxKeyIndex = 0;
    config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
    config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
    config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
    config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
    config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
    config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
    updateNetwork(wifiManager, config);
  }

  // Adding a WPA or WPA2 network
  private static void changeNetworkWPA(WifiManager wifiManager, WifiParsedResult wifiResult) {
    WifiConfiguration config = changeNetworkCommon(wifiResult);
    // Hex passwords that are 64 bits long are not to be quoted.
    config.preSharedKey = quoteNonHex(wifiResult.getPassword(), 64);
    config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
    config.allowedProtocols.set(WifiConfiguration.Protocol.WPA); // For WPA
    config.allowedProtocols.set(WifiConfiguration.Protocol.RSN); // For WPA2
    config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
    config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_EAP);
    config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
    config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
    config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
    config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
    updateNetwork(wifiManager, config);
  }

  // Adding an open, unsecured network
  private static void changeNetworkUnEncrypted(WifiManager wifiManager, WifiParsedResult wifiResult) {
    WifiConfiguration config = changeNetworkCommon(wifiResult);
    config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
    updateNetwork(wifiManager, config);
  }

  private static Integer findNetworkInExistingConfig(WifiManager wifiManager, String ssid) {
    Iterable<WifiConfiguration> existingConfigs = wifiManager.getConfiguredNetworks();
    if (existingConfigs != null) {
      for (WifiConfiguration existingConfig : existingConfigs) {
        String existingSSID = existingConfig.SSID;
        if (existingSSID != null && existingSSID.equals(ssid)) {
          return existingConfig.networkId;
        }
      }
    }
    return null;
  }

  private static String quoteNonHex(String value, int... allowedLengths) {
    return isHexOfLength(value, allowedLengths) ? value : convertToQuotedString(value);
  }

  /**
   * Encloses the incoming string inside double quotes, if it isn't already quoted.
   * @param s the input string
   * @return a quoted string, of the form "input".  If the input string is null, it returns null
   * as well.
   */
  private static String convertToQuotedString(String s) {
    if (s == null || s.isEmpty()) {
      return null;
    }
    // If already quoted, return as-is
    if (s.charAt(0) == '"' && s.charAt(s.length() - 1) == '"') {
      return s;
    }
    return '\"' + s + '\"';
  }

  /**
   * @param value input to check
   * @param allowedLengths allowed lengths, if any
   * @return true if value is a non-null, non-empty string of hex digits, and if allowed lengths are given, has
   *  an allowed length
   */
  private static boolean isHexOfLength(CharSequence value, int... allowedLengths) {
    if (value == null || !HEX_DIGITS.matcher(value).matches()) {
      return false;
    }
    if (allowedLengths.length == 0) {
      return true;
    }
    for (int length : allowedLengths) {
      if (value.length() == length) {
        return true;
      }
    }
    return false;
  }

}
