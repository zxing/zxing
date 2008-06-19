/*
 *  GridSampler.cpp
 *  zxing
 *
 *  Created by Christian Brunschen on 18/05/2008.
 *  Copyright 2008 Google Inc. All rights reserved.
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

#include "GridSampler.h"
#include "PerspectiveTransform.h"
#include "ReaderException.h"
#include <iostream>
#include <sstream>

namespace common {
  GridSampler GridSampler::gridSampler;
  
  GridSampler::GridSampler() { }
  
  Ref<BitMatrix> GridSampler::sampleGrid(Ref<MonochromeBitmapSource> image,
                                         int dimension,
                                         float p1ToX, float p1ToY,
                                         float p2ToX, float p2ToY,
                                         float p3ToX, float p3ToY,
                                         float p4ToX, float p4ToY,
                                         float p1FromX, float p1FromY,
                                         float p2FromX, float p2FromY,
                                         float p3FromX, float p3FromY,
                                         float p4FromX, float p4FromY) {
    Ref<PerspectiveTransform> transform
    (PerspectiveTransform::quadrilateralToQuadrilateral
     (p1ToX, p1ToY, p2ToX, p2ToY, p3ToX, p3ToY, p4ToX, p4ToY,
      p1FromX, p1FromY, p2FromX, p2FromY, p3FromX, p3FromY, p4FromX, p4FromY));
    
    Ref<BitMatrix> bits(new BitMatrix(dimension));
    valarray<float> points(dimension << 1);
    for (int i = 0; i < dimension; i++) {
      int max = points.size();
      float iValue = (float) i + 0.5f;
      for (int j = 0; j < max; j+= 2) {
        points[j] = (float) (j >> 1) + 0.5f;
        points[j + 1] = iValue;
      }
      transform->transformPoints(points);
      checkAndNudgePoints(image, points);
      for (int j = 0; j < max; j += 2) {
        if (image->isBlack((int) points[j], (int) points[j + 1])) {
          bits->set(i, j >> 1);
        }
      }
    }
    
    return bits;
  }
  
  void GridSampler::checkAndNudgePoints(Ref<MonochromeBitmapSource> image,
                                        valarray<float> &points) {
    int width = image->getWidth();
    int height = image->getHeight();
    // Check and nudge points from start until we see some that are OK:
    bool nudged = true;
    for (size_t offset = 0; offset < points.size() && nudged; offset += 2) {
      int x = (int) points[offset];
      int y = (int) points[offset + 1];
      if (x < -1 || x > width || y < -1 || y > height) {
        ostringstream s;
        s << "Transformed point out of bounds at " << x << "," << y;
        throw new ReaderException(s.str().c_str());
      }
      nudged = false;
      if (x == -1) {
        points[offset] = 0.0f;
        nudged = true;
      } else if (x == width) {
        points[offset] = width - 1;
        nudged = true;
      }
      if (y == -1) {
        points[offset + 1] = 0.0f;
        nudged = true;
      } else if (y == height) {
        points[offset + 1] = height - 1;
        nudged = true;
      }
    }
    // Check and nudge points from end:
    nudged = true;
    for (size_t offset = points.size() - 2; offset >= 0 && nudged; offset -= 2) {
      int x = (int) points[offset];
      int y = (int) points[offset + 1];
      if (x < -1 || x > width || y < -1 || y > height) {
        ostringstream s;
        s << "Transformed point out of bounds at " << x << "," << y;
        throw new ReaderException(s.str().c_str());
      }
      nudged = false;
      if (x == -1) {
        points[offset] = 0.0f;
        nudged = true;
      } else if (x == width) {
        points[offset] = width - 1;
        nudged = true;
      }
      if (y == -1) {
        points[offset + 1] = 0.0f;
        nudged = true;
      } else if (y == height) {
        points[offset + 1] = height - 1;
        nudged = true;
      }
    }
  }
  
  GridSampler &GridSampler::getInstance() {
    return gridSampler;
  }
}
