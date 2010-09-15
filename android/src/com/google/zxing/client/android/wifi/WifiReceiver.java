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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiManager;
import android.util.Log;
import android.widget.TextView;

import com.google.zxing.client.android.R;

/**
 * Get a broadcast when the network is connected, and kill the activity.
 */
final class WifiReceiver extends BroadcastReceiver {

  private static final String TAG = WifiReceiver.class.getSimpleName();

  private final WifiManager mWifiManager;
  private final WifiActivity parent;
  private final TextView statusView;

  WifiReceiver(WifiManager wifiManager, WifiActivity wifiActivity, TextView statusView, String ssid) {
    this.parent = wifiActivity;
    this.statusView = statusView;
    this.mWifiManager = wifiManager;
  }

  @Override
  public void onReceive(Context context, Intent intent) {
    if (intent.getAction().equals(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION)) {
      handleChange(
          (SupplicantState) intent.getParcelableExtra(WifiManager.EXTRA_NEW_STATE),
          intent.hasExtra(WifiManager.EXTRA_SUPPLICANT_ERROR));
    } else if (intent.getAction().equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)){
      handleNetworkStateChanged((NetworkInfo) intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO));
    } else if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
      ConnectivityManager con = (ConnectivityManager) parent.getSystemService(Context.CONNECTIVITY_SERVICE);
      NetworkInfo[] s = con.getAllNetworkInfo();
      for (NetworkInfo i : s){
        if (i.getTypeName().contentEquals("WIFI")){
          NetworkInfo.State state = i.getState();
          String ssid = mWifiManager.getConnectionInfo().getSSID();

          if (state == NetworkInfo.State.CONNECTED && ssid != null){
            mWifiManager.saveConfiguration();
            String label = parent.getString(R.string.wifi_connected);
            statusView.setText(label + '\n' + ssid);
            Runnable delayKill = new Killer(parent);
            delayKill.run();
          }
          if (state == NetworkInfo.State.DISCONNECTED){
            Log.d(TAG, "Got state: " + state + " for ssid: " + ssid);
            parent.gotError();
          }
        }
      }
    }
  }

  private void handleNetworkStateChanged(NetworkInfo networkInfo) {
    NetworkInfo.DetailedState state = networkInfo.getDetailedState();
    if (state == NetworkInfo.DetailedState.FAILED){
      Log.d(TAG, "Detailed Network state failed");
      parent.gotError();
    }
  }

  private void handleChange(SupplicantState state, boolean hasError) {
    if (hasError || state == SupplicantState.INACTIVE){
      Log.d(TAG, "Found an error");
      parent.gotError();
    }
  }
}