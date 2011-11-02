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

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.Writer;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

import java.util.Map;

/**
 * @author Jacob Haynes
 */
public final class PDF417Writer implements Writer {

  @Override
  public BitMatrix encode(String contents,
                          BarcodeFormat format,
                          int width,
                          int height,
                          Map<EncodeHintType,?> hints) throws WriterException {
    return encode(contents, format, width, height);
  }

  @Override
  public BitMatrix encode(String contents,
                          BarcodeFormat format,
                          int width,
                          int height) throws WriterException {
    PDF417 encoder = initializeEncoder(format, false);
    return bitMatrixFromEncoder(encoder, contents, width, height);
  }

  public BitMatrix encode(String contents,
                          BarcodeFormat format,
                          boolean compact,
                          int width,
                          int height,
                          int minCols,
                          int maxCols,
                          int minRows,
                          int maxRows,
                          Compaction compaction) throws WriterException {
    PDF417 encoder = initializeEncoder(format, compact);

    // Set options: dimensions and byte compaction
    encoder.setDimensions(maxCols, minCols, maxRows, minRows);
    encoder.setCompaction(compaction);

    return bitMatrixFromEncoder(encoder, contents, width, height);
  }

  /**
   * Initializes the encoder based on the format (whether it's compact or not)
   */
  private static PDF417 initializeEncoder(BarcodeFormat format, boolean compact) {
    if (format != BarcodeFormat.PDF_417) {
      throw new IllegalArgumentException("Can only encode PDF_417, but got " + format);
    }

    PDF417 encoder = new PDF417();
    encoder.setCompact(compact);
    return encoder;
  }

  /**
   * Takes encoder, accounts for width/height, and retrieves bit matrix
   */
  private static BitMatrix bitMatrixFromEncoder(PDF417 encoder,
                                                String contents,
                                                int width,
                                                int height) throws WriterException {
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
      return bitMatrixFrombitArray(scaledMatrix);
    }
    return bitMatrixFrombitArray(originalScale);
  }

  /**
   * This takes an array holding the values of the PDF 417
   *
   * @param input a byte array of information with 0 is black, and 1 is white
   * @return BitMatrix of the input
   */
  private static BitMatrix bitMatrixFrombitArray(byte[][] input) {
    //Creates a small whitespace boarder around the barcode
    int whiteSpace = 30;

    //Creates the bitmatrix with extra space for whtespace
    BitMatrix output = new BitMatrix(input.length + 2 * whiteSpace, input[0].length + 2 * whiteSpace);
    output.clear();
    for (int ii = 0; ii < input.length; ii++) {
      for (int jj = 0; jj < input[0].length; jj++) {
        // Zero is white in the bytematrix
        if (input[ii][jj] == 1) {
          output.set(ii + whiteSpace, jj + whiteSpace);
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
