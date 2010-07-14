/*
 *  OneDResultPoint.cpp
 *  ZXing
 *
 *  Created by Lukasz Warchol on 10-01-20.
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

#include "OneDResultPoint.h"

namespace zxing {
	namespace oned {
		
		OneDResultPoint::OneDResultPoint(float posX, float posY) : posX_(posX), posY_(posY){
		}
		
		float OneDResultPoint::getX() const {
			return posX_;
		}
		
		float OneDResultPoint::getY() const {
			return posY_;
		}
	}
}
