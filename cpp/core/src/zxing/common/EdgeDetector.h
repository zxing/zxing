/*
 *  EdgeDetector.h
 *  zxing
 *
 *  Created by Ralf Kistner on 7/12/2009.
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

#ifndef EDGEDETECTOR_H_
#define EDGEDETECTOR_H_

#include <vector>
#include <zxing/common/BitMatrix.h>
#include <zxing/common/Point.h>

namespace zxing {
namespace EdgeDetector {

void findEdgePoints(std::vector<Point>& points, const BitMatrix& image, Point start, Point end, bool invert, int skip, float deviation);
Line findLine(const BitMatrix& image, Line estimate, bool invert, int deviation, float threshold, int skip);

Point intersection(Line a, Line b);

}
}
#endif /* EDGEDETECTOR_H_ */
