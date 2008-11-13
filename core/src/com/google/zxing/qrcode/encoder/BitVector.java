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
 * @author satorux@google.com (Satoru Takabayashi) - creator
 * @author dswitkin@google.com (Daniel Switkin) - ported from C++
 */
public final class BitVector {

  private int size_;
  private String bytes_;

  public BitVector() {
    size_ = 0;
  }

  // Return the bit value at "index".
  public int at(final int index) {
    Debug.DCHECK_LE(0, index);
    Debug.DCHECK_LT(index, size_);
    final uint8 byte = bytes_.at(index / 8);
    return (byte >> (7 - (index % 8))) & 1;
  }

  // Return the number of bits in the bit vector.
  public int size() {
    return size_;
  }

  // Return the number of bytes in the bit vector.
  public int num_bytes() {
    return size_ / 8;
  }

  // Append one bit to the bit vector.
  public void AppendBit(final int bit) {
    Debug.DCHECK(bit == 0 || bit == 1);
    final int num_bits_in_last_byte = size_ % 8;
    // We'll expand bytes_ if we don't have bits in the last byte.
    if (num_bits_in_last_byte == 0) {
      bytes_.push_back(0);
    }
    // Modify the last byte.
    bytes_[bytes_.size() - 1] |= (bit << (7 - num_bits_in_last_byte));
    ++size_;
  }

  // Append "num_bits" bits in "value" to the bit vector.
  // REQUIRES: 0<= num_bits <= 32.
  //
  // Examples:
  // - AppendBits(0x00, 1) adds 0.
  // - AppendBits(0x00, 4) adds 0000.
  // - AppendBits(0xff, 8) adds 11111111.
  public void AppendBits(final uint32 value, final int num_bits) {
    Debug.DCHECK(num_bits >= 0 && num_bits <= 32);
    int num_bits_left = num_bits;
    while (num_bits_left > 0) {
      // Optimization for byte-oriented appending.
      if (size_ % 8 == 0 && num_bits_left >= 8) {
        final uint8 byte = (value >> (num_bits_left - 8)) & 0xff;
        bytes_.push_back(byte);
        size_ += 8;
        num_bits_left -= 8;
      } else {
        final int bit = (value >> (num_bits_left - 1)) & 1;
        AppendBit(bit);
        --num_bits_left;
      }
    }
  }

  // Append "bytes".
  public void AppendBytes(final StringPiece &bytes) {
    for (int i = 0; i < bytes.size(); ++i) {
      AppendBits(bytes[i], 8);
    }
  }

  // Append "bits".
  public void AppendBitVector(final BitVector &bits) {
    for (int i = 0; i < bits.size(); ++i) {
      AppendBit(bits.at(i));
    }
  }

  // Modify the bit vector by XOR'ing with "other"
  public void XOR(final BitVector &other) {
    Debug.DCHECK_EQ(size_, other.size());
    for (int i = 0; i < bytes_.size(); ++i) {
      // The last byte could be incomplete (i.e. not have 8 bits in
      // it) but there is no problem since 0 XOR 0 == 0.
      bytes_[i] ^= other.ToString()[i];
    }
  }

  // Return the content of the bit vector as String.
  public final String &ToString() {
    return bytes_;
  }

  // Return String like "01110111" for debugging.
  public String ToASCII() {
    String result;
    result.reserve(size_);
    for (int i = 0; i < size_; ++i) {
      if (at(i) == 0) {
        result.append("0");
      } else if (at(i) == 1) {
        result.append("1");
      } else {
        Debug.DCHECK(false);
      }
    }
    return result;
  }

}
