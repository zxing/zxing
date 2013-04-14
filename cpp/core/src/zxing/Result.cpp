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

using zxing::Result;
using zxing::Ref;
using zxing::ArrayRef;
using zxing::String;
using zxing::ResultPoint;

// VC++
using zxing::BarcodeFormat;

Result::Result(Ref<String> text,
               ArrayRef<char> rawBytes,
               ArrayRef< Ref<ResultPoint> > resultPoints,
               BarcodeFormat format) :
  text_(text), rawBytes_(rawBytes), resultPoints_(resultPoints), format_(format) {
}

Result::~Result() {
}

Ref<String> Result::getText() {
  return text_;
}

ArrayRef<char> Result::getRawBytes() {
  return rawBytes_;
}

ArrayRef< Ref<ResultPoint> > const& Result::getResultPoints() const {
  return resultPoints_;
}

ArrayRef< Ref<ResultPoint> >& Result::getResultPoints() {
  return resultPoints_;
}

zxing::BarcodeFormat Result::getBarcodeFormat() const {
  return format_;
}
