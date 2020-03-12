/*
 *  UPCAReader.cpp
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

#include "ZXingUPCAReader.h"

#include <memory>                         // for allocator

#include "zxing/BinaryBitmap.h"           // for BinaryBitmap
#include "zxing/DecodeHints.h"            // for DecodeHints
#include "zxing/Result.h"                 // for Result
#include "zxing/ResultPoint.h"            // for ResultPoint
#include "zxing/common/BitArray.h"        // for BitArray
#include "zxing/common/Str.h"             // for String
#include "zxing/oned/ZXingEAN13Reader.h"  // for EAN13Reader
#include <zxing/ReaderException.h>

namespace pping {
  namespace oned {
    UPCAReader::UPCAReader() : ean13Reader() {
    }

    FallibleRef<Result> UPCAReader::decodeRow(int rowNumber, Ref<BitArray> row) MB_NOEXCEPT_EXCEPT_BADALLOC {
      return maybeReturnResult(ean13Reader.decodeRow(rowNumber, row));
    }

    FallibleRef<Result> UPCAReader::decodeRow(int rowNumber, Ref<BitArray> row, int startGuardBegin,
        int startGuardEnd) MB_NOEXCEPT_EXCEPT_BADALLOC {
      return maybeReturnResult(ean13Reader.decodeRow(rowNumber, row, startGuardBegin,
          startGuardEnd));
    }

    FallibleRef<Result> UPCAReader::decode(Ref<BinaryBitmap> image, DecodeHints hints) MB_NOEXCEPT_EXCEPT_BADALLOC {
      return maybeReturnResult(ean13Reader.decode(image, hints));
    }

    int UPCAReader::decodeMiddle(Ref<BitArray> row, int startGuardBegin, int startGuardEnd,
        std::string& resultString) {
      return ean13Reader.decodeMiddle(row, startGuardBegin, startGuardEnd, resultString);
    }

    FallibleRef<Result> UPCAReader::maybeReturnResult(FallibleRef<Result> result) {
        if (!result)
            return result.error();
        return maybeReturnResult(*result);
    }

    FallibleRef<Result> UPCAReader::maybeReturnResult(Ref<Result> result) {
      if (result.empty()) {
        return result;
      }
      const std::string& text = (result->getText())->getText();
      if (text[0] == '0') {
        Ref<String> resultString(new String(text.substr(1)));
        Ref<Result> res(new Result(resultString, result->getRawBytes(), result->getResultPoints(),
            BarcodeFormat::UPC_A));
        return res;
      }
      return failure<ReaderException>();
    }

    BarcodeFormat UPCAReader::getBarcodeFormat(){
      return BarcodeFormat::UPC_A;
    }
  }
}
