#ifndef __DECODER_H__
#define __DECODER_H__

/*
 *  Decoder.h
 *  zxing
 *
 *  Created by Christian Brunschen on 20/05/2008.
 *  Copyright 2008 ZXing authors All rights reserved.
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

#include "../../common/reedsolomon/ReedSolomonDecoder.h"
#include "../../common/reedsolomon/GF256.h"
#include "../../common/Counted.h"
#include "../../common/Array.h"
#include "../../common/DecoderResult.h"
#include "../../common/BitMatrix.h"
#include <valarray>

namespace qrcode {
  namespace decoder {
    
    using namespace common;
    using namespace reedsolomon;
    using namespace std;
    
    class Decoder {
    private:
      ReedSolomonDecoder rsDecoder_;
      
      void correctErrors(ArrayRef<unsigned char> bytes,
                         int numDataCodewords);
      
    public:
      Decoder() : rsDecoder_(GF256::QR_CODE_FIELD) { }
      Ref<DecoderResult> decode(Ref<BitMatrix> bits);
    };
    
  }
}


#endif // __DECODER_H__
