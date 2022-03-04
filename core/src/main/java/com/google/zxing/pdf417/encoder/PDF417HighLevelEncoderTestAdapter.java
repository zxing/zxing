/*
 * Copyright 2022 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.zxing.pdf417.encoder;

import com.google.zxing.WriterException;

import java.nio.charset.Charset;

/**
 * Test adapter for PDF417HighLevelEncoder to be called solely from unit tests.
 */

public final class PDF417HighLevelEncoderTestAdapter {

  private PDF417HighLevelEncoderTestAdapter() {
  }

  public static String encodeHighLevel(String msg, 
                                       Compaction compaction,
                                       Charset encoding,
                                       boolean autoECI) throws WriterException {
    return PDF417HighLevelEncoder.encodeHighLevel(msg, compaction, encoding, autoECI);
  }
}
