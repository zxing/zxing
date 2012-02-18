// -*- mode:c++; tab-width:2; indent-tabs-mode:nil; c-basic-offset:2 -*-
/*
 *  AtztecDetecorResult.h
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

#include <zxing/common/DetectorResult.h>

#ifndef ZXingWidget_AtztecDetecorResult_h
#define ZXingWidget_AtztecDetecorResult_h

namespace zxing {
    namespace aztec {
        class AztecDetectorResult : public DetectorResult {
        private:
            bool compact_;
            int nbDatablocks_, nbLayers_;
        public:
            AztecDetectorResult(Ref<BitMatrix> bits, std::vector<Ref<ResultPoint> > points, bool compact, int nbDatablocks, int nbLayers);
            bool isCompact();
            int getNBDatablocks();
            int getNBLayers();
        };
    }
}

#endif
