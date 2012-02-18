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
#include <zxing/qrcode/QRCodeReader.h>
#include <zxing/datamatrix/DataMatrixReader.h>
#include <zxing/aztec/AztecReader.h>
#include <zxing/oned/MultiFormatUPCEANReader.h>
#include <zxing/oned/MultiFormatOneDReader.h>
#include <zxing/ReaderException.h>

namespace zxing {
  MultiFormatReader::MultiFormatReader() {

  }
  
  Ref<Result> MultiFormatReader::decode(Ref<BinaryBitmap> image) {
    setHints(DecodeHints::DEFAULT_HINT);
    return decodeInternal(image);
  }

  Ref<Result> MultiFormatReader::decode(Ref<BinaryBitmap> image, DecodeHints hints) {
    setHints(hints);
    return decodeInternal(image);
  }

  Ref<Result> MultiFormatReader::decodeWithState(Ref<BinaryBitmap> image) {
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

    bool addOneDReader = hints.containsFormat(BarcodeFormat_UPC_E) ||
                         hints.containsFormat(BarcodeFormat_UPC_A) ||
                         hints.containsFormat(BarcodeFormat_EAN_8) ||
                         hints.containsFormat(BarcodeFormat_EAN_13) ||
                         hints.containsFormat(BarcodeFormat_CODE_128) ||
                         hints.containsFormat(BarcodeFormat_CODE_39) ||
                         hints.containsFormat(BarcodeFormat_ITF);
    if (addOneDReader && !tryHarder) {
      readers_.push_back(Ref<Reader>(new zxing::oned::MultiFormatOneDReader(hints)));
    }
    if (hints.containsFormat(BarcodeFormat_QR_CODE)) {
      readers_.push_back(Ref<Reader>(new zxing::qrcode::QRCodeReader()));
    }
    if (hints.containsFormat(BarcodeFormat_DATA_MATRIX)) {
      readers_.push_back(Ref<Reader>(new zxing::datamatrix::DataMatrixReader()));
    }
    if (hints.containsFormat(BarcodeFormat_AZTEC)) {
      readers_.push_back(Ref<Reader>(new zxing::aztec::AztecReader()));
    }
    //TODO: add PDF417 here once PDF417 reader is implemented
    if (addOneDReader && tryHarder) {
      readers_.push_back(Ref<Reader>(new zxing::oned::MultiFormatOneDReader(hints)));
    }
    if (readers_.size() == 0) {
      if (!tryHarder) {
        readers_.push_back(Ref<Reader>(new zxing::oned::MultiFormatOneDReader(hints)));
      }
      readers_.push_back(Ref<Reader>(new zxing::qrcode::QRCodeReader()));
      if (tryHarder) {
        readers_.push_back(Ref<Reader>(new zxing::oned::MultiFormatOneDReader(hints)));
      }
    }
  }

  Ref<Result> MultiFormatReader::decodeInternal(Ref<BinaryBitmap> image) {
    for (unsigned int i = 0; i < readers_.size(); i++) {
      try {
        return readers_[i]->decode(image, hints_);
      } catch (ReaderException const& re) {
        // continue
      }
    }
    throw ReaderException("No code detected");
  }
  
  MultiFormatReader::~MultiFormatReader() {

  }
}
