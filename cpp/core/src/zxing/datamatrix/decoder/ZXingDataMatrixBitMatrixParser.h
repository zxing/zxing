#pragma once

/*
 *  BitMatrixParser.h
 *  zxing
 *
 *  Created by Luiz Silva on 09/02/2010.
 *  Copyright 2010 ZXing authors All rights reserved.
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

#include <stddef.h>                                   // for size_t
#include <zxing/common/Array.h>                       // for ArrayRef
#include <zxing/common/BitMatrix.h>                   // for BitMatrix
#include <zxing/common/Counted.h>                     // for Ref, Counted
#include <zxing/datamatrix/ZXingDataMatrixVersion.h>  // for Version

namespace pping {
namespace datamatrix {

class BitMatrixParser : public Counted {
private:
  Ref<BitMatrix> bitMatrix_;
  Ref<Version> parsedVersion_;
  Ref<BitMatrix> readBitMatrix_;

  int copyBit(size_t x, size_t y, int versionBits);

  static FallibleRef<Version> parseVersion(Ref<BitMatrix> bitMatrix) MB_NOEXCEPT_EXCEPT_BADALLOC;

public:
  static FallibleRef<BitMatrixParser> createBitMatrixParser(Ref<BitMatrix> bitMatrix) MB_NOEXCEPT_EXCEPT_BADALLOC;

  FallibleRef<Version> readVersion(Ref<BitMatrix> bitMatrix) MB_NOEXCEPT_EXCEPT_BADALLOC;
  Fallible<ArrayRef<unsigned char>> readCodewords() MB_NOEXCEPT_EXCEPT_BADALLOC;
  bool readModule(int row, int column, int numRows, int numColumns);

private:
  BitMatrixParser(Ref<BitMatrix> bitMatrix, Ref<Version> parsedVersion, Ref<BitMatrix> readBitMatrix);

  int readUtah(int row, int column, int numRows, int numColumns);
  int readCorner1(int numRows, int numColumns);
  int readCorner2(int numRows, int numColumns);
  int readCorner3(int numRows, int numColumns);
  int readCorner4(int numRows, int numColumns);
  static FallibleRef<BitMatrix> extractDataRegion(Ref<BitMatrix> bitMatrix, Ref<Version> parsedVersion) MB_NOEXCEPT_EXCEPT_BADALLOC;
};

}
}

