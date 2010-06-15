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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.zxing.client.android.Intents;
import com.google.zxing.client.android.R;
import com.google.zxing.client.android.wifi.Killer;
import com.google.zxing.client.android.wifi.NetworkUtil;
import com.google.zxing.client.android.wifi.NetworkSetting;

/**
 * A new activity showing the progress of Wifi connection
 * @author Vikram Aggarwal
 *
 */
public class WifiActivity extends Activity  {
  public static enum NetworkType {
    NETWORK_WEP, NETWORK_WPA,
  }

  /**
   * Get a broadcast when the network is connected, and kill the activity.
   */
  class ConnectedReceiver extends BroadcastReceiver {
    Activity parent = null;
    public ConnectedReceiver(WifiActivity wifiActivity) {
      parent = wifiActivity;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
      if (intent.getAction().equals(android.net.ConnectivityManager.CONNECTIVITY_ACTION)){
        ConnectivityManager con = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo[] s = con.getAllNetworkInfo();
        for (NetworkInfo i : s){
          if (i.getTypeName().contentEquals("WIFI")){
            NetworkInfo.State state = i.getState();
            if (state == NetworkInfo.State.CONNECTED){
              statusT.setText("Connected!");
              Killer delay_kill = new Killer(parent);
              delay_kill.run();
            }
          }
        }
      }
    }
  }

  private static final String tag = "NetworkActivity";
  WifiManager mWifiManager = null;
  TextView statusT = null;
  ImageView statusI = null;
  ConnectedReceiver rec = null;
  boolean debug = true;

  private int changeNetwork(NetworkSetting setting) {
    // If the password is empty, this is an unencrypted network
    if (setting.getPassword() == null || setting.getPassword() == "") {
      return changeNetworkUnEncrypted(setting);
    }
    if (setting.getNetworkType() == NetworkType.NETWORK_WPA) {
      return changeNetworkWPA(setting); 
    } else {
      return changeNetworkWEP(setting);
    }
  }

  private WifiConfiguration changeNetworkCommon(NetworkSetting input){
    statusT.setText("Creating settings...");
    if (debug) {
      Log.d(tag, "adding new configuration: \nSSID: " + input.getSsid() + "\nPassword: \""
          + input.getPassword() + "\"\nType: " + input.getNetworkType());
    }
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
    boolean disableOthers = false;
    statusT.setText("Changing Network...");
    return updateNetwork(config, disableOthers);
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
    if (debug){
      Log.d(tag, "Empty password prompting a simple account setting");
    }
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
    List <WifiConfiguration> existingConfigs = mWifiManager.getConfiguredNetworks();
    for (int i = 0; i < existingConfigs.size(); i++){
      if (existingConfigs.get(i).SSID.compareTo(ssid) == 0){
        return existingConfigs.get(i);
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

    // TODO(vikrama): Error checking here, to ensure ssid exists.
    NetworkType networkT = null;
    if (networkType.contains("WPA")) {
      networkT = NetworkType.NETWORK_WPA;
    }
    else if (networkType.contains("WEP")) {
      networkT = NetworkType.NETWORK_WEP;
    }
    else {
      // Got an incorrect network type
      finish();
      return;
    }

    setContentView(R.layout.network);
    statusT = (TextView) findViewById(R.id.networkStatus);
    // This is not available before onCreate
    mWifiManager = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);

    // So we know when the network changes
    rec = new ConnectedReceiver(this);
    registerReceiver(rec, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));

    if (password == null)
      password = "";
    if (debug) {
      Log.d(tag, "adding new configuration: \nSSID: " + ssid + "\nPassword: \"" + password + "\"\nType: " + networkT);
    }
    NetworkSetting setting = new NetworkSetting(ssid, password, networkT);
    changeNetwork(setting);
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    if (rec != null)
      unregisterReceiver(rec);
    rec = null;
  }

  /**
   * Update the network: either create a new network or modify an existing network
   * @param config: the new network configuration
   * @param disableOthers: true if other networks must be disabled
   * @return network ID of the connected network.
   */
  private int updateNetwork(WifiConfiguration config, boolean disableOthers){
    WifiConfiguration existing = findNetworkInExistingConfig(config.SSID);
    int networkId;
    if (existing == null){
      statusT.setText("Creating network...");
      networkId = mWifiManager.addNetwork(config);
    } else {
      statusT.setText("Modifying network...");
      networkId = mWifiManager.updateNetwork(config);
    }
    if (networkId == -1){
      return networkId;
    }
    if (!mWifiManager.enableNetwork(networkId, disableOthers)) {
      return -1;
    }
    mWifiManager.saveConfiguration();
    return networkId;
  }
}