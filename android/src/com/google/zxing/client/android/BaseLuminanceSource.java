/*
 * Copyright 2009 ZXing authors
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

package com.google.zxing.client.android;

import com.google.zxing.LuminanceSource;

import android.graphics.Bitmap;

/**
 * An extension of LuminanceSource which adds some Android-specific methods.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
public abstract class BaseLuminanceSource extends LuminanceSource {
  public BaseLuminanceSource(int width, int height) {
    super(width, height);
  }

  /**
   * Requests the width of the underlying platform's bitmap.
   *
   * @return The width in pixels.
   */
  public abstract int getDataWidth();

  /**
   * Requests the height of the underlying platform's bitmap.
   *
   * @return The height in pixels.
   */
  public abstract int getDataHeight();

  /**
   * Creates a greyscale Android Bitmap from the YUV data based on the crop rectangle.
   *
   * @return An 8888 bitmap.
   */
  public abstract Bitmap renderCroppedGreyscaleBitmap();

  /**
   * Creates a color Android Bitmap from the YUV data, ignoring the crop rectangle.
   *
   * @param halfSize If true, downsample to 50% in each dimension, otherwise not.
   * @return An 8888 bitmap.
   */
  public abstract Bitmap renderFullColorBitmap(boolean halfSize);
}
