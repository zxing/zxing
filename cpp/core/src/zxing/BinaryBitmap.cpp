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
	
}
