#pragma once
/*
 *  UPCAReader.h
 *  ZXing
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

#include <zxing/DecodeHints.h>             // for DecodeHints
#include <zxing/oned/ZXingEAN13Reader.h>   // for EAN13Reader
#include <string>                          // for string

#include "zxing/BarcodeFormat.h"           // for BarcodeFormat
#include "zxing/common/Counted.h"          // for Ref
#include "zxing/oned/ZXingUPCEANReader.h"  // for UPCEANReader

namespace pping {
class BinaryBitmap;
class BitArray;
class Result;
}  // namespace pping

namespace pping {
  namespace oned {
    class UPCAReader : public UPCEANReader {

    private:
      EAN13Reader ean13Reader;
      static FallibleRef<Result> maybeReturnResult(Ref<Result> result);
      static FallibleRef<Result> maybeReturnResult(FallibleRef<Result> result);

    public:
      UPCAReader();

      int decodeMiddle(Ref<BitArray> row, int startGuardBegin, int startGuardEnd,
          std::string& resultString);

      FallibleRef<Result> decodeRow(int rowNumber, Ref<BitArray> row) MB_NOEXCEPT_EXCEPT_BADALLOC;
      FallibleRef<Result> decodeRow(int rowNumber, Ref<BitArray> row, int startGuardBegin,
          int startGuardEnd) MB_NOEXCEPT_EXCEPT_BADALLOC;
      FallibleRef<Result> decode(Ref<BinaryBitmap> image, DecodeHints hints) MB_NOEXCEPT_EXCEPT_BADALLOC;

      BarcodeFormat getBarcodeFormat();
    };
  }
}

