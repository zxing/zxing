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

public class BenchmarkItem {

  private String mPath;
  private int[] mTimes;
  private int mPosition;
  private boolean mDecoded;
  private BarcodeFormat mFormat;

  public BenchmarkItem(String path, int runs) {
    mPath = path;
    mTimes = new int[runs];
    mPosition = 0;
    mDecoded = false;
    mFormat = null;
  }

  // I'm storing these separately instead of as a running total so I can add features like
  // calculating the min and max later, or ignoring outliers.
  public void addResult(int milliseconds) {
    mTimes[mPosition] = milliseconds;
    mPosition++;
  }

  public void setDecoded(boolean decoded) {
    mDecoded = decoded;
  }

  public void setFormat(BarcodeFormat format) {
    mFormat = format;
  }

  public String getPath() {
    return mPath;
  }

  public int getAverageMilliseconds() {
    int size = mTimes.length;
    int total = 0;
    for (int x = 0; x < size; x++) {
      total += mTimes[x];
    }
    if (size > 0) {
      return total / size;
    } else {
      return 0;
    }
  }

  public int getCount() {
    return mTimes.length;
  }

  public boolean getDecoded() {
    return mDecoded;
  }

  public BarcodeFormat getFormat() {
    return mFormat;
  }

}
