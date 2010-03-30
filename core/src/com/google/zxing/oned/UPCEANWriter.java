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

package com.google.zxing.oned;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.Writer;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

import java.util.Hashtable;

/**
 * <p>Encapsulates functionality and implementation that is common to UPC and EAN families
 * of one-dimensional barcodes.</p>
 *
 * @author aripollak@gmail.com (Ari Pollak)
 */
public abstract class UPCEANWriter implements Writer {

  public BitMatrix encode(String contents, BarcodeFormat format, int width, int height)
  throws WriterException {
    return encode(contents, format, width, height, null);
  }

  public BitMatrix encode(String contents, BarcodeFormat format, int width, int height,
      Hashtable hints) throws WriterException {
    if (contents == null || contents.length() == 0) {
      throw new IllegalArgumentException("Found empty contents");
    }

    if (width < 0 || height < 0) {
      throw new IllegalArgumentException("Requested dimensions are too small: "
          + width + 'x' + height);
    }

    byte[] code = encode(contents);
    return renderResult(code, width, height);
  }

  /** @return a byte array of horizontal pixels (0 = white, 1 = black) */
  private static BitMatrix renderResult(byte[] code, int width, int height) {
    int inputWidth = code.length;
    // Add quiet zone on both sides
    int fullWidth = inputWidth + (UPCEANReader.START_END_PATTERN.length << 1);
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
   * @param startColor
   *          starting color - 0 for white, 1 for black
   * @return the number of elements added to target.
   */
   protected static int appendPattern(byte[] target, int pos, int[] pattern, int startColor) {
    if (startColor != 0 && startColor != 1) {
      throw new IllegalArgumentException(
          "startColor must be either 0 or 1, but got: " + startColor);
    }

    byte color = (byte) startColor;
    int numAdded = 0;
    for (int i = 0; i < pattern.length; i++) {
      for (int j = 0; j < pattern[i]; j++) {
        target[pos] = color;
        pos += 1;
        numAdded += 1;
      }
      color ^= 1; // flip color after each segment
    }
    return numAdded;
  }

  /** @return a byte array of horizontal pixels (0 = white, 1 = black) */
  public abstract byte[] encode(String contents);

}
