/*
 *  GridSampler.cpp
 *  zxing
 *
 *  Created by Christian Brunschen on 18/05/2008.
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

#include <zxing/common/GridSampler.h>
#include <zxing/common/PerspectiveTransform.h>
#include <zxing/ReaderException.h>
#include <iostream>
#include <sstream>

namespace zxing {
using namespace std;

GridSampler GridSampler::gridSampler;

GridSampler::GridSampler() {
}

Ref<BitMatrix> GridSampler::sampleGrid(Ref<BitMatrix> image, int dimension, Ref<PerspectiveTransform> transform) {
  Ref<BitMatrix> bits(new BitMatrix(dimension));
  vector<float> points(dimension << 1, (const float)0.0f);
  for (int y = 0; y < dimension; y++) {
    int max = points.size();
    float yValue = (float)y + 0.5f;
    for (int x = 0; x < max; x += 2) {
      points[x] = (float)(x >> 1) + 0.5f;
      points[x + 1] = yValue;
    }
    transform->transformPoints(points);
    checkAndNudgePoints(image, points);
    for (int x = 0; x < max; x += 2) {
      if (image->get((int)points[x], (int)points[x + 1])) {
        bits->set(x >> 1, y);
      }
    }
  }
  return bits;
}

Ref<BitMatrix> GridSampler::sampleGrid(Ref<BitMatrix> image, int dimensionX, int dimensionY, Ref<PerspectiveTransform> transform) {
  Ref<BitMatrix> bits(new BitMatrix(dimensionX, dimensionY));
  vector<float> points(dimensionX << 1, (const float)0.0f);
  for (int y = 0; y < dimensionY; y++) {
    int max = points.size();
    float yValue = (float)y + 0.5f;
    for (int x = 0; x < max; x += 2) {
      points[x] = (float)(x >> 1) + 0.5f;
      points[x + 1] = yValue;
    }
    transform->transformPoints(points);
    checkAndNudgePoints(image, points);
    for (int x = 0; x < max; x += 2) {
      if (image->get((int)points[x], (int)points[x + 1])) {
        bits->set(x >> 1, y);
      }
    }
  }
  return bits;
}

Ref<BitMatrix> GridSampler::sampleGrid(Ref<BitMatrix> image, int dimension, float p1ToX, float p1ToY, float p2ToX,
                                       float p2ToY, float p3ToX, float p3ToY, float p4ToX, float p4ToY, float p1FromX, float p1FromY, float p2FromX,
                                       float p2FromY, float p3FromX, float p3FromY, float p4FromX, float p4FromY) {
  Ref<PerspectiveTransform> transform(PerspectiveTransform::quadrilateralToQuadrilateral(p1ToX, p1ToY, p2ToX, p2ToY,
                                      p3ToX, p3ToY, p4ToX, p4ToY, p1FromX, p1FromY, p2FromX, p2FromY, p3FromX, p3FromY, p4FromX, p4FromY));

  return sampleGrid(image, dimension, transform);

}

void GridSampler::checkAndNudgePoints(Ref<BitMatrix> image, vector<float> &points) {
  int width = image->getWidth();
  int height = image->getHeight();


  // The Java code assumes that if the start and end points are in bounds, the rest will also be.
  // However, in some unusual cases points in the middle may also be out of bounds.
  // Since we can't rely on an ArrayIndexOutOfBoundsException like Java, we check every point.

  for (size_t offset = 0; offset < points.size(); offset += 2) {
    int x = (int)points[offset];
    int y = (int)points[offset + 1];
    if (x < -1 || x > width || y < -1 || y > height) {
      ostringstream s;
      s << "Transformed point out of bounds at " << x << "," << y;
      throw ReaderException(s.str().c_str());
    }

    if (x == -1) {
      points[offset] = 0.0f;
    } else if (x == width) {
      points[offset] = width - 1;
    }
    if (y == -1) {
      points[offset + 1] = 0.0f;
    } else if (y == height) {
      points[offset + 1] = height - 1;
    }
  }

}

GridSampler &GridSampler::getInstance() {
  return gridSampler;
}
}
