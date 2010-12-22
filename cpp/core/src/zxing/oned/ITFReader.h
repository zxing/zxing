#ifndef __ITF_READER_H__
#define __ITF_READER_H__

/*
 *  ITFReader.h
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
		class ITFReader : public OneDReader {
			
		private:
			//static const unsigned int MAX_AVG_VARIANCE = (unsigned int) (PATTERN_MATCH_RESULT_SCALE_FACTOR * 0.42f);
      enum {MAX_AVG_VARIANCE = (unsigned int) (PATTERN_MATCH_RESULT_SCALE_FACTOR * 0.42f)};
			//static const int MAX_INDIVIDUAL_VARIANCE = (int) (PATTERN_MATCH_RESULT_SCALE_FACTOR * 0.8f);
			enum {MAX_INDIVIDUAL_VARIANCE = (int) (PATTERN_MATCH_RESULT_SCALE_FACTOR * 0.8f)};
			// Stores the actual narrow line width of the image being decoded.
			int narrowLineWidth;
			
			int* decodeStart(Ref<BitArray> row);																		//throws ReaderException
			int* decodeEnd(Ref<BitArray> row);																				//throws ReaderException 
			static void decodeMiddle(Ref<BitArray> row, int payloadStart, int payloadEnd, std::string& resultString);	//throws ReaderException
			void validateQuietZone(Ref<BitArray> row, int startPattern);												//throws ReaderException 
			static int skipWhiteSpace(Ref<BitArray> row);																//throws ReaderException 
			
			static int* findGuardPattern(Ref<BitArray> row, int rowOffset, const int pattern[], int patternLen);		//throws ReaderException
			static int decodeDigit(int counters[], int countersLen);													//throws ReaderException 
			
			void append(char* s, char c);
		public:
			Ref<Result> decodeRow(int rowNumber, Ref<BitArray> row);									///throws ReaderException
			ITFReader();
			~ITFReader();
		};
	}
}

#endif
