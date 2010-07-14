/*
 *  MultiFormatBarcodeReader.cpp
 *  ZXing
 *
 *  Created by Lukasz Warchol on 10-01-26.
 *  Modified by Luiz Silva on 09/02/2010.
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

#include <zxing/MultiFormatReader.h>
#include <zxing/qrcode/QRCodeReader.h>
//#include <zxing/datamatrix/DataMatrixReader.h>
#include <zxing/oned/MultiFormatUPCEANReader.h>
#include <zxing/oned/MultiFormatOneDReader.h>
#include <zxing/ReaderException.h>

namespace zxing {
	MultiFormatReader::MultiFormatReader() {
		readers.push_back(new zxing::qrcode::QRCodeReader());
		//readers.push_back(new zxing::datamatrix::DataMatrixReader());
		readers.push_back(new zxing::oned::MultiFormatUPCEANReader());
		readers.push_back(new zxing::oned::MultiFormatOneDReader());
	}
	
	Ref<Result> MultiFormatReader::decode(Ref<BinaryBitmap> image){
		for (unsigned int i = 0; i < readers.size(); i++) {
			try {
				return readers[i]->decode(image);
			} catch (ReaderException re) {
				// continue
			}
		}
		throw ReaderException("No code detected");
	}
	
	MultiFormatReader::~MultiFormatReader(){
		for (unsigned int i = 0; i < readers.size(); i++) {
			delete readers[i];
		}
	}
}
