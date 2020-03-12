#pragma once

/*
 *  EdgeDetector.h
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




#include <zxing/common/BitMatrix.h>
#include <zxing/common/Point.h>

#include <vector>

namespace pping {

    //* 2012-05-16 hfn Because the two symbols "min" and "max" are used for other purposes sometimes,
    //* we have defined the functions "Minimum" and "Maximum" for int and float type values.
    //* 2012-05-23 hfn as templates
template<class T> T Maximum(T x, T y) {return (x) >= (y) ? (x) : (y);}
template<class T> T Minimum(T x, T y) {return (x) <= (y) ? (x) : (y);}

namespace EdgeDetector {

//* 2012-04-20 hfn included:
//* 2012-05-08 hfn "INFINITY": s. NaNny.h
#ifdef _MSC_VER
#endif


void findEdgePoints(std::vector<Point>& points, const BitMatrix& image, Point start, Point end, bool invert, int skip, float deviation);
Line findLine(const BitMatrix& image, Line estimate, bool invert, int deviation, float threshold, int skip);

Point intersection(Line a, Line b);

}
}
