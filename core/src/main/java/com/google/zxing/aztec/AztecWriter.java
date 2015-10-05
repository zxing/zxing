/*
 * Copyright 2013 ZXing authors
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

package com.google.zxing.aztec;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.Writer;
import com.google.zxing.aztec.encoder.AztecCode;
import com.google.zxing.aztec.encoder.Encoder;
import com.google.zxing.common.BitMatrix;

import java.nio.charset.Charset;
import java.util.Map;

public final class AztecWriter implements Writer {
  
  private static final Charset DEFAULT_CHARSET = Charset.forName("ISO-8859-1");

  @Override
  public BitMatrix encode(String contents, BarcodeFormat format, int width, int height) {
    return encode(contents, format, width, height, null);
  }

  @Override
  public BitMatrix encode(String contents, BarcodeFormat format, int width, int height, Map<EncodeHintType,?> hints) {
    Charset charset = DEFAULT_CHARSET;
    int eccPercent = Encoder.DEFAULT_EC_PERCENT;
    int layers = Encoder.DEFAULT_AZTEC_LAYERS;
    if (hints != null) {
      if (hints.containsKey(EncodeHintType.CHARACTER_SET)) {
        charset = Charset.forName(hints.get(EncodeHintType.CHARACTER_SET).toString());
      }
      if (hints.containsKey(EncodeHintType.ERROR_CORRECTION)) {
        eccPercent = Integer.parseInt(hints.get(EncodeHintType.ERROR_CORRECTION).toString());
      }
      if (hints.containsKey(EncodeHintType.AZTEC_LAYERS)) {
        layers = Integer.parseInt(hints.get(EncodeHintType.AZTEC_LAYERS).toString());
      }
    }
    return encode(contents, format, width, height, charset, eccPercent, layers);
  }

  private static BitMatrix encode(String contents, BarcodeFormat format,
                                  int width, int height,
                                  Charset charset, int eccPercent, int layers) {
    if (format != BarcodeFormat.AZTEC) {
      throw new IllegalArgumentException("Can only encode AZTEC, but got " + format);
    }
    AztecCode aztec = Encoder.encode(contents.getBytes(charset), eccPercent, layers);
    return renderResult(aztec, width, height);
  }

  private static BitMatrix renderResult(AztecCode code, int width, int height) {
    BitMatrix input = code.getMatrix();
    if (input == null) {
      throw new IllegalStateException();
    }
    int inputWidth = input.getWidth();
    int inputHeight = input.getHeight();
    int outputWidth = Math.max(width, inputWidth);
    int outputHeight = Math.max(height, inputHeight);

    int multiple = Math.min(outputWidth / inputWidth, outputHeight / inputHeight);
    int leftPadding = (outputWidth - (inputWidth * multiple)) / 2;
    int topPadding = (outputHeight - (inputHeight * multiple)) / 2;

    BitMatrix output = new BitMatrix(outputWidth, outputHeight);

    for (int inputY = 0, outputY = topPadding; inputY < inputHeight; inputY++, outputY += multiple) {
      // Write the contents of this row of the barcode
      for (int inputX = 0, outputX = leftPadding; inputX < inputWidth; inputX++, outputX += multiple) {
        if (input.get(inputX, inputY)) {
          output.setRegion(outputX, outputY, multiple, multiple);
        }
      }
    }
    return output;
  }
}
