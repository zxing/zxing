// -*- mode:c++; tab-width:2; indent-tabs-mode:nil; c-basic-offset:2 -*-
/*
 *  AtztecDetecorResult.cpp
 *  zxing
 *
 *  Created by Lukas Stabe on 08/02/2012.
 *  Copyright 2012 ZXing authors All rights reserved.
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

#include <zxing/aztec/AztecDetectorResult.h>

namespace zxing {
    namespace aztec {
        AztecDetectorResult::AztecDetectorResult(Ref<BitMatrix> bits, std::vector<Ref<ResultPoint> > points, bool compact, int nbDatablocks, int nbLayers)
        : DetectorResult(bits, points),
        compact_(compact),
        nbDatablocks_(nbDatablocks),
        nbLayers_(nbLayers) {
        };
        
        bool AztecDetectorResult::isCompact() {
            return compact_;
        }
        
        int AztecDetectorResult::getNBDatablocks() {
            return nbDatablocks_;
        }
        
        int AztecDetectorResult::getNBLayers() {
            return nbLayers_;
        }
    }
}