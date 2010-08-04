#ifndef __CODE_128_READER_H__
#define __CODE_128_READER_H__
/*
 *  Code128Reader.h
 *  ZXing
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

#include <zxing/oned/OneDReader.h>
#include <zxing/common/BitArray.h>
#include <zxing/Result.h>

namespace zxing {
	namespace oned {
		class Code128Reader : public OneDReader {
			
		private:
			static const unsigned int MAX_AVG_VARIANCE = (unsigned int) (PATTERN_MATCH_RESULT_SCALE_FACTOR * 0.25f);
			static const int MAX_INDIVIDUAL_VARIANCE = (int) (PATTERN_MATCH_RESULT_SCALE_FACTOR * 0.7f);
			
			static const int CODE_SHIFT = 98;
			
			static const int CODE_CODE_C = 99;
			static const int CODE_CODE_B = 100;
			static const int CODE_CODE_A = 101;
			
			static const int CODE_FNC_1 = 102;
			static const int CODE_FNC_2 = 97;
			static const int CODE_FNC_3 = 96;
			static const int CODE_FNC_4_A = 101;
			static const int CODE_FNC_4_B = 100;
			
			static const int CODE_START_A = 103;
			static const int CODE_START_B = 104;
			static const int CODE_START_C = 105;
			static const int CODE_STOP = 106;
			
			static int* findStartPattern(Ref<BitArray> row);
			static int decodeCode(Ref<BitArray> row, int counters[], int countersCount, int rowOffset);
			
			void append(char* s, char c);
		public:
			Ref<Result> decodeRow(int rowNumber, Ref<BitArray> row);
			Code128Reader();
			~Code128Reader();
		};
	}
}

#endif
