/*
 * Copyright 2008 Google Inc.
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

package com.google.zxing.client.result;

import java.io.UnsupportedEncodingException;

/**
 * <p>Superclass for classes encapsulating results in the NDEF format.
 * See <a href="http://www.nfc-forum.org/specs/">http://www.nfc-forum.org/specs/</a>.</p>
 *
 * <p>This code supports a limited subset of NDEF messages, ones that are plausibly
 * useful in 2D barcode formats. This generally includes 1-record messages, no chunking,
 * "short record" syntax, no ID field.</p>
 *
 * @author srowen@google.com (Sean Owen)
 */
abstract class AbstractNDEFParsedResult extends ParsedReaderResult {

  /**
   * MB  = 1 (start of record)
   * ME  = 1 (also end of record)
   * CF  = 0 (not a chunk)
   * SR  = 1 (assume short record)
   * ID  = 0 (ID length field omitted)
   * TNF = 0 (= 1, well-known type)
   *       0
   *       1
   */
  private static final int HEADER_VALUE = 0xD1;
  private static final int MASK = 0xFF;

  AbstractNDEFParsedResult(ParsedReaderResultType type) {
    super(type);
  }

  static boolean isMaybeNDEF(byte[] bytes) {
    return
        bytes != null &&
        bytes.length >= 4 &&
        ((bytes[0] & MASK) == HEADER_VALUE) && 
        ((bytes[1] & 0xFF) == 1);
  }

  static String bytesToString(byte[] bytes, int offset, int length, String encoding) {
    try {
      return new String(bytes, offset, length, encoding);
    } catch (UnsupportedEncodingException uee) {
      throw new RuntimeException("Platform does not support required encoding: " + uee);
    }
  }

}