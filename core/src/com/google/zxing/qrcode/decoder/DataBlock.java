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

package com.google.zxing.qrcode.decoder;

/**
 * <p>Encapsulates a block of data within a QR Code. QR Codes may split their data into
 * multiple blocks, each of which is a unit of data and error-correction codewords. Each
 * is represented by an instance of this class.</p>
 *
 * @author Sean Owen
 */
final class DataBlock {

  private final int numDataCodewords;
  private final byte[] codewords;

  private DataBlock(int numDataCodewords, byte[] codewords) {
    this.numDataCodewords = numDataCodewords;
    this.codewords = codewords;
  }

  /**
   * <p>When QR Codes use multiple data blocks, they are actually interleaved.
   * That is, the first byte of data block 1 to n is written, then the second bytes, and so on. This
   * method will separate the data into original blocks.</p>
   *
   * @param rawCodewords bytes as read directly from the QR Code
   * @param version version of the QR Code
   * @param ecLevel error-correction level of the QR Code
   * @return {@link DataBlock}s containing original bytes, "de-interleaved" from representation in the
   *         QR Code
   */
  static DataBlock[] getDataBlocks(byte[] rawCodewords,
                                   Version version,
                                   ErrorCorrectionLevel ecLevel) {

    if (rawCodewords.length != version.getTotalCodewords()) {
      throw new IllegalArgumentException();
    }

    // Figure out the number and size of data blocks used by this version and
    // error correction level
    Version.ECBlocks ecBlocks = version.getECBlocksForLevel(ecLevel);

    // First count the total number of data blocks
    int totalBlocks = 0;
    Version.ECB[] ecBlockArray = ecBlocks.getECBlocks();
    for (int i = 0; i < ecBlockArray.length; i++) {
      totalBlocks += ecBlockArray[i].getCount();
    }

    // Now establish DataBlocks of the appropriate size and number of data codewords
    DataBlock[] result = new DataBlock[totalBlocks];
    int numResultBlocks = 0;
    for (int j = 0; j < ecBlockArray.length; j++) {
      Version.ECB ecBlock = ecBlockArray[j];
      for (int i = 0; i < ecBlock.getCount(); i++) {
        int numDataCodewords = ecBlock.getDataCodewords();
        int numBlockCodewords = ecBlocks.getECCodewordsPerBlock() + numDataCodewords;
        result[numResultBlocks++] = new DataBlock(numDataCodewords, new byte[numBlockCodewords]);
      }
    }

    // All blocks have the same amount of data, except that the last n
    // (where n may be 0) have 1 more byte. Figure out where these start.
    int shorterBlocksTotalCodewords = result[0].codewords.length;
    int longerBlocksStartAt = result.length - 1;
    while (longerBlocksStartAt >= 0) {
      int numCodewords = result[longerBlocksStartAt].codewords.length;
      if (numCodewords == shorterBlocksTotalCodewords) {
        break;
      }
      longerBlocksStartAt--;
    }
    longerBlocksStartAt++;

    int shorterBlocksNumDataCodewords = shorterBlocksTotalCodewords - ecBlocks.getECCodewordsPerBlock();
    // The last elements of result may be 1 element longer;
    // first fill out as many elements as all of them have
    int rawCodewordsOffset = 0;
    for (int i = 0; i < shorterBlocksNumDataCodewords; i++) {
      for (int j = 0; j < numResultBlocks; j++) {
        result[j].codewords[i] = rawCodewords[rawCodewordsOffset++];
      }
    }
    // Fill out the last data block in the longer ones
    for (int j = longerBlocksStartAt; j < numResultBlocks; j++) {
      result[j].codewords[shorterBlocksNumDataCodewords] = rawCodewords[rawCodewordsOffset++];
    }
    // Now add in error correction blocks
    int max = result[0].codewords.length;
    for (int i = shorterBlocksNumDataCodewords; i < max; i++) {
      for (int j = 0; j < numResultBlocks; j++) {
        int iOffset = j < longerBlocksStartAt ? i : i + 1;
        result[j].codewords[iOffset] = rawCodewords[rawCodewordsOffset++];
      }
    }
    return result;
  }

  int getNumDataCodewords() {
    return numDataCodewords;
  }

  byte[] getCodewords() {
    return codewords;
  }

}
