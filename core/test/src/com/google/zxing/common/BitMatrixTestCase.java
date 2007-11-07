/*
 * Copyright 2007 Google Inc.
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

package com.google.zxing.common;

import junit.framework.TestCase;

/**
 * @author srowen@google.com (Sean Owen)
 */
public final class BitMatrixTestCase extends TestCase {

  public void testGetSet() {
    BitMatrix matrix = new BitMatrix(33);
    assertEquals(33, matrix.getDimension());
    for (int i = 0; i < 33; i++) {
      for (int j = 0; j < 33; j++) {
        if (i * j % 3 == 0) {
          matrix.set(i, j);
        }
      }
    }
    for (int i = 0; i < 33; i++) {
      for (int j = 0; j < 33; j++) {
        assertEquals(i * j % 3 == 0, matrix.get(i, j));
      }
    }
  }

  public void testSetRegion() {
    BitMatrix matrix = new BitMatrix(5);
    matrix.setRegion(1, 1, 3, 3);
    for (int i = 0; i < 5; i++) {
      for (int j = 0; j < 5; j++) {
        assertEquals(i >= 1 && i <= 3 && j >= 1 && j <= 3, matrix.get(i, j));
      }
    }
  }

  public void testGetBits() {
    BitMatrix matrix = new BitMatrix(6);
    matrix.set(0, 0);
    matrix.set(5, 5);
    int[] bits = matrix.getBits();
    assertEquals(1, bits[0]);
    assertEquals(8, bits[1]);
  }

}