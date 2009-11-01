/*
 *  BinaryBitmap.cpp
 *  zxing
 *
 *  Created by Ralf Kistner on 19/10/2009.
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

#include <zxing/BinaryBitmap.h>

namespace zxing {

BinaryBitmap::BinaryBitmap(Ref<Binarizer> binarizer) : bits_(NULL), binarizer_(binarizer) {

}

BinaryBitmap::~BinaryBitmap() {
}


Ref<BitMatrix> BinaryBitmap::getBlackMatrix() {
  if (bits_ == NULL) {
    bits_ = binarizer_->getBlackMatrix();
  }
  return bits_;
}

Ref<LuminanceSource> BinaryBitmap::getSource() {
  return binarizer_->getSource();
}

}
