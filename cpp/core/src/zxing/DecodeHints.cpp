/*
 *  DecodeHintType.cpp
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

#include <zxing/DecodeHints.h>
#include <zxing/common/IllegalArgumentException.h>  // for IllegalArgumentException

#include "zxing/BarcodeFormat.h"                    // for BarcodeFormat, BarcodeFormat::AZTEC_BARCODE, BarcodeFormat::CODE_128, BarcodeFormat::CODE_39, BarcodeFo...
#include "zxing/ResultPointCallback.h"              // for ResultPointCallback

#include <Utils/Macros.h>

namespace pping {

#ifndef _WIN32
const DecodeHintType DecodeHints::CHARACTER_SET;
#endif // ifndef WIN32

const DecodeHints DecodeHints::PRODUCT_HINT(
    BARCODEFORMAT_UPC_E_HINT |
    BARCODEFORMAT_UPC_A_HINT |
    BARCODEFORMAT_EAN_8_HINT |
    BARCODEFORMAT_EAN_13_HINT);

const DecodeHints DecodeHints::ONED_HINT(
    BARCODEFORMAT_UPC_E_HINT |
    BARCODEFORMAT_UPC_A_HINT |
    BARCODEFORMAT_EAN_8_HINT |
    BARCODEFORMAT_EAN_13_HINT |
    BARCODEFORMAT_CODE_128_HINT |
    BARCODEFORMAT_CODE_39_HINT |
    BARCODEFORMAT_ITF_HINT);

const DecodeHints DecodeHints::DEFAULT_HINT(
    BARCODEFORMAT_UPC_E_HINT |
    BARCODEFORMAT_UPC_A_HINT |
    BARCODEFORMAT_EAN_8_HINT |
    BARCODEFORMAT_EAN_13_HINT |
    BARCODEFORMAT_CODE_128_HINT |
    BARCODEFORMAT_CODE_39_HINT |
    BARCODEFORMAT_ITF_HINT |
    BARCODEFORMAT_DATA_MATRIX_HINT |
    BARCODEFORMAT_AZTEC_HINT |
    BARCODEFORMAT_QR_CODE_HINT);

DecodeHints::DecodeHints() {
  hints = 0;
}

DecodeHints::DecodeHints(DecodeHintType init) {
  hints = init;
}

void DecodeHints::addFormat(BarcodeFormat toadd) noexcept {
  switch (toadd) {
    case BarcodeFormat::AZTEC_BARCODE: hints |= BARCODEFORMAT_AZTEC_HINT; break;
    case BarcodeFormat::QR_CODE: hints |= BARCODEFORMAT_QR_CODE_HINT; break;
    case BarcodeFormat::DATA_MATRIX: hints |= BARCODEFORMAT_DATA_MATRIX_HINT; break;
    case BarcodeFormat::UPC_E: hints |= BARCODEFORMAT_UPC_E_HINT; break;
    case BarcodeFormat::UPC_A: hints |= BARCODEFORMAT_UPC_A_HINT; break;
    case BarcodeFormat::EAN_8: hints |= BARCODEFORMAT_EAN_8_HINT; break;
    case BarcodeFormat::EAN_13: hints |= BARCODEFORMAT_EAN_13_HINT; break;
    case BarcodeFormat::CODE_128: hints |= BARCODEFORMAT_CODE_128_HINT; break;
    case BarcodeFormat::CODE_39: hints |= BARCODEFORMAT_CODE_39_HINT; break;
    case BarcodeFormat::ITF: hints |= BARCODEFORMAT_ITF_HINT; break;
    default: MB_ASSERTM(false, "%s", "Unrecognized barcode format");
  }
}

bool DecodeHints::containsFormat(BarcodeFormat tocheck) const noexcept {
  DecodeHintType checkAgainst;
  switch (tocheck) {
    case BarcodeFormat::AZTEC_BARCODE: checkAgainst = BARCODEFORMAT_AZTEC_HINT; break;
    case BarcodeFormat::QR_CODE: checkAgainst = BARCODEFORMAT_QR_CODE_HINT; break;
    case BarcodeFormat::DATA_MATRIX: checkAgainst = BARCODEFORMAT_DATA_MATRIX_HINT; break;
    case BarcodeFormat::UPC_E: checkAgainst = BARCODEFORMAT_UPC_E_HINT; break;
    case BarcodeFormat::UPC_A: checkAgainst = BARCODEFORMAT_UPC_A_HINT; break;
    case BarcodeFormat::EAN_8: checkAgainst = BARCODEFORMAT_EAN_8_HINT; break;
    case BarcodeFormat::EAN_13: checkAgainst = BARCODEFORMAT_EAN_13_HINT; break;
    case BarcodeFormat::CODE_128: checkAgainst = BARCODEFORMAT_CODE_128_HINT; break;
    case BarcodeFormat::CODE_39: checkAgainst = BARCODEFORMAT_CODE_39_HINT; break;
    case BarcodeFormat::ITF: checkAgainst = BARCODEFORMAT_ITF_HINT; break;
    default:
      MB_ASSERTM(false, "%s", "Unrecognized barcode format");
      return false;
  }
  return (hints & checkAgainst) != 0;
}

void DecodeHints::setTryHarder(bool toset) {
  if (toset) {
    hints |= TRYHARDER_HINT;
  } else {
    hints &= ~TRYHARDER_HINT;
  }
}

bool DecodeHints::getTryHarder() const {
  return (hints & TRYHARDER_HINT) != 0;
}

void DecodeHints::setResultPointCallback(Ref<ResultPointCallback> const& _callback) {
    callback = _callback;
}

Ref<ResultPointCallback> DecodeHints::getResultPointCallback() const {
    return callback;
}

} /* namespace */
