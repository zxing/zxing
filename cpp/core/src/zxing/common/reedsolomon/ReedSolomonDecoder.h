#ifndef __REED_SOLOMON_DECODER_H__
#define __REED_SOLOMON_DECODER_H__

/*
 *  ReedSolomonDecoder.h
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

#include <memory>
#include <vector>
#include <zxing/common/Counted.h>
#include <zxing/common/Array.h>
#include <zxing/common/reedsolomon/GenericGFPoly.h>
#include <zxing/common/reedsolomon/GenericGF.h>

namespace zxing {
class GenericGFPoly;
class GenericGF;

class ReedSolomonDecoder {
private:
  Ref<GenericGF> field;
public:
  ReedSolomonDecoder(Ref<GenericGF> fld);
  ~ReedSolomonDecoder();
  void decode(ArrayRef<int> received, int twoS);
private:
  std::vector<Ref<GenericGFPoly> > runEuclideanAlgorithm(Ref<GenericGFPoly> a, Ref<GenericGFPoly> b, int R);
  ArrayRef<int> findErrorLocations(Ref<GenericGFPoly> errorLocator);
  ArrayRef<int> findErrorMagnitudes(Ref<GenericGFPoly> errorEvaluator, ArrayRef<int> errorLocations, bool dataMatrix);
};
}

#endif // __REED_SOLOMON_DECODER_H__
