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

import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.google.zxing.client.android.Intents;
import com.google.zxing.client.android.R;

/**
 * A new activity showing the progress of Wifi connection
 *
 * TODO(viki): Tell the user when the network is not available here
 * TODO(viki): Incorrect password, could not connect, give an error
 * 
 * @author Vikram Aggarwal
 */
public class WifiActivity extends Activity  {

  private static final String TAG = WifiActivity.class.getSimpleName();

  private WifiManager wifiManager;
  private TextView statusView;
  private ConnectedReceiver connectedReceiver;

  public enum NetworkType {
    NETWORK_WEP, NETWORK_WPA, NETWORK_NOPASS, NETWORK_INVALID,
  }

  private int changeNetwork(NetworkSetting setting) {
    // All the ways this can be wrong:

    // If the SSID is empty, throw an error and return
    if (setting.getSsid() == null || setting.getSsid().length() == 0) {
      return doError(R.string.wifi_ssid_missing);
    }
    // If the network type is invalid
    if (setting.getNetworkType() == NetworkType.NETWORK_INVALID){
      return doError(R.string.wifi_type_incorrect);
    }

    // If the password is empty, this is an unencrypted network
    if (setting.getPassword() == null || setting.getPassword().length() == 0 ||
        setting.getNetworkType() == null ||
        setting.getNetworkType() == NetworkType.NETWORK_NOPASS) {
      return changeNetworkUnEncrypted(setting);
    }
    if (setting.getNetworkType() == NetworkType.NETWORK_WPA) {
      return changeNetworkWPA(setting);
    } else {
      return changeNetworkWEP(setting);
    }
  }

  private int doError(int resource_string) {
    statusView.setText(resource_string);
    return -1;
  }

  private WifiConfiguration changeNetworkCommon(NetworkSetting input){
    statusView.setText(R.string.wifi_creating_network);
    Log.d(TAG, "Adding new configuration: \nSSID: " + input.getSsid() + "\nType: " + input.getNetworkType());
    WifiConfiguration config = new WifiConfiguration();

    config.allowedAuthAlgorithms.clear();
    config.allowedGroupCiphers.clear();
    config.allowedKeyManagement.clear();
    config.allowedPairwiseCiphers.clear();
    config.allowedProtocols.clear();

    // Android API insists that an ascii SSID must be quoted to be correctly handled.
    config.SSID = NetworkUtil.convertToQuotedString(input.getSsid());
    config.hiddenSSID = true;
    return config;
  }

  private int requestNetworkChange(WifiConfiguration config){
    statusView.setText(R.string.wifi_changing_network);
    return updateNetwork(config, false);
  }

  // Adding a WEP network
  private int changeNetworkWEP(NetworkSetting input) {
    WifiConfiguration config = changeNetworkCommon(input);
    if (NetworkUtil.isHexWepKey(input.getPassword())) {
      config.wepKeys[0] = input.getPassword();
    } else {
      config.wepKeys[0] = NetworkUtil.convertToQuotedString(input.getPassword());
    }
    config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
    config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
    config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
    config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
    config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
    config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
    config.wepTxKeyIndex = 0;
    return requestNetworkChange(config);
  }

  // Adding a WPA or WPA2 network
  private int changeNetworkWPA(NetworkSetting input) {
    WifiConfiguration config = changeNetworkCommon(input);
    config.preSharedKey = NetworkUtil.convertToQuotedString(input.getPassword());
    config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
    config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
    // For WPA
    config.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
    // For WPA2
    config.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
    return requestNetworkChange(config);
  }

  // Adding an open, unsecured network
  private int changeNetworkUnEncrypted(NetworkSetting input){
    WifiConfiguration config = changeNetworkCommon(input);
    Log.d(TAG, "Empty password prompting a simple account setting");
    config.wepKeys[0] = "";
    config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
    config.wepTxKeyIndex = 0;
    return requestNetworkChange(config);
  }

  /**
   * If the given ssid name exists in the settings, then change its password to the one given here, and save
   * @param ssid
   */
  private WifiConfiguration findNetworkInExistingConfig(String ssid){
    List <WifiConfiguration> existingConfigs = wifiManager.getConfiguredNetworks();
    for (WifiConfiguration existingConfig : existingConfigs) {
      if (existingConfig.SSID.equals(ssid)) {
        return existingConfig;
      }
    }
    return null;
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    Intent intent = getIntent();
    if (intent == null || (!intent.getAction().equals(Intents.WifiConnect.ACTION))) {
      finish();
      return;
    }

    String ssid = intent.getStringExtra(Intents.WifiConnect.SSID);
    String password = intent.getStringExtra(Intents.WifiConnect.PASSWORD);
    String networkType = intent.getStringExtra(Intents.WifiConnect.TYPE);
    setContentView(R.layout.network);
    statusView = (TextView) findViewById(R.id.networkStatus);

    // TODO(vikrama): Error checking here, to ensure ssid exists.
    NetworkType networkT;
    if (networkType.equals("WPA")) {
      networkT = NetworkType.NETWORK_WPA;
    } else if (networkType.equals("WEP")) {
      networkT = NetworkType.NETWORK_WEP;
    } else if (networkType.equals("nopass")) {
     networkT = NetworkType.NETWORK_NOPASS;
    } else {
      // Got an incorrect network type.  Give an error
      doError(R.string.wifi_type_incorrect);
      return;
    }

    // This is not available before onCreate
    wifiManager = (WifiManager) this.getSystemService(WIFI_SERVICE);

    // So we know when the network changes
    connectedReceiver = new ConnectedReceiver(this, statusView);
    registerReceiver(connectedReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));

    if (password == null) {
      password = "";
    }
    Log.d(TAG, "Adding new configuration: \nSSID: " + ssid + "Type: " + networkT);
    NetworkSetting setting = new NetworkSetting(ssid, password, networkT);
    changeNetwork(setting);
  }

  @Override
  protected void onDestroy() {
    if (connectedReceiver != null) {
      unregisterReceiver(connectedReceiver);
      connectedReceiver = null;
    }
    super.onDestroy();
  }

  /**
   * Update the network: either create a new network or modify an existing network
   * @param config the new network configuration
   * @param disableOthers true if other networks must be disabled
   * @return network ID of the connected network.
   */
  private int updateNetwork(WifiConfiguration config, boolean disableOthers){
    int networkId;
    if (findNetworkInExistingConfig(config.SSID) == null){
      statusView.setText(R.string.wifi_creating_network);
      networkId = wifiManager.addNetwork(config);
    } else {
      statusView.setText(R.string.wifi_modifying_network);
      networkId = wifiManager.updateNetwork(config);
    }
    if (networkId == -1 || !wifiManager.enableNetwork(networkId, disableOthers)) {
      return -1;
    }
    wifiManager.saveConfiguration();
    return networkId;
  }
}