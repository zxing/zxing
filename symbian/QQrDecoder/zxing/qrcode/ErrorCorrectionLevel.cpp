/*
 *  ErrorCorrectionLevel.cpp
 *  zxing
 *
 *  Created by Christian Brunschen on 15/05/2008.
 *  Copyright 2008 ZXing authors All rights reserved.
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

#include <zxing/qrcode/ErrorCorrectionLevel.h>

namespace zxing {
namespace qrcode {

ErrorCorrectionLevel::ErrorCorrectionLevel(int inOrdinal) :
    ordinal_(inOrdinal) {
}

int ErrorCorrectionLevel::ordinal() {
  return ordinal_;
}

ErrorCorrectionLevel& ErrorCorrectionLevel::forBits(int bits) {
  if (bits < 0 || bits >= N_LEVELS) {
    throw ReaderException("Ellegal error correction level bits");
  }
  return *FOR_BITS[bits];
}

ErrorCorrectionLevel ErrorCorrectionLevel::L(0);
ErrorCorrectionLevel ErrorCorrectionLevel::M(1);
ErrorCorrectionLevel ErrorCorrectionLevel::Q(2);
ErrorCorrectionLevel ErrorCorrectionLevel::H(3);
ErrorCorrectionLevel *ErrorCorrectionLevel::FOR_BITS[] = { &M, &L, &H, &Q };
int ErrorCorrectionLevel::N_LEVELS = 4;

}
}
