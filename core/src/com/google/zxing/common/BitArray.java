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

/**
 * <p>A simple, fast array of bits, represented compactly by an array of ints internally.</p>
 *
 * @author Sean Owen
 */
public final class BitArray {

  // TODO: I have changed these members to be public so ProGuard can inline get() and set(). Ideally
  // they'd be private and we'd use the -allowaccessmodification flag, but Dalvik rejects the
  // resulting binary at runtime on Android. If we find a solution to this, these should be changed
  // back to private.
  public int[] bits;
  public int size;

  public BitArray() {
    this.size = 0;
    this.bits = new int[1];
  }

  public BitArray(int size) {
    this.size = size;
    this.bits = makeArray(size);
  }

  public int getSize() {
    return size;
  }

  public int getSizeInBytes() {
    return (size + 7) >> 3;
  }

  private void ensureCapacity(int size) {
    if (size > bits.length << 5) {
      int[] newBits = makeArray(size);
      System.arraycopy(bits, 0, newBits, 0, bits.length);
      this.bits = newBits;
    }
  }

  /**
   * @param i bit to get
   * @return true iff bit i is set
   */
  public boolean get(int i) {
    return (bits[i >> 5] & (1 << (i & 0x1F))) != 0;
  }

  /**
   * Sets bit i.
   *
   * @param i bit to set
   */
  public void set(int i) {
    bits[i >> 5] |= 1 << (i & 0x1F);
  }

  /**
   * Flips bit i.
   *
   * @param i bit to set
   */
  public void flip(int i) {
    bits[i >> 5] ^= 1 << (i & 0x1F);
  }

  /**
   * Sets a block of 32 bits, starting at bit i.
   *
   * @param i first bit to set
   * @param newBits the new value of the next 32 bits. Note again that the least-significant bit
   * corresponds to bit i, the next-least-significant to i+1, and so on.
   */
  public void setBulk(int i, int newBits) {
    bits[i >> 5] = newBits;
  }

  /**
   * Clears all bits (sets to false).
   */
  public void clear() {
    int max = bits.length;
    for (int i = 0; i < max; i++) {
      bits[i] = 0;
    }
  }

  /**
   * Efficient method to check if a range of bits is set, or not set.
   *
   * @param start start of range, inclusive.
   * @param end end of range, exclusive
   * @param value if true, checks that bits in range are set, otherwise checks that they are not set
   * @return true iff all bits are set or not set in range, according to value argument
   * @throws IllegalArgumentException if end is less than or equal to start
   */
  public boolean isRange(int start, int end, boolean value) {
    if (end < start) {
      throw new IllegalArgumentException();
    }
    if (end == start) {
      return true; // empty range matches
    }
    end--; // will be easier to treat this as the last actually set bit -- inclusive    
    int firstInt = start >> 5;
    int lastInt = end >> 5;
    for (int i = firstInt; i <= lastInt; i++) {
      int firstBit = i > firstInt ? 0 : start & 0x1F;
      int lastBit = i < lastInt ? 31 : end & 0x1F;
      int mask;
      if (firstBit == 0 && lastBit == 31) {
        mask = -1;
      } else {
        mask = 0;
        for (int j = firstBit; j <= lastBit; j++) {
          mask |= 1 << j;
        }
      }

      // Return false if we're looking for 1s and the masked bits[i] isn't all 1s (that is,
      // equals the mask, or we're looking for 0s and the masked portion is not all 0s
      if ((bits[i] & mask) != (value ? mask : 0)) {
        return false;
      }
    }
    return true;
  }

  public void appendBit(boolean bit) {
    ensureCapacity(size + 1);
    if (bit) {
      bits[size >> 5] |= (1 << (size & 0x1F));
    }
    size++;
  }

  /**
   * Appends the least-significant bits, from value, in order from most-significant to
   * least-significant. For example, appending 6 bits from 0x000001E will append the bits
   * 0, 1, 1, 1, 1, 0 in that order.
   */
  public void appendBits(int value, int numBits) {
    if (numBits < 0 || numBits > 32) {
      throw new IllegalArgumentException("Num bits must be between 0 and 32");
    }
    ensureCapacity(size + numBits);
    for (int numBitsLeft = numBits; numBitsLeft > 0; numBitsLeft--) {
      appendBit(((value >> (numBitsLeft - 1)) & 0x01) == 1);
    }
  }

  public void appendBitArray(BitArray other) {
    int otherSize = other.getSize();
    ensureCapacity(size + otherSize);
    for (int i = 0; i < otherSize; i++) {
      appendBit(other.get(i));
    }
  }

  public void xor(BitArray other) {
    if (bits.length != other.bits.length) {
      throw new IllegalArgumentException("Sizes don't match");
    }
    for (int i = 0; i < bits.length; i++) {
      // The last byte could be incomplete (i.e. not have 8 bits in
      // it) but there is no problem since 0 XOR 0 == 0.
      bits[i] ^= other.bits[i];
    }
  }

  /**
   *
   * @param bitOffset first bit to start writing
   * @param array array to write into. Bytes are written most-significant byte first. This is the opposite
   *  of the internal representation, which is exposed by {@link #getBitArray()}
   * @param offset position in array to start writing
   * @param numBytes how many bytes to write
   */
  public void toBytes(int bitOffset, byte[] array, int offset, int numBytes) {
    for (int i = 0; i < numBytes; i++) {
      int theByte = 0;
      for (int j = 0; j < 8; j++) {
        if (get(bitOffset)) {
          theByte |= 1 << (7 - j);
        }
        bitOffset++;
      }
      array[offset + i] = (byte) theByte;
    }
  }

  /**
   * @return underlying array of ints. The first element holds the first 32 bits, and the least
   *         significant bit is bit 0.
   */
  public int[] getBitArray() {
    return bits;
  }

  /**
   * Reverses all bits in the array.
   */
  public void reverse() {
    int[] newBits = new int[bits.length];
    int size = this.size;
    for (int i = 0; i < size; i++) {
      if (get(size - i - 1)) {
        newBits[i >> 5] |= 1 << (i & 0x1F);
      }
    }
    bits = newBits;
  }

  private static int[] makeArray(int size) {
    return new int[(size + 31) >> 5];
  }
  
  public String toString() {
    StringBuffer result = new StringBuffer(size);
    for (int i = 0; i < size; i++) {
      if ((i & 0x07) == 0) {
        result.append(' ');
      }
      result.append(get(i) ? 'X' : '.');
    }
    return result.toString();
  }

}