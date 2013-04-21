#ifndef __MODULUS_GFPOLY_PDF_H__
#define __MODULUS_GFPOLY_PDF_H__

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

#include <zxing/common/Counted.h>
#include <zxing/common/Array.h>
#include <zxing/common/DecoderResult.h>
#include <zxing/common/BitMatrix.h>

namespace zxing {
namespace pdf417 {
namespace decoder {
namespace ec {

class ModulusGF;

/**
 * @author Sean Owen
 * @see com.google.zxing.common.reedsolomon.GenericGFPoly
 */
class ModulusPoly: public Counted {

  private:
	ModulusGF &field_;
	ArrayRef<int> coefficients_;
  public:
	ModulusPoly(ModulusGF& field, ArrayRef<int> coefficients);
	~ModulusPoly();
	ArrayRef<int> getCoefficients();
	int getDegree();
	bool isZero();
	int getCoefficient(int degree);
	int evaluateAt(int a);
	Ref<ModulusPoly> add(Ref<ModulusPoly> other);
	Ref<ModulusPoly> subtract(Ref<ModulusPoly> other);
	Ref<ModulusPoly> multiply(Ref<ModulusPoly> other);
	Ref<ModulusPoly> negative();
	Ref<ModulusPoly> multiply(int scalar);
	Ref<ModulusPoly> multiplyByMonomial(int degree, int coefficient);
	std::vector<Ref<ModulusPoly> > divide(Ref<ModulusPoly> other);
	#if 0
    public String toString();
	#endif
};

}
}
}
}

#endif /* __MODULUS_GFPOLY_PDF_H__ */
