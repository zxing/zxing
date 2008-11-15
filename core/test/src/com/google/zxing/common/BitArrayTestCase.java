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

package com.google.zxing.common;

import junit.framework.TestCase;

/**
 * @author Sean Owen
 */
public final class BitArrayTestCase extends TestCase {

  public void testGetSet() {
    BitArray array = new BitArray(33);
    for (int i = 0; i < 33; i++) {
      assertFalse(array.get(i));
      array.set(i);
      assertTrue(array.get(i));
    }
  }

  public void testSetBulk() {
    BitArray array = new BitArray(64);
    array.setBulk(32, 0xFFFF0000);
    for (int i = 0; i < 48; i++) {
      assertFalse(array.get(i));
    }
    for (int i = 48; i < 64; i++) {
      assertTrue(array.get(i));
    }
  }

  public void testClear() {
    BitArray array = new BitArray(32);
    for (int i = 0; i < 32; i++) {
      array.set(i);
    }
    array.clear();
    for (int i = 0; i < 32; i++) {
      assertFalse(array.get(i));
    }
  }

  public void testGetArray() {
    BitArray array = new BitArray(64);
    array.set(0);
    array.set(63);
    int[] ints = array.getBitArray();
    assertEquals(1, ints[0]);
    assertEquals(Integer.MIN_VALUE, ints[1]);
  }

  public void testIsRange() {
    BitArray array = new BitArray(64);
    assertTrue(array.isRange(0, 64, false));
    assertFalse(array.isRange(0, 64, true));
    array.set(32);
    assertTrue(array.isRange(32, 33, true));
    array.set(31);
    assertTrue(array.isRange(31, 33, true));
    array.set(34);
    assertFalse(array.isRange(31, 35, true));
    for (int i = 0; i < 31; i++) {
      array.set(i);
    }
    assertTrue(array.isRange(0, 33, true));
    for (int i = 33; i < 64; i++) {
      array.set(i);
    }
    assertTrue(array.isRange(0, 64, true));
    assertFalse(array.isRange(0, 64, false));
  }

}