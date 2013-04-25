#ifndef __MODULUS_GF_PDF_H__
#define __MODULUS_GF_PDF_H__
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

class ModulusPoly;

/**
 * <p>A field based on powers of a generator integer, modulo some modulus.</p>
 *
 * @author Sean Owen
 * @see com.google.zxing.common.reedsolomon.GenericGF
 */
class ModulusGF {

  public: 
	static ModulusGF PDF417_GF;

  private:
	ArrayRef<int> expTable_;
	ArrayRef<int> logTable_;
	Ref<ModulusPoly> zero_;
	Ref<ModulusPoly> one_;
	int modulus_;

  public:
	ModulusGF(int modulus, int generator);
	Ref<ModulusPoly> getZero();
	Ref<ModulusPoly> getOne();
	Ref<ModulusPoly> buildMonomial(int degree, int coefficient);

	int add(int a, int b);
	int subtract(int a, int b);
	int exp(int a);
	int log(int a);
	int inverse(int a);
	int multiply(int a, int b);
	int getSize();
  
};

}
}
}
}

#endif /* __MODULUS_GF_PDF_H__ */
