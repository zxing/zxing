/*
 *  BitMatrixParser.cpp
 *  zxing
 *
 *  Created by Christian Brunschen on 20/05/2008.
 *  Copyright 2008 ZXing authors All rights reserved.
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

#include "BitMatrixParser.h"
#include "DataMask.h"

namespace qrcode {
  namespace decoder {
    
    using namespace common;
    
    int BitMatrixParser::copyBit(size_t i, size_t j, int versionBits) {
      return bitMatrix_->get(i, j) ? 
        (versionBits << 1) | 0x1 
        : versionBits << 1;
    }
    
    BitMatrixParser::BitMatrixParser(Ref<BitMatrix> bitMatrix) : 
    bitMatrix_(bitMatrix), parsedVersion_(0), parsedFormatInfo_() {
      int dimension = bitMatrix->getDimension();
      if ((dimension < 21) || (dimension & 0x03) != 1) {
        throw new ReaderException("Dimension must be 1 mod 4 and >= 21");
      }
    }
    
    Ref<FormatInformation> BitMatrixParser::readFormatInformation() {
      if (parsedFormatInfo_ != 0) {
        return parsedFormatInfo_;
      }
      
      // Read top-left format info bits
      int formatInfoBits = 0;
      for (int j = 0; j < 6; j++) {
        formatInfoBits = copyBit(8, j, formatInfoBits);
      }
      // .. and skip a bit in the timing pattern ...
      formatInfoBits = copyBit(8, 7, formatInfoBits);
      formatInfoBits = copyBit(8, 8, formatInfoBits);
      formatInfoBits = copyBit(7, 8, formatInfoBits);
      // .. and skip a bit in the timing pattern ...
      for (int i = 5; i >= 0; i--) {
        formatInfoBits = copyBit(i, 8, formatInfoBits);
      }
      
      parsedFormatInfo_ = FormatInformation::decodeFormatInformation(formatInfoBits);
      if (parsedFormatInfo_ != 0) {
        return parsedFormatInfo_;
      }
      
      // Hmm, failed. Try the top-right/bottom-left pattern
      int dimension = bitMatrix_->getDimension();
      formatInfoBits = 0;
      int iMin = dimension - 8;
      for (int i = dimension - 1; i >= iMin; i--) {
        formatInfoBits = copyBit(i, 8, formatInfoBits);
      }
      for (int j = dimension - 7; j < dimension; j++) {
        formatInfoBits = copyBit(8, j, formatInfoBits);
      }
      
      parsedFormatInfo_ = FormatInformation::decodeFormatInformation(formatInfoBits);
      if (parsedFormatInfo_ != 0) {
        return parsedFormatInfo_;
      }
      throw new ReaderException("Could not decode format information");
    }
    
    Version *BitMatrixParser::readVersion() {
      if (parsedVersion_ != 0) {
        return parsedVersion_;
      }
      
      int dimension = bitMatrix_->getDimension();
      
      int provisionalVersion = (dimension - 17) >> 2;
      if (provisionalVersion <= 6) {
        return Version::getVersionForNumber(provisionalVersion);
      }
      
      // Read top-right version info: 3 wide by 6 tall
      int versionBits = 0;
      for (int i = 5; i >= 0; i--) {
        int jMin = dimension - 11;
        for (int j = dimension - 9; j >= jMin; j--) {
          versionBits = copyBit(i, j, versionBits);
        }
      }
      
      parsedVersion_ = Version::decodeVersionInformation(versionBits);
      if (parsedVersion_ != 0) {
        return parsedVersion_;
      }
      
      // Hmm, failed. Try bottom left: 6 wide by 3 tall
      versionBits = 0;
      for (int j = 5; j >= 0; j--) {
        int iMin = dimension - 11;
        for (int i = dimension - 9; i >= iMin; i--) {
          versionBits = copyBit(i, j, versionBits);
        }
      }
      
      parsedVersion_ = Version::decodeVersionInformation(versionBits);
      if (parsedVersion_ != 0) {
        return parsedVersion_;
      }
      throw new ReaderException("Could not decode version");
    }
    
    ArrayRef<unsigned char> BitMatrixParser::readCodewords() {
      Ref<FormatInformation> formatInfo = readFormatInformation();
      Version *version = readVersion();
      
      // Get the data mask for the format used in this QR Code. This will exclude
      // some bits from reading as we wind through the bit matrix.
      DataMask &dataMask = DataMask::forReference((int) formatInfo->getDataMask());
      int dimension = bitMatrix_->getDimension();
      dataMask.unmaskBitMatrix(bitMatrix_->getBits(), dimension);
      
      Ref<BitMatrix> functionPattern = version->buildFunctionPattern();
      
      bool readingUp = true;
      ArrayRef<unsigned char> result(version->getTotalCodewords());
      int resultOffset = 0;
      int currentByte = 0;
      int bitsRead = 0;
      // Read columns in pairs, from right to left
      size_t D = functionPattern.object_->dimension_;
      for (int j = dimension - 1; j > 0; j -= 2) {
        if (j == 6) {
          // Skip whole column with vertical alignment pattern;
          // saves time and makes the other code proceed more cleanly
          j--;
        }
        // Read alternatingly from bottom to top then top to bottom
        for (int count = 0; count < dimension; count++) {
          int i = readingUp ? dimension - 1 - count : count;
          for (int col = 0; col < 2; col++) {
            // Ignore bits covered by the function pattern
            if (functionPattern.object_->dimension_ != D) {
              cout << "function pattern dimension changed from " << D << " to " << functionPattern.object_->dimension_ << "\n";
              D = functionPattern.object_->dimension_;
            }
            if (!functionPattern->get(i, j - col)) {
              // Read a bit
              bitsRead++;
              currentByte <<= 1;
              if (bitMatrix_->get(i, j - col)) {
                currentByte |= 1;
              }
              // If we've made a whole byte, save it off
              if (bitsRead == 8) {
                result[resultOffset++] = (unsigned char) currentByte;
                bitsRead = 0;
                currentByte = 0;
              }
            }
          }
        }
        readingUp = !readingUp; // switch directions
      }
      if (resultOffset != version->getTotalCodewords()) {
        throw new ReaderException("Did not read all codewords");
      }
      return result;
    }
    
  }
}
