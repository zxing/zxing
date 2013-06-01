/*
 * Copyright 2013 ZXing authors
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

package com.google.zxing.aztec.encoder;

import com.google.zxing.common.BitArray;

abstract class Token {
  
  static final Token EMPTY = new SimpleToken(null, 0, 0, 0);
  
  private final Token previous;
  private final int totalBitCount; // For debugging purposes, only
  
  Token(Token previous, int totalBitCount) {
    this.previous = previous;
    this.totalBitCount = totalBitCount;
  }
  
  final Token getPrevious() {
    return previous;
  }

  final int getTotalBitCount() {
    return totalBitCount;
  }

  final Token add(int value, int bitCount) {
    return new SimpleToken(this, this.totalBitCount + bitCount, value, bitCount);
   }

  final Token addBinaryShift(int start, int byteCount) {
    int bitCount = (byteCount * 8) + (byteCount <= 31 ? 10 : byteCount <= 62 ? 20 : 21);
    return new BinaryShiftToken(this, this.totalBitCount + bitCount, start, byteCount);
  }

  abstract void appendTo(BitArray bitArray, byte[] text);

}
