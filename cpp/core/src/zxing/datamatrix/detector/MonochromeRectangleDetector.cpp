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

#include <zxing/ReaderException.h>
#include <zxing/datamatrix/detector/MonochromeRectangleDetector.h>
#include <sstream>

namespace zxing {
namespace datamatrix {

std::vector<Ref<CornerPoint> > MonochromeRectangleDetector::detect() {
    int height = image_->getHeight();
    int width = image_->getWidth();
    int halfHeight = height >> 1;
    int halfWidth = width >> 1;
    int deltaY = max(1, height / (MAX_MODULES << 3));
    int deltaX = max(1, width / (MAX_MODULES << 3));

    int top = 0;
    int bottom = height;
    int left = 0;
    int right = width;
    Ref<CornerPoint> pointA(findCornerFromCenter(halfWidth, 0, left, right,
        halfHeight, -deltaY, top, bottom, halfWidth >> 1));
    top = (int) pointA->getY() - 1;
    Ref<CornerPoint> pointB(findCornerFromCenter(halfWidth, -deltaX, left, right,
        halfHeight, 0, top, bottom, halfHeight >> 1));
    left = (int) pointB->getX() - 1;
    Ref<CornerPoint> pointC(findCornerFromCenter(halfWidth, deltaX, left, right,
        halfHeight, 0, top, bottom, halfHeight >> 1));
    right = (int) pointC->getX() + 1;
    Ref<CornerPoint> pointD(findCornerFromCenter(halfWidth, 0, left, right,
        halfHeight, deltaY, top, bottom, halfWidth >> 1));
    bottom = (int) pointD->getY() + 1;

    // Go try to find point A again with better information -- might have been off at first.
    pointA.reset(findCornerFromCenter(halfWidth, 0, left, right,
        halfHeight, -deltaY, top, bottom, halfWidth >> 2));
	std::vector<Ref<CornerPoint> > corners(4);
  	corners[0].reset(pointA);
  	corners[1].reset(pointB);
  	corners[2].reset(pointC);
  	corners[3].reset(pointD);
    return corners;
  }

Ref<CornerPoint> MonochromeRectangleDetector::findCornerFromCenter(int centerX, int deltaX, int left, int right,
      int centerY, int deltaY, int top, int bottom, int maxWhiteRun) {    
	  int* lastRange = NULL;
    for (int y = centerY, x = centerX;
         y < bottom && y >= top && x < right && x >= left;
         y += deltaY, x += deltaX) {
      int* range;
      if (deltaX == 0) {
        // horizontal slices, up and down
        range = blackWhiteRange(y, maxWhiteRun, left, right, true);
      } else {
        // vertical slices, left and right
        range = blackWhiteRange(x, maxWhiteRun, top, bottom, false);
      }
      if (range == NULL) {
        if (lastRange == NULL) {
			    throw ReaderException("Couldn't find corners (lastRange = NULL) ");
        } else {
        // lastRange was found
        if (deltaX == 0) {
          int lastY = y - deltaY;
          if (lastRange[0] < centerX) {
            if (lastRange[1] > centerX) {
              // straddle, choose one or the other based on direction
			        Ref<CornerPoint> result(new CornerPoint(deltaY > 0 ? lastRange[0] : lastRange[1], lastY));
			        return result;
            }
			      Ref<CornerPoint> result(new CornerPoint(lastRange[0], lastY));
			      return result;
          } else {
			      Ref<CornerPoint> result(new CornerPoint(lastRange[1], lastY));
			      return result;
            }
        } else {
          int lastX = x - deltaX;
          if (lastRange[0] < centerY) {
            if (lastRange[1] > centerY) {
			        Ref<CornerPoint> result(new CornerPoint(lastX, deltaX < 0 ? lastRange[0] : lastRange[1]));
			        return result;
            }
			      Ref<CornerPoint> result(new CornerPoint(lastX, lastRange[0]));
			      return result;
          } else {
			      Ref<CornerPoint> result(new CornerPoint(lastX, lastRange[1]));
			      return result;
            }
          }
        }
      }
      lastRange = range;
    }
    throw ReaderException("Couldn't find corners");
  }

int* MonochromeRectangleDetector::blackWhiteRange(int fixedDimension, int maxWhiteRun, int minDim, int maxDim,
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
    int* result;
    if (end > start) {
      result = new int [2];
      result[0] = start;
      result[1] = end;
    }
    else
    	result = NULL;
	  return result;
  }
}
}
