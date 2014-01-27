/*
 * Copyright 2012 ZXing authors
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

package com.google.zxing.pdf417;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.Writer;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.pdf417.encoder.Compaction;
import com.google.zxing.pdf417.encoder.Dimensions;
import com.google.zxing.pdf417.encoder.PDF417;

import java.nio.charset.Charset;
import java.util.Map;

/**
 * @author Jacob Haynes
 * @author qwandor@google.com (Andrew Walbran)
 */
public final class PDF417Writer implements Writer {

  /**
   * default white space (margin) around the code
   */
  static final int WHITE_SPACE = 30;

  @Override
  public BitMatrix encode(String contents,
                          BarcodeFormat format,
                          int width,
                          int height,
                          Map<EncodeHintType,?> hints) throws WriterException {
    if (format != BarcodeFormat.PDF_417) {
      throw new IllegalArgumentException("Can only encode PDF_417, but got " + format);
    }

    PDF417 encoder = new PDF417();
    int margin = WHITE_SPACE;

    if (hints != null) {
      if (hints.containsKey(EncodeHintType.PDF417_COMPACT)) {
        encoder.setCompact((Boolean) hints.get(EncodeHintType.PDF417_COMPACT));
      }
      if (hints.containsKey(EncodeHintType.PDF417_COMPACTION)) {
        encoder.setCompaction((Compaction) hints.get(EncodeHintType.PDF417_COMPACTION));
      }
      if (hints.containsKey(EncodeHintType.PDF417_DIMENSIONS)) {
        Dimensions dimensions = (Dimensions) hints.get(EncodeHintType.PDF417_DIMENSIONS);
        encoder.setDimensions(dimensions.getMaxCols(),
                              dimensions.getMinCols(),
                              dimensions.getMaxRows(),
                              dimensions.getMinRows());
      }
      if (hints.containsKey(EncodeHintType.MARGIN)) {
        margin = ((Number) hints.get(EncodeHintType.MARGIN)).intValue();
      }
      if (hints.containsKey(EncodeHintType.CHARACTER_SET)) {
        String encoding = (String) hints.get(EncodeHintType.CHARACTER_SET);
        encoder.setEncoding(Charset.forName(encoding));
      }
    }

    return bitMatrixFromEncoder(encoder, contents, width, height, margin);
  }

  @Override
  public BitMatrix encode(String contents,
                          BarcodeFormat format,
                          int width,
                          int height) throws WriterException {
    return encode(contents, format, width, height, null);
  }

  /**
   * Takes encoder, accounts for width/height, and retrieves bit matrix
   */
  private static BitMatrix bitMatrixFromEncoder(PDF417 encoder,
                                                String contents,
                                                int width,
                                                int height,
                                                int margin) throws WriterException {
    int errorCorrectionLevel = 2;
    encoder.generateBarcodeLogic(contents, errorCorrectionLevel);

    int lineThickness = 2;
    int aspectRatio = 4;
    byte[][] originalScale = encoder.getBarcodeMatrix().getScaledMatrix(lineThickness, aspectRatio * lineThickness);
    boolean rotated = false;
    if ((height > width) ^ (originalScale[0].length < originalScale.length)) {
      originalScale = rotateArray(originalScale);
      rotated = true;
    }

    int scaleX = width / originalScale[0].length;
    int scaleY = height / originalScale.length;

    int scale;
    if (scaleX < scaleY) {
      scale = scaleX;
    } else {
      scale = scaleY;
    }

    if (scale > 1) {
      byte[][] scaledMatrix =
          encoder.getBarcodeMatrix().getScaledMatrix(scale * lineThickness, scale * aspectRatio * lineThickness);
      if (rotated) {
        scaledMatrix = rotateArray(scaledMatrix);
      }
      return bitMatrixFrombitArray(scaledMatrix, margin);
    }
    return bitMatrixFrombitArray(originalScale, margin);
  }

  /**
   * This takes an array holding the values of the PDF 417
   *
   * @param input a byte array of information with 0 is black, and 1 is white
   * @param margin border around the barcode
   * @return BitMatrix of the input
   */
  private static BitMatrix bitMatrixFrombitArray(byte[][] input, int margin) {
    // Creates the bitmatrix with extra space for whitespace
    BitMatrix output = new BitMatrix(input[0].length + 2 * margin, input.length + 2 * margin);
    output.clear();
    for (int y = 0, yOutput = output.getHeight() - margin - 1; y < input.length; y++, yOutput--) {
      for (int x = 0; x < input[0].length; x++) {
        // Zero is white in the bytematrix
        if (input[y][x] == 1) {
          output.set(x + margin, yOutput);
        }
      }
    }
    return output;
  }

  /**
   * Takes and rotates the it 90 degrees
   */
  private static byte[][] rotateArray(byte[][] bitarray) {
    byte[][] temp = new byte[bitarray[0].length][bitarray.length];
    for (int ii = 0; ii < bitarray.length; ii++) {
      // This makes the direction consistent on screen when rotating the
      // screen;
      int inverseii = bitarray.length - ii - 1;
      for (int jj = 0; jj < bitarray[0].length; jj++) {
        temp[jj][inverseii] = bitarray[ii][jj];
      }
    }
    return temp;
  }

}
