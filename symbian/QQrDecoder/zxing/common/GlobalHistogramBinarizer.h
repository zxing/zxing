/*
 *  GlobalHistogramBinarizer.h
 *  zxing
 *
 *  Created by Ralf Kistner on 16/10/2009.
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

#ifndef GLOBALHISTOGRAMBINARIZER_H_
#define GLOBALHISTOGRAMBINARIZER_H_

#include <vector>
#include <zxing/Binarizer.h>
#include <zxing/common/BitArray.h>
#include <zxing/common/BitMatrix.h>

namespace zxing {
	
	class GlobalHistogramBinarizer : public Binarizer {
	public:
		GlobalHistogramBinarizer(Ref<LuminanceSource> source);
		virtual ~GlobalHistogramBinarizer();
		
		virtual Ref<BitArray> estimateBlackRow(int y, Ref<BitArray> row);
		virtual Ref<BitMatrix> estimateBlackMatrix();
		static int estimate(std::vector<int> &histogram);
	};
	
}

#endif /* GLOBALHISTOGRAMBINARIZER_H_ */
