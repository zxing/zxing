/*
 *  OneDReader.h
 *  ZXing
 *
 *  Created by Lukasz Warchol on 10-01-15.
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

#pragma once

#include <zxing/Reader.h>
#include <zxing/common/BitArray.h>
#include <zxing/BinaryBitmap.h>

namespace zxing {
	namespace oned {
		class OneDReader : public Reader {
		private:
			static const int INTEGER_MATH_SHIFT = 8;
			
			Ref<Result> doDecode(Ref<BinaryBitmap> image);
		public:
			static const int PATTERN_MATCH_RESULT_SCALE_FACTOR = 1 << INTEGER_MATH_SHIFT;
			
			OneDReader();
			virtual Ref<Result> decode(Ref<BinaryBitmap> image);
			virtual Ref<Result> decodeRow(int rowNumber, Ref<BitArray> row) = 0;
			
			static unsigned int patternMatchVariance(int counters[], int countersSize, const int pattern[], int maxIndividualVariance);
			static void recordPattern(Ref<BitArray> row, int start, int counters[], int countersCount);
			virtual ~OneDReader();
		};
	}
}
