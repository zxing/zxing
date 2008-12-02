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
import com.google.zxing.WriterException;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.util.Vector;

/**
 * @author satorux@google.com (Satoru Takabayashi) - creator
 * @author dswitkin@google.com (Daniel Switkin) - ported from C++
 */
public final class Encoder {

  // The original table is defined in the table 5 of JISX0510:2004 (p.19).
  private static final int[] ALPHANUMERIC_TABLE = {
      -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,  // 0x00-0x0f
      -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,  // 0x10-0x1f
      36, -1, -1, -1, 37, 38, -1, -1, -1, -1, 39, 40, -1, 41, 42, 43,  // 0x20-0x2f
      0,   1,  2,  3,  4,  5,  6,  7,  8,  9, 44, -1, -1, -1, -1, -1,  // 0x30-0x3f
      -1, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24,  // 0x40-0x4f
      25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, -1, -1, -1, -1, -1,  // 0x50-0x5f
  };

  private static final class RSBlockInfo {

    final int numBytes;
    final int[][] blockInfo;

    public RSBlockInfo(int numBytes, int[][] blockInfo) {
      this.numBytes = numBytes;
      this.blockInfo = blockInfo;
    }

  }

  // The table is from table 12 of JISX0510:2004 (p. 30). The "blockInfo" parts are ordered by
  // L, M, Q, H. Within each blockInfo, the 0th element is getNumECBytes, and the 1st element is
  // getNumRSBlocks. The table was doublechecked by komatsu.
  private static final RSBlockInfo[] RS_BLOCK_TABLE = {
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

    private final ByteArray dataBytes;
    private final ByteArray errorCorrectionBytes;

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

  // Encode "bytes" with the error correction level "getECLevel". The encoding mode will be chosen
  // internally by chooseMode(). On success, store the result in "qrCode" and return true.
  // We recommend you to use QRCode.EC_LEVEL_L (the lowest level) for
  // "getECLevel" since our primary use is to show QR code on desktop screens. We don't need very
  // strong error correction for this purpose.
  //
  // Note that there is no way to encode bytes in MODE_KANJI. We might want to add EncodeWithMode()
  // with which clients can specify the encoding mode. For now, we don't need the functionality.
  public static void encode(final ByteArray bytes, ErrorCorrectionLevel ecLevel, QRCode qrCode)
      throws WriterException {
    // Step 1: Choose the mode (encoding).
    final int mode = chooseMode(bytes);

    // Step 2: Append "bytes" into "dataBits" in appropriate encoding.
    BitVector dataBits = new BitVector();
    appendBytes(bytes, mode, dataBits);
    // Step 3: Initialize QR code that can contain "dataBits".
    final int numInputBytes = dataBits.sizeInBytes();
    initQRCode(numInputBytes, ecLevel, mode, qrCode);

    // Step 4: Build another bit vector that contains header and data.
    BitVector headerAndDataBits = new BitVector();
    appendModeInfo(qrCode.getMode(), headerAndDataBits);
    appendLengthInfo(bytes.size(), qrCode.getVersion(), qrCode.getMode(), headerAndDataBits);
    headerAndDataBits.appendBitVector(dataBits);

    // Step 5: Terminate the bits properly.
    terminateBits(qrCode.getNumDataBytes(), headerAndDataBits);

    // Step 6: Interleave data bits with error correction code.
    BitVector finalBits = new BitVector();
    interleaveWithECBytes(headerAndDataBits, qrCode.getNumTotalBytes(), qrCode.getNumDataBytes(),
        qrCode.getNumRSBlocks(), finalBits);

    // Step 7: Choose the mask pattern and set to "qrCode".
    ByteMatrix matrix = new ByteMatrix(qrCode.getMatrixWidth(), qrCode.getMatrixWidth());
    qrCode.setMaskPattern(chooseMaskPattern(finalBits, qrCode.getECLevel(), qrCode.getVersion(),
        matrix));

    // Step 8.  Build the matrix and set it to "qrCode".
    MatrixUtil.buildMatrix(finalBits, qrCode.getECLevel(), qrCode.getVersion(),
        qrCode.getMaskPattern(), matrix);
    qrCode.setMatrix(matrix);
    // Step 9.  Make sure we have a valid QR Code.
    if (!qrCode.isValid()) {
      throw new WriterException("Invalid QR code: " + qrCode.toString());
    }
  }

  // Return the code point of the table used in alphanumeric mode. Return -1 if there is no
  // corresponding code in the table.
  static int getAlphanumericCode(int code) {
    if (code < ALPHANUMERIC_TABLE.length) {
      return ALPHANUMERIC_TABLE[code];
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
  public static int chooseMode(final ByteArray bytes) throws WriterException {
    boolean hasNumeric = false;
    boolean hasAlphanumeric = false;
    boolean hasOther = false;
    for (int i = 0; i < bytes.size(); ++i) {
      final int oneByte = bytes.at(i);
      if (oneByte >= '0' && oneByte <= '9') {
        hasNumeric = true;
      } else if (getAlphanumericCode(oneByte) != -1) {
        hasAlphanumeric = true;
      } else {
        hasOther = true;
      }
    }
    if (hasOther) {
      return QRCode.MODE_8BIT_BYTE;
    } else if (hasAlphanumeric) {
      return QRCode.MODE_ALPHANUMERIC;
    } else if (hasNumeric) {
      return QRCode.MODE_NUMERIC;
    }
    // "bytes" must be empty to reach here.
    if (!bytes.empty()) {
      throw new WriterException("Bytes left over");
    }
    return QRCode.MODE_8BIT_BYTE;
  }

  private static int chooseMaskPattern(final BitVector bits, ErrorCorrectionLevel ecLevel, int version,
      ByteMatrix matrix) throws WriterException {
    if (!QRCode.isValidMatrixWidth(matrix.width())) {
      throw new WriterException("Invalid matrix width: " + matrix.width());
    }

    int minPenalty = Integer.MAX_VALUE;  // Lower penalty is better.
    int bestMaskPattern = -1;
    // We try all mask patterns to choose the best one.
    for (int maskPattern = 0; maskPattern < QRCode.NUM_MASK_PATTERNS; maskPattern++) {
      MatrixUtil.buildMatrix(bits, ecLevel, version, maskPattern, matrix);
      final int penalty = MaskUtil.calculateMaskPenalty(matrix);
      if (penalty < minPenalty) {
        minPenalty = penalty;
        bestMaskPattern = maskPattern;
      }
    }
    return bestMaskPattern;
  }

  // Initialize "qrCode" according to "numInputBytes", "ecLevel", and "mode". On success, modify
  // "qrCode" and return true.
  private static void initQRCode(int numInputBytes, ErrorCorrectionLevel ecLevel, int mode, QRCode qrCode)
      throws WriterException {
    qrCode.setECLevel(ecLevel);
    qrCode.setMode(mode);

    // In the following comments, we use numbers of Version 7-H.
    for (int i = 0; i < RS_BLOCK_TABLE.length; ++i) {
      final RSBlockInfo row = RS_BLOCK_TABLE[i];
      // numBytes = 196
      final int numBytes = row.numBytes;
      // getNumECBytes = 130
      final int numEcBytes  = row.blockInfo[ecLevel.ordinal()][0];
      // getNumRSBlocks = 5
      final int numRSBlocks = row.blockInfo[ecLevel.ordinal()][1];
      // getNumDataBytes = 196 - 130 = 66
      final int numDataBytes = numBytes - numEcBytes;
      // We want to choose the smallest version which can contain data of "numInputBytes" + some
      // extra bits for the header (mode info and length info). The header can be three bytes
      // (precisely 4 + 16 bits) at most. Hence we do +3 here.
      if (numDataBytes >= numInputBytes + 3) {
        // Yay, we found the proper rs block info!
        qrCode.setVersion(i + 1);
        qrCode.setNumTotalBytes(numBytes);
        qrCode.setNumDataBytes(numDataBytes);
        qrCode.setNumRSBlocks(numRSBlocks);
        // getNumECBytes = 196 - 66 = 130
        qrCode.setNumECBytes(numBytes - numDataBytes);
        // matrix width = 21 + 6 * 4 = 45
        qrCode.setMatrixWidth(21 + i * 4);
        return;
      }
    }
    throw new WriterException("Cannot find proper rs block info (input data too big?)");
  }

  // Terminate bits as described in 8.4.8 and 8.4.9 of JISX0510:2004 (p.24).
  static void terminateBits(int numDataBytes, BitVector bits) throws WriterException {
    final int capacity = numDataBytes * 8;
    if (bits.size() > capacity) {
      throw new WriterException("data bits cannot fit in the QR Code" + bits.size() + " > " + capacity);
    }
    // Append termination bits. See 8.4.8 of JISX0510:2004 (p.24) for details.
    for (int i = 0; i < 4 && bits.size() < capacity; ++i) {
      bits.appendBit(0);
    }
    final int numBitsInLastByte = bits.size() % 8;
    // If the last byte isn't 8-bit aligned, we'll add padding bits.
    if (numBitsInLastByte > 0) {
      final int numPaddingBits = 8 - numBitsInLastByte;
      for (int i = 0; i < numPaddingBits; ++i) {
        bits.appendBit(0);
      }
    }
    // Should be 8-bit aligned here.
    if (bits.size() % 8 != 0) {
      throw new WriterException("Number of bits is not a multiple of 8");
    }
    // If we have more space, we'll fill the space with padding patterns defined in 8.4.9 (p.24).
    final int numPaddingBytes = numDataBytes - bits.sizeInBytes();
    for (int i = 0; i < numPaddingBytes; ++i) {
      if (i % 2 == 0) {
        bits.appendBits(0xec, 8);
      } else {
        bits.appendBits(0x11, 8);
      }
    }
    if (bits.size() != capacity) {
      throw new WriterException("Bits size does not equal capacity");
    }
  }

  // Get number of data bytes and number of error correction bytes for block id "blockID". Store
  // the result in "numDataBytesInBlock", and "numECBytesInBlock". See table 12 in 8.5.1 of
  // JISX0510:2004 (p.30)
  static void getNumDataBytesAndNumECBytesForBlockID(int numTotalBytes, int numDataBytes,
      int numRSBlocks, int blockID, int[] numDataBytesInBlock,
      int[] numECBytesInBlock) throws WriterException {
    if (blockID >= numRSBlocks) {
      throw new WriterException("Block ID too large");
    }
    // numRsBlocksInGroup2 = 196 % 5 = 1
    final int numRsBlocksInGroup2 = numTotalBytes % numRSBlocks;
    // numRsBlocksInGroup1 = 5 - 1 = 4
    final int numRsBlocksInGroup1 = numRSBlocks - numRsBlocksInGroup2;
    // numTotalBytesInGroup1 = 196 / 5 = 39
    final int numTotalBytesInGroup1 = numTotalBytes / numRSBlocks;
    // numTotalBytesInGroup2 = 39 + 1 = 40
    final int numTotalBytesInGroup2 = numTotalBytesInGroup1 + 1;
    // numDataBytesInGroup1 = 66 / 5 = 13
    final int numDataBytesInGroup1 = numDataBytes / numRSBlocks;
    // numDataBytesInGroup2 = 13 + 1 = 14
    final int numDataBytesInGroup2 = numDataBytesInGroup1 + 1;
    // numEcBytesInGroup1 = 39 - 13 = 26
    final int numEcBytesInGroup1 = numTotalBytesInGroup1 - numDataBytesInGroup1;
    // numEcBytesInGroup2 = 40 - 14 = 26
    final int numEcBytesInGroup2 = numTotalBytesInGroup2 - numDataBytesInGroup2;
    // Sanity checks.
    // 26 = 26
    if (numEcBytesInGroup1 != numEcBytesInGroup2) {
      throw new WriterException("EC bytes mismatch");
    }
    // 5 = 4 + 1.
    if (numRSBlocks != numRsBlocksInGroup1 + numRsBlocksInGroup2) {
      throw new WriterException("RS blocks mismatch");
    }
    // 196 = (13 + 26) * 4 + (14 + 26) * 1
    if (numTotalBytes !=
        ((numDataBytesInGroup1 + numEcBytesInGroup1) *
            numRsBlocksInGroup1) +
            ((numDataBytesInGroup2 + numEcBytesInGroup2) *
                numRsBlocksInGroup2)) {
      throw new WriterException("Total bytes mismatch");
    }

    if (blockID < numRsBlocksInGroup1) {
      numDataBytesInBlock[0] = numDataBytesInGroup1;
      numECBytesInBlock[0] = numEcBytesInGroup1;
    } else {
      numDataBytesInBlock[0] = numDataBytesInGroup2;
      numECBytesInBlock[0] = numEcBytesInGroup2;
    }
  }

  // Interleave "bits" with corresponding error correction bytes. On success, store the result in
  // "result" and return true. The interleave rule is complicated. See 8.6
  // of JISX0510:2004 (p.37) for details.
  static void interleaveWithECBytes(final BitVector bits, int numTotalBytes,
      int numDataBytes, int numRSBlocks, BitVector result) throws WriterException {

    // "bits" must have "getNumDataBytes" bytes of data.
    if (bits.sizeInBytes() != numDataBytes) {
      throw new WriterException("Number of bits and data bytes does not match");
    }

    // Step 1.  Divide data bytes into blocks and generate error correction bytes for them. We'll
    // store the divided data bytes blocks and error correction bytes blocks into "blocks".
    int dataBytesOffset = 0;
    int maxNumDataBytes = 0;
    int maxNumEcBytes = 0;

    // Since, we know the number of reedsolmon blocks, we can initialize the vector with the number.
    Vector blocks = new Vector(numRSBlocks);

    for (int i = 0; i < numRSBlocks; ++i) {
      int[] numDataBytesInBlock = new int[1];
      int[] numEcBytesInBlock = new int[1];
      getNumDataBytesAndNumECBytesForBlockID(
          numTotalBytes, numDataBytes, numRSBlocks, i,
          numDataBytesInBlock, numEcBytesInBlock);

      ByteArray dataBytes = new ByteArray();
      dataBytes.set(bits.getArray(), dataBytesOffset, numDataBytesInBlock[0]);
      ByteArray ecBytes = generateECBytes(dataBytes, numEcBytesInBlock[0]);
      blocks.addElement(new BlockPair(dataBytes, ecBytes));

      maxNumDataBytes = Math.max(maxNumDataBytes, dataBytes.size());
      maxNumEcBytes = Math.max(maxNumEcBytes, ecBytes.size());
      dataBytesOffset += numDataBytesInBlock[0];
    }
    if (numDataBytes != dataBytesOffset) {
      throw new WriterException("Data bytes does not match offset");
    }

    // First, place data blocks.
    for (int i = 0; i < maxNumDataBytes; ++i) {
      for (int j = 0; j < blocks.size(); ++j) {
        final ByteArray dataBytes = ((BlockPair) blocks.elementAt(j)).getDataBytes();
        if (i < dataBytes.size()) {
          result.appendBits(dataBytes.at(i), 8);
        }
      }
    }
    // Then, place error correction blocks.
    for (int i = 0; i < maxNumEcBytes; ++i) {
      for (int j = 0; j < blocks.size(); ++j) {
        final ByteArray ecBytes = ((BlockPair) blocks.elementAt(j)).getErrorCorrectionBytes();
        if (i < ecBytes.size()) {
          result.appendBits(ecBytes.at(i), 8);
        }
      }
    }
    if (numTotalBytes != result.sizeInBytes()) {  // Should be same.
      throw new WriterException("Interleaving error: " + numTotalBytes + " and " + result.sizeInBytes() +
        " differ.");
    }
  }

  static ByteArray generateECBytes(ByteArray dataBytes, int numEcBytesInBlock) {
    int numDataBytes = dataBytes.size();
    int[] toEncode = new int[numDataBytes + numEcBytesInBlock];
    for (int i = 0; i < numDataBytes; i++) {
      toEncode[i] = dataBytes.at(i);
    }
    new ReedSolomonEncoder(GF256.QR_CODE_FIELD).encode(toEncode, numEcBytesInBlock);

    ByteArray ecBytes = new ByteArray(numEcBytesInBlock);
    for (int i = 0; i < numEcBytesInBlock; i++) {
      ecBytes.set(i, toEncode[numDataBytes + i]);
    }
    return ecBytes;
  }

  // Append mode info. On success, store the result in "bits" and return true. On error, return
  // false.
  static void appendModeInfo(int mode, BitVector bits) throws WriterException {
    final int code = QRCode.getModeCode(mode);
    bits.appendBits(code, 4);
  }


  // Append length info. On success, store the result in "bits" and return true. On error, return
  // false.
  static void appendLengthInfo(int numBytes, int version, int mode, BitVector bits) throws WriterException {
    int numLetters = numBytes;
    // In Kanji mode, a letter is represented in two bytes.
    if (mode == QRCode.MODE_KANJI) {
      if (numLetters % 2 != 0) {
        throw new WriterException("Number of letters must be even");
      }
      numLetters /= 2;
    }

    final int numBits = QRCode.getNumBitsForLength(version, mode);
    if (numLetters > ((1 << numBits) - 1)) {
      throw new WriterException(numLetters + "is bigger than" + ((1 << numBits) - 1));
    }
    bits.appendBits(numLetters, numBits);
  }

  // Append "bytes" in "mode" mode (encoding) into "bits". On success, store the result in "bits"
  // and return true.
  static void appendBytes(final ByteArray bytes, int mode, BitVector bits) throws WriterException {
    switch (mode) {
      case QRCode.MODE_NUMERIC:
        appendNumericBytes(bytes, bits);
        break;
      case QRCode.MODE_ALPHANUMERIC:
        appendAlphanumericBytes(bytes, bits);
        break;
      case QRCode.MODE_8BIT_BYTE:
        append8BitBytes(bytes, bits);
        break;
      case QRCode.MODE_KANJI:
        appendKanjiBytes(bytes, bits);
        break;
      default:
        throw new WriterException("Invalid mode: " + mode);
    }
  }

  // Append "bytes" to "bits" using QRCode.MODE_NUMERIC mode. On success, store the result in "bits"
  // and return true.
  static void appendNumericBytes(final ByteArray bytes, BitVector bits) throws WriterException {
    // Validate all the bytes first.
    for (int i = 0; i < bytes.size(); ++i) {
      int oneByte = bytes.at(i);
      if (oneByte < '0' || oneByte > '9') {
        throw new WriterException("Non-digit found");
      }
    }
    for (int i = 0; i < bytes.size();) {
      final int num1 = bytes.at(i) - '0';
      if (i + 2 < bytes.size()) {
        // Encode three numeric letters in ten bits.
        final int num2 = bytes.at(i + 1) - '0';
        final int num3 = bytes.at(i + 2) - '0';
        bits.appendBits(num1 * 100 + num2 * 10 + num3, 10);
        i += 3;
      } else if (i + 1 < bytes.size()) {
        // Encode two numeric letters in seven bits.
        final int num2 = bytes.at(i + 1) - '0';
        bits.appendBits(num1 * 10 + num2, 7);
        i += 2;
      } else {
        // Encode one numeric letter in four bits.
        bits.appendBits(num1, 4);
        ++i;
      }
    }
  }

  // Append "bytes" to "bits" using QRCode.MODE_ALPHANUMERIC mode. On success, store the result in
  // "bits" and return true.
  static void appendAlphanumericBytes(final ByteArray bytes, BitVector bits) throws WriterException {
    for (int i = 0; i < bytes.size();) {
      final int code1 = getAlphanumericCode(bytes.at(i));
      if (code1 == -1) {
        throw new WriterException();
      }
      if (i + 1 < bytes.size()) {
        final int code2 = getAlphanumericCode(bytes.at(i + 1));
        if (code2 == -1) {
          throw new WriterException();
        }
        // Encode two alphanumeric letters in 11 bits.
        bits.appendBits(code1 * 45 + code2, 11);
        i += 2;
      } else {
        // Encode one alphanumeric letter in six bits.
        bits.appendBits(code1, 6);
        ++i;
      }
    }
  }

  // Append "bytes" to "bits" using QRCode.MODE_8BIT_BYTE mode. On success, store the result in
  // "bits" and return true.
  static void append8BitBytes(final ByteArray bytes, BitVector bits) {
    for (int i = 0; i < bytes.size(); ++i) {
      bits.appendBits(bytes.at(i), 8);
    }
  }

  // Append "bytes" to "bits" using QRCode.MODE_KANJI mode. On success, store the result in "bits"
  // and return true. See 8.4.5 of JISX0510:2004 (p.21) for how to encode
  // Kanji bytes.
  static void appendKanjiBytes(final ByteArray bytes, BitVector bits) throws WriterException {
    if (bytes.size() % 2 != 0) {
      throw new WriterException("Number of bytes must be even");
    }
    for (int i = 0; i < bytes.size(); i += 2) {
      if (!isValidKanji(bytes.at(i), bytes.at(i + 1))) {
        throw new WriterException("Invalid Kanji at " + i);
      }
      final int code = (bytes.at(i) << 8) | bytes.at(i + 1);
      int subtracted = -1;
      if (code >= 0x8140 && code <= 0x9ffc) {
        subtracted = code - 0x8140;
      } else if (code >= 0xe040 && code <= 0xebbf) {
        subtracted = code - 0xc140;
      }
      if (subtracted == -1) {
        throw new WriterException("Invalid byte sequence: " + bytes);
      }
      final int encoded = ((subtracted >> 8) * 0xc0) + (subtracted & 0xff);
      bits.appendBits(encoded, 13);
    }
  }

  // Check if "byte1" and "byte2" can compose a valid Kanji letter (2-byte Shift_JIS letter). The
  // numbers are from http://ja.wikipedia.org/wiki/Shift_JIS.
  static boolean isValidKanji(final int byte1, final int byte2) {
    return (byte2 != 0x7f &&
        ((byte1 >= 0x81 && byte1 <= 0x9f &&
            byte2 >= 0x40 && byte2 <= 0xfc) ||
            ((byte1 >= 0xe0 && byte1 <= 0xfc &&
                byte2 >= 0x40 && byte2 <= 0xfc))));
  }

  // Check if "bytes" is a valid Kanji sequence. Used by the unit tests.
  static boolean isValidKanjiSequence(final ByteArray bytes) {
    if (bytes.size() % 2 != 0) {
      return false;
    }
    int i = 0;
    for (; i < bytes.size(); i += 2) {
      if (!isValidKanji(bytes.at(i), bytes.at(i + 1))) {
        break;
      }
    }
    return i == bytes.size();  // Consumed all bytes?
  }

}
