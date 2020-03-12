// -*- mode:c++; tab-width:2; indent-tabs-mode:nil; c-basic-offset:2 -*-

#pragma once

/*
 *  AlignmentPattern.h
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

#include <zxing/ResultPoint.h>     // for ResultPoint

#include "zxing/common/Counted.h"  // for Ref
#if defined _WIN32 || defined _W64
#include <winCompat.h>
#endif

namespace pping {
    namespace qrcode {
        
        class AlignmentPattern : public ResultPoint {
        private:
            float estimatedModuleSize_;
            
        public:
            AlignmentPattern(float posX, float posY, float estimatedModuleSize) noexcept;
            bool aboutEquals(float moduleSize, float i, float j) const noexcept;
      Ref<AlignmentPattern> combineEstimate(float i, float j,
                                            float newModuleSize) const MB_NOEXCEPT_EXCEPT_BADALLOC;
        };
        
    }
}

