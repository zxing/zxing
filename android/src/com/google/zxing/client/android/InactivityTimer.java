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

package com.google.zxing.client.android;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.util.Log;

import com.google.zxing.client.android.common.executor.AsyncTaskExecInterface;
import com.google.zxing.client.android.common.executor.AsyncTaskExecManager;

/**
 * Finishes an activity after a period of inactivity if the device is on battery power.
 */
final class InactivityTimer {

  private static final String TAG = InactivityTimer.class.getSimpleName();

  private static final long INACTIVITY_DELAY_MS = 5 * 60 * 1000L;

  private final Activity activity;
  private final AsyncTaskExecInterface taskExec;
  private final BroadcastReceiver powerStatusReceiver;
  private InactivityAsyncTask inactivityTask;

  InactivityTimer(Activity activity) {
    this.activity = activity;
    taskExec = new AsyncTaskExecManager().build();
    powerStatusReceiver = new PowerStatusReceiver();
    onActivity();
  }

  synchronized void onActivity() {
    cancel();
    inactivityTask = new InactivityAsyncTask();
    taskExec.execute(inactivityTask);
  }

  public void onPause() {
    cancel();
    activity.unregisterReceiver(powerStatusReceiver);
  }

  public void onResume(){
    activity.registerReceiver(powerStatusReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
    onActivity();
  }

  private synchronized  void cancel() {
    AsyncTask<?,?,?> task = inactivityTask;
    if (task != null) {
      task.cancel(true);
      inactivityTask = null;
    }
  }

  void shutdown() {
    cancel();
  }

  private final class PowerStatusReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent){
      if (Intent.ACTION_BATTERY_CHANGED.equals(intent.getAction())) {
        // 0 indicates that we're on battery
        boolean onBatteryNow = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) <= 0;
        if (onBatteryNow) {
          InactivityTimer.this.onActivity();
        } else {
          InactivityTimer.this.cancel();
        }
      }
    }
  }

  private final class InactivityAsyncTask extends AsyncTask<Object,Object,Object> {
    @Override
    protected Object doInBackground(Object... objects) {
      try {
        Thread.sleep(INACTIVITY_DELAY_MS);
        Log.i(TAG, "Finishing activity due to inactivity");
        activity.finish();
      } catch (InterruptedException e) {
        // continue without killing
      }
      return null;
    }
  }

}
