#ifndef __DETECTOR_RESULT_H__
#define __DETECTOR_RESULT_H__

/*
 *  DetectorResult.h
 *  zxing
 *
 *  Created by Christian Brunschen on 14/05/2008.
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

#include "Counted.h"
#include "Array.h"
#include "BitMatrix.h"
#include "ResultPoint.h"

namespace common {
  class DetectorResult : public Counted {
  private:
    Ref<BitMatrix> bits_;
    ArrayRef<Ref<ResultPoint> > points_;
    
  public:
    DetectorResult(Ref<BitMatrix> bits, ArrayRef<Ref<ResultPoint> >points) :
    bits_(bits), points_(points) { }
    Ref<BitMatrix> getBits() { return bits_; }
    ArrayRef<Ref<ResultPoint> > getPoints() { return points_; }
  };
}

#endif // __DETECTOR_RESULT_H__