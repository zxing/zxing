/*
 *  MultiFormatUPCEANReader.cpp
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
#include "ZXingMultiFormatUPCEANReader.h"

#include <zxing/oned/ZXingEAN13Reader.h>  // for EAN13Reader
#include <zxing/oned/ZXingEAN8Reader.h>   // for EAN8Reader
#include <zxing/oned/ZXingUPCAReader.h>   // for UPCAReader
#include <zxing/oned/ZXingUPCEReader.h>   // for UPCEReader
#include <string>                         // for string

#include "zxing/BarcodeFormat.h"          // for BarcodeFormat::EAN_13, BarcodeFormat::UPC_A, BarcodeFormat::EAN_8, BarcodeFormat::UPC_E
#include "zxing/Result.h"                 // for Result
#include "zxing/ResultPoint.h"            // for ResultPoint
#include "zxing/common/BitArray.h"        // for BitArray
#include "zxing/common/Str.h"             // for String
#include "zxing/oned/ZXingOneDReader.h"   // for OneDReader
#include "zxing/ReaderException.h"

namespace pping {
  namespace oned {

    MultiFormatUPCEANReader::MultiFormatUPCEANReader(DecodeHints hints) : readers() {
      if (hints.containsFormat(BarcodeFormat::EAN_13)) {
        readers.push_back(Ref<OneDReader>(new EAN13Reader()));
      } else if (hints.containsFormat(BarcodeFormat::UPC_A)) {
        readers.push_back(Ref<OneDReader>(new UPCAReader()));
      }
      if (hints.containsFormat(BarcodeFormat::EAN_8)) {
        readers.push_back(Ref<OneDReader>(new EAN8Reader()));
      }
      if (hints.containsFormat(BarcodeFormat::UPC_E)) {
        readers.push_back(Ref<OneDReader>(new UPCEReader()));
      }
      if (readers.size() == 0) {
        readers.push_back(Ref<OneDReader>(new EAN13Reader()));
        // UPC-A is covered by EAN-13
        readers.push_back(Ref<OneDReader>(new EAN8Reader()));
        readers.push_back(Ref<OneDReader>(new UPCEReader()));
      }
    }

    FallibleRef<Result> MultiFormatUPCEANReader::decodeRow(int rowNumber, Ref<BitArray> row) MB_NOEXCEPT_EXCEPT_BADALLOC {
      // Compute this location once and reuse it on multiple implementations
      int size = (int)readers.size();
      for (int i = 0; i < size; i++) {
        Ref<OneDReader> reader = readers[i];

        auto const getResult(reader->decodeRow(rowNumber, row));
        if(!getResult || (*getResult).empty())
            continue;

        auto const result = *getResult;

        // Special case: a 12-digit code encoded in UPC-A is identical to a "0"
        // followed by those 12 digits encoded as EAN-13. Each will recognize such a code,
        // UPC-A as a 12-digit string and EAN-13 as a 13-digit string starting with "0".
        // Individually these are correct and their readers will both read such a code
        // and correctly call it EAN-13, or UPC-A, respectively.
        //
        // In this case, if we've been looking for both types, we'd like to call it
        // a UPC-A code. But for efficiency we only run the EAN-13 decoder to also read
        // UPC-A. So we special case it here, and convert an EAN-13 result to a UPC-A
        // result if appropriate.
        if (result->getBarcodeFormat() == BarcodeFormat::EAN_13) {
          const std::string& text = (result->getText())->getText();
          if (text[0] == '0') {
            Ref<String> resultString(new String(text.substr(1)));
            Ref<Result> res(new Result(resultString, result->getRawBytes(),
                result->getResultPoints(), BarcodeFormat::UPC_A));
            return res;
          }
        }
        return result;
      }
      return failure<ReaderException>("Can't decode row");
    }
  }
}
