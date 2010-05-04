#ifndef __GF256_POLY_H__
#define __GF256_POLY_H__

/*
 *  GF256Poly.h
 *  zxing
 *
 *  Created by Christian Brunschen on 05/05/2008.
 *  Copyright 2008 Google UK. All rights reserved.
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
#include <zxing/common/Counted.h>
#include <zxing/common/Array.h>

namespace zxing {
class GF256;

class GF256Poly : public Counted {
private:
  GF256 &field;
  ArrayRef<int> coefficients;
  void fixCoefficients();
public:
  GF256Poly(GF256 &field, ArrayRef<int> c);
  ~GF256Poly();

  int getDegree();
  bool isZero();
  int getCoefficient(int degree);
  int evaluateAt(int a);
  Ref<GF256Poly> addOrSubtract(Ref<GF256Poly> other);
  Ref<GF256Poly> multiply(Ref<GF256Poly> other);
  Ref<GF256Poly> multiply(int scalar);
  Ref<GF256Poly> multiplyByMonomial(int degree, int coefficient);
  const char *description() const;
  friend std::ostream& operator<<(std::ostream& out, const GF256Poly& poly);

};
}

#endif // __GF256_POLY_H__
