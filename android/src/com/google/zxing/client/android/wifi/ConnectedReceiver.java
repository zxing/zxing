package com.google.zxing.client.android.wifi;

import com.google.zxing.client.android.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.widget.TextView;

/**
 * Get a broadcast when the network is connected, and kill the activity.
 */
final class ConnectedReceiver extends BroadcastReceiver {

  private final Activity parent;
  private final TextView statusView;

  ConnectedReceiver(Activity wifiActivity, TextView statusView) {
    parent = wifiActivity;
    this.statusView = statusView;
  }

  @Override
  public void onReceive(Context context, Intent intent) {
    if (intent.getAction().equals(android.net.ConnectivityManager.CONNECTIVITY_ACTION)) {
      ConnectivityManager con = (ConnectivityManager) parent.getSystemService(Context.CONNECTIVITY_SERVICE);
      NetworkInfo[] s = con.getAllNetworkInfo();
      for (NetworkInfo i : s){
        if (i.getTypeName().contentEquals("WIFI")){
          NetworkInfo.State state = i.getState();
          if (state == NetworkInfo.State.CONNECTED){
            statusView.setText("Connected!");
            Runnable delayKill = new Killer(parent);
            delayKill.run();
          }
        }
      }
    }
  }
}
