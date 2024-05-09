#pragma once
/*
 *  DecodeHintType.h
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

#include <zxing/common/Str.h>           // for Str
#include <zxing/Binarizer.h>            // for Binarizer
#include <zxing/BarcodeFormat.h>        // for BarcodeFormat, BarcodeFormat::AZTEC_BARCODE, BarcodeFormat::CODE_128, BarcodeFormat::CODE_39, BarcodeFormat::Barcod...
#include <zxing/ResultPointCallback.h>  // for ResultPointCallback

#include "zxing/common/Counted.h"       // for Ref

namespace pping {

typedef unsigned int DecodeHintType;

class DecodeHints {

 private:

  DecodeHintType hints;

  Ref<ResultPointCallback> callback;

 public:

  static const DecodeHintType BARCODEFORMAT_QR_CODE_HINT = 1 << static_cast<int>(BarcodeFormat::QR_CODE);
  static const DecodeHintType BARCODEFORMAT_DATA_MATRIX_HINT = 1 << static_cast<int>(BarcodeFormat::DATA_MATRIX);
  static const DecodeHintType BARCODEFORMAT_UPC_E_HINT = 1 << static_cast<int>(BarcodeFormat::UPC_E);
  static const DecodeHintType BARCODEFORMAT_UPC_A_HINT = 1 << static_cast<int>(BarcodeFormat::UPC_A);
  static const DecodeHintType BARCODEFORMAT_EAN_8_HINT = 1 << static_cast<int>(BarcodeFormat::EAN_8);
  static const DecodeHintType BARCODEFORMAT_EAN_13_HINT = 1 << static_cast<int>(BarcodeFormat::EAN_13);
  static const DecodeHintType BARCODEFORMAT_CODE_128_HINT = 1 << static_cast<int>(BarcodeFormat::CODE_128);
  static const DecodeHintType BARCODEFORMAT_CODE_39_HINT = 1 << static_cast<int>(BarcodeFormat::CODE_39);
  static const DecodeHintType BARCODEFORMAT_ITF_HINT = 1 << static_cast<int>(BarcodeFormat::ITF);
  static const DecodeHintType BARCODEFORMAT_AZTEC_HINT = 1 << static_cast<int>(BarcodeFormat::AZTEC_BARCODE);
  static const DecodeHintType CHARACTER_SET = 1 << 30;
  static const DecodeHintType TRYHARDER_HINT = static_cast< DecodeHintType >( 1 << 31 );

  static const DecodeHints PRODUCT_HINT;
  static const DecodeHints ONED_HINT;
  static const DecodeHints DEFAULT_HINT;

  DecodeHints();
  DecodeHints(DecodeHintType init);

  void addFormat(BarcodeFormat toadd) noexcept;
  bool containsFormat(BarcodeFormat tocheck) const noexcept;
  void setTryHarder(bool toset);
  bool getTryHarder() const;

  void setResultPointCallback(Ref<ResultPointCallback> const&);
  Ref<ResultPointCallback> getResultPointCallback() const;

};

}

