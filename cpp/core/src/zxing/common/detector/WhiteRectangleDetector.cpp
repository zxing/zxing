// -*- mode:c++; tab-width:2; indent-tabs-mode:nil; c-basic-offset:2 -*-
/*
 *  WhiteRectangleDetector.cpp
 *  y_wmk
 *
 *  Created by Luiz Silva on 09/02/2010.
 *  Copyright 2010 y_wmk authors All rights reserved.
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

#include <stddef.h>                            // for NULL
#include <zxing/NotFoundException.h>           // for NotFoundException
#include <zxing/common/detector/WhiteRectangleDetector.h>
#include <zxing/common/detector/math_utils.h>  // for round, distance, math_utils

#include "zxing/ResultPoint.h"                 // for ResultPoint
#include "zxing/common/BitMatrix.h"            // for BitMatrix
#include "zxing/common/Counted.h"              // for Ref

namespace math_utils = pping::common::detector::math_utils;

namespace pping {
using namespace std;

int WhiteRectangleDetector::INIT_SIZE = 30;
int WhiteRectangleDetector::CORR = 1;

WhiteRectangleDetector::WhiteRectangleDetector(Ref<BitMatrix> image, int leftInit, int rightInit, int upInit, int downInit) noexcept : image_(image), leftInit_(leftInit), rightInit_(rightInit), downInit_(downInit), upInit_(upInit)
{
    width_ = static_cast<int>(image->getWidth());
    height_ = static_cast<int>(image->getHeight());
}

/**
 * <p>
 * Detects a candidate barcode-like rectangular region within an image. It
 * starts around the center of the image, increases the size of the candidate
 * region until it finds a white rectangular region.
 * </p>
 *
 * @return {@link vector<Ref<ResultPoint> >} describing the corners of the rectangular
 *         region. The first and last points are opposed on the diagonal, as
 *         are the second and third. The first point will be the topmost
 *         point and the last, the bottommost. The second point will be
 *         leftmost and the third, the rightmost
*/
FallibleRef<WhiteRectangleDetector> WhiteRectangleDetector::createWhiteRectangleDetector(Ref<BitMatrix> image) MB_NOEXCEPT_EXCEPT_BADALLOC
{
    auto const width = (int)image->getWidth();
    auto const height = (int)image->getHeight();

    auto const leftInit = (width - INIT_SIZE) >> 1;
    auto const rightInit = (width + INIT_SIZE) >> 1;
    auto const upInit = (height - INIT_SIZE) >> 1;
    auto const downInit = (height + INIT_SIZE) >> 1;

    if (upInit < 0 || leftInit < 0 || downInit >= height || rightInit >= width)
      return failure<NotFoundException>("Invalid dimensions for WhiteRectangleDetector");

    return new WhiteRectangleDetector(image, leftInit, rightInit, upInit, downInit);
}

FallibleRef<WhiteRectangleDetector> WhiteRectangleDetector::createWhiteRectangleDetector(Ref<BitMatrix> image, int initSize, int x, int y) MB_NOEXCEPT_EXCEPT_BADALLOC
{
    auto const width = (int)image->getWidth();
    auto const height = (int)image->getHeight();

    int halfsize = initSize >> 1;
    auto const leftInit = x - halfsize;
    auto const rightInit = x + halfsize;
    auto const upInit = y - halfsize;
    auto const downInit = y + halfsize;

    if (upInit < 0 || leftInit < 0 || downInit >= height || rightInit >= width)
      return failure<NotFoundException>("Invalid dimensions for WhiteRectangleDetector");

    return new WhiteRectangleDetector(image, leftInit, rightInit, upInit, downInit);
}

Fallible<std::vector<Ref<ResultPoint>>> WhiteRectangleDetector::detect() MB_NOEXCEPT_EXCEPT_BADALLOC {
  int left = leftInit_;
  int right = rightInit_;
  int up = upInit_;
  int down = downInit_;

  bool sizeExceeded = false;
  bool aBlackPointFoundOnBorder = true;
  bool atLeastOneBlackPointFoundOnBorder = false;

  while (aBlackPointFoundOnBorder) {
    aBlackPointFoundOnBorder = false;

    // .....
    // .   |
    // .....
    bool rightBorderNotWhite = true;
    while (rightBorderNotWhite && right < width_) {
      rightBorderNotWhite = containsBlackPoint(up, down, right, false);
      if (rightBorderNotWhite) {
        right++;
        aBlackPointFoundOnBorder = true;
      }
    }

    if (right >= width_) {
      sizeExceeded = true;
      break;
    }

    // .....
    // .   .
    // .___.
    bool bottomBorderNotWhite = true;
    while (bottomBorderNotWhite && down < height_) {
      bottomBorderNotWhite = containsBlackPoint(left, right, down, true);
      if (bottomBorderNotWhite) {
        down++;
        aBlackPointFoundOnBorder = true;
      }
    }

    if (down >= height_) {
      sizeExceeded = true;
      break;
    }

    // .....
    // |   .
    // .....
    bool leftBorderNotWhite = true;
    while (leftBorderNotWhite && left >= 0) {
      leftBorderNotWhite = containsBlackPoint(up, down, left, false);
      if (leftBorderNotWhite) {
        left--;
        aBlackPointFoundOnBorder = true;
      }
    }

    if (left < 0) {
      sizeExceeded = true;
      break;
    }

    // .___.
    // .   .
    // .....
    bool topBorderNotWhite = true;
    while (topBorderNotWhite && up >= 0) {
      topBorderNotWhite = containsBlackPoint(left, right, up, true);
      if (topBorderNotWhite) {
        up--;
        aBlackPointFoundOnBorder = true;
      }
    }

    if (up < 0) {
      sizeExceeded = true;
      break;
    }

    if (aBlackPointFoundOnBorder) {
      atLeastOneBlackPointFoundOnBorder = true;
    }

  }
  if (!sizeExceeded && atLeastOneBlackPointFoundOnBorder) {

    int maxSize = right - left;

    Ref<ResultPoint> z(NULL);
    //go up right
    for (int i = 1; i < maxSize; i++) {
      z = getBlackPointOnSegment((float)left, (float)(down - i), (float)(left + i), (float)down);
      if (z != NULL) {
        break;
      }
    }

    if (z == NULL) {
      return failure<NotFoundException>("z == NULL");
    }

    Ref<ResultPoint> t(NULL);
    //go down right
    for (int i = 1; i < maxSize; i++) {
      t = getBlackPointOnSegment((float)left, (float)(up + i), (float)(left + i), (float)up);
      if (t != NULL) {
        break;
      }
    }

    if (t == NULL) {
      return failure<NotFoundException>("t == NULL");
    }

    Ref<ResultPoint> x(NULL);
    //go down left
    for (int i = 1; i < maxSize; i++) {
      x = getBlackPointOnSegment((float)right, (float)(up + i), (float)(right - i), (float)up);
      if (x != NULL) {
        break;
      }
    }

    if (x == NULL) {
      return failure<NotFoundException>("x == NULL");
    }

    Ref<ResultPoint> y(NULL);
    //go up left
    for (int i = 1; i < maxSize; i++) {
      y = getBlackPointOnSegment((float)right, (float)(down - i), (float)(right - i), (float)down);
      if (y != NULL) {
        break;
      }
    }

    if (y == NULL) {
      return failure<NotFoundException>("y == NULL");
    }

    return centerEdges(y, z, x, t);

  } else {
    return failure<NotFoundException>("No black point found on border");
  }
}

Ref<ResultPoint> WhiteRectangleDetector::getBlackPointOnSegment(float aX, float aY, float bX, float bY) {
  int dist = math_utils::round(math_utils::distance(aX, aY, bX, bY));
  float xStep = (bX - aX) / (float)dist;
  float yStep = (bY - aY) / (float)dist;

  for (int i = 0; i < dist; i++) {
    int x = math_utils::round(aX + (float)i * xStep);
    int y = math_utils::round(aY + (float)i * yStep);
    if (image_->get(x, y)) {
      Ref<ResultPoint> point(new ResultPoint((float)x, (float)y));
      return point;
    }
  }
  Ref<ResultPoint> point(NULL);
  return point;
}

/**
 * recenters the points of a constant distance towards the center
 *
 * @param y bottom most point
 * @param z left most point
 * @param x right most point
 * @param t top most point
 * @return {@link vector<Ref<ResultPoint> >} describing the corners of the rectangular
 *         region. The first and last points are opposed on the diagonal, as
 *         are the second and third. The first point will be the topmost
 *         point and the last, the bottommost. The second point will be
 *         leftmost and the third, the rightmost
 */
vector<Ref<ResultPoint> > WhiteRectangleDetector::centerEdges(Ref<ResultPoint> y, Ref<ResultPoint> z,
                                  Ref<ResultPoint> x, Ref<ResultPoint> t) {

  //
  //       t            t
  //  z                      x
  //        x    OR    z
  //   y                    y
  //

  float yi = y->getX();
  float yj = y->getY();
  float zi = z->getX();
  float zj = z->getY();
  float xi = x->getX();
  float xj = x->getY();
  float ti = t->getX();
  float tj = t->getY();

  std::vector<Ref<ResultPoint> > corners(4);
  if (yi < (float)width_/2) {
    Ref<ResultPoint> pointA(new ResultPoint(ti - (float)CORR, tj + (float)CORR));
    Ref<ResultPoint> pointB(new ResultPoint(zi + (float)CORR, zj + (float)CORR));
    Ref<ResultPoint> pointC(new ResultPoint(xi - (float)CORR, xj - (float)CORR));
    Ref<ResultPoint> pointD(new ResultPoint(yi + (float)CORR, yj - (float)CORR));
      corners[0].reset(pointA);
      corners[1].reset(pointB);
      corners[2].reset(pointC);
      corners[3].reset(pointD);
  } else {
    Ref<ResultPoint> pointA(new ResultPoint(ti + (float)CORR, tj + (float)CORR));
    Ref<ResultPoint> pointB(new ResultPoint(zi + (float)CORR, zj - (float)CORR));
    Ref<ResultPoint> pointC(new ResultPoint(xi - (float)CORR, xj + (float)CORR));
    Ref<ResultPoint> pointD(new ResultPoint(yi - (float)CORR, yj - (float)CORR));
      corners[0].reset(pointA);
      corners[1].reset(pointB);
      corners[2].reset(pointC);
      corners[3].reset(pointD);
  }
  return corners;
}

/**
 * Determines whether a segment contains a black point
 *
 * @param a          min value of the scanned coordinate
 * @param b          max value of the scanned coordinate
 * @param fixed      value of fixed coordinate
 * @param horizontal set to true if scan must be horizontal, false if vertical
 * @return true if a black point has been found, else false.
 */
bool WhiteRectangleDetector::containsBlackPoint(int a, int b, int fixed, bool horizontal) {
  if (horizontal) {
    for (int x = a; x <= b; x++) {
      if (image_->get(x, fixed)) {
        return true;
      }
    }
  } else {
    for (int y = a; y <= b; y++) {
      if (image_->get(fixed, y)) {
        return true;
      }
    }
  }

  return false;
}
}
