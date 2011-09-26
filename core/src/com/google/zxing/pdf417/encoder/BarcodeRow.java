/*
 * Copyright 2011 ZXing authors
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
 * @author Jacob Haynes
 */
final class BarcodeRow {

  private final byte[] row;
  //A tacker for position in the bar
  private int currentLocation;

  /**
   * Creates a Barcode row of the width
   *
   * @param width
   */
  BarcodeRow(int width) {
    this.row = new byte[width];
    currentLocation = 0;
  }

  /**
   * Sets a specific location in the bar
   *
   * @param x The location in the bar
   * @param value Black if true, white if false;
   */
  void set(int x, byte value) {
    row[x] = value;
  }

  /**
   * Sets a specific location in the bar
   *
   * @param x The location in the bar
   * @param black Black if true, white if false;
   */
  void set(int x, boolean black) {
    row[x] = (byte) (black ? 1 : 0);
  }

  /**
   * @param black A boolean which is true if the bar black false if it is white
   * @param width How many spots wide the bar is.
   */
  void addBar(boolean black, int width) {
    for (int ii = 0; ii < width; ii++) {
      set(currentLocation++, black);
    }
  }

  byte[] getRow() {
    return row;
  }

  /**
   * This function scales the row
   *
   * @param scale How much you want the image to be scaled, must be greater than or equal to 1.
   * @return the scaled row
   */
  byte[] getScaledRow(int scale) {
    byte[] output = new byte[row.length * scale];
    for (int i = 0; i < output.length; i++) {
      output[i] = row[i / scale];
    }
    return output;
  }
}
