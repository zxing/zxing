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

/**
 * JAVAPORT: This should be combined with BitArray in the future, although that class is not yet
 * dynamically resizeable. This implementation is reasonable but there is a lot of function calling
 * in loops I'd like to get rid of.
 *
 * @author satorux@google.com (Satoru Takabayashi) - creator
 * @author dswitkin@google.com (Daniel Switkin) - ported from C++
 */
public final class BitVector {

  private int sizeInBits;
  private byte[] array;

  // For efficiency, start out with some room to work.
  private static final int DEFAULT_SIZE_IN_BYTES = 32;

  public BitVector() {
    sizeInBits = 0;
    array = new byte[DEFAULT_SIZE_IN_BYTES];
  }

  // Return the bit value at "index".
  public int at(int index) {
    if (index < 0 || index >= sizeInBits) {
      throw new IllegalArgumentException("Bad index: " + index);
    }
    int value = array[index >> 3] & 0xff;
    return (value >> (7 - (index & 0x7))) & 1;
  }

  // Return the number of bits in the bit vector.
  public int size() {
    return sizeInBits;
  }

  // Return the number of bytes in the bit vector.
  public int sizeInBytes() {
    return (sizeInBits + 7) >> 3;
  }

  // Append one bit to the bit vector.
  public void appendBit(int bit) {
    if (!(bit == 0 || bit == 1)) {
      throw new IllegalArgumentException("Bad bit");
    }
    int numBitsInLastByte = sizeInBits & 0x7;
    // We'll expand array if we don't have bits in the last byte.
    if (numBitsInLastByte == 0) {
      appendByte(0);
      sizeInBits -= 8;
    }
    // Modify the last byte.
    array[sizeInBits >> 3] |= (bit << (7 - numBitsInLastByte));
    ++sizeInBits;
  }

  // Append "numBits" bits in "value" to the bit vector.
  // REQUIRES: 0<= numBits <= 32.
  //
  // Examples:
  // - appendBits(0x00, 1) adds 0.
  // - appendBits(0x00, 4) adds 0000.
  // - appendBits(0xff, 8) adds 11111111.
  public void appendBits(int value, int numBits) {
    if (numBits < 0 || numBits > 32) {
      throw new IllegalArgumentException("Num bits must be between 0 and 32");
    }
    int numBitsLeft = numBits;
    while (numBitsLeft > 0) {
      // Optimization for byte-oriented appending.
      if ((sizeInBits & 0x7) == 0 && numBitsLeft >= 8) {
        int newByte = (value >> (numBitsLeft - 8)) & 0xff;
        appendByte(newByte);
        numBitsLeft -= 8;
      } else {
        int bit = (value >> (numBitsLeft - 1)) & 1;
        appendBit(bit);
        --numBitsLeft;
      }
    }
  }

  // Append "bits".
  public void appendBitVector(BitVector bits) {
    int size = bits.size();
    for (int i = 0; i < size; ++i) {
      appendBit(bits.at(i));
    }
  }

  // Modify the bit vector by XOR'ing with "other"
  public void xor(BitVector other) {
    if (sizeInBits != other.size()) {
      throw new IllegalArgumentException("BitVector sizes don't match");
    }
    int sizeInBytes = (sizeInBits + 7) >> 3;
    for (int i = 0; i < sizeInBytes; ++i) {
      // The last byte could be incomplete (i.e. not have 8 bits in
      // it) but there is no problem since 0 XOR 0 == 0.
      array[i] ^= other.array[i];
    }
  }

  // Return String like "01110111" for debugging.
  public String toString() {
    StringBuffer result = new StringBuffer(sizeInBits);
    for (int i = 0; i < sizeInBits; ++i) {
      if (at(i) == 0) {
        result.append('0');
      } else if (at(i) == 1) {
        result.append('1');
      } else {
        throw new IllegalArgumentException("Byte isn't 0 or 1");
      }
    }
    return result.toString();
  }

  // Callers should not assume that array.length is the exact number of bytes needed to hold
  // sizeInBits - it will typically be larger for efficiency.
  public byte[] getArray() {
    return array;
  }

  // Add a new byte to the end, possibly reallocating and doubling the size of the array if we've
  // run out of room.
  private void appendByte(int value) {
    if ((sizeInBits >> 3) == array.length) {
      byte[] newArray = new byte[(array.length << 1)];
      System.arraycopy(array, 0, newArray, 0, array.length);
      array = newArray;
    }
    array[sizeInBits >> 3] = (byte) value;
    sizeInBits += 8;
  }

}
