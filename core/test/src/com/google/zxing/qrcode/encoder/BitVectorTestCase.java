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

import junit.framework.TestCase;

/**
 * @author satorux@google.com (Satoru Takabayashi) - creator
 * @author dswitkin@google.com (Daniel Switkin) - ported from C++
 */
public class BitVectorTestCase extends TestCase {

  private static int getUnsignedByte(BitVector v, int index) {
    return v.getArray()[index] & 0xff;
  }

  public void testAppendBit() {
    BitVector v = new BitVector();
    assertEquals(0, v.sizeInBytes());
    // 1
    v.appendBit(1);
    assertEquals(1, v.size());
    assertEquals(0x80, getUnsignedByte(v, 0));
    // 10
    v.appendBit(0);
    assertEquals(2, v.size());
    assertEquals(0x80, getUnsignedByte(v, 0));
    // 101
    v.appendBit(1);
    assertEquals(3, v.size());
    assertEquals(0xa0, getUnsignedByte(v, 0));
    // 1010
    v.appendBit(0);
    assertEquals(4, v.size());
    assertEquals(0xa0, getUnsignedByte(v, 0));
    // 10101
    v.appendBit(1);
    assertEquals(5, v.size());
    assertEquals(0xa8, getUnsignedByte(v, 0));
    // 101010
    v.appendBit(0);
    assertEquals(6, v.size());
    assertEquals(0xa8, getUnsignedByte(v, 0));
    // 1010101
    v.appendBit(1);
    assertEquals(7, v.size());
    assertEquals(0xaa, getUnsignedByte(v, 0));
    // 10101010
    v.appendBit(0);
    assertEquals(8, v.size());
    assertEquals(0xaa, getUnsignedByte(v, 0));
    // 10101010 1
    v.appendBit(1);
    assertEquals(9, v.size());
    assertEquals(0xaa, getUnsignedByte(v, 0));
    assertEquals(0x80, getUnsignedByte(v, 1));
    // 10101010 10
    v.appendBit(0);
    assertEquals(10, v.size());
    assertEquals(0xaa, getUnsignedByte(v, 0));
    assertEquals(0x80, getUnsignedByte(v, 1));
  }

  public void testAppendBits() {
    {
      BitVector v = new BitVector();
      v.appendBits(0x1, 1);
      assertEquals(1, v.size());
      assertEquals(0x80, getUnsignedByte(v, 0));
    }
    {
      BitVector v = new BitVector();
      v.appendBits(0xff, 8);
      assertEquals(8, v.size());
      assertEquals(0xff, getUnsignedByte(v, 0));
    }
    {
      BitVector v = new BitVector();
      v.appendBits(0xff7, 12);
      assertEquals(12, v.size());
      assertEquals(0xff, getUnsignedByte(v, 0));
      assertEquals(0x70, getUnsignedByte(v, 1));
    }
  }

  public void testNumBytes() {
    BitVector v = new BitVector();
    assertEquals(0, v.sizeInBytes());
    v.appendBit(0);
    // 1 bit was added in the vector, so 1 byte should be consumed.
    assertEquals(1, v.sizeInBytes());
    v.appendBits(0, 7);
    assertEquals(1, v.sizeInBytes());
    v.appendBits(0, 8);
    assertEquals(2, v.sizeInBytes());
    v.appendBits(0, 1);
    // We now have 17 bits, so 3 bytes should be consumed.
    assertEquals(3, v.sizeInBytes());
  }

  public void testAppendBitVector() {
    BitVector v1 = new BitVector();
    v1.appendBits(0xbe, 8);
    BitVector v2 = new BitVector();
    v2.appendBits(0xef, 8);
    v1.appendBitVector(v2);
    // beef = 1011 1110 1110 1111
    assertEquals("1011111011101111", v1.toString());
  }

  public void testXOR() {
    {
      BitVector v1 = new BitVector();
      v1.appendBits(0x5555aaaa, 32);
      BitVector v2 = new BitVector();
      v2.appendBits(0xaaaa5555, 32);
      v1.xor(v2);
      assertEquals(0xff, getUnsignedByte(v1, 0));
      assertEquals(0xff, getUnsignedByte(v1, 1));
      assertEquals(0xff, getUnsignedByte(v1, 2));
      assertEquals(0xff, getUnsignedByte(v1, 3));
    }
    {
      BitVector v1 = new BitVector();
      v1.appendBits(0x2a, 7);  // 010 1010
      BitVector v2 = new BitVector();
      v2.appendBits(0x55, 7);  // 101 0101
      v1.xor(v2);
      assertEquals(0xfe, getUnsignedByte(v1, 0));  // 1111 1110
    }
  }

  public void testAt() {
    BitVector v = new BitVector();
    v.appendBits(0xdead, 16);  // 1101 1110 1010 1101
    assertEquals(1, v.at(0));
    assertEquals(1, v.at(1));
    assertEquals(0, v.at(2));
    assertEquals(1, v.at(3));

    assertEquals(1, v.at(4));
    assertEquals(1, v.at(5));
    assertEquals(1, v.at(6));
    assertEquals(0, v.at(7));

    assertEquals(1, v.at(8));
    assertEquals(0, v.at(9));
    assertEquals(1, v.at(10));
    assertEquals(0, v.at(11));

    assertEquals(1, v.at(12));
    assertEquals(1, v.at(13));
    assertEquals(0, v.at(14));
    assertEquals(1, v.at(15));
  }

  public void testToString() {
    BitVector v = new BitVector();
    v.appendBits(0xdead, 16);  // 1101 1110 1010 1101
    assertEquals("1101111010101101", v.toString());
  }

}
