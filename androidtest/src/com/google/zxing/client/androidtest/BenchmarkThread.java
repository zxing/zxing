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

import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;

import android.os.Debug;
import android.os.Message;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

final class BenchmarkThread extends Thread {

  private static final String TAG = BenchmarkThread.class.getSimpleName();
  private static final int RUNS = 10;

  private final BenchmarkActivity activity;
  private final String path;
  private MultiFormatReader multiFormatReader;

  BenchmarkThread(BenchmarkActivity activity, String path) {
    this.activity = activity;
    this.path = path;
  }

  @Override
  public void run() {
    multiFormatReader = new MultiFormatReader();
    multiFormatReader.setHints(null);
    // Try to get in a known state before starting the benchmark
    System.gc();

    List<BenchmarkItem> items = new ArrayList<BenchmarkItem>();
    walkTree(path, items);
    Message message = Message.obtain(activity.handler, R.id.benchmark_done);
    message.obj = items;
    message.sendToTarget();
  }

  // Recurse to allow subdirectories
  private void walkTree(String currentPath, List<BenchmarkItem> items) {
    File file = new File(currentPath);
    if (file.isDirectory()) {
      String[] files = file.list();
      Arrays.sort(files);
      for (String fileName : files) {
        walkTree(file.getAbsolutePath() + '/' + fileName, items);
      }
    } else {
      BenchmarkItem item = decode(currentPath);
      if (item != null) {
        items.add(item);
      }
    }
  }

  private BenchmarkItem decode(String path) {
    RGBLuminanceSource source;
    try {
      source = new RGBLuminanceSource(path);
    } catch (FileNotFoundException e) {
      Log.e(TAG, e.toString());
      return null;
    }

    BenchmarkItem item = new BenchmarkItem(path, RUNS);
    for (int x = 0; x < RUNS; x++) {
      boolean success;
      Result result = null;
      // Using this call instead of getting the time should eliminate a lot of variability due to
      // scheduling and what else is happening in the system.
      long now = Debug.threadCpuTimeNanos();
      try {
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
        result = multiFormatReader.decodeWithState(bitmap);
        success = true;
      } catch (ReaderException e) {
        success = false;
      }
      now = Debug.threadCpuTimeNanos() - now;
      if (x == 0) {
        item.setDecoded(success);
        item.setFormat(result != null ? result.getBarcodeFormat() : null);
      }
      item.addResult((int) (now / 1000));
    }
    return item;
  }

}
