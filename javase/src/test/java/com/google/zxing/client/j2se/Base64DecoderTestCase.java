/*
 * Copyright 2017 ZXing authors
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

import org.junit.Assert;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

/**
 * Tests {@link Base64Decoder}.
 */
public final class Base64DecoderTestCase extends Assert {
  
  @Test
  public void testEncode() {
    Base64Decoder decoder = Base64Decoder.getInstance();
    assertArrayEquals("foo".getBytes(StandardCharsets.UTF_8), decoder.decode("Zm9v"));
  }
  
}
