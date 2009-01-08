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
namespace com.google.zxing.common
{
    using System;
    using System.Text;

    /// <summary> A class which wraps a 2D array of bytes. The default usage is signed. If you want to use it as a
    /// unsigned container, it's up to you to do byteValue & 0xff at each location.
    /// *
    /// JAVAPORT: I'm not happy about the argument ordering throughout the file, as I always like to have
    /// the horizontal component first, but this is for compatibility with the C++ code. The original
    /// code was a 2D array of ints, but since it only ever gets assigned -1, 0, and 1, I'm going to use
    /// less memory and go with bytes.
    /// *
    /// </summary>
    /// <author>  dswitkin@google.com (Daniel Switkin)
    /// 
    /// </author>
    public sealed class BitArray
    {
      // TODO: I have changed these members to be public so ProGuard can inline get() and set(). Ideally
      // they'd be private and we'd use the -allowaccessmodification flag, but Dalvik rejects the
      // resulting binary at runtime on Android. If we find a solution to this, these should be changed
      // back to private.
      public int[] bits;
      public int Size;

      public BitArray(int size) {
        if (size < 1) {
          throw new Exception("size must be at least 1");
        }
        this.Size = size;
        this.bits = makeArray(size);
      }

      public int getSize() {
        return Size;
      }

      /**
       * @param i bit to get
       * @return true iff bit i is set
       */
      public bool get(int i) {
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
       * corresponds to bit i, the next-least-significant to i+1, and so on.
       */
      public void setBulk(int i, int newBits) {
        bits[i >> 5] = newBits;
      }

      /**
       * Clears all bits (sets to false).
       */
      public void clear() {
        int max = bits.Length;
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
      public bool isRange(int start, int end, bool value) {
        if (end < start) {
          throw new Exception();
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
        int[] newBits = makeArray(Size);
        int max = newBits.Length;
        for (int i = 0; i < max; i++) {
          newBits[i] = 0;
        }
        int size = this.Size;
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
      
      public String toString() {
        StringBuilder  result = new StringBuilder(Size);
        for (int i = 0; i < Size; i++) {
          if (i % 8 == 0) {
            result.Append(' ');
          }
          result.Append(get(i) ? 'X' : '.');
        }
        return result.ToString();
      }
        }
}