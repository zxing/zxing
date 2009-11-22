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

public final class PlanarYUV420LuminanceSource extends AbstractPlanarYUVLuminanceSource {

  public PlanarYUV420LuminanceSource(byte[] yuvData, int dataWidth, int dataHeight,
                                     int left, int top, int width, int height) {
    super(yuvData, dataWidth, dataHeight, left, top, width, height);
  }

  @Override
  public LuminanceSource crop(int left, int top, int width, int height) {
    return new PlanarYUV420LuminanceSource(
        getYUVData(), getDataWidth(), getDataHeight(), left, top, width, height);
  }

  @Override
  public Bitmap renderFullColorBitmap(boolean halfSize) {
    // TODO implement halfSize
    int width = getWidth();
    int height = getHeight();
    int dataWidth = getDataWidth();
    int dataHeight = getDataHeight();
    byte[] yuv = getYUVData();
    int expectedYBytes = dataWidth * dataHeight;
    int expectedUBytes = expectedYBytes >> 2;
    int expectedVBytes = expectedYBytes >> 2;
    int expectedBytes = expectedYBytes + expectedUBytes + expectedVBytes;
    if (yuv.length != expectedBytes) {
      throw new IllegalStateException("Expected " + expectedBytes + " bytes");
    }

    int[] pixels = new int[width * height];
    int inputYOffset = getTop() * getDataWidth() + getLeft();
    int uOffset = expectedYBytes;
    int vOffset = expectedYBytes + expectedUBytes;
    
    for (int y = 0; y < height; y++) {
      int outputOffset = y * width;
      for (int x = 0; x < width; x++) {
        int yOffset = inputYOffset + x;
        int yDataRow = yOffset / dataWidth;
        int yDataOffset = yOffset % dataWidth;
        int uvOffset = ((yDataRow >> 1) * dataWidth + yDataOffset) >> 1;
        int y1 = yuv[yOffset] & 0xFF;
        int u = yuv[uOffset + uvOffset] & 0xFF;
        int v = yuv[vOffset + uvOffset] & 0XFF;
        pixels[outputOffset + x] =
            OPAQUE_ALPHA | InterleavedYUV422LuminanceSource.yuvToRGB(y1, u, v);
      }
      inputYOffset += dataWidth;
    }

    Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
    bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
    return bitmap;
  }

}
