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

package com.google.zxing.web;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests {@link OutputUtils}.
 */
public final class OutputUtilsTestCase extends Assert {
  
  @Test
  public void testOutput() {
    byte[] array = { 0, 1, -1, 127, -128, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12 };
    assertEquals(
        "00 01 ff 7f 80 02 03 04   05 06 07 08 09 0a 0b 0c\n", 
        OutputUtils.arrayToString(array));
  }
  
}
