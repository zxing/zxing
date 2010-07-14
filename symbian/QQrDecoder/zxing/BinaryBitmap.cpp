/*
 *  BinaryBitmap.cpp
 *  zxing
 *
 *  Created by Ralf Kistner on 19/10/2009.
 *  Copyright 2008 ZXing authors All rights reserved.
 *  Modified by Lukasz Warchol on 02/02/2010.
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
	
	BinaryBitmap::BinaryBitmap(Ref<Binarizer> binarizer) : bits_(NULL), array_bits_(NULL), binarizer_(binarizer), cached_y_(-1) {
		
	}
	
	BinaryBitmap::~BinaryBitmap() {
	}
	
	Ref<BitArray> BinaryBitmap::getBlackRow(int y, Ref<BitArray> row) {
		if (array_bits_ == NULL && cached_y_ != y) {
			array_bits_ = binarizer_->getBlackRow(y, row);
			cached_y_ = y;
		}
		return array_bits_;
	}
	
	Ref<BitMatrix> BinaryBitmap::getBlackMatrix() {
		if (bits_ == NULL) {
			bits_ = binarizer_->getBlackMatrix();
		}
		return bits_;
	}
	int BinaryBitmap::getWidth() {
		return getSource()->getWidth();
	}
	int BinaryBitmap::getHeight() {
		return getSource()->getHeight();
	}
	
	Ref<LuminanceSource> BinaryBitmap::getSource() {
		return binarizer_->getSource();
	}
	
}
