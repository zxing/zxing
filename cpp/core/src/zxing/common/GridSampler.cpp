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

#include <stddef.h>                             // for size_t
#include <zxing/ReaderException.h>              // for ReaderException
#include <zxing/common/GridSampler.h>
#include <zxing/common/PerspectiveTransform.h>  // for PerspectiveTransform
#include <string>                               // for allocator, basic_string, char_traits

#include "zxing/common/BitMatrix.h"             // for BitMatrix
#include "zxing/common/Counted.h"               // for Ref
#include "zxing/common/Error.hpp"

namespace pping {
using namespace std;

GridSampler GridSampler::gridSampler;

GridSampler::GridSampler() {
}

FallibleRef<BitMatrix> GridSampler::sampleGrid(Ref<BitMatrix> image, int dimension, Ref<PerspectiveTransform> transform) MB_NOEXCEPT_EXCEPT_BADALLOC
#if !defined( DEBUG ) && defined( __clang__ )
    /** @note
     * In release mode, ASAN finds container-overflow when accessing first
     * vector element, even though it is correctly initialised.
     *                            (08.01.2018.) (Nenad Miksa)
     */
    __attribute__(( no_sanitize( "address" ) ))
#endif
{
  Ref<BitMatrix> bits(new BitMatrix(dimension));
  vector<float> points(dimension << 1, (float)0.0f);
  for (int y = 0; y < dimension; y++) {
    int max = (int)points.size();
    float yValue = (float)y + 0.5f;
    for (int x = 0; x < max; x += 2) {
      points[x] = (float)(x >> 1) + 0.5f;
      points[x + 1] = yValue;
    }
    transform->transformPoints(points);

    auto const tryCheckAndNudge(checkAndNudgePoints(image, points));
    if(!tryCheckAndNudge)
        return tryCheckAndNudge.error();

    for (int x = 0; x < max; x += 2) {
      auto const imageX = static_cast<size_t>(points[x] + 0.5f);
      auto const imageY = static_cast<size_t>(points[x + 1] + 0.5f);

      if (imageX < image->getWidth() && imageY < image->getHeight() && image->get(imageX, imageY))
      {
        bits->set(x >> 1, y);
      }
    }
  }
  return bits;
}

FallibleRef<BitMatrix> GridSampler::sampleGrid(Ref<BitMatrix> image, int dimensionX, int dimensionY, Ref<PerspectiveTransform> transform) MB_NOEXCEPT_EXCEPT_BADALLOC
#if !defined( DEBUG ) && defined( __clang__ )
    /** @note
     * In release mode, ASAN finds container-overflow when accessing first
     * vector element, even though it is correctly initialised.
     *                            (08.01.2018.) (Nenad Miksa)
     */
    __attribute__(( no_sanitize( "address" ) ))
#endif
{
  Ref<BitMatrix> bits(new BitMatrix(dimensionX, dimensionY));
  vector<float> points(dimensionX << 1, (float)0.0f);
  for (int y = 0; y < dimensionY; y++) {
    int max = (int)points.size();
    float yValue = (float)y + 0.5f;
    for (int x = 0; x < max; x += 2) {
      points[x] = (float)(x >> 1) + 0.5f;
      points[x + 1] = yValue;
    }
    transform->transformPoints(points);

    auto const tryCheckAndNudge(checkAndNudgePoints(image, points));
    if(!tryCheckAndNudge)
        return tryCheckAndNudge.error();

    for (int x = 0; x < max; x += 2) {
      if (image->get((int)points[x], (int)points[x + 1])) {
        bits->set(x >> 1, y);
      }
    }
  }
  return bits;
}

FallibleRef<BitMatrix> GridSampler::sampleGrid(Ref<BitMatrix> image, int dimension, float p1ToX, float p1ToY, float p2ToX,
                                       float p2ToY, float p3ToX, float p3ToY, float p4ToX, float p4ToY, float p1FromX, float p1FromY, float p2FromX,
                                       float p2FromY, float p3FromX, float p3FromY, float p4FromX, float p4FromY) MB_NOEXCEPT_EXCEPT_BADALLOC {
  Ref<PerspectiveTransform> transform(PerspectiveTransform::quadrilateralToQuadrilateral(p1ToX, p1ToY, p2ToX, p2ToY,
                                      p3ToX, p3ToY, p4ToX, p4ToY, p1FromX, p1FromY, p2FromX, p2FromY, p3FromX, p3FromY, p4FromX, p4FromY));

  return sampleGrid(image, dimension, transform);

}

Fallible<void> GridSampler::checkAndNudgePoints(Ref<BitMatrix> image, vector<float> &points)
#if !defined( DEBUG ) && defined( __clang__ )
    /** @note
     * In release mode, ASAN finds container-overflow when accessing first
     * vector element, even though it is correctly initialised.
     *                            (08.01.2018.) (Nenad Miksa)
     */
    __attribute__(( no_sanitize( "address" ) ))
#endif
{
  int width = (int)image->getWidth();
  int height = (int)image->getHeight();


  // The Java code assumes that if the start and end points are in bounds, the rest will also be.
  // However, in some unusual cases points in the middle may also be out of bounds.
  // Since we can't rely on an ArrayIndexOutOfBoundsException like Java, we check every point.

  for (size_t offset = 0; offset < points.size(); offset += 2) {
    int x = (int)points[offset];
    int y = (int)points[offset + 1];

    if (x < -1 || x > width || y < -1 || y > height) {
      auto const s("Transformed point out of bounds at " + std::to_string(x) + "," + std::to_string(y));

      return failure<ReaderException>(s.c_str());
    }

    if (x == -1) {
      points[offset] = 0.0f;
    } else if (x == width) {
      points[offset] = (float)(width - 1);
    }
    if (y == -1) {
      points[offset + 1] = 0.0f;
    } else if (y == height) {
      points[offset + 1] = (float)(height - 1);
    }
  }
  return success();
}

GridSampler &GridSampler::getInstance() noexcept {
  return gridSampler;
}
}
