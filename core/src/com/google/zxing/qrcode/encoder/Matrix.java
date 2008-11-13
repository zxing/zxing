/*
 * Copyright 2008 ZXing authors
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

package com.google.zxing.qrcode.encoder;

/**
 * A class which wraps a 2D array.
 *
 * JAVAPORT: I'm not happy about the argument ordering throughout the file, as I always like to have
 * the horizontal component first, but this is for compatibility with the C++ code. The original
 * code was a 2D array of ints, but since it only ever gets assigned zeros and ones, I'm going to
 * use less memory and go with bytes.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class Matrix {

  private final byte[][] bytes;
  private final int height;
  private final int width;

  public Matrix(int height, int width) {
    bytes = new byte[height][width];
    this.height = height;
    this.width = width;
  }

  public final int height() {
    return height;
  }

  public final int width() {
    return width;
  }

  public final byte get(int y, int x) {
    return bytes[y][x];
  }

  public final void set(int y, int x, byte value) {
    bytes[y][x] = value;
  }

  public final void set(int y, int x, int value) {
    bytes[y][x] = (byte) value;
  }

}
