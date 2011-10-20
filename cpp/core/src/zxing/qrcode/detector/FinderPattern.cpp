// -*- mode:c++; tab-width:2; indent-tabs-mode:nil; c-basic-offset:2 -*-
/*
 *  FinderPattern.cpp
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

#include <zxing/qrcode/detector/FinderPattern.h>

namespace zxing {
	namespace qrcode {
		
		using namespace std;
		
		FinderPattern::FinderPattern(float posX, float posY, float estimatedModuleSize) :
		ResultPoint(posX,posY), estimatedModuleSize_(estimatedModuleSize), count_(1) {
		}
		
		FinderPattern::FinderPattern(float posX, float posY, float estimatedModuleSize, int count) :
		ResultPoint(posX,posY), estimatedModuleSize_(estimatedModuleSize), count_(count) {
		}
		
		int FinderPattern::getCount() const {
			return count_;
		}
		
		float FinderPattern::getEstimatedModuleSize() const {
			return estimatedModuleSize_;
		}
		
		void FinderPattern::incrementCount() {
			count_++;
		}
		
/*
		bool FinderPattern::aboutEquals(float moduleSize, float i, float j) const {
			return abs(i - posY_) <= moduleSize && abs(j - posX_) <= moduleSize && (abs(moduleSize - estimatedModuleSize_)
																					<= 1.0f || abs(moduleSize - estimatedModuleSize_) / estimatedModuleSize_ <= 0.1f);
		}
*/
    bool FinderPattern::aboutEquals(float moduleSize, float i, float j) const {
      if (abs(i - getY()) <= moduleSize && abs(j - getX()) <= moduleSize) {
        float moduleSizeDiff = abs(moduleSize - estimatedModuleSize_);
        return moduleSizeDiff <= 1.0f || moduleSizeDiff <= estimatedModuleSize_;
      }
      return false;
    }
		
    Ref<FinderPattern> FinderPattern::combineEstimate(float i, float j, float newModuleSize) const {
      int combinedCount = count_ + 1;
      float combinedX = (count_ * getX() + j) / combinedCount;
      float combinedY = (count_ * getY() + i) / combinedCount;
      float combinedModuleSize = (count_ * getEstimatedModuleSize() + newModuleSize) / combinedCount;
      return Ref<FinderPattern>(new FinderPattern(combinedX, combinedY, combinedModuleSize, combinedCount));
    }
	}
}
