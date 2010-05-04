/*
 *  AlignmentPattern.cpp
 *  zxing
 *
 *  Created by Christian Brunschen on 13/05/2008.
 *  Copyright 2008 ZXing authors All rights reserved.
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

#include <zxing/qrcode/detector/AlignmentPattern.h>

namespace zxing {
namespace qrcode {

using namespace std;

AlignmentPattern::AlignmentPattern(float posX, float posY, float estimatedModuleSize) :
    posX_(posX), posY_(posY), estimatedModuleSize_(estimatedModuleSize) {
}

float AlignmentPattern::getX() const {
  return posX_;
}

float AlignmentPattern::getY() const {
  return posY_;
}

bool AlignmentPattern::aboutEquals(float moduleSize, float i, float j) const {
  return abs(i - posY_) <= moduleSize && abs(j - posX_) <= moduleSize && (abs(moduleSize - estimatedModuleSize_)
         <= 1.0f || abs(moduleSize - estimatedModuleSize_) / estimatedModuleSize_ <= 0.1f);
}

}
}
