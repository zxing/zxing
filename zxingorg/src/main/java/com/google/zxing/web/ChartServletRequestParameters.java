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

package com.google.zxing.web;

import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.nio.charset.Charset;

/**
 * Parameters parsed from request for {@link ChartServlet}.
 *
 * @author Sean Owen
 */
final class ChartServletRequestParameters {

  private final int width;
  private final int height;
  private final Charset outputEncoding;
  private final ErrorCorrectionLevel ecLevel;
  private final int margin;
  private final String text;

  ChartServletRequestParameters(int width,
                                int height,
                                Charset outputEncoding,
                                ErrorCorrectionLevel ecLevel,
                                int margin,
                                String text) {
    this.width = width;
    this.height = height;
    this.outputEncoding = outputEncoding;
    this.ecLevel = ecLevel;
    this.margin = margin;
    this.text = text;
  }

  int getWidth() {
    return width;
  }

  int getHeight() {
    return height;
  }

  Charset getOutputEncoding() {
    return outputEncoding;
  }

  ErrorCorrectionLevel getEcLevel() {
    return ecLevel;
  }

  int getMargin() {
    return margin;
  }

  String getText() {
    return text;
  }

}
