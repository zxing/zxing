#pragma once

/*
 *  Version.h
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

#include <zxing/common/Counted.h>  // for Counted, Ref
#include <zxing/common/Error.hpp>

#include <vector>                  // for vector

namespace pping {
class BitMatrix;
}  // namespace pping

namespace pping {
namespace qrcode {

class ErrorCorrectionLevel;

class ECB {
private:
  int count_;
  int dataCodewords_;
public:
  ECB(int count, int dataCodewords);
  int getCount();
  int getDataCodewords();
};

class ECBlocks {
private:
  int ecCodewords_;
  std::vector<ECB*> ecBlocks_;
public:
  ECBlocks(int ecCodewords, ECB *ecBlocks);
  ECBlocks(int ecCodewords, ECB *ecBlocks1, ECB *ecBlocks2);
  int getECCodewords();
  std::vector<ECB*>& getECBlocks();
  ~ECBlocks();
};

class Version : public Counted {

private:
  int versionNumber_;
  std::vector<int> &alignmentPatternCenters_;
  std::vector<ECBlocks*> ecBlocks_;
  int totalCodewords_;

public:
  static unsigned int VERSION_DECODE_INFO[];
  constexpr static int N_VERSION_DECODE_INFOS = 34;

  Version(int versionNumber, std::vector<int> *alignmentPatternCenters, ECBlocks *ecBlocks1, ECBlocks *ecBlocks2,
          ECBlocks *ecBlocks3, ECBlocks *ecBlocks4) MB_NOEXCEPT_EXCEPT_BADALLOC;
  ~Version();
  int getVersionNumber();
  std::vector<int> &getAlignmentPatternCenters();
  int getTotalCodewords();
  int getDimensionForVersion();
  ECBlocks &getECBlocksForLevel(ErrorCorrectionLevel &ecLevel);
  static Fallible<Version *> getProvisionalVersionForDimension(int dimension) MB_NOEXCEPT_EXCEPT_BADALLOC;
  static Fallible<Version *> getVersionForNumber(int versionNumber) MB_NOEXCEPT_EXCEPT_BADALLOC;
  static Fallible<Version *> decodeVersionInformation(unsigned int versionBits) MB_NOEXCEPT_EXCEPT_BADALLOC;
  FallibleRef<BitMatrix> buildFunctionPattern();
  static int buildVersions();
};
}
}

