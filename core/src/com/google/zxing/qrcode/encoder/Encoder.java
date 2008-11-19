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

import com.google.zxing.common.ByteMatrix;
import com.google.zxing.common.ByteArray;
import com.google.zxing.common.reedsolomon.GF256;
import com.google.zxing.common.reedsolomon.ReedSolomonEncoder;

import java.util.Vector;

/**
 * @author satorux@google.com (Satoru Takabayashi) - creator
 * @author dswitkin@google.com (Daniel Switkin) - ported from C++
 */
public final class Encoder {

  // The original table is defined in the table 5 of JISX0510:2004 (p.19).
  private static final int kAlphanumericTable[] = {
      -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,  // 0x00-0x0f
      -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,  // 0x10-0x1f
      36, -1, -1, -1, 37, 38, -1, -1, -1, -1, 39, 40, -1, 41, 42, 43,  // 0x20-0x2f
      0,   1,  2,  3,  4,  5,  6,  7,  8,  9, 44, -1, -1, -1, -1, -1,  // 0x30-0x3f
      -1, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24,  // 0x40-0x4f
      25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, -1, -1, -1, -1, -1,  // 0x50-0x5f
  };

  private static final class RSBlockInfo {

    int num_bytes;
    int block_info[][];

    public RSBlockInfo(int num_bytes, int[][] block_info) {
      this.num_bytes = num_bytes;
      this.block_info = block_info;
    }

  }

  // The table is from table 12 of JISX0510:2004 (p. 30). The "block_info" parts are ordered by
  // L, M, Q, H. Within each block_info, the 0th element is num_ec_bytes, and the 1st element is
  // num_rs_blocks. The table was doublechecked by komatsu.
  private static final RSBlockInfo kRSBlockTable[] = {
      new RSBlockInfo(  26, new int[][]{ {  7,  1}, {  10,  1}, {  13,  1}, {  17,  1}}),  // Version  1
      new RSBlockInfo(  44, new int[][]{ { 10,  1}, {  16,  1}, {  22,  1}, {  28,  1}}),  // Version  2
      new RSBlockInfo(  70, new int[][]{ { 15,  1}, {  26,  1}, {  36,  2}, {  44,  2}}),  // Version  3
      new RSBlockInfo( 100, new int[][]{ { 20,  1}, {  36,  2}, {  52,  2}, {  64,  4}}),  // Version  4
      new RSBlockInfo( 134, new int[][]{ { 26,  1}, {  48,  2}, {  72,  4}, {  88,  4}}),  // Version  5
      new RSBlockInfo( 172, new int[][]{ { 36,  2}, {  64,  4}, {  96,  4}, { 112,  4}}),  // Version  6
      new RSBlockInfo( 196, new int[][]{ { 40,  2}, {  72,  4}, { 108,  6}, { 130,  5}}),  // Version  7
      new RSBlockInfo( 242, new int[][]{ { 48,  2}, {  88,  4}, { 132,  6}, { 156,  6}}),  // Version  8
      new RSBlockInfo( 292, new int[][]{ { 60,  2}, { 110,  5}, { 160,  8}, { 192,  8}}),  // Version  9
      new RSBlockInfo( 346, new int[][]{ { 72,  4}, { 130,  5}, { 192,  8}, { 224,  8}}),  // Version 10
      new RSBlockInfo( 404, new int[][]{ { 80,  4}, { 150,  5}, { 224,  8}, { 264, 11}}),  // Version 11
      new RSBlockInfo( 466, new int[][]{ { 96,  4}, { 176,  8}, { 260, 10}, { 308, 11}}),  // Version 12
      new RSBlockInfo( 532, new int[][]{ {104,  4}, { 198,  9}, { 288, 12}, { 352, 16}}),  // Version 13
      new RSBlockInfo( 581, new int[][]{ {120,  4}, { 216,  9}, { 320, 16}, { 384, 16}}),  // Version 14
      new RSBlockInfo( 655, new int[][]{ {132,  6}, { 240, 10}, { 360, 12}, { 432, 18}}),  // Version 15
      new RSBlockInfo( 733, new int[][]{ {144,  6}, { 280, 10}, { 408, 17}, { 480, 16}}),  // Version 16
      new RSBlockInfo( 815, new int[][]{ {168,  6}, { 308, 11}, { 448, 16}, { 532, 19}}),  // Version 17
      new RSBlockInfo( 901, new int[][]{ {180,  6}, { 338, 13}, { 504, 18}, { 588, 21}}),  // Version 18
      new RSBlockInfo( 991, new int[][]{ {196,  7}, { 364, 14}, { 546, 21}, { 650, 25}}),  // Version 19
      new RSBlockInfo(1085, new int[][]{ {224,  8}, { 416, 16}, { 600, 20}, { 700, 25}}),  // Version 20
      new RSBlockInfo(1156, new int[][]{ {224,  8}, { 442, 17}, { 644, 23}, { 750, 25}}),  // Version 21
      new RSBlockInfo(1258, new int[][]{ {252,  9}, { 476, 17}, { 690, 23}, { 816, 34}}),  // Version 22
      new RSBlockInfo(1364, new int[][]{ {270,  9}, { 504, 18}, { 750, 25}, { 900, 30}}),  // Version 23
      new RSBlockInfo(1474, new int[][]{ {300, 10}, { 560, 20}, { 810, 27}, { 960, 32}}),  // Version 24
      new RSBlockInfo(1588, new int[][]{ {312, 12}, { 588, 21}, { 870, 29}, {1050, 35}}),  // Version 25
      new RSBlockInfo(1706, new int[][]{ {336, 12}, { 644, 23}, { 952, 34}, {1110, 37}}),  // Version 26
      new RSBlockInfo(1828, new int[][]{ {360, 12}, { 700, 25}, {1020, 34}, {1200, 40}}),  // Version 27
      new RSBlockInfo(1921, new int[][]{ {390, 13}, { 728, 26}, {1050, 35}, {1260, 42}}),  // Version 28
      new RSBlockInfo(2051, new int[][]{ {420, 14}, { 784, 28}, {1140, 38}, {1350, 45}}),  // Version 29
      new RSBlockInfo(2185, new int[][]{ {450, 15}, { 812, 29}, {1200, 40}, {1440, 48}}),  // Version 30
      new RSBlockInfo(2323, new int[][]{ {480, 16}, { 868, 31}, {1290, 43}, {1530, 51}}),  // Version 31
      new RSBlockInfo(2465, new int[][]{ {510, 17}, { 924, 33}, {1350, 45}, {1620, 54}}),  // Version 32
      new RSBlockInfo(2611, new int[][]{ {540, 18}, { 980, 35}, {1440, 48}, {1710, 57}}),  // Version 33
      new RSBlockInfo(2761, new int[][]{ {570, 19}, {1036, 37}, {1530, 51}, {1800, 60}}),  // Version 34
      new RSBlockInfo(2876, new int[][]{ {570, 19}, {1064, 38}, {1590, 53}, {1890, 63}}),  // Version 35
      new RSBlockInfo(3034, new int[][]{ {600, 20}, {1120, 40}, {1680, 56}, {1980, 66}}),  // Version 36
      new RSBlockInfo(3196, new int[][]{ {630, 21}, {1204, 43}, {1770, 59}, {2100, 70}}),  // Version 37
      new RSBlockInfo(3362, new int[][]{ {660, 22}, {1260, 45}, {1860, 62}, {2220, 74}}),  // Version 38
      new RSBlockInfo(3532, new int[][]{ {720, 24}, {1316, 47}, {1950, 65}, {2310, 77}}),  // Version 39
      new RSBlockInfo(3706, new int[][]{ {750, 25}, {1372, 49}, {2040, 68}, {2430, 81}}),  // Version 40
  };

  private static final class BlockPair {

    private ByteArray dataBytes;
    private ByteArray errorCorrectionBytes;

    public BlockPair(ByteArray data, ByteArray errorCorrection) {
      dataBytes = data;
      errorCorrectionBytes = errorCorrection;
    }

    public ByteArray getDataBytes() {
      return dataBytes;
    }

    public ByteArray getErrorCorrectionBytes() {
      return errorCorrectionBytes;
    }

  }

  // Encode "bytes" with the error correction level "ec_level". The encoding mode will be chosen
  // internally by ChooseMode(). On success, store the result in "qr_code" and return true. On
  // error, return false. We recommend you to use QRCode.EC_LEVEL_L (the lowest level) for
  // "ec_level" since our primary use is to show QR code on desktop screens. We don't need very
  // strong error correction for this purpose.
  //
  // Note that there is no way to encode bytes in MODE_KANJI. We might want to add EncodeWithMode()
  // with which clients can specify the encoding mode. For now, we don't need the functionality.
  public static boolean Encode(final ByteArray bytes, int ec_level, QRCode qr_code) {
    // Step 1: Choose the mode (encoding).
    final int mode = ChooseMode(bytes);

    // Step 2: Append "bytes" into "data_bits" in appropriate encoding.
    BitVector data_bits = new BitVector();
    if (!AppendBytes(bytes, mode, data_bits)) {
      return false;
    }
    // Step 3: Initialize QR code that can contain "data_bits".
    final int num_input_bytes = data_bits.num_bytes();
    if (!InitQRCode(num_input_bytes, ec_level, mode, qr_code)) {
      return false;
    }

    // Step 4: Build another bit vector that contains header and data.
    BitVector header_and_data_bits = new BitVector();
    if (!AppendModeInfo(qr_code.mode(), header_and_data_bits)) {
      return false;
    }
    if (!AppendLengthInfo(bytes.size(), qr_code.version(), qr_code.mode(), header_and_data_bits)) {
      return false;
    }
    header_and_data_bits.AppendBitVector(data_bits);

    // Step 5: Terminate the bits properly.
    if (!TerminateBits(qr_code.num_data_bytes(), header_and_data_bits)) {
      return false;
    }

    // Step 6: Interleave data bits with error correction code.
    BitVector final_bits = new BitVector();
    InterleaveWithECBytes(header_and_data_bits, qr_code.num_total_bytes(), qr_code.num_data_bytes(),
        qr_code.num_rs_blocks(), final_bits);

    // Step 7: Choose the mask pattern and set to "qr_code".
    ByteMatrix matrix = new ByteMatrix(qr_code.matrix_width(), qr_code.matrix_width());
    qr_code.set_mask_pattern(ChooseMaskPattern(final_bits, qr_code.ec_level(), qr_code.version(),
        matrix));
    if (qr_code.mask_pattern() == -1) {
      // There was an error.
      return false;
    }

    // Step 8.  Build the matrix and set it to "qr_code".
    MatrixUtil.BuildMatrix(final_bits, qr_code.ec_level(), qr_code.version(),
        qr_code.mask_pattern(), matrix);
    qr_code.set_matrix(matrix);
    // Step 9.  Make sure we have a valid QR Code.
    if (!qr_code.IsValid()) {
      Debug.LOG_ERROR("Invalid QR code: " + qr_code.toString());
      return false;
    }
    return true;
  }

  // Return the code point of the table used in alphanumeric mode. Return -1 if there is no
  // corresponding code in the table.
  static int GetAlphanumericCode(int code) {
    if (code < kAlphanumericTable.length) {
      return kAlphanumericTable[code];
    }
    return -1;
  }

  // Choose the best mode by examining the content of "bytes". The function is guaranteed to return
  // a valid mode.
  //
  // Note that this function does not return MODE_KANJI, as we cannot distinguish Shift_JIS from
  // other encodings such as ISO-8859-1, from data bytes alone. For example "\xE0\xE0" can be
  // interpreted as one character in Shift_JIS, but also two characters in ISO-8859-1.
  //
  // JAVAPORT: This MODE_KANJI limitation sounds like a problem for us.
  public static int ChooseMode(final ByteArray bytes) {
    boolean has_numeric = false;
    boolean has_alphanumeric = false;
    boolean has_other = false;
    for (int i = 0; i < bytes.size(); ++i) {
      final int oneByte = bytes.at(i);
      if (oneByte >= '0' && oneByte <= '9') {
        has_numeric = true;
      } else if (GetAlphanumericCode(oneByte) != -1) {
        has_alphanumeric = true;
      } else {
        has_other = true;
      }
    }
    if (has_other) {
      return QRCode.MODE_8BIT_BYTE;
    } else if (has_alphanumeric) {
      return QRCode.MODE_ALPHANUMERIC;
    } else if (has_numeric) {
      return QRCode.MODE_NUMERIC;
    }
    // "bytes" must be empty to reach here.
    Debug.DCHECK(bytes.empty());
    return QRCode.MODE_8BIT_BYTE;
  }

  private static int ChooseMaskPattern(final BitVector bits, int ec_level, int version,
      ByteMatrix matrix) {
    if (!QRCode.IsValidMatrixWidth(matrix.width())) {
      Debug.LOG_ERROR("Invalid matrix width: " + matrix.width());
      return -1;
    }

    int min_penalty = Integer.MAX_VALUE;  // Lower penalty is better.
    int best_mask_pattern = -1;
    // We try all mask patterns to choose the best one.
    for (int i = 0; i < QRCode.kNumMaskPatterns; ++i) {
      final int mask_pattern = i;
      if (!MatrixUtil.BuildMatrix(bits, ec_level, version,
          mask_pattern, matrix)) {
        return -1;
      }
      final int penalty = MaskUtil.CalculateMaskPenalty(matrix);
      Debug.LOG_INFO("mask_pattern: " + mask_pattern + ", " + "penalty: " + penalty);
      if (penalty < min_penalty) {
        min_penalty = penalty;
        best_mask_pattern = mask_pattern;
      }
    }
    return best_mask_pattern;
  }

  // Initialize "qr_code" according to "num_input_bytes", "ec_level", and "mode". On success, modify
  // "qr_code" and return true. On error, return false.
  private static boolean InitQRCode(int num_input_bytes, int ec_level, int mode, QRCode qr_code) {
    qr_code.set_ec_level(ec_level);
    qr_code.set_mode(mode);

    if (!QRCode.IsValidECLevel(ec_level)) {
      Debug.LOG_ERROR("Invalid EC level: " + ec_level);
      return false;
    }

    // In the following comments, we use numbers of Version 7-H.
    for (int i = 0; i < kRSBlockTable.length; ++i) {
      final RSBlockInfo row = kRSBlockTable[i];
      // num_bytes = 196
      final int num_bytes = row.num_bytes;
      // num_ec_bytes = 130
      final int num_ec_bytes  = row.block_info[ec_level][0];
      // num_rs_blocks = 5
      final int num_rs_blocks = row.block_info[ec_level][1];
      // num_data_bytes = 196 - 130 = 66
      final int num_data_bytes = num_bytes - num_ec_bytes;
      // We want to choose the smallest version which can contain data of "num_input_bytes" + some
      // extra bits for the header (mode info and length info). The header can be three bytes
      // (precisely 4 + 16 bits) at most. Hence we do +3 here.
      if (num_data_bytes >= num_input_bytes + 3) {
        // Yay, we found the proper rs block info!
        qr_code.set_version(i + 1);
        qr_code.set_num_total_bytes(num_bytes);
        qr_code.set_num_data_bytes(num_data_bytes);
        qr_code.set_num_rs_blocks(num_rs_blocks);
        // num_ec_bytes = 196 - 66 = 130
        qr_code.set_num_ec_bytes(num_bytes - num_data_bytes);
        // num_matrix_width = 21 + 6 * 4 = 45
        qr_code.set_matrix_width(21 + i * 4);
        return true;
      }
    }
    Debug.LOG_ERROR("Cannot find proper rs block info (input data too big?)");
    return false;
  }

  // Terminate bits as described in 8.4.8 and 8.4.9 of JISX0510:2004 (p.24).
  static boolean TerminateBits(int num_data_bytes, BitVector bits) {
    final int capacity = num_data_bytes * 8;
    if (bits.size() > capacity) {
      Debug.LOG_ERROR("data bits cannot fit in the QR Code" + bits.size() + " > " + capacity);
      return false;
    }
    // Append termination bits. See 8.4.8 of JISX0510:2004 (p.24) for details.
    for (int i = 0; i < 4 && bits.size() < capacity; ++i) {
      bits.AppendBit(0);
    }
    final int num_bits_in_last_byte = bits.size() % 8;
    // If the last byte isn't 8-bit aligned, we'll add padding bits.
    if (num_bits_in_last_byte > 0) {
      final int num_padding_bits = 8 - num_bits_in_last_byte;
      for (int i = 0; i < num_padding_bits; ++i) {
        bits.AppendBit(0);
      }
    }
    // Should be 8-bit aligned here.
    Debug.DCHECK_EQ(0, bits.size() % 8);
    // If we have more space, we'll fill the space with padding patterns defined in 8.4.9 (p.24).
    final int num_padding_bytes = num_data_bytes - bits.num_bytes();
    for (int i = 0; i < num_padding_bytes; ++i) {
      if (i % 2 == 0) {
        bits.AppendBits(0xec, 8);
      } else {
        bits.AppendBits(0x11, 8);
      }
    }
    Debug.DCHECK_EQ(bits.size(), capacity);  // Should be same.
    return bits.size() == capacity;
  }

  // Get number of data bytes and number of error correction bytes for block id "block_id". Store
  // the result in "num_data_bytes_in_block", and "num_ec_bytes_in_block". See table 12 in 8.5.1 of
  // JISX0510:2004 (p.30)
  static void GetNumDataBytesAndNumECBytesForBlockID(int num_total_bytes, int num_data_bytes,
      int num_rs_blocks, int block_id, int[] num_data_bytes_in_block,
      int[] num_ec_bytes_in_block) {
    Debug.DCHECK_LT(block_id, num_rs_blocks);
    // num_rs_blocks_in_group2 = 196 % 5 = 1
    final int num_rs_blocks_in_group2 = num_total_bytes % num_rs_blocks;
    // num_rs_blocks_in_group1 = 5 - 1 = 4
    final int num_rs_blocks_in_group1 = num_rs_blocks - num_rs_blocks_in_group2;
    // num_total_bytes_in_group1 = 196 / 5 = 39
    final int num_total_bytes_in_group1 = num_total_bytes / num_rs_blocks;
    // num_total_bytes_in_group2 = 39 + 1 = 40
    final int num_total_bytes_in_group2 = num_total_bytes_in_group1 + 1;
    // num_data_bytes_in_group1 = 66 / 5 = 13
    final int num_data_bytes_in_group1 = num_data_bytes / num_rs_blocks;
    // num_data_bytes_in_group2 = 13 + 1 = 14
    final int num_data_bytes_in_group2 = num_data_bytes_in_group1 + 1;
    // num_ec_bytes_in_group1 = 39 - 13 = 26
    final int num_ec_bytes_in_group1 = num_total_bytes_in_group1 -
        num_data_bytes_in_group1;
    // num_ec_bytes_in_group2 = 40 - 14 = 26
    final int num_ec_bytes_in_group2 = num_total_bytes_in_group2 -
        num_data_bytes_in_group2;
    // Sanity checks.
    // 26 = 26
    Debug.DCHECK_EQ(num_ec_bytes_in_group1, num_ec_bytes_in_group2);
    // 5 = 4 + 1.
    Debug.DCHECK_EQ(num_rs_blocks, num_rs_blocks_in_group1 + num_rs_blocks_in_group2);
    // 196 = (13 + 26) * 4 + (14 + 26) * 1
    Debug.DCHECK_EQ(num_total_bytes,
        ((num_data_bytes_in_group1 + num_ec_bytes_in_group1) *
            num_rs_blocks_in_group1) +
            ((num_data_bytes_in_group2 + num_ec_bytes_in_group2) *
                num_rs_blocks_in_group2));

    if (block_id < num_rs_blocks_in_group1) {
      num_data_bytes_in_block[0] = num_data_bytes_in_group1;
      num_ec_bytes_in_block[0] = num_ec_bytes_in_group1;
    } else {
      num_data_bytes_in_block[0] = num_data_bytes_in_group2;
      num_ec_bytes_in_block[0] = num_ec_bytes_in_group2;
    }
  }

  // Interleave "bits" with corresponding error correction bytes. On success, store the result in
  // "result" and return true. On error, return false. The interleave rule is complicated. See 8.6
  // of JISX0510:2004 (p.37) for details.
  static boolean InterleaveWithECBytes(final BitVector bits, int num_total_bytes,
      int num_data_bytes, int num_rs_blocks, BitVector result) {

    // "bits" must have "num_data_bytes" bytes of data.
    Debug.DCHECK(bits.num_bytes() == num_data_bytes);

    // Step 1.  Divide data bytes into blocks and generate error correction bytes for them. We'll
    // store the divided data bytes blocks and error correction bytes blocks into "blocks".
    int data_bytes_offset = 0;
    int max_num_data_bytes = 0;
    int max_num_ec_bytes = 0;

    // Since, we know the number of reedsolmon blocks, we can initialize the vector with the number.
    Vector blocks = new Vector(num_rs_blocks);

    for (int i = 0; i < num_rs_blocks; ++i) {
      int[] num_data_bytes_in_block = new int[1];
      int[] num_ec_bytes_in_block = new int[1];
      GetNumDataBytesAndNumECBytesForBlockID(
          num_total_bytes, num_data_bytes, num_rs_blocks, i,
          num_data_bytes_in_block, num_ec_bytes_in_block);

      ByteArray data_bytes = new ByteArray();
      data_bytes.set(bits.getArray(), data_bytes_offset, num_data_bytes_in_block[0]);
      ByteArray ec_bytes = GenerateECBytes(data_bytes, num_ec_bytes_in_block[0]);
      blocks.addElement(new BlockPair(data_bytes, ec_bytes));

      max_num_data_bytes = Math.max(max_num_data_bytes, data_bytes.size());
      max_num_ec_bytes = Math.max(max_num_ec_bytes, ec_bytes.size());
      data_bytes_offset += num_data_bytes_in_block[0];
    }
    Debug.DCHECK_EQ(num_data_bytes, data_bytes_offset);

    // First, place data blocks.
    for (int i = 0; i < max_num_data_bytes; ++i) {
      for (int j = 0; j < blocks.size(); ++j) {
        final ByteArray data_bytes = ((BlockPair) blocks.elementAt(j)).getDataBytes();
        if (i < data_bytes.size()) {
          result.AppendBits(data_bytes.at(i), 8);
        }
      }
    }
    // Then, place error correction blocks.
    for (int i = 0; i < max_num_ec_bytes; ++i) {
      for (int j = 0; j < blocks.size(); ++j) {
        final ByteArray ec_bytes = ((BlockPair) blocks.elementAt(j)).getErrorCorrectionBytes();
        if (i < ec_bytes.size()) {
          result.AppendBits(ec_bytes.at(i), 8);
        }
      }
    }
    if (num_total_bytes == result.num_bytes()) {  // Should be same.
      return true;
    }
    Debug.LOG_ERROR("Interleaving error: " + num_total_bytes + " and " + result.num_bytes() +
        " differ.");
    return false;
  }

  static ByteArray GenerateECBytes(ByteArray data_bytes, int num_ec_bytes_in_block) {
    int numDataBytes = data_bytes.size();
    int[] toEncode = new int[numDataBytes + num_ec_bytes_in_block];
    for (int i = 0; i < numDataBytes; i++) {
      toEncode[i] = data_bytes.at(i);
    }
    new ReedSolomonEncoder(GF256.QR_CODE_FIELD).encode(toEncode, num_ec_bytes_in_block);

    ByteArray ec_bytes = new ByteArray(num_ec_bytes_in_block);
    for (int i = 0; i < num_ec_bytes_in_block; i++) {
      ec_bytes.set(i, toEncode[numDataBytes + i]);
    }
    return ec_bytes;
  }

  // Append mode info. On success, store the result in "bits" and return true. On error, return
  // false.
  static boolean AppendModeInfo(int mode, BitVector bits) {
    final int code = QRCode.GetModeCode(mode);
    if (code == -1) {
      Debug.LOG_ERROR("Invalid mode: " + mode);
      return false;
    }
    bits.AppendBits(code, 4);
    return true;
  }


  // Append length info. On success, store the result in "bits" and return true. On error, return
  // false.
  static boolean AppendLengthInfo(int num_bytes, int version, int mode, BitVector bits) {
    int num_letters = num_bytes;
    // In Kanji mode, a letter is represented in two bytes.
    if (mode == QRCode.MODE_KANJI) {
      Debug.DCHECK_EQ(0, num_letters % 2);
      num_letters /= 2;
    }

    final int num_bits = QRCode.GetNumBitsForLength(version, mode);
    if (num_bits == -1) {
      Debug.LOG_ERROR("num_bits unset");
      return false;
    }
    if (num_letters > ((1 << num_bits) - 1)) {
      Debug.LOG_ERROR(num_letters + "is bigger than" + ((1 << num_bits) - 1));
      return false;
    }
    bits.AppendBits(num_letters, num_bits);
    return true;
  }

  // Append "bytes" in "mode" mode (encoding) into "bits". On success, store the result in "bits"
  // and return true. On error, return false.
  static boolean AppendBytes(final ByteArray bytes, int mode, BitVector bits) {
    switch (mode) {
      case QRCode.MODE_NUMERIC:
        return AppendNumericBytes(bytes, bits);
      case QRCode.MODE_ALPHANUMERIC:
        return AppendAlphanumericBytes(bytes, bits);
      case QRCode.MODE_8BIT_BYTE:
        return Append8BitBytes(bytes, bits);
      case QRCode.MODE_KANJI:
        return AppendKanjiBytes(bytes, bits);
      default:
        break;
    }
    Debug.LOG_ERROR("Invalid mode: " + mode);
    return false;
  }

  // Append "bytes" to "bits" using QRCode.MODE_NUMERIC mode. On success, store the result in "bits"
  // and return true. On error, return false.
  static boolean AppendNumericBytes(final ByteArray bytes, BitVector bits) {
    // Validate all the bytes first.
    for (int i = 0; i < bytes.size(); ++i) {
      int oneByte = bytes.at(i);
      if (oneByte < '0' || oneByte > '9') {
        return false;
      }
    }
    for (int i = 0; i < bytes.size();) {
      final int num1 = bytes.at(i) - '0';
      if (i + 2 < bytes.size()) {
        // Encode three numeric letters in ten bits.
        final int num2 = bytes.at(i + 1) - '0';
        final int num3 = bytes.at(i + 2) - '0';
        bits.AppendBits(num1 * 100 + num2 * 10 + num3, 10);
        i += 3;
      } else if (i + 1 < bytes.size()) {
        // Encode two numeric letters in seven bits.
        final int num2 = bytes.at(i + 1) - '0';
        bits.AppendBits(num1 * 10 + num2, 7);
        i += 2;
      } else {
        // Encode one numeric letter in four bits.
        bits.AppendBits(num1, 4);
        ++i;
      }
    }
    return true;
  }

  // Append "bytes" to "bits" using QRCode.MODE_ALPHANUMERIC mode. On success, store the result in
  // "bits" and return true. On error, return false.
  static boolean AppendAlphanumericBytes(final ByteArray bytes, BitVector bits) {
    for (int i = 0; i < bytes.size();) {
      final int code1 = GetAlphanumericCode(bytes.at(i));
      if (code1 == -1) {
        return false;
      }
      if (i + 1 < bytes.size()) {
        final int code2 = GetAlphanumericCode(bytes.at(i + 1));
        if (code2 == -1) {
          return false;
        }
        // Encode two alphanumeric letters in 11 bits.
        bits.AppendBits(code1 * 45 + code2, 11);
        i += 2;
      } else {
        // Encode one alphanumeric letter in six bits.
        bits.AppendBits(code1, 6);
        ++i;
      }
    }
    return true;
  }

  // Append "bytes" to "bits" using QRCode.MODE_8BIT_BYTE mode. On success, store the result in
  // "bits" and return true. On error, return false.
  static boolean Append8BitBytes(final ByteArray bytes, BitVector bits) {
    for (int i = 0; i < bytes.size(); ++i) {
      bits.AppendBits(bytes.at(i), 8);
    }
    return true;
  }

  // Append "bytes" to "bits" using QRCode.MODE_KANJI mode. On success, store the result in "bits"
  // and return true. On error, return false. See 8.4.5 of JISX0510:2004 (p.21) for how to encode
  // Kanji bytes.
  static boolean AppendKanjiBytes(final ByteArray bytes, BitVector bits) {
    if (bytes.size() % 2 != 0) {
      // JAVAPORT: Our log implementation throws, which causes the unit test to fail.
      //Debug.LOG_ERROR("Invalid byte sequence: " + bytes);
      return false;
    }
    for (int i = 0; i < bytes.size(); i += 2) {
      Debug.DCHECK(IsValidKanji(bytes.at(i), bytes.at(i + 1)));
      final int code = (bytes.at(i) << 8) | bytes.at(i + 1);
      int subtracted = -1;
      if (code >= 0x8140 && code <= 0x9ffc) {
        subtracted = code - 0x8140;
      } else if (code >= 0xe040 && code <= 0xebbf) {
        subtracted = code - 0xc140;
      }
      if (subtracted == -1) {
        Debug.LOG_ERROR("Invalid byte sequence: " + bytes);
        return false;
      }
      final int encoded = ((subtracted >> 8) * 0xc0) + (subtracted & 0xff);
      bits.AppendBits(encoded, 13);
    }
    return true;
  }

  // Check if "byte1" and "byte2" can compose a valid Kanji letter (2-byte Shift_JIS letter). The
  // numbers are from http://ja.wikipedia.org/wiki/Shift_JIS.
  static boolean IsValidKanji(final int byte1, final int byte2) {
    return (byte2 != 0x7f &&
        ((byte1 >= 0x81 && byte1 <= 0x9f &&
            byte2 >= 0x40 && byte2 <= 0xfc) ||
            ((byte1 >= 0xe0 && byte1 <= 0xfc &&
                byte2 >= 0x40 && byte2 <= 0xfc))));
  }

  // Check if "bytes" is a valid Kanji sequence. Used by the unit tests.
  static boolean IsValidKanjiSequence(final ByteArray bytes) {
    if (bytes.size() % 2 != 0) {
      return false;
    }
    int i = 0;
    for (; i < bytes.size(); i += 2) {
      if (!IsValidKanji(bytes.at(i), bytes.at(i + 1))) {
        break;
      }
    }
    return i == bytes.size();  // Consumed all bytes?
  }

}
