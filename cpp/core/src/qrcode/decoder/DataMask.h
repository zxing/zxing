#ifndef __DATA_MASK_H__
#define __DATA_MASK_H__

/*
 *  DataMask.h
 *  zxing
 *
 *  Created by Christian Brunschen on 19/05/2008.
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

#include "../../common/Array.h"
#include <vector>

namespace qrcode {
  namespace decoder {
    using namespace common;
    using namespace std;
    
    class DataMask {
    private:
      static vector<DataMask*> DATA_MASKS;
      
    protected:

    public:
      static int buildDataMasks();
      DataMask() { }
      virtual ~DataMask() { }
      virtual void unmaskBitMatrix(valarray<unsigned int> &bits,
                                   int dimension) = 0;
      static DataMask& forReference(int reference);
    };
    
  }
}

#endif // __DATA_MASK_H__
