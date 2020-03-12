/*
 *  MultiFormatOneDReader.cpp
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

#include "ZXingMultiFormatOneDReader.h"

#include <zxing/oned/ZXingCode128Reader.h>            // for Code128Reader
#include <zxing/oned/ZXingCode39Reader.h>             // for Code39Reader
#include <zxing/oned/ZXingITFReader.h>                // for ITFReader
#include <zxing/oned/ZXingMultiFormatUPCEANReader.h>  // for MultiFormatUPCEANReader

#include "zxing/BarcodeFormat.h"                      // for BarcodeFormat::CODE_128, BarcodeFormat::CODE_39, BarcodeFormat::EAN_13, BarcodeFormat::Barcod...
#include "zxing/Result.h"                             // for Result
#include "zxing/common/BitArray.h"                    // for BitArray
#include "zxing/oned/ZXingOneDReader.h"               // for OneDReader
#include "zxing/ReaderException.h"

namespace pping {
  namespace oned {
    MultiFormatOneDReader::MultiFormatOneDReader(DecodeHints hints) : readers() {
      if (hints.containsFormat(BarcodeFormat::EAN_13) ||
          hints.containsFormat(BarcodeFormat::EAN_8) ||
          hints.containsFormat(BarcodeFormat::UPC_A) ||
          hints.containsFormat(BarcodeFormat::UPC_E)) {
        readers.push_back(Ref<OneDReader>(new MultiFormatUPCEANReader(hints)));
      }
      if (hints.containsFormat(BarcodeFormat::CODE_39)) {
        readers.push_back(Ref<OneDReader>(new Code39Reader()));
      }
      if (hints.containsFormat(BarcodeFormat::CODE_128)) {
        readers.push_back(Ref<OneDReader>(new Code128Reader()));
      }
      if (hints.containsFormat(BarcodeFormat::ITF)) {
        readers.push_back(Ref<OneDReader>(new ITFReader()));
      }
      if (readers.size() == 0) {
        readers.push_back(Ref<OneDReader>(new MultiFormatUPCEANReader(hints)));
        readers.push_back(Ref<OneDReader>(new Code39Reader()));
        readers.push_back(Ref<OneDReader>(new Code128Reader()));
        readers.push_back(Ref<OneDReader>(new ITFReader()));
      }
    }

    FallibleRef<Result> MultiFormatOneDReader::decodeRow(int rowNumber, Ref<BitArray> row) MB_NOEXCEPT_EXCEPT_BADALLOC {
      int size = (int)readers.size();
      for (int i = 0; i < size; i++) {
        OneDReader* reader = readers[i];

        auto const result(reader->decodeRow(rowNumber, row));
        if(result && !(*result).empty())
            return *result;
      }
      return failure<ReaderException>("Can't decode row");
    }
  }
}
