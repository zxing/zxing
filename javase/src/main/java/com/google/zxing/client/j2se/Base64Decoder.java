/*
 * Copyright 2016 ZXing authors
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

package com.google.zxing.client.j2se;

/**
 * Abstraction over Base 64 decoding implementations that work across Java versions.
 */
abstract class Base64Decoder {

  private static final Base64Decoder INSTANCE;
  static {
    Base64Decoder instance;
    try {
      Class.forName("java.util.Base64");
      // If succeeds, then:
      instance = new Java8Base64Decoder();
    } catch (ClassNotFoundException cnfe) {
      instance = new JAXBBase64Decoder();
    }
    INSTANCE = instance;
  }

  /**
   * @param s Base-64 encoded string
   * @return bytes that the string encodes
   */
  abstract byte[] decode(String s);

  static Base64Decoder getInstance() {
    return INSTANCE;
  }

}
