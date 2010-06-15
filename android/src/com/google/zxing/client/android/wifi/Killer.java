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

import android.app.Activity;
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

  public void run() {
    final Handler handler = new Handler();
    Timer t = new Timer();
    t.schedule(new TimerTask() {
      @Override
      public void run() {
        handler.post(new Runnable() {
          public void run() {
            parent.finish();
          }
        });
      }
    }, DELAY_MS);
  }
}
