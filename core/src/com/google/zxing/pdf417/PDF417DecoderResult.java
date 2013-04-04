/*
 * Copyright 2007 ZXing authors
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

import java.util.List;

import com.google.zxing.common.DecoderResult;

/**
 * Encapsulates the meta data required for Macro PDF417 
 *
 * @author Guenther Grau
 */
public final class PDF417DecoderResult extends DecoderResult {

  private final PDF417ResultMetadata resultMetadata;

  public PDF417DecoderResult(byte[] rawBytes,
                             String text,
                             List<byte[]> byteSegments,
                             String ecLevel,
                             PDF417ResultMetadata resultMetadata) {
    super(rawBytes, text, byteSegments, ecLevel);
    this.resultMetadata = resultMetadata;
  }

  public PDF417ResultMetadata getResultMetadata() {
    return resultMetadata;
  }
}