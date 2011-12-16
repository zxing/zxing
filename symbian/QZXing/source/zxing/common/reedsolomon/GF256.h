#ifndef __GF256_H__
#define __GF256_H__

/*
 *  GF256.h
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

namespace zxing {
class GF256Poly;

class GF256 {
  /**
   * <p>This class contains utility methods for performing mathematical
   * operations over the Galois Field GF(256). Operations use a given
   * primitive polynomial in calculations.</p>
   *
   * <p>Throughout this package, elements of GF(256) are represented as an
   * <code>int</code> for convenience and speed (but at the cost of memory).
   * Only the bottom 8 bits are really used.</p>
   *
   * @author srowen@google.com (Sean Owen)
   * @author christian.brunschen@gmail.com (Christian Brunschen)
   */
private:
  std::vector<int> exp_;
  std::vector<int> log_;
  Ref<GF256Poly> zero_;
  Ref<GF256Poly> one_;

  GF256(int primitive);

public:
  Ref<GF256Poly> getZero();
  Ref<GF256Poly> getOne();
  Ref<GF256Poly> buildMonomial(int degree, int coefficient);
  static int addOrSubtract(int a, int b);
  int exp(int a);
  int log(int a);
  int inverse(int a);
  int multiply(int a, int b);

  static GF256 QR_CODE_FIELD;
  static GF256 DATA_MATRIX_FIELD;

  friend std::ostream& operator<<(std::ostream& out, const GF256& field);
};
}

#endif // __GF256_H__
