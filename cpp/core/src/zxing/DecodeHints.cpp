// -*- mode:c++; tab-width:2; indent-tabs-mode:nil; c-basic-offset:2 -*-
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
#include <zxing/common/IllegalArgumentException.h>

using zxing::Ref;
using zxing::ResultPointCallback;
using zxing::DecodeHintType;
using zxing::DecodeHints;

// VC++
using zxing::BarcodeFormat;

const DecodeHintType DecodeHints::CHARACTER_SET;

const DecodeHints DecodeHints::PRODUCT_HINT(
  UPC_A_HINT |
  UPC_E_HINT |
  EAN_13_HINT |
  EAN_8_HINT |
  RSS_14_HINT
  );

const DecodeHints DecodeHints::ONED_HINT(
  CODE_39_HINT |
  CODE_93_HINT |
  CODE_128_HINT |
  ITF_HINT |
  CODABAR_HINT |
  DecodeHints::PRODUCT_HINT
  );

const DecodeHints DecodeHints::DEFAULT_HINT(
  ONED_HINT |
  QR_CODE_HINT |
  DATA_MATRIX_HINT |
  AZTEC_HINT |
  PDF_417_HINT
  );

DecodeHints::DecodeHints() {
  hints = 0;
}

DecodeHints::DecodeHints(DecodeHintType init) {
  hints = init;
}

void DecodeHints::addFormat(BarcodeFormat toadd) {
  switch (toadd) {
  case BarcodeFormat::AZTEC: hints |= AZTEC_HINT; break;
  case BarcodeFormat::CODABAR: hints |= CODABAR_HINT; break;
  case BarcodeFormat::CODE_39: hints |= CODE_39_HINT; break;
  case BarcodeFormat::CODE_93: hints |= CODE_93_HINT; break;
  case BarcodeFormat::CODE_128: hints |= CODE_128_HINT; break;
  case BarcodeFormat::DATA_MATRIX: hints |= DATA_MATRIX_HINT; break;
  case BarcodeFormat::EAN_8: hints |= EAN_8_HINT; break;
  case BarcodeFormat::EAN_13: hints |= EAN_13_HINT; break;
  case BarcodeFormat::ITF: hints |= ITF_HINT; break;
  case BarcodeFormat::MAXICODE: hints |= MAXICODE_HINT; break;
  case BarcodeFormat::PDF_417: hints |= PDF_417_HINT; break;
  case BarcodeFormat::QR_CODE: hints |= QR_CODE_HINT; break;
  case BarcodeFormat::RSS_14: hints |= RSS_14_HINT; break;
  case BarcodeFormat::RSS_EXPANDED: hints |= RSS_EXPANDED_HINT; break;
  case BarcodeFormat::UPC_A: hints |= UPC_A_HINT; break;
  case BarcodeFormat::UPC_E: hints |= UPC_E_HINT; break;
  case BarcodeFormat::UPC_EAN_EXTENSION: hints |= UPC_EAN_EXTENSION_HINT; break;
  default: throw IllegalArgumentException("Unrecognizd barcode format");
  }
}

bool DecodeHints::containsFormat(BarcodeFormat tocheck) const {
  DecodeHintType checkAgainst = 0;
  switch (tocheck) {
  case BarcodeFormat::AZTEC: checkAgainst |= AZTEC_HINT; break;
  case BarcodeFormat::CODABAR: checkAgainst |= CODABAR_HINT; break;
  case BarcodeFormat::CODE_39: checkAgainst |= CODE_39_HINT; break;
  case BarcodeFormat::CODE_93: checkAgainst |= CODE_93_HINT; break;
  case BarcodeFormat::CODE_128: checkAgainst |= CODE_128_HINT; break;
  case BarcodeFormat::DATA_MATRIX: checkAgainst |= DATA_MATRIX_HINT; break;
  case BarcodeFormat::EAN_8: checkAgainst |= EAN_8_HINT; break;
  case BarcodeFormat::EAN_13: checkAgainst |= EAN_13_HINT; break;
  case BarcodeFormat::ITF: checkAgainst |= ITF_HINT; break;
  case BarcodeFormat::MAXICODE: checkAgainst |= MAXICODE_HINT; break;
  case BarcodeFormat::PDF_417: checkAgainst |= PDF_417_HINT; break;
  case BarcodeFormat::QR_CODE: checkAgainst |= QR_CODE_HINT; break;
  case BarcodeFormat::RSS_14: checkAgainst |= RSS_14_HINT; break;
  case BarcodeFormat::RSS_EXPANDED: checkAgainst |= RSS_EXPANDED_HINT; break;
  case BarcodeFormat::UPC_A: checkAgainst |= UPC_A_HINT; break;
  case BarcodeFormat::UPC_E: checkAgainst |= UPC_E_HINT; break;
  case BarcodeFormat::UPC_EAN_EXTENSION: checkAgainst |= UPC_EAN_EXTENSION_HINT; break;
  default: throw IllegalArgumentException("Unrecognizd barcode format");
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

DecodeHints zxing::operator | (DecodeHints const& l, DecodeHints const& r) {
  DecodeHints result (l);
  result.hints |= r.hints;
  if (!result.callback) {
    result.callback = r.callback;
  }
  return result;
}
