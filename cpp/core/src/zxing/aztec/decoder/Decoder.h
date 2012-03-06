// -*- mode:c++; tab-width:2; indent-tabs-mode:nil; c-basic-offset:2 -*-
/*
 *  Decoder.h
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

#include <zxing/common/DecoderResult.h>
#include <zxing/common/BitMatrix.h>
#include <zxing/common/Str.h>
#include <zxing/aztec/AztecDetectorResult.h>

namespace zxing {
    namespace aztec {
        
        class Decoder : public Counted {
        private:
            enum Table {
                UPPER,
                LOWER,
                MIXED,
                DIGIT,
                PUNCT,
                BINARY
            };
            
            static Table getTable(char t);
            static const char* getCharacter(Table table, int code);
            
            int numCodewords_;
            int codewordSize_;
            Ref<AztecDetectorResult> ddata_;
            int invertedBitCount_;
            
            Ref<String> getEncodedData(Ref<BitArray> correctedBits);
            Ref<BitArray> correctBits(Ref<BitArray> rawbits);
            Ref<BitArray> extractBits(Ref<BitMatrix> matrix);
            static Ref<BitMatrix> removeDashedLines(Ref<BitMatrix> matrix);
            static int readCode(Ref<BitArray> rawbits, int startIndex, int length);
            
            
        public:
            Decoder();
            Ref<DecoderResult> decode(Ref<AztecDetectorResult> detectorResult);
        };
        
    }
}

