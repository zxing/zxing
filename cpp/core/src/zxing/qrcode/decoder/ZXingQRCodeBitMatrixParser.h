#pragma once

/*
 *  BitMatrixParser.h
 *  zxing
 *
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

#include <stddef.h>                          // for size_t
#include <zxing/common/Array.h>              // for ArrayRef
#include <zxing/common/BitMatrix.h>          // for BitMatrix
#include <zxing/common/Counted.h>            // for Ref, Counted
#include <zxing/common/Error.hpp>
#include <zxing/qrcode/FormatInformation.h>  // for FormatInformation

namespace pping {
namespace qrcode {

class Version;

class BitMatrixParser : public Counted {
private:
  Ref<BitMatrix> bitMatrix_;
  Version *parsedVersion_ = nullptr;
  Ref<FormatInformation> parsedFormatInfo_;

  int copyBit(size_t x, size_t y, int versionBits);

public:
  static FallibleRef<BitMatrixParser> createBitMatrixParser(Ref<BitMatrix> bitMatrix) MB_NOEXCEPT_EXCEPT_BADALLOC;

  FallibleRef<FormatInformation> readFormatInformation() MB_NOEXCEPT_EXCEPT_BADALLOC;
  Fallible<Version *> readVersion() noexcept;
  Fallible<ArrayRef<unsigned char>> readCodewords();

  Fallible<void>    remask();
  void              mirror();
  void              resetFormatInformation() noexcept;
  void              resetVersion()           noexcept;

private:
  BitMatrixParser(Ref<BitMatrix> bitMatrix) noexcept;
  BitMatrixParser(const BitMatrixParser&);
  BitMatrixParser& operator =(const BitMatrixParser&);

  bool parseVersion(const int dimension, const bool top);
};

}
}

