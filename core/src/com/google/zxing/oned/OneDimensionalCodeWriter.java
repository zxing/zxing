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

package com.google.zxing.oned;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.Writer;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

import java.util.Map;

/**
 * <p>Encapsulates functionality and implementation that is common to one-dimensional barcodes.</p>
 *
 * @author dsbnatut@gmail.com (Kazuki Nishiura)
 */
public abstract class OneDimensionalCodeWriter implements Writer {

  private final int sidesMargin;

  protected OneDimensionalCodeWriter(int sidesMargin) {
    this.sidesMargin = sidesMargin;
  }

  @Override
  public BitMatrix encode(String contents, BarcodeFormat format, int width, int height)
      throws WriterException {
    return encode(contents, format, width, height, null);
  }

  /**
   * Encode the contents following specified format.
   * {@code width} and {@code height} are required size. This method may return bigger size
   * {@code BitMatrix} when specified size is too small. The user can set both {@code width} and
   * {@code height} to zero to get minimum size barcode. If negative value is set to {@code width}
   * or {@code height}, {@code IllegalArgumentException} is thrown.
   */
  @Override
  public BitMatrix encode(String contents,
                          BarcodeFormat format,
                          int width,
                          int height,
                          Map<EncodeHintType,?> hints) throws WriterException {
    if (contents.length() == 0) {
      throw new IllegalArgumentException("Found empty contents");
    }

    if (width < 0 || height < 0) {
      throw new IllegalArgumentException("Negative size is not allowed. Input: "
                                             + width + 'x' + height);
    }

    byte[] code = encode(contents);
    return renderResult(code, width, height);
  }

  /**
   * @return a byte array of horizontal pixels (0 = white, 1 = black)
   */
  private BitMatrix renderResult(byte[] code, int width, int height) {
    int inputWidth = code.length;
    // Add quiet zone on both sides.
    int fullWidth = inputWidth + sidesMargin;
    int outputWidth = Math.max(width, fullWidth);
    int outputHeight = Math.max(1, height);

    int multiple = outputWidth / fullWidth;
    int leftPadding = (outputWidth - (inputWidth * multiple)) / 2;

    BitMatrix output = new BitMatrix(outputWidth, outputHeight);
    for (int inputX = 0, outputX = leftPadding; inputX < inputWidth; inputX++, outputX += multiple) {
      if (code[inputX] == 1) {
        output.setRegion(outputX, 0, multiple, outputHeight);
      }
    }
    return output;
  }


  /**
   * Appends the given pattern to the target array starting at pos.
   *
   * @param startColor starting color - 0 for white, 1 for black
   * @return the number of elements added to target.
   */
  protected static int appendPattern(byte[] target, int pos, int[] pattern, int startColor) {
    if (startColor != 0 && startColor != 1) {
      throw new IllegalArgumentException(
          "startColor must be either 0 or 1, but got: " + startColor);
    }

    byte color = (byte) startColor;
    int numAdded = 0;
    for (int len : pattern) {
      for (int j = 0; j < len; j++) {
        target[pos] = color;
        pos += 1;
        numAdded += 1;
      }
      color ^= 1; // flip color after each segment
    }
    return numAdded;
  }

  /**
   * Encode the contents to byte array expression of one-dimensional barcode.
   * Start code and end code should be included in result, and side margins should not be included.
   *
   * @return a byte array of horizontal pixels (0 = white, 1 = black)
   */
  public abstract byte[] encode(String contents);
}

