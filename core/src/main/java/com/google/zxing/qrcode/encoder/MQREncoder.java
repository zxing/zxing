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

import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitArray;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.google.zxing.qrcode.decoder.Mode;
import com.google.zxing.qrcode.decoder.MQRVersion;

import java.util.Map;

/**
 * Micro QR Encoder class.
 * Some specifications of MicroQR are different from QR.
 * Many data encoding and error-correcting algorithm methods are defined at the class "Encode".
 */
public final class MQREncoder {

  private MQREncoder() {
  }

  public static MQRCode encode(String content,
                              ErrorCorrectionLevel ecLevel,
                              Map<EncodeHintType,?> hints) throws WriterException {

    // Determine what character encoding has been specified by the caller, if any
    String encoding = Encoder.DEFAULT_BYTE_MODE_ENCODING;
    boolean hasEncodingHint = hints != null && hints.containsKey(EncodeHintType.CHARACTER_SET);
    if (hasEncodingHint) {
      encoding = hints.get(EncodeHintType.CHARACTER_SET).toString();
    }

    // Pick an encoding mode appropriate for the content. Note that this will not attempt to use
    // multiple modes / segments even if that were more efficient. Twould be nice.
    Mode mode = Encoder.chooseMode(content, encoding);

    // Collect data within the main segment, separately, to count its size if needed. Don't add it to
    // main payload yet.
    BitArray dataBits = new BitArray();
    Encoder.appendBytes(content, mode, dataBits, encoding);

    // This will store the header information, like mode and
    // length, as well as "header" segments like an ECI segment.
    BitArray headerBits = new BitArray();

    // Choose version before emitting the mode info bits
    MQRVersion version;
    if (hints != null && hints.containsKey(EncodeHintType.QR_VERSION)) {
      int versionNumber = Integer.parseInt(hints.get(EncodeHintType.QR_VERSION).toString());
      version = MQRVersion.getVersionForNumber(versionNumber);
      int bitsNeeded = calculateBitsNeeded(mode, dataBits, version);
      if (!willFit(bitsNeeded, version, ecLevel)) {
        throw new WriterException("Data too big for requested version");
      }
    } else {
      version = recommendVersion(ecLevel, mode, dataBits);
    }

    // Write the mode marker and length info
    // Find "length" of main segment and write it
    int numLetters = mode == Mode.BYTE ? dataBits.getSizeInBytes() : content.length();
    appendModeInfo(version, mode, numLetters, headerBits);

    BitArray headerAndDataBits = new BitArray();
    headerAndDataBits.appendBitArray(headerBits);
    headerAndDataBits.appendBitArray(dataBits);

    // Terminate the bits properly.
    MQRVersion.ECBlocks ecBlocks = version.getECBlocksForLevel(ecLevel);
    terminateBits(ecBlocks.getDataBits(), ecBlocks.getDataCodewords(), version, headerAndDataBits);

    // Interleave data bits with error correction code.
    BitArray finalBits = Encoder.interleaveWithECBytes(headerAndDataBits,
            ecBlocks.getDataCodewords() + ecBlocks.getECCodewords(),
            ecBlocks.getDataCodewords(),1);

    MQRCode qrCode = new MQRCode();

    qrCode.setECLevel(ecLevel);
    qrCode.setMode(mode);
    qrCode.setVersion(version);

    //  Choose the mask pattern and set to "qrCode".
    int dimension = version.getDimensionForVersion();
    ByteMatrix matrix = new ByteMatrix(dimension, dimension);
    int maskPattern = chooseMaskPattern(finalBits, ecLevel, version, matrix);
    qrCode.setMaskPattern(maskPattern);

    // Build the matrix and set it to "qrCode".
    MatrixUtil.buildMatrixMicro(finalBits, ecLevel, version, maskPattern, matrix);
    qrCode.setMatrix(matrix);

    return qrCode;
  }
  
  /**
   * Decides the smallest version of MicroQR code that will contain all of the provided data.
   *
   * @throws WriterException if the data cannot fit in any version
   */
  private static MQRVersion recommendVersion(ErrorCorrectionLevel ecLevel,
                                             Mode mode, BitArray dataBits) throws WriterException {
    int provisionalBitsNeeded = dataBits.getSize();
    MQRVersion provisionalVersion = chooseVersion(provisionalBitsNeeded, ecLevel);

    // Use that guess to calculate the right version. I am still not sure this works in 100% of cases.
    int bitsNeeded = calculateBitsNeeded(mode, dataBits, provisionalVersion);
    return chooseVersion(bitsNeeded, ecLevel);
  }

  // See Annex1 2.3 of JISX0510:2004 (p.106) for details.
  private static int calculateBitsNeeded(Mode mode,
                                         BitArray dataBits,
                                         MQRVersion version) {
    // mode info bits
    int headerBits = version.getVersionNumber() - 1;

    // charactor count bits
    headerBits += version.getVersionNumber() + 2;
    switch (mode) {
    case NUMERIC:
      break;
    case ALPHANUMERIC:
      headerBits -= 1;
      break;
    case BYTE:
      headerBits -= 1;
      break;
    case KANJI:
      headerBits -= 2;
      break;
    default:
      headerBits -= 2;
    }
    
    return headerBits + dataBits.getSize();
  }

  public static Mode chooseMode(String content) {
    return Encoder.chooseMode(content, null);
  }

  private static int chooseMaskPattern(BitArray bits,
                                       ErrorCorrectionLevel ecLevel,
                                       MQRVersion version,
                                       ByteMatrix matrix) throws WriterException {

    int maxScore = 0;  // Larger score is better.
    int bestMaskPattern = -1;
    // We try all mask patterns to choose the best one.
    for (int maskPattern = 0; maskPattern < MQRCode.NUM_MASK_PATTERNS; maskPattern++) {
      MatrixUtil.buildMatrixMicro(bits, ecLevel, version, maskPattern, matrix);
      int score = calculateMaskScore(matrix);
      if (score > maxScore) {
        maxScore = score;
        bestMaskPattern = maskPattern;
      }
    }
    return bestMaskPattern;
  }

  // The mask score calculation..
  // See Annex1 2.5.3 of JISX0510:2004 (p.114) for details.
  private static int calculateMaskScore(ByteMatrix matrix) {
    int sum1 = 0;
    int sum2 = 0;
    for (int i = 1; i < matrix.getWidth(); i++) {
      if (matrix.get(i,matrix.getHeight() - 1) == 1) {
        sum1 += 1;
      }
      if (matrix.get(matrix.getWidth() - 1, i) == 1) {
        sum2 += 1;
      }
    }
    return sum1 * 16 + sum2;
  }

  private static MQRVersion chooseVersion(int numInputBits, ErrorCorrectionLevel ecLevel) throws WriterException {
    for (int versionNum = 1; versionNum <= 4; versionNum++) {
      MQRVersion version = MQRVersion.getVersionForNumber(versionNum);
      if (willFit(numInputBits, version, ecLevel)) {
        return version;
      }
    }
    throw new WriterException("Data too big");
  }
  
  /**
   * @return true if the number of input bits will fit in a code with the specified version and
   * error correction level.
   */
  private static boolean willFit(int numInputBits, MQRVersion version, ErrorCorrectionLevel ecLevel) {
    MQRVersion.ECBlocks ecBlocks = version.getECBlocksForLevel(ecLevel);
    int numBits = ecBlocks.getDataBits();
    return numBits >= numInputBits;
  }
  
  /**
   * Terminate bits as described in Annex1 2.3.6 and 2.3.7 of JISX0510:2004 (p.112).
   */
  private static void terminateBits(int numDataBits, int numDataBytes, MQRVersion version, BitArray bits) throws WriterException {
    if (bits.getSize() > numDataBits) {
      throw new WriterException("data bits cannot fit in the MicroQR Code" + bits.getSize() + " > " +
          numDataBits);
    }
    int termBits = version.getVersionNumber() * 2 + 1;
    for (int i = 0; i < termBits && bits.getSize() < numDataBits; ++i) {
      bits.appendBit(false);
    }

    // padding 0 for 8bits
    if (bits.getSize() < numDataBits && (bits.getSize() % 8) > 0) {
      bits.appendBits(0, 8 - (bits.getSize() % 8));
    }

    // If we have more space, we'll fill the space with padding patterns defined in 8.4.9 (p.24).
    int numPaddingBytes = numDataBytes - bits.getSizeInBytes();
    for (int i = 0; i < numPaddingBytes; ++i) {
      if (i == (numPaddingBytes - 1) && version.isHalfBytesAtLast()) {
        bits.appendBits(0, 4);
      } else {
        bits.appendBits((i & 0x01) == 0 ? 0xEC : 0x11, 8);
      }
    }
    if (bits.getSize() != numDataBits) {
      throw new WriterException("Bits size does not equal capacity");
    }
  }

  /**
   * Append mode info. On success, store the result in "bits".
   * See Annex1 2.3 of JISX0510:2004 (p.106) for details.
   */
  private static void appendModeInfo(MQRVersion version, Mode mode, int contentLength, BitArray bits) throws WriterException {
    switch (version.getVersionNumber()) {
    case 1:
      // no mode info
      bits.appendBits(contentLength, 3);
      break;
    case 2:
      switch (mode) {
      case NUMERIC:
        bits.appendBit(false);
        bits.appendBits(contentLength, 4);
        break;
      case ALPHANUMERIC:
        bits.appendBit(true);
        bits.appendBits(contentLength, 3);
        break;
      default:
        throw new WriterException("Not supported mode[" + mode + "] and version [" + version + "]");
      }
      break;
    case 3:
      switch (mode) {
      case NUMERIC:
        bits.appendBits(0,2);
        bits.appendBits(contentLength, 5);
        break;
      case ALPHANUMERIC:
        bits.appendBits(1,2);
        bits.appendBits(contentLength, 4);
        break;
      case BYTE:
        bits.appendBits(2,2);
        bits.appendBits(contentLength, 4);
        break;
      case KANJI:
        bits.appendBits(3,2);
        bits.appendBits(contentLength, 3);
        break;
      default:
        throw new WriterException("Not supported mode[" + mode + "] and version [" + version + "]");
      }
      break;
    case 4:
      switch (mode) {
      case NUMERIC:
        bits.appendBits(0,3);
        bits.appendBits(contentLength, 6);
        break;
      case ALPHANUMERIC:
        bits.appendBits(1,3);
        bits.appendBits(contentLength, 5);
        break;
      case BYTE:
        bits.appendBits(2,3);
        bits.appendBits(contentLength, 5);
        break;
      case KANJI:
        bits.appendBits(3,3);
        bits.appendBits(contentLength, 4);
        break;
      default:
        throw new WriterException("Not supported mode[" + mode + "] and version [" + version + "]");
      }
      break;
      default:
        throw new WriterException("Not supported mode[" + mode + "] and version [" + version + "]");
    }
  }

}
