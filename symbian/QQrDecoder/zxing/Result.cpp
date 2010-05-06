/*
 *  Result.cpp
 *  zxing
 *
 *  Created by Christian Brunschen on 13/05/2008.
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

#include <zxing/Result.h>

namespace zxing {
using namespace std;

Result::Result(Ref<String> text, ArrayRef<unsigned char> rawBytes, std::vector<Ref<ResultPoint> > resultPoints,
               BarcodeFormat format) :
    text_(text), rawBytes_(rawBytes), resultPoints_(resultPoints), format_(format) {
}

Result::~Result() {
}

Ref<String> Result::getText() {
  return text_;
}

ArrayRef<unsigned char> Result::getRawBytes() {
  return rawBytes_;
}

const std::vector<Ref<ResultPoint> >& Result::getResultPoints() const {
  return resultPoints_;
}

BarcodeFormat Result::getBarcodeFormat() const {
  return format_;
}

ostream& operator<<(ostream &out, Result& result) {
  if (result.text_ != 0) {
    out << result.text_->getText();
  } else {
    out << "[" << result.rawBytes_->size() << " bytes]";
  }
  return out;
}

}
