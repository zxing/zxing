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

import com.google.zxing.ReaderException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.reedsolomon.ReedSolomonDecoder;
import com.google.zxing.common.reedsolomon.ReedSolomonException;

/**
 * @author srowen@google.com (Sean Owen)
 */
public final class Decoder {

  private Decoder() {
  }

  public static String decode(boolean[][] image) throws ReaderException {
    int dimension = image.length;
    BitMatrix bits = new BitMatrix(dimension);
    for (int i = 0; i < dimension; i++) {
      for (int j = 0; j < dimension; j++) {
        if (image[i][j]) {
          bits.set(i, j);
        }
      }
    }
    return decode(bits);
  }

  public static String decode(BitMatrix bits) throws ReaderException {
    BitMatrixParser parser = new BitMatrixParser(bits);
    Version version = parser.readVersion();
    ErrorCorrectionLevel ecLevel = parser.readFormatInformation().getErrorCorrectionLevel();
    byte[] codewords = parser.readCodewords();
    DataBlock[] dataBlocks = DataBlock.getDataBlocks(codewords, version, ecLevel);
    int totalBytes = 0;
    for (int i = 0; i < dataBlocks.length; i++) {
      totalBytes += dataBlocks[i].getNumDataCodewords();
    }
    byte[] resultBytes = new byte[totalBytes];
    int resultOffset = 0;
    for (int j = 0; j < dataBlocks.length; j++) {
      DataBlock dataBlock = dataBlocks[j];
      byte[] codewordBytes = dataBlock.getCodewords();
      int numDataCodewords = dataBlock.getNumDataCodewords();
      correctErrors(codewordBytes, numDataCodewords);
      for (int i = 0; i < numDataCodewords; i++) {
        resultBytes[resultOffset++] = codewordBytes[i];
      }
    }

    return DecodedBitStreamParser.decode(resultBytes, version);
  }

  private static void correctErrors(byte[] codewordBytes, int numDataCodewords)
      throws ReaderException {
    int numCodewords = codewordBytes.length;
    int[] codewordsInts = new int[numCodewords];
    for (int i = 0; i < numCodewords; i++) {
      codewordsInts[i] = codewordBytes[i] & 0xFF;
    }
    int numECCodewords = codewordBytes.length - numDataCodewords;
    try {
      ReedSolomonDecoder.decode(codewordsInts, numECCodewords);
    } catch (ReedSolomonException rse) {
      throw new ReaderException(rse.toString());
    }
    for (int i = 0; i < numDataCodewords; i++) {
      codewordBytes[i] = (byte) codewordsInts[i];
    }
  }

}
