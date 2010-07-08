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

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.widget.TextView;

import com.google.zxing.client.android.R;

/**
 * Get a broadcast when the network is connected, and kill the activity.
 */
final class ConnectedReceiver extends BroadcastReceiver {

  private final Activity parent;
  private final TextView statusView;

  ConnectedReceiver(Activity wifiActivity, TextView statusView) {
    this.parent = wifiActivity;
    this.statusView = statusView;
  }

  @Override
  public void onReceive(Context context, Intent intent) {
    if (intent.getAction().equals(android.net.ConnectivityManager.CONNECTIVITY_ACTION)) {
      ConnectivityManager con = (ConnectivityManager) parent.getSystemService(Context.CONNECTIVITY_SERVICE);
      final NetworkInfo[] s = con.getAllNetworkInfo();
      for (NetworkInfo i : s){
        if (i.getTypeName().contentEquals("WIFI")){
          NetworkInfo.State state = i.getState();
          if (state == NetworkInfo.State.CONNECTED){
            statusView.setText(R.string.wifi_connected);
            Runnable delayKill = new Killer(parent);
            delayKill.run();
          }
        }
      }
    }
  }
}
