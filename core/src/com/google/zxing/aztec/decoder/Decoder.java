/*
 * Copyright 2010 ZXing authors
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

package com.google.zxing.aztec.decoder;

import com.google.zxing.FormatException;
import com.google.zxing.aztec.AztecDetectorResult;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.DecoderResult;
import com.google.zxing.common.reedsolomon.GenericGF;
import com.google.zxing.common.reedsolomon.ReedSolomonDecoder;
import com.google.zxing.common.reedsolomon.ReedSolomonException;

/**
 * <p>The main class which implements Aztec Code decoding -- as opposed to locating and extracting
 * the Aztec Code from an image.</p>
 *
 * @author David Olivier
 */
public final class Decoder {

  private enum Table {
    UPPER,
    LOWER,
    MIXED,
    DIGIT,
    PUNCT,
    BINARY
  }

  private static final int[] NB_BITS_COMPACT = {
      0, 104, 240, 408, 608
  };

  private static final int[] NB_BITS = {
      0, 128, 288, 480, 704, 960, 1248, 1568, 1920, 2304, 2720, 3168, 3648, 4160, 4704, 5280, 5888, 6528,
      7200, 7904, 8640, 9408, 10208, 11040, 11904, 12800, 13728, 14688, 15680, 16704, 17760, 18848, 19968
  };

  private static final int[] NB_DATABLOCK_COMPACT = {
      0, 17, 40, 51, 76
  };

  private static final int[] NB_DATABLOCK = {
      0, 21, 48, 60, 88, 120, 156, 196, 240, 230, 272, 316, 364, 416, 470, 528, 588, 652, 720, 790, 864,
      940, 1020, 920, 992, 1066, 1144, 1224, 1306, 1392, 1480, 1570, 1664
  };

  private static final String[] UPPER_TABLE = {
      "CTRL_PS", " ", "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P",
      "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z", "CTRL_LL", "CTRL_ML", "CTRL_DL", "CTRL_BS"
  };

  private static final String[] LOWER_TABLE = {
      "CTRL_PS", " ", "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p",
      "q", "r", "s", "t", "u", "v", "w", "x", "y", "z", "CTRL_US", "CTRL_ML", "CTRL_DL", "CTRL_BS"
  };

  private static final String[] MIXED_TABLE = {
      "CTRL_PS", " ", "\1", "\2", "\3", "\4", "\5", "\6", "\7", "\b", "\t", "\n",
      "\13", "\f", "\r", "\33", "\34", "\35", "\36", "\37", "@", "\\", "^", "_",
      "`", "|", "~", "\177", "CTRL_LL", "CTRL_UL", "CTRL_PL", "CTRL_BS"
  };

  private static final String[] PUNCT_TABLE = {
      "", "\r", "\r\n", ". ", ", ", ": ", "!", "\"", "#", "$", "%", "&", "'", "(", ")",
      "*", "+", ",", "-", ".", "/", ":", ";", "<", "=", ">", "?", "[", "]", "{", "}", "CTRL_UL"
  };

  private static final String[] DIGIT_TABLE = {
      "CTRL_PS", " ", "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", ",", ".", "CTRL_UL", "CTRL_US"
  };

  private int numCodewords;
  private int codewordSize;
  private AztecDetectorResult ddata;
  private int invertedBitCount;

  public DecoderResult decode(AztecDetectorResult detectorResult) throws FormatException {
    ddata = detectorResult;
    BitMatrix matrix = detectorResult.getBits();

    if (!ddata.isCompact()) {
      matrix = removeDashedLines(ddata.getBits());
    }

    boolean[] rawbits = extractBits(matrix);

    boolean[] correctedBits = correctBits(rawbits);

    String result = getEncodedData(correctedBits);

    return new DecoderResult(null, result, null, null);
  }

  /**
   * Gets the string encoded in the aztec code bits
   *
   * @return the decoded string
   * @throws FormatException if the input is not valid
   */
  private String getEncodedData(boolean[] correctedBits) throws FormatException {

    int endIndex = codewordSize * ddata.getNbDatablocks() - invertedBitCount;
    if (endIndex > correctedBits.length) {
      throw FormatException.getFormatInstance();
    }

    Table lastTable = Table.UPPER;
    Table table = Table.UPPER;
    int startIndex = 0;
    StringBuilder result = new StringBuilder(20);
    boolean end = false;
    boolean shift = false;
    boolean switchShift = false;
    boolean binaryShift = false;

    while (!end) {

      if (shift) {
        // the table is for the next character only
        switchShift = true;
      } else {
        // save the current table in case next one is a shift
        lastTable = table;
      }

      int code;
      if (binaryShift) {
        if (endIndex - startIndex < 5) {
          break;
        }

        int length = readCode(correctedBits, startIndex, 5);
        startIndex += 5;
        if (length == 0) {
          if (endIndex - startIndex < 11) {
            break;
          }

          length = readCode(correctedBits, startIndex, 11) + 31;
          startIndex += 11;
        }
        for (int charCount = 0; charCount < length; charCount++) {
          if (endIndex - startIndex < 8) {
            end = true;
            break;
          }

          code = readCode(correctedBits, startIndex, 8);
          result.append((char) code);
          startIndex += 8;
        }
        binaryShift = false;
      } else {
        if (table == Table.BINARY) {
          if (endIndex - startIndex < 8) {
            break;
          }
          code = readCode(correctedBits, startIndex, 8);
          startIndex += 8;

          result.append((char) code);

        } else {
          int size = 5;

          if (table == Table.DIGIT) {
            size = 4;
          }

          if (endIndex - startIndex < size) {
            break;
          }

          code = readCode(correctedBits, startIndex, size);
          startIndex += size;

          String str = getCharacter(table, code);
          if (str.startsWith("CTRL_")) {
            // Table changes
            table = getTable(str.charAt(5));

            if (str.charAt(6) == 'S') {
              shift = true;
              if (str.charAt(5) == 'B') {
                binaryShift = true;
              }
            }
          } else {
            result.append(str);
          }


        }
      }

      if (switchShift) {
        table = lastTable;
        shift = false;
        switchShift = false;
      }

    }
    return result.toString();
  }


  /**
   * gets the table corresponding to the char passed
   */
  private static Table getTable(char t) {
    switch (t) {
      case 'L':
        return Table.LOWER;
      case 'P':
        return Table.PUNCT;
      case 'M':
        return Table.MIXED;
      case 'D':
        return Table.DIGIT;
      case 'B':
        return Table.BINARY;
      case 'U':
      default:
        return Table.UPPER;
    }
  }

  /**
   * Gets the character (or string) corresponding to the passed code in the given table
   *
   * @param table the table used
   * @param code the code of the character
   */
  private static String getCharacter(Table table, int code) {
    switch (table) {
      case UPPER:
        return UPPER_TABLE[code];
      case LOWER:
        return LOWER_TABLE[code];
      case MIXED:
        return MIXED_TABLE[code];
      case PUNCT:
        return PUNCT_TABLE[code];
      case DIGIT:
        return DIGIT_TABLE[code];
      default:
        return "";
    }
  }

  /**
   * <p>Performs RS error correction on an array of bits.</p>
   *
   * @return the corrected array
   * @throws FormatException if the input contains too many errors
   */
  private boolean[] correctBits(boolean[] rawbits) throws FormatException {
    GenericGF gf;

    if (ddata.getNbLayers() <= 2) {
      codewordSize = 6;
      gf = GenericGF.AZTEC_DATA_6;
    } else if (ddata.getNbLayers() <= 8) {
      codewordSize = 8;
      gf = GenericGF.AZTEC_DATA_8;
    } else if (ddata.getNbLayers() <= 22) {
      codewordSize = 10;
      gf = GenericGF.AZTEC_DATA_10;
    } else {
      codewordSize = 12;
      gf = GenericGF.AZTEC_DATA_12;
    }

    int numDataCodewords = ddata.getNbDatablocks();
    int numECCodewords;
    int offset;

    if (ddata.isCompact()) {
      offset = NB_BITS_COMPACT[ddata.getNbLayers()] - numCodewords * codewordSize;
      numECCodewords = NB_DATABLOCK_COMPACT[ddata.getNbLayers()] - numDataCodewords;
    } else {
      offset = NB_BITS[ddata.getNbLayers()] - numCodewords * codewordSize;
      numECCodewords = NB_DATABLOCK[ddata.getNbLayers()] - numDataCodewords;
    }

    int[] dataWords = new int[numCodewords];
    for (int i = 0; i < numCodewords; i++) {
      int flag = 1;
      for (int j = 1; j <= codewordSize; j++) {
        if (rawbits[codewordSize * i + codewordSize - j + offset]) {
          dataWords[i] += flag;
        }
        flag <<= 1;
      }

      //if (dataWords[i] >= flag) {
      //  flag++;
      //}
    }

    try {
      ReedSolomonDecoder rsDecoder = new ReedSolomonDecoder(gf);
      rsDecoder.decode(dataWords, numECCodewords);
    } catch (ReedSolomonException rse) {
      throw FormatException.getFormatInstance();
    }

    offset = 0;
    invertedBitCount = 0;

    boolean[] correctedBits = new boolean[numDataCodewords * codewordSize];
    for (int i = 0; i < numDataCodewords; i++) {

      boolean seriesColor = false;
      int seriesCount = 0;
      int flag = 1 << (codewordSize - 1);

      for (int j = 0; j < codewordSize; j++) {

        boolean color = (dataWords[i] & flag) == flag;

        if (seriesCount == codewordSize - 1) {

          if (color == seriesColor) {
            //bit must be inverted
            throw FormatException.getFormatInstance();
          }

          seriesColor = false;
          seriesCount = 0;
          offset++;
          invertedBitCount++;
        } else {

          if (seriesColor == color) {
            seriesCount++;
          } else {
            seriesCount = 1;
            seriesColor = color;
          }

          correctedBits[i * codewordSize + j - offset] = color;

        }

        flag >>>= 1;
      }
    }

    return correctedBits;
  }

  /**
   * Gets the array of bits from an Aztec Code matrix
   *
   * @return the array of bits
   * @throws FormatException if the matrix is not a valid aztec code
   */
  private boolean[] extractBits(BitMatrix matrix) throws FormatException {

    boolean[] rawbits;
    if (ddata.isCompact()) {
      if (ddata.getNbLayers() > NB_BITS_COMPACT.length) {
        throw FormatException.getFormatInstance();
      }
      rawbits = new boolean[NB_BITS_COMPACT[ddata.getNbLayers()]];
      numCodewords = NB_DATABLOCK_COMPACT[ddata.getNbLayers()];
    } else {
      if (ddata.getNbLayers() > NB_BITS.length) {
        throw FormatException.getFormatInstance();
      }
      rawbits = new boolean[NB_BITS[ddata.getNbLayers()]];
      numCodewords = NB_DATABLOCK[ddata.getNbLayers()];
    }

    int layer = ddata.getNbLayers();
    int size = matrix.getHeight();
    int rawbitsOffset = 0;
    int matrixOffset = 0;

    while (layer != 0) {

      int flip = 0;
      for (int i = 0; i < 2 * size - 4; i++) {
        rawbits[rawbitsOffset + i] = matrix.get(matrixOffset + flip, matrixOffset + i / 2);
        rawbits[rawbitsOffset + 2 * size - 4 + i] = matrix.get(matrixOffset + i / 2, matrixOffset + size - 1 - flip);
        flip = (flip + 1) % 2;
      }

      flip = 0;
      for (int i = 2 * size + 1; i > 5; i--) {
        rawbits[rawbitsOffset + 4 * size - 8 + (2 * size - i) + 1] =
            matrix.get(matrixOffset + size - 1 - flip, matrixOffset + i / 2 - 1);
        rawbits[rawbitsOffset + 6 * size - 12 + (2 * size - i) + 1] =
            matrix.get(matrixOffset + i / 2 - 1, matrixOffset + flip);
        flip = (flip + 1) % 2;
      }

      matrixOffset += 2;
      rawbitsOffset += 8 * size - 16;
      layer--;
      size -= 4;
    }

    return rawbits;
  }

  /**
   * Transforms an Aztec code matrix by removing the control dashed lines
   */
  private static BitMatrix removeDashedLines(BitMatrix matrix) {
    int nbDashed = 1 + 2 * ((matrix.getWidth() - 1) / 2 / 16);
    BitMatrix newMatrix = new BitMatrix(matrix.getWidth() - nbDashed, matrix.getHeight() - nbDashed);

    int nx = 0;

    for (int x = 0; x < matrix.getWidth(); x++) {

      if ((matrix.getWidth() / 2 - x) % 16 == 0) {
        continue;
      }

      int ny = 0;
      for (int y = 0; y < matrix.getHeight(); y++) {

        if ((matrix.getWidth() / 2 - y) % 16 == 0) {
          continue;
        }

        if (matrix.get(x, y)) {
          newMatrix.set(nx, ny);
        }
        ny++;
      }
      nx++;
    }

    return newMatrix;
  }

  /**
   * Reads a code of given length and at given index in an array of bits
   */
  private static int readCode(boolean[] rawbits, int startIndex, int length) {
    int res = 0;

    for (int i = startIndex; i < startIndex + length; i++) {
      res <<= 1;
      if (rawbits[i]) {
        res++;
      }
    }

    return res;
  }

}
