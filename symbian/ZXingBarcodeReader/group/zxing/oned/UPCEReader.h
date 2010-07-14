/*
 *  UPCEReader.h
 *  ZXing
 *
 *  Created by Lukasz Warchol on 10-01-26.
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

#include <zxing/oned/UPCEANReader.h>
#include <zxing/Result.h>

namespace zxing {
	namespace oned {
		class UPCEReader : public UPCEANReader {
			
		private:
			static void determineFirstDigit(std::string& resultString, int lgPatternFound);								//throws ReaderException
			static void determineNumSysAndCheckDigit(std::string& resultString, int lgPatternFound);						//throws ReaderException
		protected:
			int* decodeEnd(Ref<BitArray> row, int endStart);															//throws ReaderException
			bool checkChecksum(std::string s);																			//throws ReaderException 
		public:
			UPCEReader();
			
			int decodeMiddle(Ref<BitArray> row, int startRange[], int startRangeLen, std::string& resultString);		//throws ReaderException
			static std::string& convertUPCEtoUPCA(std::string upce);
			
			BarcodeFormat getBarcodeFormat();
		};
	}
}
