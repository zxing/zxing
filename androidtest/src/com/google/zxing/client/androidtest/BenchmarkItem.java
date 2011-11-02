/*
 * Copyright (C) 2008 Google Inc.
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

import com.google.zxing.BarcodeFormat;

public final class BenchmarkItem {

  private final String path;
  private final int[] times;
  private int position;
  private boolean decoded;
  private BarcodeFormat format;

  public BenchmarkItem(String path, int runs) {
    if (runs <= 0) {
      throw new IllegalArgumentException();
    }
    this.path = path;
    times = new int[runs];
    position = 0;
    decoded = false;
    format = null;
  }

  public void addResult(int microseconds) {
    times[position] = microseconds;
    position++;
  }

  public void setDecoded(boolean decoded) {
    this.decoded = decoded;
  }

  public void setFormat(BarcodeFormat format) {
    this.format = format;
  }

  @Override
  public String toString() {
    StringBuilder result = new StringBuilder(30);
    result.append(decoded ? "DECODED " + format.toString() + ": " : "FAILED: ");
    result.append(path);
    result.append(" (");
    result.append(getAverageTime());
    result.append(" us average)");
    return result.toString();
  }

  /**
   * Calculates the average time but throws out the maximum as an outlier first.
   *
   * @return The average decoding time in microseconds.
   */
  public int getAverageTime() {
    int size = times.length;
    int total = 0;
    int max = times[0];
    for (int x = 0; x < size; x++) {
      int time = times[x];
      total += time;
      if (time > max) {
        max = time;
      }
    }
    total -= max;
    size--;
    return size > 0 ? total / size : 0;
  }

}
