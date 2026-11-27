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

import java.util.Arrays;

/**
 * <p>A simple, fast array of bits, represented compactly by an array of ints internally.</p>
 *
 * @author Sean Owen
 */
public final class BitArray implements Cloneable {

  private static final int[] EMPTY_BITS = {};
  private static final float LOAD_FACTOR = 0.75f;

  private int[] bits;
  private int size;

  public BitArray() {
    this.size = 0;
    this.bits = EMPTY_BITS;
  }

  public BitArray(int size) {
    this.size = size;
    this.bits = makeArray(size);
  }

  // For testing only
  BitArray(int[] bits, int size) {
    this.bits = bits;
    this.size = size;
  }

  public int getSize() {
    return size;
  }

  public int getSizeInBytes() {
    return (size + 7) / 8;
  }

  private void ensureCapacity(int newSize) {
    if (newSize > bits.length * 32) {
      int[] newBits = makeArray((int) Math.ceil(newSize / LOAD_FACTOR));
      System.arraycopy(bits, 0, newBits, 0, bits.length);
      this.bits = newBits;
    }
  }

  /**
   * @param i bit to get
   * @return true iff bit i is set
   */
  public boolean get(int i) {
    return (bits[i >>> 5] & (1 << (i & 0x1F))) != 0;
  }

  /**
   * Sets bit i.
   *
   * @param i bit to set
   */
  public void set(int i) {
    bits[i >>> 5] |= 1 << (i & 0x1F);
  }

  /**
   * Flips bit i.
   *
   * @param i bit to set
   */
  public void flip(int i) {
    bits[i >>> 5] ^= 1 << (i & 0x1F);
  }

  /**
   * @param from first bit to check
   * @return index of first bit that is set, starting from the given index, or size if none are set
   *  at or beyond this given index
   * @see #getNextUnset(int)
   */
  public int getNextSet(int from) {
    if (from >= size) {
      return size;
    }
    int bitsOffset = from >>> 5;
    int currentBits = bits[bitsOffset];
    // mask off lesser bits first
    currentBits &= -(1 << (from & 0x1F));
    while (currentBits == 0) {
      if (++bitsOffset == bits.length) {
        return size;
      }
      currentBits = bits[bitsOffset];
    }
    int result = (bitsOffset << 5) + Integer.numberOfTrailingZeros(currentBits);
    return Math.min(result, size);
  }

  /**
   * @param from index to start looking for unset bit
   * @return index of next unset bit, or {@code size} if none are unset until the end
   * @see #getNextSet(int)
   */
  public int getNextUnset(int from) {
    if (from >= size) {
      return size;
    }
    int bitsOffset = from >>> 5;
    int currentBits = ~bits[bitsOffset];
    // mask off lesser bits first
    currentBits &= -(1 << (from & 0x1F));
    while (currentBits == 0) {
      if (++bitsOffset == bits.length) {
        return size;
      }
      currentBits = ~bits[bitsOffset];
    }
    int result = (bitsOffset << 5) + Integer.numberOfTrailingZeros(currentBits);
    return Math.min(result, size);
  }

  /**
   * Sets a block of 32 bits, starting at bit i.
   *
   * @param i first bit to set
   * @param newBits the new value of the next 32 bits. Note again that the least-significant bit
   * corresponds to bit i, the next-least-significant to i+1, and so on.
   */
  public void setBulk(int i, int newBits) {
    bits[i >>> 5] = newBits;
  }

  /**
   * Sets a range of bits.
   *
   * @param start start of range, inclusive.
   * @param end end of range, exclusive
   */
  public void setRange(int start, int end) {
    if (end < start || start < 0 || end > size) {
      throw new IllegalArgumentException();
    }
    if (end == start) {
      return;
    }
    end--; // will be easier to treat this as the last actually set bit -- inclusive
    int firstInt = start >>> 5;
    int lastInt = end >>> 5;
    for (int i = firstInt; i <= lastInt; i++) {
      int firstBit = i > firstInt ? 0 : start & 0x1F;
      int lastBit = i < lastInt ? 31 : end & 0x1F;
      // Ones from firstBit to lastBit, inclusive
      int mask = (2 << lastBit) - (1 << firstBit);
      bits[i] |= mask;
    }
  }

  /**
   * Clears all bits (sets to false).
   */
  public void clear() {
    Arrays.fill(bits, 0);
  }

  /**
   * Efficient method to check if a range of bits is set, or not set.
   *
   * @param start start of range, inclusive.
   * @param end end of range, exclusive
   * @param value if true, checks that bits in range are set, otherwise checks that they are not set
   * @return true iff all bits are set or not set in range, according to value argument
   * @throws IllegalArgumentException if end is less than start or the range is not contained in the array
   */
  public boolean isRange(int start, int end, boolean value) {
    if (end < start || start < 0 || end > size) {
      throw new IllegalArgumentException();
    }
    if (end == start) {
      return true; // empty range matches
    }
    end--; // will be easier to treat this as the last actually set bit -- inclusive
    int firstInt = start >>> 5;
    int lastInt = end >>> 5;
    for (int i = firstInt; i <= lastInt; i++) {
      int firstBit = i > firstInt ? 0 : start & 0x1F;
      int lastBit = i < lastInt ? 31 : end & 0x1F;
      // Ones from firstBit to lastBit, inclusive
      int mask = (2 << lastBit) - (1 << firstBit);

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
      bits[size >>> 5] |= 1 << (size & 0x1F);
    }
    size++;
  }

  /**
   * Appends the least-significant bits, from value, in order from most-significant to
   * least-significant. For example, appending 6 bits from 0x000001E will append the bits
   * 0, 1, 1, 1, 1, 0 in that order.
   *
   * @param value {@code int} containing bits to append
   * @param numBits bits from value to append
   */
  public void appendBits(int value, int numBits) {
    if (numBits < 0 || numBits > 32) {
      throw new IllegalArgumentException("Num bits must be between 0 and 32");
    }
    int nextSize = size;
    ensureCapacity(nextSize + numBits);
    for (int numBitsLeft = numBits - 1; numBitsLeft >= 0; numBitsLeft--) {
      if ((value & (1 << numBitsLeft)) != 0) {
        bits[nextSize >>> 5] |= 1 << (nextSize & 0x1F);
      }
      nextSize++;
    }
    size = nextSize;
  }

  public void appendBitArray(BitArray other) {
    int otherSize = other.size;
    ensureCapacity(size + otherSize);
    for (int i = 0; i < otherSize; i++) {
      appendBit(other.get(i));
    }
  }

  public void xor(BitArray other) {
    if (size != other.size) {
      throw new IllegalArgumentException("Sizes don't match");
    }
    for (int i = 0; i < bits.length; i++) {
      // The last int could be incomplete (i.e. not have 32 bits in
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
    if (size == 0) {
      return;
    }
    int oldBitsLen = (size - 1) / 32 + 1;
    int[] bits = this.bits;
    // Reverse word order and bits within each word in place
    for (int i = 0, j = oldBitsLen - 1; i <= j; i++, j--) {
      int left = Integer.reverse(bits[i]);
      int right = Integer.reverse(bits[j]);
      bits[i] = right;
      bits[j] = left;
    }
    // Now correct the ints if the bit size isn't a multiple of 32
    int leftOffset = oldBitsLen * 32 - size;
    if (leftOffset != 0) {
      int rightShift = 32 - leftOffset;
      int currentInt = bits[0] >>> leftOffset;
      for (int i = 1; i < oldBitsLen; i++) {
        int nextInt = bits[i];
        bits[i - 1] = currentInt | (nextInt << rightShift);
        currentInt = nextInt >>> leftOffset;
      }
      bits[oldBitsLen - 1] = currentInt;
    }
    if (oldBitsLen < bits.length) {
      Arrays.fill(bits, oldBitsLen, bits.length, 0);
    }
  }

  private static int[] makeArray(int size) {
    return new int[(size + 31) / 32];
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof BitArray)) {
      return false;
    }
    BitArray other = (BitArray) o;
    return size == other.size && Arrays.equals(bits, other.bits);
  }

  @Override
  public int hashCode() {
    return 31 * size + Arrays.hashCode(bits);
  }

  @Override
  public String toString() {
    StringBuilder result = new StringBuilder(size + (size / 8) + 1);
    for (int i = 0; i < size; i++) {
      if ((i & 0x07) == 0) {
        result.append(' ');
      }
      result.append(get(i) ? 'X' : '.');
    }
    return result.toString();
  }

  @Override
  public BitArray clone() {
    return new BitArray(bits.clone(), size);
  }

}
