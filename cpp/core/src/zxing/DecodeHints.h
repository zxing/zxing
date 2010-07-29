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
#ifndef DECODEHINTS_H_
#define DECODEHINTS_H_

#include <zxing/BarcodeFormat.h>

namespace zxing {

typedef unsigned int DecodeHintType;

class DecodeHints {

 private:

  static const DecodeHintType BARCODEFORMAT_QR_CODE_HINT = 1 << BarcodeFormat_QR_CODE;
  static const DecodeHintType BARCODEFORMAT_DATA_MATRIX_HINT = 1 << BarcodeFormat_DATA_MATRIX;
  static const DecodeHintType BARCODEFORMAT_UPC_E_HINT = 1 << BarcodeFormat_UPC_E;
  static const DecodeHintType BARCODEFORMAT_UPC_A_HINT = 1 << BarcodeFormat_UPC_A;
  static const DecodeHintType BARCODEFORMAT_EAN_8_HINT = 1 << BarcodeFormat_EAN_8;
  static const DecodeHintType BARCODEFORMAT_EAN_13_HINT = 1 << BarcodeFormat_EAN_13;
  static const DecodeHintType BARCODEFORMAT_CODE_128_HINT = 1 << BarcodeFormat_CODE_128;
  static const DecodeHintType BARCODEFORMAT_CODE_39_HINT = 1 << BarcodeFormat_CODE_39;
  static const DecodeHintType BARCODEFORMAT_ITF_HINT = 1 << BarcodeFormat_ITF;
  static const DecodeHintType TRYHARDER_HINT = 1 << 31;

  DecodeHintType hints;

 public:

  static const DecodeHintType BARCODEFORMAT_PRODUCT_HINT =
      BARCODEFORMAT_UPC_E_HINT |
      BARCODEFORMAT_UPC_A_HINT |
      BARCODEFORMAT_EAN_8_HINT |
      BARCODEFORMAT_EAN_13_HINT;

  static const DecodeHintType BARCODEFORMAT_ONED_HINT =
      BARCODEFORMAT_PRODUCT_HINT |
      BARCODEFORMAT_CODE_128_HINT |
      BARCODEFORMAT_CODE_39_HINT |
      BARCODEFORMAT_ITF_HINT;

  static const DecodeHintType BARCODEFORMAT_ANY_HINT =
      BARCODEFORMAT_ONED_HINT |
// TODO: uncomment once this passes QA
//      BARCODEFORMAT_DATA_MATRIX_HINT |
      BARCODEFORMAT_QR_CODE_HINT;

  static const DecodeHintType DEFAULT_HINTS = BARCODEFORMAT_ANY_HINT;

  DecodeHints();
  DecodeHints(DecodeHintType init);
  ~DecodeHints();
  void addFormat(BarcodeFormat toadd);
  bool containsFormat(BarcodeFormat tocheck) const;
  void setTryHarder(bool toset);
  bool getTryHarder() const;

};

}

#endif
