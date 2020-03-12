// -*- mode:c++; tab-width:2; indent-tabs-mode:nil; c-basic-offset:2 -*-
/*
 *  Detector.cpp
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

#include <zxing/NotFoundException.h>                        // for NotFoundException
#include <zxing/aztec/detector/ZxingAztecDetector.h>        // for Point, Detector
#include <zxing/common/GridSampler.h>                       // for GridSampler
#include <zxing/common/detector/WhiteRectangleDetector.h>   // for WhiteRectangleDetector
#include <zxing/common/detector/math_utils.h>               // for round, math_utils

#include <zxing/common/reedsolomon/GenericGF.h>             // for GenericGF, GenericGF::AZTEC_PARAM
#include <zxing/common/reedsolomon/ReedSolomonDecoder.h>    // for ReedSolomonDecoder
#include <zxing/common/reedsolomon/ReedSolomonException.h>  // for ReedSolomonException
#include <vector>                                           // for vector, allocator

#include "zxing/ReaderException.h"                          // for ReaderException
#include "zxing/ResultPoint.h"                              // for ResultPoint
#include "zxing/aztec/AztecDetectorResult.h"                // for AztecDetectorResult
#include "zxing/common/Array.h"                             // for ArrayRef, Array
#include "zxing/common/BitArray.h"                          // for BitArray
#include "zxing/common/BitMatrix.h"                         // for BitMatrix
#include "zxing/common/Counted.h"                           // for Ref
#include "zxing/common/Error.hpp"                           // for Fallible

#include <Utils/Macros.h>

#include <cmath>

using pping::aztec::Detector;
using pping::aztec::Point;
using pping::aztec::AztecDetectorResult;
using pping::Ref;
using pping::ResultPoint;
using pping::BitArray;
using pping::BitMatrix;
namespace math_utils = pping::common::detector::math_utils;
                
Detector::Detector(Ref<BitMatrix> image) noexcept:
  image_(image),
  nbLayers_(0),
  nbDataBlocks_(0),
  nbCenterLayers_(0) {
        
}
        
// using namespace std;

pping::FallibleRef<AztecDetectorResult> Detector::detect() MB_NOEXCEPT_EXCEPT_BADALLOC {
  Ref<Point> pCenter = getMatrixCenter();

  auto const tryGetBullEyePoints(getBullEyeCornerPoints(pCenter));
  if(!tryGetBullEyePoints)
      return tryGetBullEyePoints.error();
            
  auto const bullEyeCornerPoints = *tryGetBullEyePoints;
            
  auto const paramExtraction(extractParameters(bullEyeCornerPoints));
  if(!paramExtraction)
      return paramExtraction.error();

  auto const getCorners(getMatrixCornerPoints(bullEyeCornerPoints));
  if(!getCorners)
      return getCorners.error();

  auto const corners = *getCorners;
            
  auto const bits = sampleGrid(image_, corners[shift_%4], corners[(shift_+3)%4], corners[(shift_+2)%4], corners[(shift_+1)%4]);
  if(!bits)
      return bits.error();
            
  // std::printf("------------\ndetected: compact:%s, nbDataBlocks:%d, nbLayers:%d\n------------\n",compact_?"YES":"NO", nbDataBlocks_, nbLayers_);

  return new AztecDetectorResult(*bits, corners, compact_, nbDataBlocks_, nbLayers_);
}
        
pping::Fallible<void> Detector::extractParameters(std::vector<Ref<Point> > bullEyeCornerPoints) MB_NOEXCEPT_EXCEPT_BADALLOC {
  // get the bits around the bull's eye
  Ref<BitArray> resab = sampleLine(bullEyeCornerPoints[0], bullEyeCornerPoints[1], 2*nbCenterLayers_+1);
  Ref<BitArray> resbc = sampleLine(bullEyeCornerPoints[1], bullEyeCornerPoints[2], 2*nbCenterLayers_+1);
  Ref<BitArray> rescd = sampleLine(bullEyeCornerPoints[2], bullEyeCornerPoints[3], 2*nbCenterLayers_+1);
  Ref<BitArray> resda = sampleLine(bullEyeCornerPoints[3], bullEyeCornerPoints[0], 2*nbCenterLayers_+1);
        
  // determin the orientation of the matrix
  auto const getAtZeroAB(resab->get(0));
  if(!getAtZeroAB)
      return getAtZeroAB.error();
  auto const getAtCenterAB(resab->get(static_cast<size_t>(2 * nbCenterLayers_)));
  if(!getAtCenterAB)
      return getAtCenterAB.error();

  bool shiftIsSet = false;

  if (*getAtZeroAB && *getAtCenterAB)
  {
    shift_ = 0;
    shiftIsSet = true;
  }
  if(!shiftIsSet)
  {
      auto const getAtZero(resbc->get(0));
      if(!getAtZero)
          return getAtZero.error();
      auto const getAtCenter(resbc->get(static_cast<size_t>(2 * nbCenterLayers_)));
      if(!getAtCenter)
          return getAtCenter.error();

      if (*getAtZero && *getAtCenter)
      {
          shift_ = 1;
          shiftIsSet = true;
      }
  }
  if(!shiftIsSet)
  {
    auto const getAtZero(rescd->get(0));
    if(!getAtZero)
        return getAtZero.error();
    auto const getAtCenter(rescd->get(static_cast<size_t>(2 * nbCenterLayers_)));
    if(!getAtCenter)
        return getAtCenter.error();

    if (*getAtZero && *getAtCenter)
    {
        shift_ = 2;
        shiftIsSet = true;
    }
  }
  if(!shiftIsSet)
  {
    auto const getAtZero(resda->get(0));
    if(!getAtZero)
        return getAtZero.error();
    auto const getAtCenter(resda->get(static_cast<size_t>(2 * nbCenterLayers_)));
    if(!getAtCenter)
        return getAtCenter.error();

    if (*getAtZero && *getAtCenter)
    {
        shift_ = 3;
        shiftIsSet = true;
    }
  }
  if(!shiftIsSet)
  {
    // std::printf("could not detemine orientation\n");
    return failure<ReaderException>("Could not detemine orientation\n");
  }
            
  //d      a
  //
  //c      b
            
  //flatten the bits in a single array
  Ref<BitArray> parameterData(new BitArray(compact_?28:40));
  Ref<BitArray> shiftedParameterData(new BitArray(compact_?28:40));
            
  if (compact_) {
    for (size_t i = 0; i < 7; i++) {
      auto const ab(resab->get(2+i));
      if (ab && *ab) shiftedParameterData->set(i);

      auto const bc(resbc->get(2+i));
      if (bc && *bc) shiftedParameterData->set(i+7);

      auto const cd(rescd->get(2+i));
      if (cd && *cd) shiftedParameterData->set(i+14);

      auto const da(resda->get(2+i));
      if (da && *da) shiftedParameterData->set(i+21);
    }
    for (size_t i = 0; i < 28; i++) {
      auto const getShifted(shiftedParameterData->get((i+shift_*7)%28));
      if (getShifted && *getShifted) parameterData->set(i);
    }
                
  } else {
    for (size_t i = 0; i < 11; i++) {
      if (i < 5) {
        auto const ab(resab->get(2+i));
        if (ab && *ab) shiftedParameterData->set(i);

        auto const bc(resbc->get(2+i));
        if (bc && *bc) shiftedParameterData->set(i+10);

        auto const cd(rescd->get(2+i));
        if (cd && *cd) shiftedParameterData->set(i+20);

        auto const da(resda->get(2+i));
        if (da && *da) shiftedParameterData->set(i+30);
      }
      if (i > 5) {
        auto const ab(resab->get(2+i));
        if (ab && *ab) shiftedParameterData->set(i-1);

        auto const bc(resbc->get(2+i));
        if (bc && *bc) shiftedParameterData->set(i+10-1);

        auto const cd(rescd->get(2+i));
        if (cd && *cd) shiftedParameterData->set(i+20-1);

        auto const da(resda->get(2+i));
        if (da && *da) shiftedParameterData->set(i+30-1);
      }
    }
    for (size_t i = 0; i < 40; i++) {
      auto const getShifted(shiftedParameterData->get((i+shift_*10)%40));
      if (getShifted && *getShifted) parameterData->set(i);
    }
  }
            
  auto const correction(correctParameterData(parameterData, compact_));
  if(!correction)
      return correction.error();
            
  getParameters(parameterData);

  return pping::success();
}
        
pping::Fallible<std::vector<Ref<ResultPoint> >> Detector::getMatrixCornerPoints(std::vector<Ref<Point> > bullEyeCornerPoints) MB_NOEXCEPT_EXCEPT_BADALLOC {
  if( nbCenterLayers_ == 0 )
  {
      return failure<ReaderException>("nbCenterLayers == 0");
  }
  float ratio = (float)(2 * nbLayers_ + (nbLayers_ > 4 ? 1 : 0) + (nbLayers_ - 4) / 8) / (2.0f * (float)nbCenterLayers_);
            
  int dx = bullEyeCornerPoints[0]->x - bullEyeCornerPoints[2]->x;
  dx += dx > 0 ? 1 : -1;
  int dy = bullEyeCornerPoints[0]->y - bullEyeCornerPoints[2]->y;
  dy += dy > 0 ? 1 : -1;
            
  int targetcx = math_utils::round((float)bullEyeCornerPoints[2]->x - ratio * (float)dx);
  int targetcy = math_utils::round((float)bullEyeCornerPoints[2]->y - ratio * (float)dy);
            
  int targetax = math_utils::round((float)bullEyeCornerPoints[0]->x + ratio * (float)dx);
  int targetay = math_utils::round((float)bullEyeCornerPoints[0]->y + ratio * (float)dy);
            
  dx = bullEyeCornerPoints[1]->x - bullEyeCornerPoints[3]->x;
  dx += dx > 0 ? 1 : -1;
  dy = bullEyeCornerPoints[1]->y - bullEyeCornerPoints[3]->y;
  dy += dy > 0 ? 1 : -1;
            
  int targetdx = math_utils::round((float)bullEyeCornerPoints[3]->x - ratio * (float)dx);
  int targetdy = math_utils::round((float)bullEyeCornerPoints[3]->y - ratio * (float)dy);
  int targetbx = math_utils::round((float)bullEyeCornerPoints[1]->x + ratio * (float)dx);
  int targetby = math_utils::round((float)bullEyeCornerPoints[1]->y + ratio * (float)dy);

  if (!isValid(targetax, targetay) ||
      !isValid(targetbx, targetby) ||
      !isValid(targetcx, targetcy) ||
      !isValid(targetdx, targetdy)) {

      return failure<ReaderException>("Matrix extends over image bounds");
  }
  std::vector<Ref<ResultPoint> > returnValue;
  returnValue.push_back(Ref<ResultPoint>(new ResultPoint((float)targetax, (float)targetay)));
  returnValue.push_back(Ref<ResultPoint>(new ResultPoint((float)targetbx, (float)targetby)));
  returnValue.push_back(Ref<ResultPoint>(new ResultPoint((float)targetcx, (float)targetcy)));
  returnValue.push_back(Ref<ResultPoint>(new ResultPoint((float)targetdx, (float)targetdy)));
                
  return returnValue;
            
}
        
pping::Fallible<void> Detector::correctParameterData(Ref<pping::BitArray> parameterData, bool compact) MB_NOEXCEPT_EXCEPT_BADALLOC {
  int numCodewords;
  int numDataCodewords;
            
  if (compact)  {
    numCodewords = 7;
    numDataCodewords = 2;
  } else {
    numCodewords = 10;
    numDataCodewords = 4;
  }
            
  int numECCodewords = numCodewords - numDataCodewords;

  if(numCodewords < 0) return failure<ReaderException>("Number of codewords should be positive");
            
  ArrayRef<int> parameterWords(new Array<int>(numCodewords));
            
  int codewordSize = 4;
  for (int i = 0; i < numCodewords; i++) {
    int flag = 1;
    for (int j = 1; j <= codewordSize; j++) {
      auto const getAtParameter(parameterData->get(codewordSize*i + codewordSize - j));
      if (getAtParameter && *getAtParameter) {
        parameterWords[i] += flag;
      }
      flag <<= 1;
    }
  }

    ReedSolomonDecoder rsDecoder(GenericGF::AZTEC_PARAM);

    auto const decoderResult(rsDecoder.decode(parameterWords, numECCodewords));
    if(!decoderResult) {
        return decoderResult.error();
    }
            
  parameterData->clear();
  for (int i = 0; i < numDataCodewords; i++) {
    int flag = 1;
    for (int j = 1; j <= codewordSize; j++) {
      if ((parameterWords[i] & flag) == flag) {
        parameterData->set(i*codewordSize+codewordSize-j);
      }
      flag <<= 1;
    }
  }
  return pping::success();
}
        
pping::Fallible<std::vector<Ref<Point>>> Detector::getBullEyeCornerPoints(Ref<pping::aztec::Point> pCenter) MB_NOEXCEPT_EXCEPT_BADALLOC {
  Ref<Point> pina = pCenter;
  Ref<Point> pinb = pCenter;
  Ref<Point> pinc = pCenter;
  Ref<Point> pind = pCenter;
            
  bool color = true;
            
  for (nbCenterLayers_ = 1; nbCenterLayers_ < 9; nbCenterLayers_++) {
    Ref<Point> pouta = getFirstDifferent(pina, color, 1, -1);
    Ref<Point> poutb = getFirstDifferent(pinb, color, 1, 1);
    Ref<Point> poutc = getFirstDifferent(pinc, color, -1, 1);
    Ref<Point> poutd = getFirstDifferent(pind, color, -1, -1);
            
    //d    a
    //
    //c    b
                
    if (nbCenterLayers_ > 2) {
      if( std::abs(distance(pind, pina)) < 1e-5f )
      {
          return failure<ReaderException>("Distance between pind and pina too small");
      }
      float q = distance(poutd, pouta) * (float)nbCenterLayers_ / (distance(pind, pina) * (float)(nbCenterLayers_ + 2));
      if (q < 0.75f || q > 1.25f || !isWhiteOrBlackRectangle(pouta, poutb, poutc, poutd)) {
        break;
      }
    }
                
    pina = pouta;
    pinb = poutb;
    pinc = poutc;
    pind = poutd;
                
    color = !color;
  }

  if (nbCenterLayers_ != 5 && nbCenterLayers_ != 7)
      return failure<ReaderException>("Encountered wrong bullseye ring count");
            
  compact_ = nbCenterLayers_ == 5;
            
  float ratio = 0.75f*2.f / (float)(2*nbCenterLayers_-3);
            
  /*
   * NESTO CUDNO ??
   */

  int dx = pina->x - pind->x;
  int dy = pina->y - pinc->y;
            
  int targetcx = math_utils::round((float)pinc->x - ratio * (float)dx);
  int targetcy = math_utils::round((float)pinc->y - ratio * (float)dy);
  int targetax = math_utils::round((float)pina->x + ratio * (float)dx);
  int targetay = math_utils::round((float)pina->y + ratio * (float)dy);
            
  dx = pinb->x - pind->x;
  dy = pinb->y - pind->y;
            
  int targetdx = math_utils::round((float)pind->x - ratio * (float)dx);
  int targetdy = math_utils::round((float)pind->y - ratio * (float)dy);
  int targetbx = math_utils::round((float)pinb->x + ratio * (float)dx);
  int targetby = math_utils::round((float)pinb->y + ratio * (float)dy);
            
  if (!isValid(targetax, targetay) ||
      !isValid(targetbx, targetby) ||
      !isValid(targetcx, targetcy) ||
      !isValid(targetdx, targetdy)) {
      return failure<ReaderException>("Bullseye extends over image bounds");
  }
            
  std::vector<Ref<Point> > returnValue;
  returnValue.push_back(Ref<Point>(new Point(targetax, targetay)));
  returnValue.push_back(Ref<Point>(new Point(targetbx, targetby)));
  returnValue.push_back(Ref<Point>(new Point(targetcx, targetcy)));
  returnValue.push_back(Ref<Point>(new Point(targetdx, targetdy)));
            
  return returnValue;
            
}
        
Ref<Point> Detector::getMatrixCenter() MB_NOEXCEPT_EXCEPT_BADALLOC {
  Ref<ResultPoint> pointA, pointB, pointC, pointD;
  bool fallback = false;

  auto const tryCreateRectangle(WhiteRectangleDetector::createWhiteRectangleDetector(image_));
  if( tryCreateRectangle )
  {
      auto const tryDetect((*tryCreateRectangle)->detect());
      if( tryDetect )
      {
          std::vector<Ref<ResultPoint> > cornerPoints = *tryDetect;
          pointA = cornerPoints[0];
          pointB = cornerPoints[1];
          pointC = cornerPoints[2];
          pointD = cornerPoints[3];

      }
      else
      {
          fallback = true;
      }
  }
  else
  {
      fallback = true;
  }

  if( fallback )
  {
      int cx = (int)image_->getWidth() / 2;
      int cy = (int)image_->getHeight() / 2;

      pointA = getFirstDifferent(Ref<Point>(new Point(cx+15/2, cy-15/2)), false,  1, -1)->toResultPoint();
      pointB = getFirstDifferent(Ref<Point>(new Point(cx+15/2, cy+15/2)), false,  1,  1)->toResultPoint();
      pointC = getFirstDifferent(Ref<Point>(new Point(cx-15/2, cy+15/2)), false, -1,  1)->toResultPoint();
      pointD = getFirstDifferent(Ref<Point>(new Point(cx-15/2, cy-15/2)), false, -1, -1)->toResultPoint();
  }

  int cx = math_utils::round((pointA->getX() + pointD->getX() + pointB->getX() + pointC->getX()) / 4);
  int cy = math_utils::round((pointA->getY() + pointD->getY() + pointB->getY() + pointC->getY()) / 4);

  fallback = false;

  auto const tryCreateDetector(WhiteRectangleDetector::createWhiteRectangleDetector(image_, 15, cx, cy));
  if( tryCreateDetector )
  {
      auto const tryDetect((*tryCreateDetector)->detect());
      if( tryDetect )
      {
          std::vector<Ref<ResultPoint> > cornerPoints = *tryDetect;
          pointA = cornerPoints[0];
          pointB = cornerPoints[1];
          pointC = cornerPoints[2];
          pointD = cornerPoints[3];
      }
      else
      {
          fallback = true;
      }
  }
  else
  {
      fallback = true;
  }

  if( fallback )
  {
      pointA = getFirstDifferent(Ref<Point>(new Point(cx+15/2, cy-15/2)), false,  1, -1)->toResultPoint();
      pointB = getFirstDifferent(Ref<Point>(new Point(cx+15/2, cy+15/2)), false,  1,  1)->toResultPoint();
      pointC = getFirstDifferent(Ref<Point>(new Point(cx-15/2, cy+15/2)), false, -1, -1)->toResultPoint();
      pointD = getFirstDifferent(Ref<Point>(new Point(cx-15/2, cy-15/2)), false, -1, -1)->toResultPoint();

  }
            
  cx = math_utils::round((pointA->getX() + pointD->getX() + pointB->getX() + pointC->getX()) / 4);
  cy = math_utils::round((pointA->getY() + pointD->getY() + pointB->getY() + pointC->getY()) / 4);
            
  return Ref<Point>(new Point(cx, cy));
            
}
        
pping::FallibleRef<BitMatrix> Detector::sampleGrid(Ref<pping::BitMatrix> image,
                                    Ref<pping::ResultPoint> topLeft,
                                    Ref<pping::ResultPoint> bottomLeft,
                                    Ref<pping::ResultPoint> bottomRight,
                                    Ref<pping::ResultPoint> topRight) {
  int dimension;
  if (compact_) {
    dimension = 4 * nbLayers_+11;
  } else {
    if (nbLayers_ <= 4) {
      dimension = 4 * nbLayers_ + 15;
    } else {
      dimension = 4 * nbLayers_ + 2 * ((nbLayers_-4)/8 + 1) + 15;
    }
  }
            
  GridSampler sampler = GridSampler::getInstance();
            
  return sampler.sampleGrid(image,
                            dimension,
                            0.5f,
                            0.5f,
                            (float)dimension - 0.5f,
                            0.5f,
                            (float)dimension - 0.5f,
                            (float)dimension - 0.5f,
                            0.5f,
                            (float)dimension - 0.5f,
                            topLeft->getX(),
                            topLeft->getY(),
                            topRight->getX(),
                            topRight->getY(),
                            bottomRight->getX(),
                            bottomRight->getY(),
                            bottomLeft->getX(),
                            bottomLeft->getY());
}
        
void Detector::getParameters(Ref<pping::BitArray> parameterData) noexcept {
  nbLayers_ = 0;
  nbDataBlocks_ = 0;
            
  int nbBitsForNbLayers;
  int nbBitsForNbDatablocks;
            
  if (compact_) {
    nbBitsForNbLayers = 2;
    nbBitsForNbDatablocks = 6;
  } else {
    nbBitsForNbLayers = 5;
    nbBitsForNbDatablocks = 11;
  }
            
  for (int i = 0; i < nbBitsForNbLayers; i++) {
    nbLayers_ <<= 1;
    auto const getAt(parameterData->get(i));
    if (getAt && *getAt) {
      nbLayers_ += 1;
    }
  }
            
  for (int i = nbBitsForNbLayers; i < nbBitsForNbLayers + nbBitsForNbDatablocks; i++) {
    nbDataBlocks_ <<= 1;
    auto const getAt(parameterData->get(i));
    if (getAt && *getAt) {
      nbDataBlocks_ += 1;
    }
  }
            
  nbLayers_ ++;
  nbDataBlocks_ ++;
}
        
Ref<BitArray> Detector::sampleLine(Ref<pping::aztec::Point> p1, Ref<pping::aztec::Point> p2, int size) MB_NOEXCEPT_EXCEPT_BADALLOC {
  MB_ASSERTM( size > 1, "%s", "Can't sample line if size <= 1" );

  Ref<BitArray> res(new BitArray(size));
            
  float d = distance(p1, p2);
  float moduleSize = d / (float)(size-1);
  float dx = moduleSize * (float)(p2->x - p1->x)/d;
  float dy = moduleSize * (float)(p2->y - p1->y)/d;
            
  float px = (float)p1->x;
  float py = (float)p1->y;
            
  for (int i = 0; i < size; i++) {
    if (image_->get(math_utils::round(px), math_utils::round(py))) res->set(i);
    px += dx;
    py += dy;
  }
            
  return res;
}
        
/*
 * Checks if most pixels of rectangle defined by given points is mostly black or mostly white
 */
bool Detector::isWhiteOrBlackRectangle(Ref<pping::aztec::Point> p1,
                                       Ref<pping::aztec::Point> p2,
                                       Ref<pping::aztec::Point> p3,
                                       Ref<pping::aztec::Point> p4) {
  int corr = 3;
            
  p1 = new Point(p1->x - corr, p1->y + corr);
  p2 = new Point(p2->x - corr, p2->y - corr);
  p3 = new Point(p3->x + corr, p3->y - corr);
  p4 = new Point(p4->x + corr, p4->y + corr);
            
  int cInit = getColor(p4, p1);
            
  if (cInit == 0) {
    return false;
  }
            
  int c = getColor(p1, p2);
            
  if (c != cInit) {
    return false;
  }
            
  c = getColor(p2, p3);
            
  if (c != cInit) {
    return false;
  }
            
  c = getColor(p3, p4);
            
  if (c != cInit) {
    return false;
  }
            
  return true;
}
        
/*
 * Checks if line given with p1 and p2 is mostly white or mostly black
 */
int Detector::getColor(Ref<pping::aztec::Point> p1, Ref<pping::aztec::Point> p2) {
  float d = distance(p1, p2);

  MB_ASSERTM( std::abs(d) >= 1e-5f, "%s", "Distance between points ~= 0" );
            
  float dx = (float)(p2->x - p1->x) / d;
  float dy = (float)(p2->y - p1->y) / d;
            
  int error = 0;
            
  float px = (float)p1->x;
  float py = (float)p1->y;
            
  bool colorModel = image_->get(p1->x, p1->y);
            
  const int intD = static_cast<int>(d);
  for (int i = 0; i < intD; i++) {
    px += dx;
    py += dy;
    if (image_->get(math_utils::round(px), math_utils::round(py)) != colorModel) {
      error ++;
    }
  }
            
  float errRatio = (float)error/d;
            
            
  if (errRatio > 0.1 && errRatio < 0.9) {
    return 0;
  }
            
  if (errRatio <= 0.1) {
    return colorModel?1:-1;
  } else {
    return colorModel?-1:1;
  }
}
        
Ref<Point> Detector::getFirstDifferent(Ref<pping::aztec::Point> init, bool color, int dx, int dy) {
  int x = init->x + dx;
  int y = init->y + dy;
            
  while (isValid(x, y) && image_->get(x, y) == color) {
    x += dx;
    y += dy;
  }
            
  x -= dx;
  y -= dy;
            
  while (isValid(x, y) && image_->get(x, y) == color) {
    x += dx;
  }
            
  x -= dx;
            
  while (isValid(x, y) && image_->get(x, y) == color) {
    y += dy;
  }
            
  y -= dy;
            
  return Ref<Point>(new Point(x, y));
}
        
bool Detector::isValid(int x, int y) {
  return x >= 0 && x < (int)image_->getWidth() && y > 0 && y < (int)image_->getHeight();
}
        
float Detector::distance(Ref<pping::aztec::Point> a, Ref<pping::aztec::Point> b) {
  return sqrtf((float)((a->x - b->x) * (a->x - b->x) + (a->y - b->y) * (a->y - b->y)));
}
