/*
 *  UPCEANReader.h
 *  ZXing
 *
 *  Created by Lukasz Warchol on 10-01-21.
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

#include <zxing/oned/OneDReader.h>
#include <zxing/common/BitArray.h>
#include <zxing/Result.h>
typedef enum UPC_EAN_PATTERNS {
	UPC_EAN_PATTERNS_L_PATTERNS = 0,
	UPC_EAN_PATTERNS_L_AND_G_PATTERNS
} UPC_EAN_PATTERNS;
namespace zxing {
	namespace oned {
		class UPCEANReader : public OneDReader {
			
		private:
			static const unsigned int MAX_AVG_VARIANCE = (unsigned int) (PATTERN_MATCH_RESULT_SCALE_FACTOR * 0.42f);
			static const int MAX_INDIVIDUAL_VARIANCE = (int) (PATTERN_MATCH_RESULT_SCALE_FACTOR * 0.7f);
			
			static int* findStartGuardPattern(Ref<BitArray> row);																	//throws ReaderException
			
			int* decodeEnd(Ref<BitArray> row, int endStart);																		//throws ReaderException
			
			static bool checkStandardUPCEANChecksum(std::string s);																	//throws ReaderException 
		protected:
			static int* findGuardPattern(Ref<BitArray> row, int rowOffset, bool whiteFirst, const int pattern[], int patternLen);	//throws ReaderException 
			
			virtual const int getMIDDLE_PATTERN_LEN();
			virtual const int* getMIDDLE_PATTERN();
			
		public:
			UPCEANReader();
			
			virtual int decodeMiddle(Ref<BitArray> row, int startRange[], int startRangeLen, std::string& resultString) = 0;			//throws ReaderException
			
			Ref<Result> decodeRow(int rowNumber, Ref<BitArray> row);
			Ref<Result> decodeRow(int rowNumber, Ref<BitArray> row, int startGuardRange[]);
			
			static int decodeDigit(Ref<BitArray> row, int counters[], int countersLen, int rowOffset, UPC_EAN_PATTERNS patternType);	//throws ReaderException 
			
			bool checkChecksum(std::string s);																						//throws ReaderException
			
			virtual BarcodeFormat getBarcodeFormat() = 0;
			virtual ~UPCEANReader();
		};
	}
}

