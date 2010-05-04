/*
 *  UPCAReader.h
 *  ZXing
 *
 *  Created by Lukasz Warchol on 10-01-25.
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

#include <zxing/oned/EAN13Reader.h>

namespace zxing {
	namespace oned {
		class UPCAReader : public UPCEANReader {
			
		private:
			EAN13Reader ean13Reader;
			static Ref<Result> maybeReturnResult(Ref<Result> result);														//throws ReaderException
			
		public:
			UPCAReader();
			
			int decodeMiddle(Ref<BitArray> row, int startRange[], int startRangeLen, std::string& resultString);			//throws ReaderException
			
			Ref<Result> decodeRow(int rowNumber, Ref<BitArray> row);														//throws ReaderException
			Ref<Result> decodeRow(int rowNumber, Ref<BitArray> row, int startGuardRange[]);									//throws ReaderException
			Ref<Result> decode(Ref<BinaryBitmap> image);
			
			BarcodeFormat getBarcodeFormat();
		};
	}
}
