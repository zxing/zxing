// -*- Mode: ObjC; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*-
/**
 * Copyright 2011 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#import "CBarcodeFormat.h"
#import "BarcodeFormat.h"

BarcodeFormat CBarcodeFormatConvert(zxing::BarcodeFormat value);

// The purpose of this function is to issue a warning when a value is added to
// zxing::BarcodeFormat.
BarcodeFormat CBarcodeFormatConvert(zxing::BarcodeFormat value) {
  switch (value) {
  case zxing::BarcodeFormat_None:
    return BarcodeFormat_None;
  case zxing::BarcodeFormat_QR_CODE:
    return BarcodeFormat_QR_CODE;
  case zxing::BarcodeFormat_DATA_MATRIX:
    return BarcodeFormat_DATA_MATRIX;
  case zxing::BarcodeFormat_UPC_E:
    return BarcodeFormat_UPC_E;
  case zxing::BarcodeFormat_UPC_A:
    return BarcodeFormat_UPC_A;
  case zxing::BarcodeFormat_EAN_8:
    return BarcodeFormat_EAN_8;
  case zxing::BarcodeFormat_EAN_13:
    return BarcodeFormat_EAN_13;
  case zxing::BarcodeFormat_CODE_128:
    return BarcodeFormat_CODE_128;
  case zxing::BarcodeFormat_CODE_39:
    return BarcodeFormat_CODE_39;
  case zxing::BarcodeFormat_ITF:
    return BarcodeFormat_ITF;
  case zxing::BarcodeFormat_AZTEC:
    return BarcodeFormat_AZTEC;
  }

  return BarcodeFormat_None;
}
