#ifndef __DECODED_BIT_STREAM_PARSER_H__
#define __DECODED_BIT_STREAM_PARSER_H__

/*
 *  DecodedBitStreamParser.h
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

#include <string>
#include <sstream>
#include "Mode.h"
#include "../../common/BitSource.h"
#include "../../common/Counted.h"
#include "../../common/Array.h"

#include <iconv.h>

namespace qrcode {
  namespace decoder {

    using namespace common;
    using namespace std;
    
    class DecodedBitStreamParser {
    private:
      static char ALPHANUMERIC_CHARS[];
      
      static char *ASCII;
      static char *ISO88591;
      static char *UTF8;
      static char *SHIFT_JIS;
      static char *EUC_JP;
      
      static void decodeKanjiSegment(Ref<BitSource> bits,
                                     ostringstream &result,
                                     int count);
      static void decodeByteSegment(Ref<BitSource> bits,
                                    ostringstream &result,
                                    int count);
      static void decodeAlphanumericSegment(Ref<BitSource> bits,
                                            ostringstream &result,
                                            int count);
      static void decodeNumericSegment(Ref<BitSource> bits,
                                       ostringstream &result,
                                       int count);
      static char *guessEncoding(unsigned char *bytes, int length);
      static void append(ostream &ost, unsigned char *bufIn, 
                         size_t nIn, const char *src);
      
    public:
      static string decode(ArrayRef<unsigned char> bytes, Version *version);
    };
    
  }
}

#endif // __DECODED_BIT_STREAM_PARSER_H__
