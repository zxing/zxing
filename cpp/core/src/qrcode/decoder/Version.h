#ifndef __VERSION_H__
#define __VERSION_H__

/*
 *  Version.h
 *  zxing
 *
 *  Created by Christian Brunschen on 14/05/2008.
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

#include "../../common/Counted.h"
#include "ErrorCorrectionLevel.h"
#include "../../ReaderException.h"
#include "../../common/BitMatrix.h"
#include <vector>
#include <valarray>

namespace qrcode {
  namespace decoder {
    using namespace std;
    using namespace common;
        
    class Version : public Counted {
    public:
      class ECB {
      private:
        int count_;
        int dataCodewords_;
      public:
        ECB(int count, int dataCodewords) : 
        count_(count), dataCodewords_(dataCodewords) { }
        int getCount() { return count_; }
        int getDataCodewords() { return dataCodewords_; }
      };
      
      class ECBlocks { 
      private:
        int ecCodewords_;
        vector<ECB*> ecBlocks_;
      public:
        ECBlocks(int ecCodewords, ECB *ecBlocks) : ecCodewords_(ecCodewords) {
          ecBlocks_.push_back(ecBlocks);
        }
        ECBlocks(int ecCodewords, ECB *ecBlocks1, ECB *ecBlocks2) : 
        ecCodewords_(ecCodewords) {
          ecBlocks_.push_back(ecBlocks1);
          ecBlocks_.push_back(ecBlocks2);
        }
        int getECCodewords() { return ecCodewords_; }
        vector<ECB*>& getECBlocks() { return ecBlocks_; }
      };
      
      
    private:      
      int versionNumber_;
      valarray<int> &alignmentPatternCenters_;
      vector<ECBlocks*> ecBlocks_;
      int totalCodewords_;
      Version(int versionNumber,
              valarray<int> *alignmentPatternCenters,
              ECBlocks *ecBlocks1,
              ECBlocks *ecBlocks2,
              ECBlocks *ecBlocks3,
              ECBlocks *ecBlocks4);
      
    public:
      static unsigned int VERSION_DECODE_INFO[];
      static int N_VERSION_DECODE_INFOS;
      static vector<Version*> VERSIONS;
      
      int getVersionNumber();
      valarray<int> &getAlignmentPatternCenters();
      int getTotalCodewords();
      int getDimensionForVersion();
      ECBlocks &getECBlocksForLevel(ErrorCorrectionLevel &ecLevel);
      static Version *getProvisionalVersionForDimension(int dimension);
      static Version *getVersionForNumber(int versionNumber);
      static Version *decodeVersionInformation(unsigned int versionBits);
      Ref<BitMatrix> buildFunctionPattern();
      static int buildVersions();
    };
  }
}

#endif // __VERSION_H__
