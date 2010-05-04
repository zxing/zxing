/*
 *  MultiFormatOneDReader.cpp
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

#include "MultiFormatOneDReader.h"

#include <zxing/oned/MultiFormatUPCEANReader.h>
#include <zxing/oned/Code39Reader.h>
#include <zxing/oned/Code128Reader.h>
#include <zxing/oned/ITFReader.h>
#include <zxing/ReaderException.h>

namespace zxing {
	namespace oned {
		MultiFormatOneDReader::MultiFormatOneDReader() : readers() {
			readers.push_back(Ref<OneDReader>(new MultiFormatUPCEANReader()));
			readers.push_back(Ref<OneDReader>(new Code39Reader()));
			readers.push_back(Ref<OneDReader>(new Code128Reader()));
			readers.push_back(Ref<OneDReader>(new ITFReader()));
		}
		
		Ref<Result> MultiFormatOneDReader::decodeRow(int rowNumber, Ref<BitArray> row){
			int size = readers.size();
			for (int i = 0; i < size; i++) {
				OneDReader* reader = readers[i];
				try {
					return reader->decodeRow(rowNumber, row);
				} catch (ReaderException re) {
					// continue
				}
			}
			throw ReaderException("No code detected");
		}
	}
}
