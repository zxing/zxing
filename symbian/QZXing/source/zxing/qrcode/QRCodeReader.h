// -*- mode:c++; tab-width:2; indent-tabs-mode:nil; c-basic-offset:2 -*-
#ifndef __QR_CODE_READER_H__
#define __QR_CODE_READER_H__

/*
 *  QRCodeReader.h
 *  zxing
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

#include <zxing/Reader.h>
#include <zxing/qrcode/decoder/Decoder.h>
#include <zxing/DecodeHints.h>

namespace zxing {
	namespace qrcode {
		
		class QRCodeReader : public Reader {
		private:
			Decoder decoder_;
			
    protected:
      Decoder& getDecoder();

		public:
			QRCodeReader();
			virtual Ref<Result> decode(Ref<BinaryBitmap> image, DecodeHints hints);
			virtual ~QRCodeReader();
			
		};
	}
}

#endif // __QR_CODE_READER_H__
