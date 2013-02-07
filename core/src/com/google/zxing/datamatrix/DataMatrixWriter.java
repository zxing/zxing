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

package com.google.zxing.datamatrix;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.Writer;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.datamatrix.encoder.DefaultPlacement;
import com.google.zxing.Dimension;
import com.google.zxing.datamatrix.encoder.ErrorCorrection;
import com.google.zxing.datamatrix.encoder.HighLevelEncoder;
import com.google.zxing.datamatrix.encoder.SymbolInfo;
import com.google.zxing.datamatrix.encoder.SymbolShapeHint;
import com.google.zxing.qrcode.encoder.ByteMatrix;

import java.util.Map;

/**
 * This object renders a Data Matrix code as a BitMatrix 2D array of greyscale values.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 * @author Guillaume Le Biller Added to zxing lib.
 */
public final class DataMatrixWriter implements Writer {

  @Override
  public BitMatrix encode(String contents, BarcodeFormat format, int width, int height) {
    return encode(contents, format, width, height, null);
  }

  @Override
  public BitMatrix encode(String contents, BarcodeFormat format, int width, int height, Map<EncodeHintType,?> hints) {

    if (contents.length() == 0) {
      throw new IllegalArgumentException("Found empty contents");
    }
    
    if (format != BarcodeFormat.DATA_MATRIX) {
      throw new IllegalArgumentException("Can only encode DATA_MATRIX, but got " + format);
    }
    
    if (width < 0 || height < 0) {
      throw new IllegalArgumentException("Requested dimensions are too small: " + width + 'x' + height);
    }

    // Try to get force shape & min / max size
    SymbolShapeHint shape = SymbolShapeHint.FORCE_NONE;
    Dimension minSize = null;
    Dimension maxSize = null;
    if (hints != null) {
      SymbolShapeHint requestedShape = (SymbolShapeHint) hints.get(EncodeHintType.DATA_MATRIX_SHAPE);
      if (requestedShape != null) {
        shape = requestedShape;
      }
      Dimension requestedMinSize = (Dimension) hints.get(EncodeHintType.MIN_SIZE);
      if (requestedMinSize != null) {
        minSize = requestedMinSize;
      }
      Dimension requestedMaxSize = (Dimension) hints.get(EncodeHintType.MAX_SIZE);
      if (requestedMaxSize != null) {
        maxSize = requestedMaxSize;
      }
    }


    //1. step: Data encodation
    String encoded = HighLevelEncoder.encodeHighLevel(contents, shape, minSize, maxSize);

    SymbolInfo symbolInfo = SymbolInfo.lookup(encoded.length(), shape, minSize, maxSize, true);

    //2. step: ECC generation
    String codewords = ErrorCorrection.encodeECC200(encoded, symbolInfo);

    //3. step: Module placement in Matrix
    DefaultPlacement placement =
        new DefaultPlacement(codewords, symbolInfo.getSymbolDataWidth(), symbolInfo.getSymbolDataHeight());
    placement.place();

    //4. step: low-level encoding
    return encodeLowLevel(placement, symbolInfo);
  }

  /**
   * Encode the given symbol info to a bit matrix.
   *
   * @param placement  The DataMatrix placement.
   * @param symbolInfo The symbol info to encode.
   * @return The bit matrix generated.
   */
  private static BitMatrix encodeLowLevel(DefaultPlacement placement, SymbolInfo symbolInfo) {
    int symbolWidth = symbolInfo.getSymbolDataWidth();
    int symbolHeight = symbolInfo.getSymbolDataHeight();

    ByteMatrix matrix = new ByteMatrix(symbolInfo.getSymbolWidth(), symbolInfo.getSymbolHeight());

    int matrixY = 0;

    for (int y = 0; y < symbolHeight; y++) {
      // Fill the top edge with alternate 0 / 1
      int matrixX;
      if ((y % symbolInfo.matrixHeight) == 0) {
        matrixX = 0;
        for (int x = 0; x < symbolInfo.getSymbolWidth(); x++) {
          matrix.set(matrixX, matrixY, (x % 2) == 0);
          matrixX++;
        }
        matrixY++;
      }
      matrixX = 0;
      for (int x = 0; x < symbolWidth; x++) {
        // Fill the right edge with full 1
        if ((x % symbolInfo.matrixWidth) == 0) {
          matrix.set(matrixX, matrixY, true);
          matrixX++;
        }
        matrix.set(matrixX, matrixY, placement.getBit(x, y));
        matrixX++;
        // Fill the right edge with alternate 0 / 1
        if ((x % symbolInfo.matrixWidth) == symbolInfo.matrixWidth - 1) {
          matrix.set(matrixX, matrixY, (y % 2) == 0);
          matrixX++;
        }
      }
      matrixY++;
      // Fill the bottom edge with full 1
      if ((y % symbolInfo.matrixHeight) == symbolInfo.matrixHeight - 1) {
        matrixX = 0;
        for (int x = 0; x < symbolInfo.getSymbolWidth(); x++) {
          matrix.set(matrixX, matrixY, true);
          matrixX++;
        }
        matrixY++;
      }
    }

    return convertByteMatrixToBitMatrix(matrix);
  }

  /**
   * Convert the ByteMatrix to BitMatrix.
   *
   * @param matrix The input matrix.
   * @return The output matrix.
   */
  private static BitMatrix convertByteMatrixToBitMatrix(ByteMatrix matrix) {
    int matrixWidgth = matrix.getWidth();
    int matrixHeight = matrix.getHeight();

    BitMatrix output = new BitMatrix(matrixWidgth, matrixHeight);
    output.clear();
    for (int i = 0; i < matrixWidgth; i++) {
      for (int j = 0; j < matrixHeight; j++) {
        // Zero is white in the bytematrix
        if (matrix.get(i, j) == 1) {
          output.set(i, j);
        }
      }
    }

    return output;
  }

}
