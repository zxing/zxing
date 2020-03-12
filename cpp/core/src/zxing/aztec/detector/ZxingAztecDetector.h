// -*- mode:c++; tab-width:2; indent-tabs-mode:nil; c-basic-offset:2 -*-
/*
 *  Detector.h
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




#include <zxing/common/BitArray.h>
#include <zxing/ResultPoint.h>
#include <zxing/common/BitMatrix.h>
#include <zxing/DecodeHints.h>
#include <zxing/aztec/AztecDetectorResult.h>

namespace pping {
    namespace aztec {
        
        class Point : public Counted {
        public:
            int x;
            int y;
            
            Ref<ResultPoint> toResultPoint() { 
                return Ref<ResultPoint>(new ResultPoint((float)x, (float)y));
            }
            
            Point(int ax, int ay):x(ax),y(ay) {};
            
        };
        
        class Detector : public Counted {
            
        private:
            Ref<BitMatrix> image_;
            
            bool compact_;
            int nbLayers_;
            int nbDataBlocks_;
            int nbCenterLayers_;
            int shift_;
            
            Fallible<void> extractParameters(std::vector<Ref<Point> > bullEyeCornerPoints) MB_NOEXCEPT_EXCEPT_BADALLOC;
            Fallible<std::vector<Ref<ResultPoint>>> getMatrixCornerPoints(std::vector<Ref<Point> > bullEyeCornerPoints) MB_NOEXCEPT_EXCEPT_BADALLOC;
            static Fallible<void> correctParameterData(Ref<BitArray> parameterData, bool compact) MB_NOEXCEPT_EXCEPT_BADALLOC;
            Fallible<std::vector<Ref<Point>>> getBullEyeCornerPoints(Ref<Point> pCenter) MB_NOEXCEPT_EXCEPT_BADALLOC;
            Ref<Point> getMatrixCenter() MB_NOEXCEPT_EXCEPT_BADALLOC;
            FallibleRef<BitMatrix> sampleGrid(Ref<BitMatrix> image,
                                      Ref<ResultPoint> topLeft,
                                      Ref<ResultPoint> bottomLeft,
                                      Ref<ResultPoint> bottomRight,
                                      Ref<ResultPoint> topRight);
            void getParameters(Ref<BitArray> parameterData) noexcept;
            Ref<BitArray> sampleLine(Ref<Point> p1, Ref<Point> p2, int size) MB_NOEXCEPT_EXCEPT_BADALLOC;
            bool isWhiteOrBlackRectangle(Ref<Point> p1,
                                         Ref<Point> p2,
                                         Ref<Point> p3,
                                         Ref<Point> p4);
            int getColor(Ref<Point> p1, Ref<Point> p2);
            Ref<Point> getFirstDifferent(Ref<Point> init, bool color, int dx, int dy);
            bool isValid(int x, int y);
            static float distance(Ref<Point> a, Ref<Point> b);
            
        public:
            Detector(Ref<BitMatrix> image) noexcept;
            FallibleRef<AztecDetectorResult> detect() MB_NOEXCEPT_EXCEPT_BADALLOC;
        };
        
    }
}
