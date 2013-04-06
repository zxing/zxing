// -*- mode:c++; tab-width:2; indent-tabs-mode:nil; c-basic-offset:2 -*-
/*
 *  Copyright 2013 ZXing authors All rights reserved.
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

#include <zxing/ZXing.h>
#include <zxing/InvertedLuminanceSource.h>

using zxing::boolean;
using zxing::Ref;
using zxing::ArrayRef;
using zxing::LuminanceSource;
using zxing::InvertedLuminanceSource;

InvertedLuminanceSource::InvertedLuminanceSource(Ref<LuminanceSource> const& delegate_)
    : Super(delegate_->getWidth(), delegate_->getHeight()), delegate(delegate_) {}  

ArrayRef<char> InvertedLuminanceSource::getRow(int y, ArrayRef<char> row) const {
  row = delegate->getRow(y, row);
  int width = getWidth();
  for (int i = 0; i < width; i++) {
    row[i] = (byte) (255 - (row[i] & 0xFF));
  }
  return row;
}

ArrayRef<char> InvertedLuminanceSource::getMatrix() const {
  ArrayRef<char> matrix = delegate->getMatrix();
  int length = getWidth() * getHeight();
  ArrayRef<char> invertedMatrix(length);
  for (int i = 0; i < length; i++) {
    invertedMatrix[i] = (byte) (255 - (matrix[i] & 0xFF));
  }
  return invertedMatrix;
}

zxing::boolean InvertedLuminanceSource::isCropSupported() const {
  return delegate->isCropSupported();
}

Ref<LuminanceSource> InvertedLuminanceSource::crop(int left, int top, int width, int height) const {
  return Ref<LuminanceSource>(new InvertedLuminanceSource(delegate->crop(left, top, width, height)));
}

boolean InvertedLuminanceSource::isRotateSupported() const {
  return delegate->isRotateSupported();
}

Ref<LuminanceSource> InvertedLuminanceSource::invert() const {
  return delegate;
}

Ref<LuminanceSource> InvertedLuminanceSource::rotateCounterClockwise() const {
  return Ref<LuminanceSource>(new InvertedLuminanceSource(delegate->rotateCounterClockwise()));
}

