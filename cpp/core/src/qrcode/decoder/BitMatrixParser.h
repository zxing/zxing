#ifndef __BIT_MATRIX_PARSER_H__
#define __BIT_MATRIX_PARSER_H__

/*
 *  BitMatrixParser.h
 *  zxing
 *
 *  Created by Christian Brunschen on 20/05/2008.
 *  Copyright 2008 Google Inc. All rights reserved.
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

#include "../../ReaderException.h"
#include "../../common/BitMatrix.h"
#include "../../common/Counted.h"
#include "../../common/Array.h"
#include "Version.h"
#include "FormatInformation.h"


namespace qrcode {
  namespace decoder {
    
    using namespace common;
    
    class BitMatrixParser : public Counted {
    private:
      Ref<BitMatrix> bitMatrix_;
      Version *parsedVersion_;
      Ref<FormatInformation> parsedFormatInfo_;
      
      int copyBit(size_t i, size_t j, int versionBits);
      
    public:
      BitMatrixParser(Ref<BitMatrix> bitMatrix);
      Ref<FormatInformation> readFormatInformation();
      Version *readVersion();
      ArrayRef<unsigned char> readCodewords();
    };
    
  }
}


#endif // __BIT_MATRIX_PARSER_H__
