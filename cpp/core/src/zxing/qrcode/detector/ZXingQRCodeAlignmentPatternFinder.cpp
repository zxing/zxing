// -*- mode:c++; tab-width:2; indent-tabs-mode:nil; c-basic-offset:2 -*-
/*
 *  AlignmentPatternFinder.cpp
 *  zxing
 *
 *  Created by Christian Brunschen on 14/05/2008.
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

#include "ZXingQRCodeAlignmentPatternFinder.h"

#include <stdlib.h>                                  // for size_t, abs
#include <zxing/ReaderException.h>                   // for ReaderException
#include <cmath>                                     // for NAN, fabs

#include "zxing/ResultPointCallback.h"               // for ResultPointCallback
#include "zxing/common/BitMatrix.h"                  // for BitMatrix
#include "zxing/common/Counted.h"                    // for Ref
#include "zxing/qrcode/detector/AlignmentPattern.h"  // for AlignmentPattern

#include <Utils/Macros.h>

#include <opencv2/core/fast_math.hpp>

namespace pping {
namespace qrcode {

using namespace std;

float AlignmentPatternFinder::centerFromEnd(vector<int> &stateCount, int end) {
  return (float)(end - stateCount[2]) - (float)stateCount[1] / 2.0f;
}

bool AlignmentPatternFinder::foundPatternCross(vector<int> &stateCount) {
  float maxVariance = moduleSize_ / 2.0f;
  for (size_t i = 0; i < 3; i++) {
    if (fabs(moduleSize_ - (float)stateCount[i]) >= maxVariance) {
      return false;
    }
  }
  return true;
}

float AlignmentPatternFinder::crossCheckVertical(size_t startI, size_t centerJ, int maxCount,
    int originalStateCountTotal) {
  int maxI = (int)image_->getHeight();
  vector<int> stateCount(3, 0);


  // Start counting up from center
  int i = (int)startI;
  while (i >= 0 && image_->get(centerJ, i) && stateCount[1] <= maxCount) {
    stateCount[1]++;
    i--;
  }
  // If already too many modules in this state or ran off the edge:
  if (i < 0 || stateCount[1] > maxCount) {
    return (float)NAN;
  }
  while (i >= 0 && !image_->get(centerJ, i) && stateCount[0] <= maxCount) {
    stateCount[0]++;
    i--;
  }
  if (stateCount[0] > maxCount) {
    return (float)NAN;
  }

  // Now also count down from center
  i = (int)startI + 1;
  while (i < maxI && image_->get(centerJ, i) && stateCount[1] <= maxCount) {
    stateCount[1]++;
    i++;
  }
  if (i == maxI || stateCount[1] > maxCount) {
    return (float)NAN;
  }
  while (i < maxI && !image_->get(centerJ, i) && stateCount[2] <= maxCount) {
    stateCount[2]++;
    i++;
  }
  if (stateCount[2] > maxCount) {
    return (float)NAN;
  }

  int stateCountTotal = stateCount[0] + stateCount[1] + stateCount[2];
  if (5 * abs(stateCountTotal - originalStateCountTotal) >= 2 * originalStateCountTotal) {
    return (float)NAN;
  }

  return foundPatternCross(stateCount) ? centerFromEnd(stateCount, i) : (float)NAN;
}

Ref<AlignmentPattern> AlignmentPatternFinder::handlePossibleCenter(vector<int> &stateCount, size_t i, size_t j) MB_NOEXCEPT_EXCEPT_BADALLOC {
  int stateCountTotal = stateCount[0] + stateCount[1] + stateCount[2];
  float centerJ = centerFromEnd(stateCount, (int)j);
  float centerI = crossCheckVertical(i, (int)centerJ, 2 * stateCount[1], stateCountTotal);
  if ( !cvIsNaN( centerI ) ) {
    float estimatedModuleSize = (float)(stateCount[0] + stateCount[1] + stateCount[2]) / 3.0f;
    int max = (int)possibleCenters_->size();
    for (int index = 0; index < max; index++) {
      Ref<AlignmentPattern> center((*possibleCenters_)[index]);
      // Look for about the same center and module size:
      if (center->aboutEquals(estimatedModuleSize, centerI, centerJ)) {
        return center->combineEstimate(centerI, centerJ, estimatedModuleSize);
      }
    }
    AlignmentPattern *tmp = new AlignmentPattern(centerJ, centerI, estimatedModuleSize);
    // Hadn't found this before; save it
    tmp->retain();
    possibleCenters_->push_back(tmp);
    if (callback_ != 0) {
      callback_->foundPossibleResultPoint(*tmp);
    }
  }
  Ref<AlignmentPattern> result;
  return result;
}

AlignmentPatternFinder::AlignmentPatternFinder(Ref<BitMatrix> image, size_t startX, size_t startY, size_t width,
                                               size_t height, float moduleSize, 
                                               Ref<ResultPointCallback>const& callback) MB_NOEXCEPT_EXCEPT_BADALLOC :
    image_(image), possibleCenters_(new vector<AlignmentPattern *> ()), startX_(startX), startY_(startY),
    width_(width), height_(height), moduleSize_(moduleSize), callback_(callback) {
}

AlignmentPatternFinder::~AlignmentPatternFinder() {
  for (size_t i = 0; i < possibleCenters_->size(); i++) {
    (*possibleCenters_)[i]->release();
    (*possibleCenters_)[i] = 0;
  }
  delete possibleCenters_;
}

pping::FallibleRef<AlignmentPattern> AlignmentPatternFinder::find() MB_NOEXCEPT_EXCEPT_BADALLOC
#ifdef __clang__
    __attribute__(( no_sanitize( "unsigned-integer-overflow" ) ))
#endif
{
  size_t maxJ = startX_ + width_;
  size_t middleI = startY_ + (height_ >> 1);
  //      Ref<BitArray> luminanceRow(new BitArray(width_));
  // We are looking for black/white/black modules in 1:1:1 ratio;
  // this tracks the number of black/white/black modules seen so far
  vector<int> stateCount(3, 0);
  for (size_t iGen = 0; iGen < height_; iGen++) {
    // Search from middle outwards

    MB_DISABLE_WARNING_PUSH
    MB_DISABLE_WARNING_MSVC( 4146 ) // unary minus operator applied to unsigned type, result still unsigned
    size_t i = middleI + ((iGen & 0x01) == 0 ? ((iGen + 1) >> 1) : -((iGen + 1) >> 1));
    MB_DISABLE_WARNING_POP

    //        image_->getBlackRow(i, luminanceRow, startX_, width_);
    stateCount[0] = 0;
    stateCount[1] = 0;
    stateCount[2] = 0;
    size_t j = startX_;
    // Burn off leading white pixels before anything else; if we start in the middle of
    // a white run, it doesn't make sense to count its length, since we don't know if the
    // white run continued to the left of the start point
    while (j < maxJ && !image_->get(j, i)) {
      j++;
    }
    int currentState = 0;
    while (j < maxJ) {
      if (image_->get(j, i)) {
        // Black pixel
        if (currentState == 1) { // Counting black pixels
          stateCount[currentState]++;
        } else { // Counting white pixels
          if (currentState == 2) { // A winner?
            if (foundPatternCross(stateCount)) { // Yes
              Ref<AlignmentPattern> confirmed(handlePossibleCenter(stateCount, i, j));
              if (confirmed != 0) {
                return confirmed;
              }
            }
            stateCount[0] = stateCount[2];
            stateCount[1] = 1;
            stateCount[2] = 0;
            currentState = 1;
          } else {
            stateCount[++currentState]++;
          }
        }
      } else { // White pixel
        if (currentState == 1) { // Counting black pixels
          currentState++;
        }
        stateCount[currentState]++;
      }
      j++;
   }
   if (foundPatternCross(stateCount)) {
      Ref<AlignmentPattern> confirmed(handlePossibleCenter(stateCount, i, maxJ));
      if (confirmed != 0) {
        return confirmed;
      }
   }

  }

  // Hmm, nothing we saw was observed and confirmed twice. If we had
  // any guess at all, return it.
  if (possibleCenters_->size() > 0) {
    Ref<AlignmentPattern> center((*possibleCenters_)[0]);
    return center;
  }
  return failure<ReaderException>("Could not find alignment pattern");
}

}
}
