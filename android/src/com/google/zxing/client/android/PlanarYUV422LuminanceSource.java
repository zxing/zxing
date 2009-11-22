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

import android.graphics.Bitmap;
import com.google.zxing.LuminanceSource;

public final class PlanarYUV422LuminanceSource extends AbstractPlanarYUVLuminanceSource {

  public PlanarYUV422LuminanceSource(byte[] yuvData, int dataWidth, int dataHeight,
                                     int left, int top, int width, int height) {
    super(yuvData, dataWidth, dataHeight, left, top, width, height);
  }

  @Override
  public LuminanceSource crop(int left, int top, int width, int height) {
    return new PlanarYUV422LuminanceSource(
        getYUVData(), getDataWidth(), getDataHeight(), left, top, width, height);
  }

  @Override
  public Bitmap renderFullColorBitmap(boolean halfSize) {
    throw new UnsupportedOperationException();
  }

}