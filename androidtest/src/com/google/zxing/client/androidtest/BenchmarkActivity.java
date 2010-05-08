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

import java.util.List;

public final class BenchmarkActivity extends Activity {

  private static final String PATH = "/sdcard/zxingbenchmark";
  private static final String TAG = "ZXingBenchmark";

  private Button mRunBenchmarkButton;
  private TextView mTextView;
  private BenchmarkThread mBenchmarkThread;

  @Override
  public void onCreate(Bundle icicle) {
    super.onCreate(icicle);

    setContentView(R.layout.benchmark);

    mRunBenchmarkButton = (Button) findViewById(R.id.benchmark_run);
    mRunBenchmarkButton.setOnClickListener(mRunBenchmark);
    mTextView = (TextView) findViewById(R.id.benchmark_help);

    mBenchmarkThread = null;
  }

  public final Button.OnClickListener mRunBenchmark = new Button.OnClickListener() {
    public void onClick(View v) {
      if (mBenchmarkThread == null) {
        mRunBenchmarkButton.setEnabled(false);
        mTextView.setText(R.string.benchmark_running);
        mBenchmarkThread = new BenchmarkThread(BenchmarkActivity.this, PATH);
        mBenchmarkThread.start();
      }
    }
  };

  public final Handler mHandler = new Handler() {
    @Override
    public void handleMessage(Message message) {
      switch (message.what) {
        case R.id.benchmark_done:
          handleBenchmarkDone(message);
          mBenchmarkThread = null;
          mRunBenchmarkButton.setEnabled(true);
          break;
        default:
          break;
      }
    }
  };

  private void handleBenchmarkDone(Message message) {
    List<BenchmarkItem> items = (List<BenchmarkItem>) message.obj;
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
    mTextView.setText(totals + "\n\n" + getString(R.string.benchmark_help));
  }

}
