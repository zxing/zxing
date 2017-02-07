/*
 * Copyright (C) 2013 ZXing authors
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

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Debug;
import android.util.Log;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Asynchronous task which actually runs benchmarks and collects timing.z
 */
public final class BenchmarkAsyncTask extends AsyncTask<Object,Object,String> {

  private static final String TAG = BenchmarkAsyncTask.class.getSimpleName();
  private static final int RUNS = 10;

  private final BenchmarkActivity benchmarkActivity;
  private final File file;

  BenchmarkAsyncTask(BenchmarkActivity benchmarkActivity, File file) {
    this.benchmarkActivity = benchmarkActivity;
    this.file = file;
  }

  @Override
  protected String doInBackground(Object... params) {
    MultiFormatReader reader = new MultiFormatReader();
    reader.setHints(null);
    // Try to get in a known state before starting the benchmark
    System.gc();

    List<BenchmarkItem> items = new ArrayList<>();
    walkTree(reader, file, items);

    int count = 0;
    int time = 0;
    for (BenchmarkItem item : items) {
      if (item != null) {
        Log.v(TAG, item.toString());
        count++;
        time += item.getAverageTime();
      }
    }
    return "TOTAL: Decoded " + count + " images in " + time + " us";
  }

  @Override
  protected void onPostExecute(String totals) {
    benchmarkActivity.onBenchmarkDone(totals);
  }

  private static void walkTree(MultiFormatReader reader,
                               File fileOrDir,
                               List<BenchmarkItem> items) {
    Log.i(TAG, "Decoding " + fileOrDir);
    if (fileOrDir.isDirectory()) {
      File[] files = fileOrDir.listFiles();
      if (files != null) {
        Arrays.sort(files);
        for (File file : files) {
          walkTree(reader, file, items);
        }
      }
    } else {
      BenchmarkItem item = decode(reader, fileOrDir);
      if (item != null) {
        items.add(item);
      }
    }
  }

  private static BenchmarkItem decode(MultiFormatReader reader, File file) {

    Bitmap imageBitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
    if (imageBitmap == null) {
      Log.e(TAG, "Couldn't open " + file);
      return null;
    }

    int width = imageBitmap.getWidth();
    int height = imageBitmap.getHeight();
    int[] pixels = new int[width * height];
    imageBitmap.getPixels(pixels, 0, width, 0, 0, width, height);

    RGBLuminanceSource source = new RGBLuminanceSource(width, height, pixels);

    BenchmarkItem item = new BenchmarkItem(file, RUNS);
    for (int x = 0; x < RUNS; x++) {
      boolean success;
      Result result = null;
      // Using this call instead of getting the time should eliminate a lot of variability due to
      // scheduling and what else is happening in the system.
      long now = Debug.threadCpuTimeNanos();
      try {
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
        result = reader.decodeWithState(bitmap);
        success = true;
      } catch (ReaderException ignored) {
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
