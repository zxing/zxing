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

package com.google.zxing.aztec.encoder;

import java.nio.charset.Charset;
import java.util.Map;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.Writer;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

public final class AztecWriter implements Writer {
  
  private static final Charset LATIN_1 = Charset.forName("ISO-8859-1");

  @Override
  public BitMatrix encode(String contents, BarcodeFormat format, int width, int height) throws WriterException {
    AztecCode aztec = Encoder.encode(contents.getBytes(LATIN_1), 30);
    return aztec.getMatrix();
  }

  @Override
  public BitMatrix encode(String contents, BarcodeFormat format, int width, int height, Map<EncodeHintType, ?> hints)
      throws WriterException {
    return encode(contents, format, width, height);
  }

}
