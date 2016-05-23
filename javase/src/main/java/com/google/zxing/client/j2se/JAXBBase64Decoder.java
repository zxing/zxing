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

import java.lang.reflect.InvocationTargetException;

/**
 * Uses {@code javax.xml.bind.DatatypeConverter}, which is not (necessarily) present
 * in Java 9.
 */
final class JAXBBase64Decoder extends Base64Decoder {
  @Override
  byte[] decode(String s) {
    try {
      return (byte[]) Class.forName("javax.xml.bind.DatatypeConverter")
          .getMethod("parseBase64Binary", String.class).invoke(null, s);
    } catch (IllegalAccessException | InvocationTargetException |
             NoSuchMethodException | ClassNotFoundException e) {
      throw new IllegalStateException(e);
    }
  }
}
