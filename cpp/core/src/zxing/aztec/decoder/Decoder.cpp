// -*- mode:c++; tab-width:2; indent-tabs-mode:nil; c-basic-offset:2 -*-
/*
 *  Decoder.cpp
 *  zxing
 *
 *  Created by Lukas Stabe on 08/02/2012.
 *  Copyright 2012 ZXing authors All rights reserved.
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

#include <zxing/aztec/decoder/Decoder.h>
#ifndef NO_ICONV
#include <iconv.h>
#endif
#include <iostream>
#include <zxing/FormatException.h>
#include <zxing/common/reedsolomon/ReedSolomonDecoder.h>
#include <zxing/common/reedsolomon/ReedSolomonException.h>
#include <zxing/common/reedsolomon/GenericGF.h>
#include <zxing/common/IllegalArgumentException.h>

using zxing::aztec::Decoder;
using zxing::DecoderResult;
using zxing::String;
using zxing::BitArray;
using zxing::BitMatrix;
using zxing::Ref;

using std::string;

namespace {
  void add(string& result, unsigned char character) {
#ifndef NO_ICONV
    char s[] = { character & 0xff };
    char* ss = s;
    size_t sl = sizeof(s);
    char d[4];
    char* ds = d;
    size_t dl = sizeof(d);
    iconv_t ic = iconv_open("UTF-8", "ISO-8859-1");
    iconv(ic, &ss, &sl, &ds, &dl);
    iconv_close(ic);
    d[sizeof(d)-dl] = 0;
    result.append(d);
#else
    result.push_back(character);
#endif
  }

  const int NB_BITS_COMPACT[] = {
    0, 104, 240, 408, 608
  };
        
  const int NB_BITS[] = {
    0, 128, 288, 480, 704, 960, 1248, 1568, 1920, 2304, 2720, 3168, 3648, 4160, 4704, 5280, 5888, 6528,
    7200, 7904, 8640, 9408, 10208, 11040, 11904, 12800, 13728, 14688, 15680, 16704, 17760, 18848, 19968
  };
        
  const int NB_DATABLOCK_COMPACT[] = {
    0, 17, 40, 51, 76
  };
        
  const int NB_DATABLOCK[] = {
    0, 21, 48, 60, 88, 120, 156, 196, 240, 230, 272, 316, 364, 416, 470, 528, 588, 652, 720, 790, 864,
    940, 1020, 920, 992, 1066, 1144, 1224, 1306, 1392, 1480, 1570, 1664
  };
        
  const char* UPPER_TABLE[] = {
    "CTRL_PS", " ", "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P",
    "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z", "CTRL_LL", "CTRL_ML", "CTRL_DL", "CTRL_BS"
  };
        
  const char* LOWER_TABLE[] = {
    "CTRL_PS", " ", "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p",
    "q", "r", "s", "t", "u", "v", "w", "x", "y", "z", "CTRL_US", "CTRL_ML", "CTRL_DL", "CTRL_BS"
  };
        
  const char* MIXED_TABLE[] = {
    "CTRL_PS", " ", "\1", "\2", "\3", "\4", "\5", "\6", "\7", "\b", "\t", "\n",
    "\13", "\f", "\r", "\33", "\34", "\35", "\36", "\37", "@", "\\", "^", "_",
    "`", "|", "~", "\177", "CTRL_LL", "CTRL_UL", "CTRL_PL", "CTRL_BS"
  };
        
  const char* PUNCT_TABLE[] = {
    "", "\r", "\r\n", ". ", ", ", ": ", "!", "\"", "#", "$", "%", "&", "'", "(", ")",
    "*", "+", ",", "-", ".", "/", ":", ";", "<", "=", ">", "?", "[", "]", "{", "}", "CTRL_UL"
  };
        
  const char* DIGIT_TABLE[] = {
    "CTRL_PS", " ", "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", ",", ".", "CTRL_UL", "CTRL_US"
  };
}
        
Decoder::Table Decoder::getTable(char t) {
  switch (t) {
  case 'L':
    return LOWER;
  case 'P':
    return PUNCT;
  case 'M':
    return MIXED;
  case 'D':
    return DIGIT;
  case 'B':
    return BINARY;
  case 'U':
  default:
    return UPPER;
  }
}
        
const char* Decoder::getCharacter(zxing::aztec::Decoder::Table table, int code) {
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
        
Decoder::Decoder() {
  // nothing
}
        
Ref<DecoderResult> Decoder::decode(Ref<zxing::aztec::AztecDetectorResult> detectorResult) {
  ddata_ = detectorResult;
            
  // std::printf("getting bits\n");
            
  Ref<BitMatrix> matrix = detectorResult->getBits();
            
  if (!ddata_->isCompact()) {
    // std::printf("removing lines\n");
    matrix = removeDashedLines(ddata_->getBits());
  }
            
  // std::printf("extracting bits\n");
  Ref<BitArray> rawbits = extractBits(matrix);
            
  // std::printf("correcting bits\n");
  Ref<BitArray> aCorrectedBits = correctBits(rawbits);
            
  // std::printf("decoding bits\n");
  Ref<String> result = getEncodedData(aCorrectedBits);
            
  // std::printf("constructing array\n");
  ArrayRef<unsigned char> arrayOut(aCorrectedBits->getSize());
  for (int i = 0; i < aCorrectedBits->count(); i++) {
    arrayOut[i] = (unsigned char)aCorrectedBits->get(i);
  }
            
  // std::printf("returning\n");
            
  return Ref<DecoderResult>(new DecoderResult(arrayOut, result));
}
        
Ref<String> Decoder::getEncodedData(Ref<zxing::BitArray> correctedBits) {
  int endIndex = codewordSize_ * ddata_->getNBDatablocks() - invertedBitCount_;
  if (endIndex > (int)correctedBits->getSize()) {
    // std::printf("invalid input\n");
    throw FormatException("invalid input data");
  }
            
  Table lastTable = UPPER;
  Table table = UPPER;
  int startIndex = 0;
  std::string result;
  bool end = false;
  bool shift = false;
  bool switchShift = false;
  bool binaryShift = false;
            
  while (!end) {
    // std::printf("decoooooding\n");
                
    if (shift) {
      switchShift = true;
    } else {
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
        add(result, code);
        startIndex += 8;
      }
      binaryShift = false;
    } else {
      if (table == BINARY) {
        if (endIndex - startIndex < 8) {
          break;
        }
        code = readCode(correctedBits, startIndex, 8);
        startIndex += 8;
                        
        add(result, code);
      } else {
        int size = 5;
                        
        if (table == DIGIT) {
          size = 4;
        }
                        
        if (endIndex - startIndex < size) {
          break;
        }
                        
        code = readCode(correctedBits, startIndex, size);
        startIndex += size;
                        
        const char *str = getCharacter(table, code);
        std::string string(str);
        if ((int)string.find("CTRL_") != -1) {
          table = getTable(str[5]);
                            
          if (str[6] == 'S') {
            shift = true;
            if (str[5] == 'B') {
              binaryShift = true;
            }
          }
        } else {
          result.append(string);
        }
                        
      }
    }
                
    if (switchShift) {
      table = lastTable;
      shift = false;
      switchShift = false;
    }
                
                
  }
            
  return Ref<String>(new String(result));
            
}
        
Ref<BitArray> Decoder::correctBits(Ref<zxing::BitArray> rawbits) {
  //return rawbits;
  // std::printf("decoding stuff:%d datablocks in %d layers\n", ddata_->getNBDatablocks(), ddata_->getNBLayers());
            
  Ref<GenericGF> gf = GenericGF::AZTEC_DATA_6;
            
  if (ddata_->getNBLayers() <= 2) {
    codewordSize_ = 6;
    gf = GenericGF::AZTEC_DATA_6;
  } else if (ddata_->getNBLayers() <= 8) {
    codewordSize_ = 8;
    gf = GenericGF::AZTEC_DATA_8;
  } else if (ddata_->getNBLayers() <= 22) {
    codewordSize_ = 10;
    gf = GenericGF::AZTEC_DATA_10;
  } else {
    codewordSize_ = 12;
    gf = GenericGF::AZTEC_DATA_12;
  }
            
  int numDataCodewords = ddata_->getNBDatablocks();
  int numECCodewords;
  int offset;
            
  if (ddata_->isCompact()) {
    offset = NB_BITS_COMPACT[ddata_->getNBLayers()] - numCodewords_ * codewordSize_;
    numECCodewords = NB_DATABLOCK_COMPACT[ddata_->getNBLayers()] - numDataCodewords;
  } else {
    offset = NB_BITS[ddata_->getNBLayers()] - numCodewords_ * codewordSize_;
    numECCodewords = NB_DATABLOCK[ddata_->getNBLayers()] - numDataCodewords;
  }
            
  ArrayRef<int> dataWords(numCodewords_);
            
  for (int i = 0; i < numCodewords_; i++) {
    int flag = 1;
    for (int j = 1; j <= codewordSize_; j++) {
      if (rawbits->get(codewordSize_ * i + codewordSize_ - j + offset)) {
        dataWords[i] += flag;
      }
      flag <<= 1;
    }
                
    //
    //
    //
  }
            
  try {
    // std::printf("trying reed solomon, numECCodewords:%d\n", numECCodewords);
    ReedSolomonDecoder rsDecoder(gf);
    rsDecoder.decode(dataWords, numECCodewords);
  } catch (ReedSolomonException rse) {
    // std::printf("got reed solomon exception:%s, throwing formatexception\n", rse.what());
    throw FormatException("rs decoding failed");
  } catch (IllegalArgumentException iae) {
    // std::printf("illegal argument exception: %s", iae.what());
  }
            
  offset = 0;
  invertedBitCount_ = 0;
            
  Ref<BitArray> correctedBits(new BitArray(numDataCodewords * codewordSize_));
  for (int i = 0; i < numDataCodewords; i++) {
                
    bool seriesColor = false;
    int seriesCount = 0;
    int flag = 1 << (codewordSize_ - 1);
                
    for (int j = 0; j < codewordSize_; j++) {
                    
      bool color = (dataWords[i] & flag) == flag;
                    
      if (seriesCount == codewordSize_ - 1) {
                        
        if (color == seriesColor) {
          throw FormatException("bit was not inverted");
        }
                        
        seriesColor = false;
        seriesCount = 0;
        offset++;
        invertedBitCount_++;
                        
      } else {
                        
        if (seriesColor == color) {
          seriesCount++;
        } else {
          seriesCount = 1;
          seriesColor = color;
        }
                        
        if (color) correctedBits->set(i * codewordSize_ + j - offset);
                        
      }
                    
      flag = (unsigned int)flag >> 1;
                    
    }
  }
            
  return correctedBits;
}
        
Ref<BitArray> Decoder::extractBits(Ref<zxing::BitMatrix> matrix) {
  std::vector<bool> rawbits;
            
  if (ddata_->isCompact()) {
    if (ddata_->getNBLayers() > 5) { //NB_BITS_COMPACT length
      throw FormatException("data is too long");
    }
    rawbits = std::vector<bool>(NB_BITS_COMPACT[ddata_->getNBLayers()]);
    numCodewords_ = NB_DATABLOCK_COMPACT[ddata_->getNBLayers()];
  } else {
    if (ddata_->getNBLayers() > 33) { //NB_BITS length
      throw FormatException("data is too long");
    }
    rawbits = std::vector<bool>(NB_BITS[ddata_->getNBLayers()]);
    numCodewords_ = NB_DATABLOCK[ddata_->getNBLayers()];
  }
            
  int layer = ddata_->getNBLayers();
  int size = matrix->getHeight();
  int rawbitsOffset = 0;
  int matrixOffset = 0;
            

  while (layer != 0) {
                
    int flip = 0;
    for (int i = 0; i < 2 * size - 4; i++) {
      rawbits[rawbitsOffset + i] = matrix->get(matrixOffset + flip, matrixOffset + i / 2);
      rawbits[rawbitsOffset + 2 * size - 4 + i] = matrix->get(matrixOffset + i / 2, matrixOffset + size - 1 - flip);
      flip = (flip + 1) % 2;
    }
                
    flip = 0;
    for (int i = 2 * size + 1; i > 5; i--) {
      rawbits[rawbitsOffset + 4 * size - 8 + (2 * size - i) + 1] =
        matrix->get(matrixOffset + size - 1 - flip, matrixOffset + i / 2 - 1);
      rawbits[rawbitsOffset + 6 * size - 12 + (2 * size - i) + 1] =
        matrix->get(matrixOffset + i / 2 - 1, matrixOffset + flip);
      flip = (flip + 1) % 2;
    }
                
    matrixOffset += 2;
    rawbitsOffset += 8 * size - 16;
    layer--;
    size -= 4;
                
  }
            
  Ref<BitArray> returnValue(new BitArray(rawbits.size()));
  for (int i = 0; i < (int)rawbits.size(); i++) {
    if (rawbits[i]) returnValue->set(i);
  }
            
  return returnValue;
            
}
        
Ref<BitMatrix> Decoder::removeDashedLines(Ref<zxing::BitMatrix> matrix) {
  int nbDashed = 1 + 2 * ((matrix->getWidth() - 1) / 2 / 16);
  Ref<BitMatrix> newMatrix(new BitMatrix(matrix->getWidth() - nbDashed, matrix->getHeight() - nbDashed));
            
  int nx = 0;
            
  for (int x = 0; x < (int)matrix->getWidth(); x++) {
                
    if ((matrix->getWidth() / 2 - x) % 16 == 0) {
      continue;
    }
                
    int ny = 0;
    for (int y = 0; y < (int)matrix->getHeight(); y++) {
                    
      if ((matrix->getWidth() / 2 - y) % 16 == 0) {
        continue;
      }
                    
      if (matrix->get(x, y)) {
        newMatrix->set(nx, ny);
      }
      ny++;
                    
    }
    nx++;
                
  }
  return newMatrix;
}
        
int Decoder::readCode(Ref<zxing::BitArray> rawbits, int startIndex, int length) {
  int res = 0;
            
  for (int i = startIndex; i < startIndex + length; i++) {
    res <<= 1;
    if (rawbits->get(i)) {
      res ++;
    }
  }
            
  return res;
}
