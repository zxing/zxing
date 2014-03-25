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
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.TextView;

public final class BenchmarkActivity extends Activity {

  private View runBenchmarkButton;
  private TextView textView;
  private AsyncTask<Object,Object,String> benchmarkTask;

  private final View.OnClickListener runBenchmark = new View.OnClickListener() {
    @Override
    public void onClick(View v) {
      if (benchmarkTask == null) {
        String path = Environment.getExternalStorageDirectory().getPath() + "/zxingbenchmark";
        benchmarkTask = new BenchmarkAsyncTask(BenchmarkActivity.this, path);
        runBenchmarkButton.setEnabled(false);
        textView.setText(R.string.benchmark_running);
        benchmarkTask.execute(AsyncTask.THREAD_POOL_EXECUTOR);
      }
    }
  };
  
  @Override
  public void onCreate(Bundle icicle) {
    super.onCreate(icicle);
    setContentView(R.layout.benchmark);
    runBenchmarkButton = findViewById(R.id.benchmark_run);
    runBenchmarkButton.setOnClickListener(runBenchmark);
    textView = (TextView) findViewById(R.id.benchmark_help);
    benchmarkTask = null;
  }

  void onBenchmarkDone(String message) {
    textView.setText(message + "\n\n" + getString(R.string.benchmark_help));
    runBenchmarkButton.setEnabled(true);
    benchmarkTask = null;
  }

}
