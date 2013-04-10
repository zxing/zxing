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

import java.nio.charset.Charset;
import java.util.Map;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.Writer;
import com.google.zxing.aztec.encoder.AztecCode;
import com.google.zxing.aztec.encoder.Encoder;
import com.google.zxing.common.BitMatrix;

public final class AztecWriter implements Writer {
  
  private static final Charset DEFAULT_CHARSET = Charset.forName("ISO-8859-1");

  @Override
  public BitMatrix encode(String contents, BarcodeFormat format, int width, int height) {
    return encode(contents, format, DEFAULT_CHARSET, Encoder.DEFAULT_EC_PERCENT);
  }

  @Override
  public BitMatrix encode(String contents, BarcodeFormat format, int width, int height, Map<EncodeHintType,?> hints) {
    String charset = (String) hints.get(EncodeHintType.CHARACTER_SET);
    Number eccPercent = (Number) hints.get(EncodeHintType.ERROR_CORRECTION);
    return encode(contents, 
                  format, 
                  charset == null ? DEFAULT_CHARSET : Charset.forName(charset),
                  eccPercent == null ? Encoder.DEFAULT_EC_PERCENT : eccPercent.intValue());
  }

  private static BitMatrix encode(String contents, BarcodeFormat format, Charset charset, int eccPercent) {
    if (format != BarcodeFormat.AZTEC) {
      throw new IllegalArgumentException("Can only encode AZTEC, but got " + format);
    }
    AztecCode aztec = Encoder.encode(contents.getBytes(charset), eccPercent);
    return aztec.getMatrix();
  }

}
