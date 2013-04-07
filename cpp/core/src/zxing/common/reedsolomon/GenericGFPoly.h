// -*- mode:c++; tab-width:2; indent-tabs-mode:nil; c-basic-offset:2 -*-
/*
 *  GenericGFPoly.h
 *  zxing
 *
 *  Created by Lukas Stabe on 13/02/2012.
 *  Copyright 2012 ZXing authors All rights reserved.
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

#ifndef GENERICGFPOLY_H
#define GENERICGFPOLY_H

#include <vector>
#include <zxing/common/Array.h>
#include <zxing/common/Counted.h>

namespace zxing {

class GenericGF;
  
class GenericGFPoly : public Counted {
private:
  Ref<GenericGF> field_;
  ArrayRef<int> coefficients_;
    
public:
  GenericGFPoly(Ref<GenericGF> field, ArrayRef<int> coefficients);
  ArrayRef<int> getCoefficients();
  int getDegree();
  bool isZero();
  int getCoefficient(int degree);
  int evaluateAt(int a);
  Ref<GenericGFPoly> addOrSubtract(Ref<GenericGFPoly> other);
  Ref<GenericGFPoly> multiply(Ref<GenericGFPoly> other);
  Ref<GenericGFPoly> multiply(int scalar);
  Ref<GenericGFPoly> multiplyByMonomial(int degree, int coefficient);
  std::vector<Ref<GenericGFPoly> > divide(Ref<GenericGFPoly> other);
    

};

}

#endif //GENERICGFPOLY_H
