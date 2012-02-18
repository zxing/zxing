// -*- mode:c++; tab-width:2; indent-tabs-mode:nil; c-basic-offset:2 -*-
/*
 *  AztecReader.h
 *  zxing
 *
 *  Created by Lukas Stabe on 08/02/2012.
 *  Copyright 2012 ZXing authors All rights reserved.
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
#include <zxing/aztec/decoder/Decoder.h>
#include <zxing/DecodeHints.h>

#ifndef ZXingWidget_AztecReader_h
#define ZXingWidget_AztecReader_h

namespace zxing {
    namespace aztec {
        
        class AztecReader : public Reader {
        private:
            Decoder decoder_;
            
        protected:
            Decoder &getDecoder();
            
        public:
            AztecReader();
            virtual Ref<Result> decode(Ref<BinaryBitmap> image);
            virtual Ref<Result> decode(Ref<BinaryBitmap> image, DecodeHints hints);
            virtual ~AztecReader();
        };
        
    }
}

#endif
