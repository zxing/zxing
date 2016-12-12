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

import java.util.Arrays;

/**
 * JAVAPORT: The original code was a 2D array of ints, but since it only ever gets assigned
 * -1, 0, and 1, I'm going to use less memory and go with bytes.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class ByteMatrix {

  private final byte[][] bytes;
  private final int width;
  private final int height;

  public ByteMatrix(int width, int height) {
    bytes = new byte[height][width];
    this.width = width;
    this.height = height;
  }

  public int getHeight() {
    return height;
  }

  public int getWidth() {
    return width;
  }

  public byte get(int x, int y) {
    return bytes[y][x];
  }

  /**
   * @return an internal representation as bytes, in row-major order. array[y][x] represents point (x,y)
   */
  public byte[][] getArray() {
    return bytes;
  }

  public void set(int x, int y, byte value) {
    bytes[y][x] = value;
  }

  public void set(int x, int y, int value) {
    bytes[y][x] = (byte) value;
  }

  public void set(int x, int y, boolean value) {
    bytes[y][x] = (byte) (value ? 1 : 0);
  }

  public void clear(byte value) {
    for (byte[] aByte : bytes) {
      Arrays.fill(aByte, value);
    }
  }

  @Override
  public String toString() {
    StringBuilder result = new StringBuilder(2 * width * height + 2);
    for (int y = 0; y < height; ++y) {
      byte[] bytesY = bytes[y];
      for (int x = 0; x < width; ++x) {
        switch (bytesY[x]) {
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
      result.append('\n');
    }
    return result.toString();
  }

}
