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

package com.google.zxing.multi;

import com.google.zxing.Reader;
import com.google.zxing.Result;
import com.google.zxing.MonochromeBitmapSource;
import com.google.zxing.ReaderException;
import com.google.zxing.CroppedMonochromeBitmapSource;

import java.util.Hashtable;

/**
 * This class attempts to decode a barcode from an image, not by scanning the whole image,
 * but by scanning subsets of the image. This is important when there may be multiple barcodes in
 * an image, and detecting a barcode may find parts of multiple barcode and fail to decode
 * (e.g. QR Codes). Instead this scans the four quadrants of the image -- and also the center 'quadrant'
 * to cover the case where a barcode is found in the center.
 *
 * @see MultipleBarcodeReader
 */
public final class ByQuadrantReader implements Reader {

  private final Reader delegate;

  public ByQuadrantReader(Reader delegate) {
    this.delegate = delegate;
  }

  public Result decode(MonochromeBitmapSource image) throws ReaderException {
    return decode(image, null);
  }

  public Result decode(MonochromeBitmapSource image, Hashtable hints) throws ReaderException {
    
    int width = image.getWidth();
    int height = image.getHeight();
    int halfWidth = width / 2;
    int halfHeight = height / 2;

    MonochromeBitmapSource topLeft = new CroppedMonochromeBitmapSource(image, 0, 0, halfWidth, halfHeight);
    try {
      return delegate.decode(topLeft, hints);
    } catch (ReaderException re) {
      // continue
    }

    MonochromeBitmapSource topRight = new CroppedMonochromeBitmapSource(image, halfWidth, 0, width, halfHeight);
    try {
      return delegate.decode(topRight, hints);
    } catch (ReaderException re) {
      // continue
    }

    MonochromeBitmapSource bottomLeft = new CroppedMonochromeBitmapSource(image, 0, halfHeight, halfWidth, height);
    try {
      return delegate.decode(bottomLeft, hints);
    } catch (ReaderException re) {
      // continue
    }

    MonochromeBitmapSource bottomRight = new CroppedMonochromeBitmapSource(image, halfWidth, halfHeight, width, height);
    try {
      return delegate.decode(bottomRight, hints);
    } catch (ReaderException re) {
      // continue
    }

    int quarterWidth = halfWidth / 2;
    int quarterHeight = halfHeight / 2;
    MonochromeBitmapSource center = new CroppedMonochromeBitmapSource(image,
                                                                      quarterWidth,
                                                                      quarterHeight,
                                                                      width - quarterWidth,
                                                                      height - quarterHeight);
    return delegate.decode(center, hints);
  }

}
