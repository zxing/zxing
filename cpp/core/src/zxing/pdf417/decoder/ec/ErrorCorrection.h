#pragma once

/*
 * Copyright 2012 ZXing authors
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
 *
 * 2012-09-17 HFN translation from Java into C++
 */

#include <zxing/common/Array.h>    // for ArrayRef
#include <zxing/common/Counted.h>  // for Counted
#include "zxing/common/Error.hpp"

#include <vector>                  // for vector

namespace pping {
namespace pdf417 {


/**
 * <p>PDF417 error correction implementation.</p>
 *
 * <p>This <a href="http://en.wikipedia.org/wiki/Reed%E2%80%93Solomon_error_correction#Example">example</a>
 * is quite useful in understanding the algorithm.</p>
 *
 * @author Sean Owen
 * @see com.google.zxing.common.reedsolomon.ReedSolomonDecoder
 */
class ModulusGF;
class ModulusPoly;

class ErrorCorrection: public Counted {

  private:
    ModulusGF &field_;

  public:
    ErrorCorrection();
    Fallible<void> decode(ArrayRef<int> received,
                     int numECCodewords,
                     ArrayRef<int> erasures) MB_NOEXCEPT_EXCEPT_BADALLOC;

  private:
    Fallible<std::vector<Ref<ModulusPoly> > > runEuclideanAlgorithm(Ref<ModulusPoly> a, Ref<ModulusPoly> b, int R) MB_NOEXCEPT_EXCEPT_BADALLOC;

    Fallible<ArrayRef<int> > findErrorLocations(Ref<ModulusPoly> errorLocator) MB_NOEXCEPT_EXCEPT_BADALLOC;
    Fallible<ArrayRef<int> > findErrorMagnitudes(Ref<ModulusPoly> errorEvaluator,
                                    Ref<ModulusPoly> errorLocator,
                                    ArrayRef<int> errorLocations) MB_NOEXCEPT_EXCEPT_BADALLOC;
};

} /* namespace pdf417 */
} /* namespace zxing */

