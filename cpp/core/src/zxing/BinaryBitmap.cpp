// -*- mode:c++; tab-width:2; indent-tabs-mode:nil; c-basic-offset:2 -*-
/*
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

#include <zxing/BinaryBitmap.h>

using zxing::Ref;
using zxing::BitArray;
using zxing::BitMatrix;
using zxing::LuminanceSource;
using zxing::BinaryBitmap;
	
// VC++
using zxing::Binarizer;

BinaryBitmap::BinaryBitmap(Ref<Binarizer> binarizer) : binarizer_(binarizer) {
}
	
BinaryBitmap::~BinaryBitmap() {
}
	
Ref<BitArray> BinaryBitmap::getBlackRow(int y, Ref<BitArray> row) {
  return binarizer_->getBlackRow(y, row);
}
	
Ref<BitMatrix> BinaryBitmap::getBlackMatrix() {
  return binarizer_->getBlackMatrix();
}
	
int BinaryBitmap::getWidth() const {
  return getLuminanceSource()->getWidth();
}
	
int BinaryBitmap::getHeight() const {
  return getLuminanceSource()->getHeight();
}
	
Ref<LuminanceSource> BinaryBitmap::getLuminanceSource() const {
  return binarizer_->getLuminanceSource();
}
	

bool BinaryBitmap::isCropSupported() const {
  return getLuminanceSource()->isCropSupported();
}

Ref<BinaryBitmap> BinaryBitmap::crop(int left, int top, int width, int height) {
  return Ref<BinaryBitmap> (new BinaryBitmap(binarizer_->createBinarizer(getLuminanceSource()->crop(left, top, width, height))));
}

bool BinaryBitmap::isRotateSupported() const {
  return getLuminanceSource()->isRotateSupported();
}

Ref<BinaryBitmap> BinaryBitmap::rotateCounterClockwise() {
  return Ref<BinaryBitmap> (new BinaryBitmap(binarizer_->createBinarizer(getLuminanceSource()->rotateCounterClockwise())));
}
