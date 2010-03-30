/*
 * Copyright 2008 ZXing authors
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

package com.google.zxing.qrcode.encoder;

import com.google.zxing.common.BitArray;
import junit.framework.TestCase;

/**
 * @author satorux@google.com (Satoru Takabayashi) - creator
 * @author dswitkin@google.com (Daniel Switkin) - ported from C++
 */
public class BitVectorTestCase extends TestCase {

  private static long getUnsignedInt(BitArray v, int index) {
    long result = 0L;
    for (int i = 0, offset = index << 3; i < 32; i++) {
      if (v.get(offset + i)) {
        result |= 1L << (31 - i);
      }
    }
    return result;
  }

  public void testAppendBit() {
    BitArray v = new BitArray();
    assertEquals(0, v.getSizeInBytes());
    // 1
    v.appendBit(true);
    assertEquals(1, v.getSize());
    assertEquals(0x80000000L, getUnsignedInt(v, 0));
    // 10
    v.appendBit(false);
    assertEquals(2, v.getSize());
    assertEquals(0x80000000L, getUnsignedInt(v, 0));
    // 101
    v.appendBit(true);
    assertEquals(3, v.getSize());
    assertEquals(0xa0000000L, getUnsignedInt(v, 0));
    // 1010
    v.appendBit(false);
    assertEquals(4, v.getSize());
    assertEquals(0xa0000000L, getUnsignedInt(v, 0));
    // 10101
    v.appendBit(true);
    assertEquals(5, v.getSize());
    assertEquals(0xa8000000L, getUnsignedInt(v, 0));
    // 101010
    v.appendBit(false);
    assertEquals(6, v.getSize());
    assertEquals(0xa8000000L, getUnsignedInt(v, 0));
    // 1010101
    v.appendBit(true);
    assertEquals(7, v.getSize());
    assertEquals(0xaa000000L, getUnsignedInt(v, 0));
    // 10101010
    v.appendBit(false);
    assertEquals(8, v.getSize());
    assertEquals(0xaa000000L, getUnsignedInt(v, 0));
    // 10101010 1
    v.appendBit(true);
    assertEquals(9, v.getSize());
    assertEquals(0xaa800000L, getUnsignedInt(v, 0));
    // 10101010 10
    v.appendBit(false);
    assertEquals(10, v.getSize());
    assertEquals(0xaa800000L, getUnsignedInt(v, 0));
  }

  public void testAppendBits() {
    {
      BitArray v = new BitArray();
      v.appendBits(0x1, 1);
      assertEquals(1, v.getSize());
      assertEquals(0x80000000L, getUnsignedInt(v, 0));
    }
    {
      BitArray v = new BitArray();
      v.appendBits(0xff, 8);
      assertEquals(8, v.getSize());
      assertEquals(0xff000000L, getUnsignedInt(v, 0));
    }
    {
      BitArray v = new BitArray();
      v.appendBits(0xff7, 12);
      assertEquals(12, v.getSize());
      assertEquals(0xff700000L, getUnsignedInt(v, 0));
    }
  }

  public void testNumBytes() {
    BitArray v = new BitArray();
    assertEquals(0, v.getSizeInBytes());
    v.appendBit(false);
    // 1 bit was added in the vector, so 1 byte should be consumed.
    assertEquals(1, v.getSizeInBytes());
    v.appendBits(0, 7);
    assertEquals(1, v.getSizeInBytes());
    v.appendBits(0, 8);
    assertEquals(2, v.getSizeInBytes());
    v.appendBits(0, 1);
    // We now have 17 bits, so 3 bytes should be consumed.
    assertEquals(3, v.getSizeInBytes());
  }

  public void testAppendBitVector() {
    BitArray v1 = new BitArray();
    v1.appendBits(0xbe, 8);
    BitArray v2 = new BitArray();
    v2.appendBits(0xef, 8);
    v1.appendBitArray(v2);
    // beef = 1011 1110 1110 1111
    assertEquals(" X.XXXXX. XXX.XXXX", v1.toString());
  }

  public void testXOR() {
    {
      BitArray v1 = new BitArray();
      v1.appendBits(0x5555aaaa, 32);
      BitArray v2 = new BitArray();
      v2.appendBits(0xaaaa5555, 32);
      v1.xor(v2);
      assertEquals(0xffffffffL, getUnsignedInt(v1, 0));
    }
    {
      BitArray v1 = new BitArray();
      v1.appendBits(0x2a, 7);  // 010 1010
      BitArray v2 = new BitArray();
      v2.appendBits(0x55, 7);  // 101 0101
      v1.xor(v2);
      assertEquals(0xfe000000L, getUnsignedInt(v1, 0));  // 1111 1110
    }
  }

  public void testAt() {
    BitArray v = new BitArray();
    v.appendBits(0xdead, 16);  // 1101 1110 1010 1101
    assertTrue(v.get(0));
    assertTrue(v.get(1));
    assertFalse(v.get(2));
    assertTrue(v.get(3));

    assertTrue(v.get(4));
    assertTrue(v.get(5));
    assertTrue(v.get(6));
    assertFalse(v.get(7));

    assertTrue(v.get(8));
    assertFalse(v.get(9));
    assertTrue(v.get(10));
    assertFalse(v.get(11));

    assertTrue(v.get(12));
    assertTrue(v.get(13));
    assertFalse(v.get(14));
    assertTrue(v.get(15));
  }

  public void testToString() {
    BitArray v = new BitArray();
    v.appendBits(0xdead, 16);  // 1101 1110 1010 1101
    assertEquals(" XX.XXXX. X.X.XX.X", v.toString());
  }

}
