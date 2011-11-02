/*
 * Copyright (C) 2008 ZXing authors
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

package com.google.zxing.client.androidtest;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public final class BenchmarkActivity extends Activity {

  private static final String TAG = BenchmarkActivity.class.getSimpleName();
  private static final String PATH = "/sdcard/zxingbenchmark";

  private Button runBenchmarkButton;
  private TextView textView;
  private BenchmarkThread benchmarkThread;

  @Override
  public void onCreate(Bundle icicle) {
    super.onCreate(icicle);

    setContentView(R.layout.benchmark);

    runBenchmarkButton = (Button) findViewById(R.id.benchmark_run);
    runBenchmarkButton.setOnClickListener(runBenchmark);
    textView = (TextView) findViewById(R.id.benchmark_help);

    benchmarkThread = null;
  }

  private final Button.OnClickListener runBenchmark = new Button.OnClickListener() {
    @Override
    public void onClick(View v) {
      if (benchmarkThread == null) {
        runBenchmarkButton.setEnabled(false);
        textView.setText(R.string.benchmark_running);
        benchmarkThread = new BenchmarkThread(BenchmarkActivity.this, PATH);
        benchmarkThread.start();
      }
    }
  };

  final Handler handler = new Handler() {
    @Override
    public void handleMessage(Message message) {
      switch (message.what) {
        case R.id.benchmark_done:
          handleBenchmarkDone(message);
          benchmarkThread = null;
          runBenchmarkButton.setEnabled(true);
          break;
        default:
          break;
      }
    }
  };

  private void handleBenchmarkDone(Message message) {
    Iterable<BenchmarkItem> items = (Iterable<BenchmarkItem>) message.obj;
    int count = 0;
    int time = 0;
    for (BenchmarkItem item : items) {
      if (item != null) {
        Log.v(TAG, item.toString());
        count++;
        time += item.getAverageTime();
      }
    }
    String totals = "TOTAL: Decoded " + count + " images in " + time + " us";
    Log.v(TAG, totals);
    textView.setText(totals + "\n\n" + getString(R.string.benchmark_help));
  }

}
