// -*- mode:c++; tab-width:2; indent-tabs-mode:nil; c-basic-offset:2 -*-
/*
 *  Binarizer.cpp
 *  zxing
 *
 *  Created by Ralf Kistner on 16/10/2009.
 *  Copyright 2008 ZXing authors All rights reserved.
 *  Modified by Lukasz Warchol on 02/02/2010.
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

#include <zxing/Binarizer.h>

namespace zxing {
	
	Binarizer::Binarizer(Ref<LuminanceSource> source) : source_(source) {
  }
	
	Binarizer::~Binarizer() {
	}
	
	Ref<LuminanceSource> Binarizer::getLuminanceSource() const {
		return source_;
	}
	
}
