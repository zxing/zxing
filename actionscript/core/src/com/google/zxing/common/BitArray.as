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

package com.google.zxing.common
{
/**
 * <p>A simple, fast array of bits, represented compactly by an array of ints internally.</p>
 *
 * @author Sean Owen
 */
    public class BitArray
    {
    	import com.google.zxing.common.flexdatatypes.StringBuilder;
    	import com.google.zxing.common.flexdatatypes.IllegalArgumentException;
    	
  // TODO: I have changed these members to be public so ProGuard can inline get() and set(). Ideally
  // they'd be private and we'd use the -allowaccessmodification flag, but Dalvik rejects the
  // resulting binary at runtime on Android. If we find a solution to this, these should be changed
  // back to private.
      public var bits:Array;
      public var Size:int;

      public function BitArray(size:int) {
        if (size < 1) {
          throw new IllegalArgumentException("common : BitArray : size must be at least 1");
        }
        this.Size = size;
        this.bits = makeArray(size);
      }

      public function getSize():int {
        return Size;
      }

      /**
       * @param i bit to get
       * @return true iff bit i is set
       */
      public function _get(i:int):Boolean {
        return (bits[i >> 5] & (1 << (i & 0x1F))) != 0;
      }

      /**
       * Sets bit i.
       *
       * @param i bit to set
       */
      public function _set(i:int):void {
        bits[i >> 5] |= 1 << (i & 0x1F);
      }

	  /**
	   * Flips bit i.
	   *
	   * @param i bit to set
	   */
	  public function flip(i:int):void {
	    bits[i >> 5] ^= 1 << (i & 0x1F);
	  }
      /**
       * Sets a block of 32 bits, starting at bit i.
       *
       * @param i first bit to set
       * @param newBits the new value of the next 32 bits. Note again that the least-significant bit
       * corresponds to bit i, the next-least-significant to i+1, and so on.
       */
      public function setBulk(i:int, newBits:int):void {
        bits[i >> 5] = newBits;
      }

      /**
       * Clears all bits (sets to false).
       */
      public function clear():void {
        var max:int = bits.length;
        for (var i:int = 0; i < max; i++) {
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
      public function isRange(start:int, end:int, value:Boolean):Boolean {
        if (end < start) {
          throw new IllegalArgumentException("common : BitArray isRange : end before start");
        }
        if (end == start) {
          return true; // empty range matches
        }
        end--; // will be easier to treat this as the last actually set bit -- inclusive    
        var firstInt:int = start >> 5;
        var lastInt:int = end >> 5;
        for (var i:int = firstInt; i <= lastInt; i++) {
          var firstBit:int = i > firstInt ? 0 : start & 0x1F;
          var lastBit:int = i < lastInt ? 31 : end & 0x1F;
          var mask:int;
          if (firstBit == 0 && lastBit == 31) {
            mask = -1;
          } else {
            mask = 0;
            for (var j:int = firstBit; j <= lastBit; j++) {
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
      public function getBitArray():Array {
        return bits;
      }

		// bas : for debugging purposes
      public function setBitArray(a:Array):void {
        bits = a;
      }
     public function setSize(siz:int):void {
        Size = siz;
      }

      /**
       * Reverses all bits in the array.
       */
      public function reverse():void {
        var newBits:Array = makeArray(Size);
        var max:int = Size;
        for (var i:int = 0; i < max; i++) { newBits[i] = 0; }//Flex : makew 
        var size:int = this.Size;
        for (var ii:int = 0; ii < size; ii++) {
          if (this._get(size - ii - 1)) {
            newBits[ii >> 5] |= 1 << (ii & 0x1F);
          }
        }
        bits = newBits;
      }

      private static function makeArray(size:int):Array {
        var arraySize:int = size >> 5;
        if ((size & 0x1F) != 0) {
          arraySize++;
        }
        return new Array(arraySize);
      }
      public function toString():String 
      {
    	var result:StringBuilder = new StringBuilder(this.Size);
    	for (var i:int = 0; i < this.Size; i++) 
    	{
      		if ((i & 0x07) == 0) 
      		{
        		result.Append(' ');
      		}
      		result.Append(_get(i) ? 'X' : '.');
    	}
    	return result.ToString();
  	}
}
}