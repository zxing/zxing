// -*- mode:c++; tab-width:2; indent-tabs-mode:nil; c-basic-offset:2 -*-
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

#include "zxing/BarcodeFormat.h"   // for BarcodeFormat
#include "zxing/ResultPoint.h"     // for ResultPoint
#include "zxing/common/Array.h"    // for ArrayRef, Array
#include "zxing/common/Counted.h"  // for Ref
#include "zxing/common/Str.h"      // for String

namespace pping {
using namespace std;

Result::Result(Ref<String> text, ArrayRef<unsigned char> rawBytes, std::vector<Ref<ResultPoint>> const & resultPoints,
               BarcodeFormat format, ArrayRef< ArrayRef<unsigned char> > byteSegments) :
  text_(text), rawBytes_(rawBytes), resultPoints_(resultPoints), format_(format), byteSegments_(byteSegments) {
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

std::vector<Ref<ResultPoint> >& Result::getResultPoints() {
  return resultPoints_;
}

BarcodeFormat Result::getBarcodeFormat() const {
  return format_;
}

ArrayRef< ArrayRef<unsigned char> > Result::getByteSegments(){
    return byteSegments_;
}

}
