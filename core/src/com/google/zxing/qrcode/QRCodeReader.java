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

import com.google.zxing.BarcodeFormat;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MonochromeBitmapSource;
import com.google.zxing.Reader;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.ResultPoint;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.DecoderResult;
import com.google.zxing.common.DetectorResult;
import com.google.zxing.qrcode.decoder.Decoder;
import com.google.zxing.qrcode.detector.Detector;

import java.util.Hashtable;

/**
 * This implementation can detect and decode QR Codes in an image.
 *
 * @author srowen@google.com (Sean Owen)
 */
public final class QRCodeReader implements Reader {

  private static final ResultPoint[] NO_POINTS = new ResultPoint[0];

  private final Decoder decoder = new Decoder();

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
    DecoderResult decoderResult;
    ResultPoint[] points;
    if (hints != null && hints.containsKey(DecodeHintType.PURE_BARCODE)) {
      BitMatrix bits = extractPureBits(image);
      decoderResult = decoder.decode(bits);
      points = NO_POINTS;
    } else {
      DetectorResult result = new Detector(image).detect();
      decoderResult = decoder.decode(result.getBits());
      points = result.getPoints();
    }
    return new Result(decoderResult.getText(), decoderResult.getRawBytes(), points, BarcodeFormat.QR_CODE);
  }

  /**
   * This method detects a barcode in a "pure" image -- that is, pure monochrome image
   * which contains only an unrotated, unskewed, image of a barcode, with some white border
   * around it. This is a specialized method that works exceptionally fast in this special
   * case.
   */
  private static BitMatrix extractPureBits(MonochromeBitmapSource image) throws ReaderException {
    // Now need to determine module size in pixels

    int height = image.getHeight();
    int width = image.getWidth();
    int minDimension = Math.min(height, width);

    // First, skip white border by tracking diagonally from the top left down and to the right:
    int borderWidth = 0;
    while (borderWidth < minDimension && !image.isBlack(borderWidth, borderWidth)) {
      borderWidth++;
    }
    if (borderWidth == minDimension) {
      throw new ReaderException("No black pixels found along diagonal");
    }

    // And then keep tracking across the top-left black module to determine module size
    int moduleEnd = borderWidth;
    while (moduleEnd < minDimension && image.isBlack(moduleEnd, moduleEnd)) {
      moduleEnd++;
    }
    if (moduleEnd == minDimension) {
      throw new ReaderException("No end to black pixels found along diagonal");
    }

    int moduleSize = moduleEnd - borderWidth;

    // And now find where the rightmost black module on the first row ends
    int rowEndOfSymbol = width - 1;
    while (rowEndOfSymbol >= 0 && !image.isBlack(rowEndOfSymbol, borderWidth)) {
      rowEndOfSymbol--;
    }
    if (rowEndOfSymbol < 0) {
      throw new ReaderException("Can't find end of rightmost black module");
    }
    rowEndOfSymbol++;

    // Make sure width of barcode is a multiple of module size
    if ((rowEndOfSymbol - borderWidth) % moduleSize != 0) {
      throw new ReaderException("Bad module size / width: " + moduleSize +
          " / " + (rowEndOfSymbol - borderWidth));
    }
    int dimension = (rowEndOfSymbol - borderWidth) / moduleSize;

    // Push in the "border" by half the module width so that we start
    // sampling in the middle of the module. Just in case the image is a
    // little off, this will help recover.
    borderWidth += moduleSize >> 1;

    int sampleDimension = borderWidth + (dimension - 1) * moduleSize;
    if (sampleDimension >= width || sampleDimension >= height) {
      throw new ReaderException("Estimated pure image size is beyond image boundaries");
    }

    // Now just read off the bits
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