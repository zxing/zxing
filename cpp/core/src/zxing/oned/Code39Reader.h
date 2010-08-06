#ifndef __CODE_39_READER_H__
#define __CODE_39_READER_H__
/*
 *  Code39Reader.h
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
		
		/**
		 * <p>Decodes Code 39 barcodes. This does not support "Full ASCII Code 39" yet.</p>
		 * Ported form Java (author Sean Owen)
		 * @author Lukasz Warchol
		 */
		class Code39Reader : public OneDReader {
			
		private:
			std::string alphabet_string;

			bool usingCheckDigit;
			bool extendedMode;
			
			static int* findAsteriskPattern(Ref<BitArray> row);														//throws ReaderException 
			static int toNarrowWidePattern(int counters[], int countersLen);
			static char patternToChar(int pattern);																	//throws ReaderException 
			static Ref<String> decodeExtended(std::string encoded);													//throws ReaderException 
			
			void append(char* s, char c);
		public:
			Code39Reader();
			Code39Reader(bool usingCheckDigit_);
			Code39Reader(bool usingCheckDigit_, bool extendedMode_);
			
			Ref<Result> decodeRow(int rowNumber, Ref<BitArray> row);
    };
	}
}

#endif
