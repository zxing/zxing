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

package com.google.zxing.qrcode;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.Writer;
import com.google.zxing.WriterException;
import com.google.zxing.common.ByteMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.google.zxing.qrcode.encoder.Encoder;
import com.google.zxing.qrcode.encoder.QRCode;

import java.util.Hashtable;

/**
 * This object renders a QR Code as a ByteMatrix 2D array of greyscale values.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class QRCodeWriter implements Writer {

  private static final int QUIET_ZONE_SIZE = 4;

  public ByteMatrix encode(String contents, BarcodeFormat format, int width, int height)
      throws WriterException {

    return encode(contents, format, width, height, null);
  }

  public ByteMatrix encode(String contents, BarcodeFormat format, int width, int height,
      Hashtable hints) throws WriterException {

    if (contents == null || contents.length() == 0) {
      throw new IllegalArgumentException("Found empty contents");
    }

    if (format != BarcodeFormat.QR_CODE) {
      throw new IllegalArgumentException("Can only encode QR_CODE, but got " + format);
    }

    if (width < 0 || height < 0) {
      throw new IllegalArgumentException("Requested dimensions are too small: " + width + 'x' +
          height);
    }

    ErrorCorrectionLevel errorCorrectionLevel = ErrorCorrectionLevel.L;
    if (hints != null) {
      ErrorCorrectionLevel requestedECLevel = (ErrorCorrectionLevel) hints.get(EncodeHintType.ERROR_CORRECTION);
      if (requestedECLevel != null) {
        errorCorrectionLevel = requestedECLevel;
      }
    }

    QRCode code = new QRCode();
    Encoder.encode(contents, errorCorrectionLevel, hints, code);
    return renderResult(code, width, height);
  }

  // Note that the input matrix uses 0 == white, 1 == black, while the output matrix uses
  // 0 == black, 255 == white (i.e. an 8 bit greyscale bitmap).
  private static ByteMatrix renderResult(QRCode code, int width, int height) {
    ByteMatrix input = code.getMatrix();
    int inputWidth = input.getWidth();
    int inputHeight = input.getHeight();
    int qrWidth = inputWidth + (QUIET_ZONE_SIZE << 1);
    int qrHeight = inputHeight + (QUIET_ZONE_SIZE << 1);
    int outputWidth = Math.max(width, qrWidth);
    int outputHeight = Math.max(height, qrHeight);

    int multiple = Math.min(outputWidth / qrWidth, outputHeight / qrHeight);
    // Padding includes both the quiet zone and the extra white pixels to accommodate the requested
    // dimensions. For example, if input is 25x25 the QR will be 33x33 including the quiet zone.
    // If the requested size is 200x160, the multiple will be 4, for a QR of 132x132. These will
    // handle all the padding from 100x100 (the actual QR) up to 200x160.
    int leftPadding = (outputWidth - (inputWidth * multiple)) / 2;
    int topPadding = (outputHeight - (inputHeight * multiple)) / 2;

    ByteMatrix output = new ByteMatrix(outputWidth, outputHeight);
    byte[][] outputArray = output.getArray();

    // We could be tricky and use the first row in each set of multiple as the temporary storage,
    // instead of allocating this separate array.
    byte[] row = new byte[outputWidth];

    // 1. Write the white lines at the top
    for (int y = 0; y < topPadding; y++) {
      setRowColor(outputArray[y], (byte) 255);
    }

    // 2. Expand the QR image to the multiple
    byte[][] inputArray = input.getArray();
    for (int y = 0; y < inputHeight; y++) {
      // a. Write the white pixels at the left of each row
      for (int x = 0; x < leftPadding; x++) {
        row[x] = (byte) 255;
      }

      // b. Write the contents of this row of the barcode
      int offset = leftPadding;
      for (int x = 0; x < inputWidth; x++) {
        byte value = (inputArray[y][x] == 1) ? 0 : (byte) 255;
        for (int z = 0; z < multiple; z++) {
          row[offset + z] = value;
        }
        offset += multiple;
      }

      // c. Write the white pixels at the right of each row
      offset = leftPadding + (inputWidth * multiple);
      for (int x = offset; x < outputWidth; x++) {
        row[x] = (byte) 255;
      }

      // d. Write the completed row multiple times
      offset = topPadding + (y * multiple);
      for (int z = 0; z < multiple; z++) {
        System.arraycopy(row, 0, outputArray[offset + z], 0, outputWidth);
      }
    }

    // 3. Write the white lines at the bottom
    int offset = topPadding + (inputHeight * multiple);
    for (int y = offset; y < outputHeight; y++) {
      setRowColor(outputArray[y], (byte) 255);
    }

    return output;
  }

  private static void setRowColor(byte[] row, byte value) {
    for (int x = 0; x < row.length; x++) {
      row[x] = value;
    }
  }

}
