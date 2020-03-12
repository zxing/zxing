/*
 *  MultiFormatBarcodeReader.cpp
 *  ZXing
 *
 *  Created by Lukasz Warchol on 10-01-26.
 *  Modified by Luiz Silva on 09/02/2010.
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

#include <zxing/MultiFormatReader.h>
#include <zxing/ReaderException.h>                  // for ReaderException
#include <zxing/aztec/AztecReader.h>                // for AztecReader
#include <zxing/datamatrix/DataMatrixReader.h>      // for DataMatrixReader
#include <zxing/oned/ZXingMultiFormatOneDReader.h>  // for MultiFormatOneDReader
#include <zxing/qrcode/QRCodeReader.h>              // for QRCodeReader

#include "zxing/BarcodeFormat.h"                    // for BarcodeFormat::AZTEC_BARCODE, BarcodeFormat::CODE_128, BarcodeFormat::CODE_39, BarcodeFormat::BarcodeFo...
#include "zxing/BinaryBitmap.h"                     // for BinaryBitmap
#include "zxing/DecodeHints.h"                      // for DecodeHints, DecodeHints::DEFAULT_HINT
#include "zxing/Reader.h"                           // for Reader
#include "zxing/Result.h"                           // for Result

namespace pping {
  MultiFormatReader::MultiFormatReader() {

  }
  
  FallibleRef<Result> MultiFormatReader::decode(Ref<BinaryBitmap> image) MB_NOEXCEPT_EXCEPT_BADALLOC {
    setHints(DecodeHints::DEFAULT_HINT);
    return decodeInternal(image);
  }

  FallibleRef<Result> MultiFormatReader::decode(Ref<BinaryBitmap> image, DecodeHints hints) MB_NOEXCEPT_EXCEPT_BADALLOC {
    setHints(hints);
    return decodeInternal(image);
  }

  FallibleRef<Result> MultiFormatReader::decodeWithState(Ref<BinaryBitmap> image) MB_NOEXCEPT_EXCEPT_BADALLOC {
    // Make sure to set up the default state so we don't crash
    if (readers_.size() == 0) {
      setHints(DecodeHints::DEFAULT_HINT);
    }
    return decodeInternal(image);
  }

  void MultiFormatReader::setHints(DecodeHints hints) {
    hints_ = hints;
    readers_.clear();
    bool tryHarder = hints.getTryHarder();

    bool addOneDReader = hints.containsFormat(BarcodeFormat::UPC_E) ||
                         hints.containsFormat(BarcodeFormat::UPC_A) ||
                         hints.containsFormat(BarcodeFormat::EAN_8) ||
                         hints.containsFormat(BarcodeFormat::EAN_13) ||
                         hints.containsFormat(BarcodeFormat::CODE_128) ||
                         hints.containsFormat(BarcodeFormat::CODE_39) ||
                         hints.containsFormat(BarcodeFormat::ITF);
    if (addOneDReader && !tryHarder) {
      readers_.push_back(Ref<Reader>(new pping::oned::MultiFormatOneDReader(hints)));
    }
    if (hints.containsFormat(BarcodeFormat::QR_CODE)) {
      readers_.push_back(Ref<Reader>(new pping::qrcode::QRCodeReader()));
    }
    if (hints.containsFormat(BarcodeFormat::DATA_MATRIX)) {
      readers_.push_back(Ref<Reader>(new pping::datamatrix::DataMatrixReader()));
    }
    if (hints.containsFormat(BarcodeFormat::AZTEC_BARCODE)) {
      readers_.push_back(Ref<Reader>(new pping::aztec::AztecReader()));
    }
    //TODO: add PDF417 here once PDF417 reader is implemented
    if (addOneDReader && tryHarder) {
      readers_.push_back(Ref<Reader>(new pping::oned::MultiFormatOneDReader(hints)));
    }
  }

  FallibleRef<Result> MultiFormatReader::decodeInternal(Ref<BinaryBitmap> image) MB_NOEXCEPT_EXCEPT_BADALLOC {
    for ( auto const & pReader : readers_ ) {
        auto result( pReader->decode(image, hints_) );
        if (result)
            return result;
    }
    return failure<ReaderException>("No code detected");
  }
  
  MultiFormatReader::~MultiFormatReader() {

  }
}
