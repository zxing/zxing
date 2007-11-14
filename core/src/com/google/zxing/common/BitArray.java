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

/**
 * <p>A simple, fast array of bits, represented compactly by an array of ints internally.</p>
 *
 * @author srowen@google.com (Sean Owen)
 */
public final class BitArray {

  private int[] bits;
  private int size;

  public BitArray(int size) {
    this.size = size;
    this.bits = makeArray(size);
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
   * Sets a block of 32 bits, starting at bit i.
   *
   * @param i first bit to set
   * @param newBits the new value of the next 32 bits. Note again that the least-significant bit
   *  corresponds to bit i, the next-least-significant to i+1, and so on.
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
   * @return underlying array of ints. The first element holds the first 32 bits, and the least
   *  significant bit is bit 0.
   */
  public int[] getBitArray() {
    return bits;
  }
  
  /**
   * Reverses all bits in the array.
   */
  public void reverse() {
    int[] newBits = makeArray(size);
    int max = newBits.length;
    for (int i = 0; i < max; i++) {
      newBits[i] = 0;
    }
    int size = this.size;
    for (int i = 0; i < size; i++) {
      if (get(size - i - 1)) {
        newBits[i >> 5] |= 1 << (i & 0x1F);
      }
    }
    bits = newBits;
  }
  
  private static int[] makeArray(int size) {
    int arraySize = size >> 5;
    if ((size & 0x1F) != 0) {
      arraySize++;
    }
    return new int[arraySize];
  }

}