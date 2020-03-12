/*
 *  MonochromeRectangleDetector.cpp
 *  zxing
 *
 *  Created by Luiz Silva on 09/02/2010.
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

#include <stddef.h>                                 // for NULL
#include <zxing/ReaderException.h>                  // for ReaderException
#include <zxing/datamatrix/detector/ZXingDataMatrixMonochromeRectangleDetector.h>

#include "zxing/common/BitMatrix.h"                 // for BitMatrix
#include "zxing/common/Counted.h"                   // for Ref
#include "zxing/datamatrix/detector/CornerPoint.h"  // for CornerPoint

namespace pping {
namespace datamatrix {

Fallible<std::vector<Ref<CornerPoint>>> MonochromeRectangleDetector::detect() MB_NOEXCEPT_EXCEPT_BADALLOC {
    int height = (int)image_->getHeight();
    int width = (int)image_->getWidth();
    int halfHeight = height >> 1;
    int halfWidth = width >> 1;
    int deltaY = max(1, (float)height / (float)(MAX_MODULES << 3));
    int deltaX = max(1, (float)width / (float)(MAX_MODULES << 3));

    int top = 0;
    int bottom = height;
    int left = 0;
    int right = width;

    auto const cornersA(findCornerFromCenter(halfWidth, 0, left, right,
                                             halfHeight, -deltaY, top, bottom, halfWidth >> 1));
    if(!cornersA)
        return cornersA.error();

    Ref<CornerPoint> pointA(*cornersA);
    top = (int) pointA->getY() - 1;

    auto const cornersB(findCornerFromCenter(halfWidth, -deltaX, left, right,
                                             halfHeight, 0, top, bottom, halfHeight >> 1));
    if(!cornersB)
        return cornersB.error();

    Ref<CornerPoint> pointB(*cornersB);
    left = (int) pointB->getX() - 1;

    auto const cornersC(findCornerFromCenter(halfWidth, deltaX, left, right,
                                             halfHeight, 0, top, bottom, halfHeight >> 1));
    if(!cornersC)
        return cornersC.error();

    Ref<CornerPoint> pointC(*cornersC);
    right = (int) pointC->getX() + 1;

    auto const cornersD(findCornerFromCenter(halfWidth, 0, left, right,
                                             halfHeight, deltaY, top, bottom, halfWidth >> 1));
    if(!cornersD)
        return cornersD.error();

    Ref<CornerPoint> pointD(*cornersD);
    bottom = (int) pointD->getY() + 1;

    // Go try to find point A again with better information -- might have been off at first.
    auto const pointACenter(findCornerFromCenter(halfWidth, 0, left, right,
                                                 halfHeight, -deltaY, top, bottom, halfWidth >> 2));
    if(!pointACenter)
        return pointACenter.error();

    pointA.reset(*pointACenter);

      std::vector<Ref<CornerPoint> > corners(4);

    corners[0].reset(pointA);
    corners[1].reset(pointB);
    corners[2].reset(pointC);
    corners[3].reset(pointD);
    return corners;
  }

FallibleRef<CornerPoint> MonochromeRectangleDetector::findCornerFromCenter(int centerX, int deltaX, int left, int right,
      int centerY, int deltaY, int top, int bottom, int maxWhiteRun) MB_NOEXCEPT_EXCEPT_BADALLOC {
    Ref<TwoInts> lastRange(NULL);
    for (int y = centerY, x = centerX;
         y < bottom && y >= top && x < right && x >= left;
         y += deltaY, x += deltaX) {
    Ref<TwoInts> range(NULL);
      if (deltaX == 0) {
        // horizontal slices, up and down
        range = blackWhiteRange(y, maxWhiteRun, left, right, true);
      } else {
        // vertical slices, left and right
        range = blackWhiteRange(x, maxWhiteRun, top, bottom, false);
      }
      if (range == NULL) {
        if (lastRange == NULL) {
                return failure<ReaderException>("Couldn't find corners (lastRange = NULL) ");
        } else {
        // lastRange was found
        if (deltaX == 0) {
          int lastY = y - deltaY;
          if (lastRange->start < centerX) {
            if (lastRange->end > centerX) {
              // straddle, choose one or the other based on direction
                    Ref<CornerPoint> result(new CornerPoint((float)(deltaY > 0 ? lastRange->start : lastRange->end), (float)lastY));
                    return result;
            }
                  Ref<CornerPoint> result(new CornerPoint((float)lastRange->start, (float)lastY));
                  return result;
          } else {
                  Ref<CornerPoint> result(new CornerPoint((float)lastRange->end, (float)lastY));
                  return result;
            }
        } else {
          int lastX = x - deltaX;
          if (lastRange->start < centerY) {
            if (lastRange->end > centerY) {
                    Ref<CornerPoint> result(new CornerPoint((float)lastX, (float)(deltaX < 0 ? lastRange->start : lastRange->end)));
                    return result;
            }
                  Ref<CornerPoint> result(new CornerPoint((float)lastX, (float)lastRange->start));
                  return result;
          } else {
                  Ref<CornerPoint> result(new CornerPoint((float)lastX, (float)lastRange->end));
                  return result;
            }
          }
        }
      }
      lastRange = range;
    }
    return failure<ReaderException>("Couldn't find corners");
  }

Ref<TwoInts> MonochromeRectangleDetector::blackWhiteRange(int fixedDimension, int maxWhiteRun, int minDim, int maxDim,
      bool horizontal) {
    
      int center = (minDim + maxDim) >> 1;

    // Scan left/up first
    int start = center;
    while (start >= minDim) {
      if (horizontal ? image_->get(start, fixedDimension) : image_->get(fixedDimension, start)) {
        start--;
      } else {
        int whiteRunStart = start;
        do {
          start--;
        } while (start >= minDim && !(horizontal ? image_->get(start, fixedDimension) :
            image_->get(fixedDimension, start)));
        int whiteRunSize = whiteRunStart - start;
        if (start < minDim || whiteRunSize > maxWhiteRun) {
          start = whiteRunStart;
          break;
        }
      }
    }
    start++;

    // Then try right/down
    int end = center;
    while (end < maxDim) {
      if (horizontal ? image_->get(end, fixedDimension) : image_->get(fixedDimension, end)) {
        end++;
      } else {
        int whiteRunStart = end;
        do {
          end++;
        } while (end < maxDim && !(horizontal ? image_->get(end, fixedDimension) :
            image_->get(fixedDimension, end)));
        int whiteRunSize = end - whiteRunStart;
        if (end >= maxDim || whiteRunSize > maxWhiteRun) {
          end = whiteRunStart;
          break;
        }
      }
    }
    end--;
    Ref<TwoInts> result(NULL);
    if (end > start) {
        result = new TwoInts;
      result->start = start;
      result->end = end;
    }
    return result;
  }
}
}
