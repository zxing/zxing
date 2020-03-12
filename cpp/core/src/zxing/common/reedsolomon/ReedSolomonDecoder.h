#pragma once

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

#include "zxing/common/Array.h"    // for ArrayRef
#include "zxing/common/Counted.h"  // for Ref
#include "zxing/common/Error.hpp"  // for Fallible

#include <vector>                  // for vector

namespace pping {
class GenericGF;
class GenericGFPoly;

class ReedSolomonDecoder {
private:
  Ref<GenericGF> field;
public:
  ReedSolomonDecoder(Ref<GenericGF> fld);
  ~ReedSolomonDecoder();
  Fallible<void> decode(ArrayRef<int> received, int twoS) MB_NOEXCEPT_EXCEPT_BADALLOC;
  Fallible<std::vector<Ref<GenericGFPoly>>> runEuclideanAlgorithm(Ref<GenericGFPoly> a, Ref<GenericGFPoly> b, int R);

private:
  Fallible<ArrayRef<int>> findErrorLocations(Ref<GenericGFPoly> errorLocator);
  Fallible<ArrayRef<int>> findErrorMagnitudes(Ref<GenericGFPoly> errorEvaluator, ArrayRef<int> errorLocations);
};
}

