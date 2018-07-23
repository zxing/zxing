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

import java.util.Deque;
import java.util.LinkedList;

import com.google.zxing.common.BitArray;

/**
 * State represents all information about a sequence necessary to generate the current output.
 * Note that a state is immutable.
 */
final class State {

  static final State INITIAL_STATE = new State(Token.EMPTY, HighLevelEncoder.MODE_UPPER, 0, 0);

  // The current mode of the encoding (or the mode to which we'll return if
  // we're in Binary Shift mode.
  private final int mode;
  // The list of tokens that we output.  If we are in Binary Shift mode, this
  // token list does *not* yet included the token for those bytes
  private final Token token;
  // If non-zero, the number of most recent bytes that should be output
  // in Binary Shift mode.
  private final int binaryShiftByteCount;
  // The total number of bits generated (including Binary Shift).
  private final int bitCount;

  private State(Token token, int mode, int binaryBytes, int bitCount) {
    this.token = token;
    this.mode = mode;
    this.binaryShiftByteCount = binaryBytes;
    this.bitCount = bitCount;
    // Make sure we match the token
    //int binaryShiftBitCount = (binaryShiftByteCount * 8) +
    //    (binaryShiftByteCount == 0 ? 0 :
    //     binaryShiftByteCount <= 31 ? 10 :
    //     binaryShiftByteCount <= 62 ? 20 : 21);
    //assert this.bitCount == token.getTotalBitCount() + binaryShiftBitCount;
  }

  int getMode() {
    return mode;
  }

  Token getToken() {
    return token;
  }

  int getBinaryShiftByteCount() {
    return binaryShiftByteCount;
  }

  int getBitCount() {
    return bitCount;
  }

  // Create a new state representing this state with a latch to a (not
  // necessary different) mode, and then a code.
  State latchAndAppend(int mode, int value) {
    //assert binaryShiftByteCount == 0;
    int bitCount = this.bitCount;
    Token token = this.token;
    if (mode != this.mode) {
      int latch = HighLevelEncoder.LATCH_TABLE[this.mode][mode];
      token = token.add(latch & 0xFFFF, latch >> 16);
      bitCount += latch >> 16;
    }
    int latchModeBitCount = mode == HighLevelEncoder.MODE_DIGIT ? 4 : 5;
    token = token.add(value, latchModeBitCount);
    return new State(token, mode, 0, bitCount + latchModeBitCount);
  }

  // Create a new state representing this state, with a temporary shift
  // to a different mode to output a single value.
  State shiftAndAppend(int mode, int value) {
    //assert binaryShiftByteCount == 0 && this.mode != mode;
    Token token = this.token;
    int thisModeBitCount = this.mode == HighLevelEncoder.MODE_DIGIT ? 4 : 5;
    // Shifts exist only to UPPER and PUNCT, both with tokens size 5.
    token = token.add(HighLevelEncoder.SHIFT_TABLE[this.mode][mode], thisModeBitCount);
    token = token.add(value, 5);
    return new State(token, this.mode, 0, this.bitCount + thisModeBitCount + 5);
  }

  // Create a new state representing this state, but an additional character
  // output in Binary Shift mode.
  State addBinaryShiftChar(int index) {
    Token token = this.token;
    int mode = this.mode;
    int bitCount = this.bitCount;
    if (this.mode == HighLevelEncoder.MODE_PUNCT || this.mode == HighLevelEncoder.MODE_DIGIT) {
      //assert binaryShiftByteCount == 0;
      int latch = HighLevelEncoder.LATCH_TABLE[mode][HighLevelEncoder.MODE_UPPER];
      token = token.add(latch & 0xFFFF, latch >> 16);
      bitCount += latch >> 16;
      mode = HighLevelEncoder.MODE_UPPER;
    }
    int deltaBitCount =
      (binaryShiftByteCount == 0 || binaryShiftByteCount == 31) ? 18 :
      (binaryShiftByteCount == 62) ? 9 : 8;
    State result = new State(token, mode, binaryShiftByteCount + 1, bitCount + deltaBitCount);
    if (result.binaryShiftByteCount == 2047 + 31) {
      // The string is as long as it's allowed to be.  We should end it.
      result = result.endBinaryShift(index + 1);
    }
    return result;
  }

  // Create the state identical to this one, but we are no longer in
  // Binary Shift mode.
  State endBinaryShift(int index) {
    if (binaryShiftByteCount == 0) {
      return this;
    }
    Token token = this.token;
    token = token.addBinaryShift(index - binaryShiftByteCount, binaryShiftByteCount);
    //assert token.getTotalBitCount() == this.bitCount;
    return new State(token, mode, 0, this.bitCount);
  }

  // Returns true if "this" state is better (or equal) to be in than "that"
  // state under all possible circumstances.
  boolean isBetterThanOrEqualTo(State other) {
    int newModeBitCount = this.bitCount + (HighLevelEncoder.LATCH_TABLE[this.mode][other.mode] >> 16);
    if (this.binaryShiftByteCount < other.binaryShiftByteCount) {
      // add additional B/S encoding cost of other, if any
      newModeBitCount += calculateBinaryShiftCost(other) - calculateBinaryShiftCost(this);
    } else if (this.binaryShiftByteCount > other.binaryShiftByteCount && other.binaryShiftByteCount > 0) {
      // maximum possible additional cost (we end up exceeding the 31 byte boundary and other state can stay beneath it)
      newModeBitCount += 10; 
    }
    return newModeBitCount <= other.bitCount;
  }

  BitArray toBitArray(byte[] text) {
    // Reverse the tokens, so that they are in the order that they should
    // be output
    Deque<Token> symbols = new LinkedList<>();
    for (Token token = endBinaryShift(text.length).token; token != null; token = token.getPrevious()) {
      symbols.addFirst(token);
    }
    BitArray bitArray = new BitArray();
    // Add each token to the result.
    for (Token symbol : symbols) {
      symbol.appendTo(bitArray, text);
    }
    //assert bitArray.getSize() == this.bitCount;
    return bitArray;
  }

  @Override
  public String toString() {
    return String.format("%s bits=%d bytes=%d", HighLevelEncoder.MODE_NAMES[mode], bitCount, binaryShiftByteCount);
  }
  
  private static int calculateBinaryShiftCost(State state) {
    if (state.binaryShiftByteCount > 62) {
      return 21; // B/S with extended length
    }
    if (state.binaryShiftByteCount > 31) {
      return 20; // two B/S
    }
    if (state.binaryShiftByteCount > 0) {
      return 10; // one B/S
    }
    return 0;
  }

}
