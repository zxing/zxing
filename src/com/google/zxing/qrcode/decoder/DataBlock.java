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

package com.google.zxing.qrcode.decoder;

/**
 * @author srowen@google.com (Sean Owen)
 */
final class DataBlock {

  private final int numDataCodewords;
  private final byte[] codewords;

  private DataBlock(int numDataCodewords, byte[] codewords) {
    this.numDataCodewords = numDataCodewords;
    this.codewords = codewords;
  }

  static DataBlock[] getDataBlocks(byte[] rawCodewords,
                                   Version version,
                                   ErrorCorrectionLevel ecLevel) {
    Version.ECBlocks ecBlocks = version.getECBlocksForLevel(ecLevel);
    int totalBlocks = 0;
    Version.ECB[] ecBlockArray = ecBlocks.getECBlocks();
    for (int i = 0; i < ecBlockArray.length; i++) {
      totalBlocks += ecBlockArray[i].getCount();
    }
    DataBlock[] result = new DataBlock[totalBlocks];
    int numResultBlocks = 0;
    for (int j = 0; j < ecBlockArray.length; j++) {
      Version.ECB ecBlock = ecBlockArray[j];
      for (int i = 0; i < ecBlock.getCount(); i++) {
        int numDataCodewords = ecBlock.getDataCodewords();
        int numBlockCodewords = ecBlocks.getECCodewords() + numDataCodewords;
        result[numResultBlocks++] =
            new DataBlock(numDataCodewords, new byte[numBlockCodewords]);
      }
    }

    // All blocks have the same amount of data, except that the last n
    // (where n may be 0) have 1 more byte. Figure out where these start.
    int shorterBlocksTotalCodewords = result[0].codewords.length;
    int longerBlocksStartAt = result.length - 1;
    while (longerBlocksStartAt >= 0) {
      int numCodewords =
          result[longerBlocksStartAt].codewords.length;
      if (numCodewords == shorterBlocksTotalCodewords) {
        break;
      }
      if (numCodewords != shorterBlocksTotalCodewords + 1) {
        throw new IllegalStateException(
            "Data block sizes differ by more than 1");
      }
      longerBlocksStartAt--;
    }
    longerBlocksStartAt++;

    int shorterBlocksNumDataCodewords =
        shorterBlocksTotalCodewords - ecBlocks.getECCodewords();
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
      result[j].codewords[shorterBlocksNumDataCodewords] =
          rawCodewords[rawCodewordsOffset++];
    }
    // Now add in error correction blocks
    int max = result[0].codewords.length;
    for (int i = shorterBlocksNumDataCodewords; i < max; i++) {
      for (int j = 0; j < numResultBlocks; j++) {
        int iOffset = j < longerBlocksStartAt ? i : i + 1;
        result[j].codewords[iOffset] = rawCodewords[rawCodewordsOffset++];
      }
    }

    if (rawCodewordsOffset != rawCodewords.length) {
      throw new IllegalStateException();
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
