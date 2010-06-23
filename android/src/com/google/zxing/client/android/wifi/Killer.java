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

import java.util.Timer;
import java.util.TimerTask;

import com.google.zxing.client.android.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;

/**
 * Close the parent after a delay.
 * @author Vikram Aggarwal
 */
final class Killer implements Runnable {

  // Three full seconds
  private static final long DELAY_MS = 3 * 1000L;

  private final Activity parent;

  Killer(Activity parent) {
    this.parent = parent;
  }
  void launchIntent(Intent intent) {
    if (intent != null) {
      intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
      try {
        parent.startActivity(intent);
      } catch (ActivityNotFoundException e) {
        AlertDialog.Builder builder = new AlertDialog.Builder(parent);
        builder.setTitle(R.string.app_name);
        builder.setMessage(R.string.msg_intent_failed);
        builder.setPositiveButton(R.string.button_ok, null);
        builder.show();
      }
    }
  }

  public void run() {
    final Handler handler = new Handler();
    Timer t = new Timer();
    t.schedule(new TimerTask() {
      @Override
      public void run() {
        handler.post(new Runnable() {
          public void run() {
            // This will kill the parent, a bad idea.
//            parent.finish();
            // This will start the browser, a better idea
            launchIntent(new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.google.com/")));
          }
        });
      }
    }, DELAY_MS);
  }
}
