/*
 *  BinaryBitmap.h
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
#ifndef BINARYBITMAP_H_
#define BINARYBITMAP_H_

#include <zxing/common/Counted.h>
#include <zxing/common/BitMatrix.h>
#include <zxing/common/BitArray.h>
#include <zxing/Binarizer.h>

namespace zxing {
	
	class BinaryBitmap : public Counted {
	private:
		Ref<BitMatrix> bits_;
		Ref<BitArray> array_bits_;
		Ref<Binarizer> binarizer_;
		int cached_y_;
		
	public:
		BinaryBitmap(Ref<Binarizer> binarizer);
		virtual ~BinaryBitmap();
		
		Ref<BitArray> getBlackRow(int y, Ref<BitArray> row);
		Ref<BitMatrix> getBlackMatrix();
		Ref<LuminanceSource> getSource();
		
		int getWidth();
		int getHeight();
	};
	
}

#endif /* BINARYBITMAP_H_ */
