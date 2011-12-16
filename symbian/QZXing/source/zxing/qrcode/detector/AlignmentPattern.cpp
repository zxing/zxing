// -*- mode:c++; tab-width:2; indent-tabs-mode:nil; c-basic-offset:2 -*-
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
    ResultPoint(posX,posY), estimatedModuleSize_(estimatedModuleSize) {
}

bool AlignmentPattern::aboutEquals(float moduleSize, float i, float j) const {
  if (abs(i - getY()) <= moduleSize && abs(j - getX()) <= moduleSize) {
    float moduleSizeDiff = abs(moduleSize - estimatedModuleSize_);
    return moduleSizeDiff <= 1.0f || moduleSizeDiff <= estimatedModuleSize_;
  }
  return false;
}

Ref<AlignmentPattern> AlignmentPattern::combineEstimate(float i, float j, float newModuleSize) const {
  float combinedX = (getX() + j) / 2.0f;
  float combinedY = (getY() + i) / 2.0f;
  float combinedModuleSize = (estimatedModuleSize_ + newModuleSize) / 2.0f;
  Ref<AlignmentPattern> result
    (new AlignmentPattern(combinedX, combinedY, combinedModuleSize));
  return result;
}

}
}
