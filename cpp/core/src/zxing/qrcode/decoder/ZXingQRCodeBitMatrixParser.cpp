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

#include <zxing/qrcode/decoder/DataMask.h>    // for DataMask
#include <zxing/qrcode/decoder/ZXingQRCodeBitMatrixParser.h>

#include "zxing/ReaderException.h"            // for ReaderException
#include "zxing/common/Array.h"               // for ArrayRef
#include "zxing/common/BitMatrix.h"           // for BitMatrix
#include "zxing/common/Counted.h"             // for Ref
#include "zxing/qrcode/FormatInformation.h"   // for FormatInformation
#include "zxing/qrcode/ZXingQRCodeVersion.h"  // for Version


namespace pping {
namespace qrcode {

int BitMatrixParser::copyBit(size_t x, size_t y, int versionBits) {
  return bitMatrix_->get(x, y) ? (versionBits << 1) | 0x1 : versionBits << 1;
}

FallibleRef<BitMatrixParser> BitMatrixParser::createBitMatrixParser(Ref<BitMatrix> bitMatrix) MB_NOEXCEPT_EXCEPT_BADALLOC
{
    size_t dimension = bitMatrix->getDimension();

    if ((dimension < 21) || (dimension & 0x03) != 1)
        return failure<ReaderException>("Dimension must be 1 mod 4 and >= 21");

    return new BitMatrixParser(bitMatrix);
}

BitMatrixParser::BitMatrixParser(Ref<BitMatrix> bitMatrix) noexcept :
    bitMatrix_(bitMatrix), parsedVersion_(0), parsedFormatInfo_() {}

FallibleRef<FormatInformation> BitMatrixParser::readFormatInformation() MB_NOEXCEPT_EXCEPT_BADALLOC {
  if (parsedFormatInfo_ != 0) {
    return parsedFormatInfo_;
  }

  // Read top-left format info bits
  int formatInfoBits1 = 0;
  for (int i = 0; i < 6; i++) {
    formatInfoBits1 = copyBit(i, 8, formatInfoBits1);
  }
  // .. and skip a bit in the timing pattern ...
  formatInfoBits1 = copyBit(7, 8, formatInfoBits1);
  formatInfoBits1 = copyBit(8, 8, formatInfoBits1);
  formatInfoBits1 = copyBit(8, 7, formatInfoBits1);
  // .. and skip a bit in the timing pattern ...
  for (int j = 5; j >= 0; j--) {
    formatInfoBits1 = copyBit(8, j, formatInfoBits1);
  }

  // Read the top-right/bottom-left pattern
  int dimension = (int)bitMatrix_->getDimension();
  int formatInfoBits2 = 0;
  int jMin = dimension - 7;
  for (int j = dimension - 1; j >= jMin; j--) {
    formatInfoBits2 = copyBit(8, j, formatInfoBits2);
  }
  for (int i = dimension - 8; i < dimension; i++) {
    formatInfoBits2 = copyBit(i, 8, formatInfoBits2);
  }
  auto const tryDecode(FormatInformation::decodeFormatInformation(formatInfoBits1,formatInfoBits2));
  if(!tryDecode)
      return tryDecode.error();

  parsedFormatInfo_ = *tryDecode;
  if(parsedFormatInfo_)
      return parsedFormatInfo_;

  return failure<ReaderException>("Format information decoding failed");
}

bool BitMatrixParser::parseVersion(const int dimension, const bool searchTop)
{
    int versionBits = 0;

    for(auto i = 5; i >= 0; --i) {
        auto const minVal = dimension - 11;
        for(auto j = dimension - 9; j >= minVal; --j) {
            if(searchTop)
                versionBits = copyBit(j, i, versionBits);
            else
                versionBits = copyBit(i, j, versionBits);
        }
    }

    auto const version(Version::decodeVersionInformation(versionBits));
    if(version)
        parsedVersion_ = *version;

    return (parsedVersion_ != nullptr) && (parsedVersion_->getDimensionForVersion() == dimension);
}

Fallible<Version *> BitMatrixParser::readVersion() noexcept {
  if (parsedVersion_ != nullptr) {
    return parsedVersion_;
  }

  int dimension = (int)bitMatrix_->getDimension();

  auto const provisionalVersion = (dimension - 17) >> 2;
  if (provisionalVersion <= 6) {

      auto const version(Version::getVersionForNumber(static_cast<int>(provisionalVersion)));
      if(version) return *version; else return version.error();
  }

  // Read top-right version info: 3 wide by 6 tall
  // If it fails, try bottom left: 6 wide by 3 tall
  if (parseVersion(dimension, true) || parseVersion(dimension, false)) {
    return parsedVersion_;
  }

  return failure<ReaderException>("Could not decode version");
}

Fallible<ArrayRef<unsigned char>> BitMatrixParser::readCodewords() {
  const auto formatInfo(readFormatInformation());
  if(!formatInfo)
      return formatInfo.error();

  auto const version = readVersion();
  if (!version)
      return version.error();

  //	cerr << *bitMatrix_ << endl;
  //	cerr << bitMatrix_->getDimension() << endl;

  // Get the data mask for the format used in this QR Code. This will exclude
  // some bits from reading as we wind through the bit matrix.
  auto const dataMask(DataMask::forReference((int)(*formatInfo)->getDataMask()));
  if(!dataMask)
      return dataMask.error();

  //	cout << (int)formatInfo->getDataMask() << endl;
  int dimension = (int)bitMatrix_->getDimension();
  dataMask->unmaskBitMatrix(*bitMatrix_, dimension);


  //		cerr << *bitMatrix_ << endl;
  //	cerr << version->getTotalCodewords() << endl;

  auto const getFunctionPattern(version->buildFunctionPattern());
  if(!getFunctionPattern)
      return getFunctionPattern.error();

  Ref<BitMatrix> functionPattern = *getFunctionPattern;

  //	cout << *functionPattern << endl;

  bool readingUp = true;
  ArrayRef<unsigned char> result(version->getTotalCodewords());
  int resultOffset = 0;
  int currentByte = 0;
  int bitsRead = 0;
  // Read columns in pairs, from right to left
  for (int x = dimension - 1; x > 0; x -= 2) {
    if (x == 6) {
      // Skip whole column with vertical alignment pattern;
      // saves time and makes the other code proceed more cleanly
      x--;
    }
    // Read alternatingly from bottom to top then top to bottom
    for (int counter = 0; counter < dimension; counter++) {
      int y = readingUp ? dimension - 1 - counter : counter;
      for (int col = 0; col < 2; col++) {
        // Ignore bits covered by the function pattern
        if (!functionPattern->get(x - col, y)) {
          // Read a bit
          bitsRead++;
          currentByte <<= 1;
          if (bitMatrix_->get(x - col, y)) {
            currentByte |= 1;
          }
          // If we've made a whole byte, save it off
          if (bitsRead == 8) {
            result[resultOffset++] = (unsigned char)currentByte;
            bitsRead = 0;
            currentByte = 0;
          }
        }
      }
    }
    readingUp = !readingUp; // switch directions
  }

  if (resultOffset != version->getTotalCodewords()) {
    return failure<ReaderException>("Did not read all codewords");
  }
  return result;
}

/**
 * Revert the mask removal done while reading the code words. The bit matrix should revert to its original state.
 */
Fallible<void> BitMatrixParser::remask()
{
    if(!readFormatInformation() || !parsedFormatInfo_)
        return failure<ReaderException>("We have no format information, and no data mask");

    auto const dataMask(DataMask::forReference(parsedFormatInfo_->getDataMask()));
    if(!dataMask)
        return dataMask.error();

    auto const dimension = bitMatrix_->getHeight();

    dataMask->unmaskBitMatrix(*bitMatrix_, dimension);
    return success();
}

void BitMatrixParser::mirror()
{
    for(size_t x = 0; x < bitMatrix_->getWidth(); ++x)
    {
        for(size_t y = x + 1; y < bitMatrix_->getHeight(); ++y)
        {
            if(bitMatrix_->get(x, y) != bitMatrix_->get(y, x))
            {
                bitMatrix_->flip(y, x);
                bitMatrix_->flip(x, y);
            }
        }
    }
    // Version and format info are no longer valid
    resetVersion();
    resetFormatInformation();
}

void BitMatrixParser::resetFormatInformation() noexcept
{
    parsedFormatInfo_ = nullptr;
}

void BitMatrixParser::resetVersion() noexcept
{
    parsedVersion_ = nullptr;
}



}
}
