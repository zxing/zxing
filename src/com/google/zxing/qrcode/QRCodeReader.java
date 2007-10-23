/*
 * Copyright 2007 Google Inc.
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

package com.google.zxing.qrcode;

import com.google.zxing.DecodeHintType;
import com.google.zxing.MonochromeBitmapSource;
import com.google.zxing.Reader;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.ResultPoint;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.Decoder;
import com.google.zxing.qrcode.detector.Detector;
import com.google.zxing.qrcode.detector.DetectorResult;

import java.util.Hashtable;

/**
 * @author srowen@google.com (Sean Owen)
 */
public final class QRCodeReader implements Reader {

  /**
   * Locates and decodes a QR code in an image.
   *
   * @return a String representing the content encoded by the QR code
   * @throws ReaderException if a QR code cannot be found, or cannot be decoded
   */
  public Result decode(MonochromeBitmapSource image) throws ReaderException {
    return decode(image, null);
  }

  public Result decode(MonochromeBitmapSource image, Hashtable hints)
      throws ReaderException {
    String text;
    ResultPoint[] points;
    if (hints != null && hints.containsKey(DecodeHintType.PURE_BARCODE)) {
      BitMatrix bits = extractPureBits(image);
      text = Decoder.decode(bits);
      points = new ResultPoint[0];
    } else {
      DetectorResult result = new Detector(image).detect();
      text = Decoder.decode(result.getBits());
      points = result.getPoints();
    }
    return new Result(text, points);
  }

  private static BitMatrix extractPureBits(MonochromeBitmapSource image)
      throws ReaderException {
    // Now need to determine module size in pixels
    // First, skip white border
    int borderWidth = 0;
    while (!image.isBlack(borderWidth, borderWidth)) {
      borderWidth++;
    }
    int moduleEnd = borderWidth;
    while (image.isBlack(moduleEnd, moduleEnd)) {
      moduleEnd++;
    }
    int moduleSize = moduleEnd - borderWidth;

    int rowEndOfSymbol = image.getWidth() - 1;
    while (!image.isBlack(rowEndOfSymbol, borderWidth)) {
      rowEndOfSymbol--;
    }
    rowEndOfSymbol++;

    if ((rowEndOfSymbol - borderWidth) % moduleSize != 0) {
      throw new ReaderException("Bad module size / width: " + moduleSize +
          " / " + (rowEndOfSymbol - borderWidth));
    }
    int dimension = (rowEndOfSymbol - borderWidth) / moduleSize;

    // Push in the "border" by half the module width so that we start
    // sampling in the middle of the module. Just in case the image is a
    // little off, this will help recover.
    borderWidth += moduleSize >> 1;

    BitMatrix bits = new BitMatrix(dimension);
    for (int i = 0; i < dimension; i++) {
      int iOffset = borderWidth + i * moduleSize;
      for (int j = 0; j < dimension; j++) {
        if (image.isBlack(borderWidth + j * moduleSize, iOffset)) {
          bits.set(i, j);
        }
      }
    }
    return bits;
  }

}