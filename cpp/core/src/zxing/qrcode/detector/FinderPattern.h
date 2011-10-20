// -*- mode:c++; tab-width:2; indent-tabs-mode:nil; c-basic-offset:2 -*-
#ifndef __FINDER_PATTERN_H__
#define __FINDER_PATTERN_H__

/*
 *  FinderPattern.h
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

#include <zxing/ResultPoint.h>
#include <cmath>

namespace zxing {
	namespace qrcode {
		
		class FinderPattern : public ResultPoint {
		private:
			float estimatedModuleSize_;
			int count_;
			
		public:
			FinderPattern(float posX, float posY, float estimatedModuleSize);
			FinderPattern(float posX, float posY, float estimatedModuleSize, int count);
			int getCount() const;
			float getEstimatedModuleSize() const;
			void incrementCount();
			bool aboutEquals(float moduleSize, float i, float j) const;
			Ref<FinderPattern> combineEstimate(float i, float j, float newModuleSize) const;
		};
	}
}

#endif // __FINDER_PATTERN_H__
