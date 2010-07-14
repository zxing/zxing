/*
 *  MultiFormatUPCEANReader.cpp
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
#include "MultiFormatUPCEANReader.h"

#include <zxing/oned/EAN13Reader.h>
#include <zxing/oned/EAN8Reader.h>
#include <zxing/oned/UPCEReader.h>
#include <zxing/oned/OneDResultPoint.h>
#include <zxing/common/Array.h>
#include <zxing/ReaderException.h>
#include <math.h>

namespace zxing {
	namespace oned {
		
		MultiFormatUPCEANReader::MultiFormatUPCEANReader() : readers() {
			readers.push_back(Ref<OneDReader>(new EAN13Reader()));
			// UPC-A is covered by EAN-13
			readers.push_back(Ref<OneDReader>(new EAN8Reader()));
			readers.push_back(Ref<OneDReader>(new UPCEReader()));
		}
		
		Ref<Result> MultiFormatUPCEANReader::decodeRow(int rowNumber, Ref<BitArray> row){			
			// Compute this location once and reuse it on multiple implementations
			int size = readers.size();
			for (int i = 0; i < size; i++) {
				Ref<OneDReader> reader = readers[i];
				Ref<Result> result;
				try {
					result = reader->decodeRow(rowNumber, row);//decodeRow(rowNumber, row, startGuardPattern);
				} catch (ReaderException re) {
					continue;
				}
				// Special case: a 12-digit code encoded in UPC-A is identical to a "0"
				// followed by those 12 digits encoded as EAN-13. Each will recognize such a code,
				// UPC-A as a 12-digit string and EAN-13 as a 13-digit string starting with "0".
				// Individually these are correct and their readers will both read such a code
				// and correctly call it EAN-13, or UPC-A, respectively.
				//
				// In this case, if we've been looking for both types, we'd like to call it
				// a UPC-A code. But for efficiency we only run the EAN-13 decoder to also read
				// UPC-A. So we special case it here, and convert an EAN-13 result to a UPC-A
				// result if appropriate.
				if (result->getBarcodeFormat() == BarcodeFormat_EAN_13) {
					const std::string& text = (result->getText())->getText();
					if (text[0] == '0') {
						Ref<String> resultString(new String(text.substr(1)));
						Ref<Result> res(new Result(resultString, result->getRawBytes(), result->getResultPoints(), BarcodeFormat_UPC_A));
						return res;
					}
				}
				return result;
			}
			throw ReaderException("No EAN code detected");
		}
	}
}
