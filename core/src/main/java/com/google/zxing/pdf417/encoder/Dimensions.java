/*
 * Copyright 2012 ZXing authors
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

package com.google.zxing.pdf417.encoder;

/**
 * Data object to specify the minimum and maximum number of rows and columns for a PDF417 barcode.
 *
 * @author qwandor@google.com (Andrew Walbran)
 */
public final class Dimensions {

  private final int minCols;
  private final int maxCols;
  private final int minRows;
  private final int maxRows;

  public Dimensions(int minCols, int maxCols, int minRows, int maxRows) {
    this.minCols = minCols;
    this.maxCols = maxCols;
    this.minRows = minRows;
    this.maxRows = maxRows;
  }

  public int getMinCols() {
    return minCols;
  }

  public int getMaxCols() {
    return maxCols;
  }

  public int getMinRows() {
    return minRows;
  }

  public int getMaxRows() {
    return maxRows;
  }

}
