#ifndef __GLOBALHISTOGRAMBINARIZER_H__
#define __GLOBALHISTOGRAMBINARIZER_H__
/*
 *  GlobalHistogramBinarizer.h
 *  zxing
 *
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

#include <vector>
#include <zxing/Binarizer.h>
#include <zxing/common/BitArray.h>
#include <zxing/common/BitMatrix.h>

namespace zxing {
	
	class GlobalHistogramBinarizer : public Binarizer {
	 private:
    Ref<BitMatrix> cached_matrix_;
	  Ref<BitArray> cached_row_;
	  int cached_row_num_;

	public:
		GlobalHistogramBinarizer(Ref<LuminanceSource> source);
		virtual ~GlobalHistogramBinarizer();
		
		virtual Ref<BitArray> getBlackRow(int y, Ref<BitArray> row);
		virtual Ref<BitMatrix> getBlackMatrix();
		static int estimate(std::vector<int> &histogram);
		Ref<Binarizer> createBinarizer(Ref<LuminanceSource> source);
	};
	
}

#endif /* GLOBALHISTOGRAMBINARIZER_H_ */
