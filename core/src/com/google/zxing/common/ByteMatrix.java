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

package com.google.zxing.common;

/**
 * A class which wraps a 2D array of bytes. The default usage is signed. If you want to use it as a
 * unsigned container, it's up to you to do byteValue & 0xff at each location.
 *
 * JAVAPORT: I'm not happy about the argument ordering throughout the file, as I always like to have
 * the horizontal component first, but this is for compatibility with the C++ code. The original
 * code was a 2D array of ints, but since it only ever gets assigned -1, 0, and 1, I'm going to use
 * less memory and go with bytes.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class ByteMatrix {

  private final byte[][] bytes;
  private final int height;
  private final int width;

  public ByteMatrix(int height, int width) {
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

  public final byte[][] getArray() {
    return bytes;
  }

  public final void set(int y, int x, byte value) {
    bytes[y][x] = value;
  }

  public final void set(int y, int x, int value) {
    bytes[y][x] = (byte) value;
  }

  public final void clear(byte value) {
    for (int y = 0; y < height; ++y) {
      for (int x = 0; x < width; ++x) {
        bytes[y][x] = value;
      }
    }
  }

  public final String toString() {
    StringBuffer result = new StringBuffer();
    for (int y = 0; y < height; ++y) {
      for (int x = 0; x < width; ++x) {
        switch (bytes[y][x]) {
          case 0:
            result.append(" 0");
            break;
          case 1:
            result.append(" 1");
            break;
          default:
            result.append("  ");
            break;
        }
      }
      result.append("\n");
    }
    return result.toString();
  }

}
