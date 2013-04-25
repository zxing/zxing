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

#include <zxing/aztec/detector/Detector.h>
#include <zxing/common/GridSampler.h>
#include <zxing/common/detector/WhiteRectangleDetector.h>
#include <zxing/common/reedsolomon/ReedSolomonDecoder.h>
#include <zxing/common/reedsolomon/ReedSolomonException.h>
#include <zxing/common/reedsolomon/GenericGF.h>
#include <iostream>
#include <zxing/common/detector/MathUtils.h>
#include <zxing/NotFoundException.h>

using std::vector;
using zxing::aztec::Detector;
using zxing::aztec::Point;
using zxing::aztec::AztecDetectorResult;
using zxing::Ref;
using zxing::ArrayRef;
using zxing::ResultPoint;
using zxing::BitArray;
using zxing::BitMatrix;
using zxing::common::detector::MathUtils;

Detector::Detector(Ref<BitMatrix> image):
  image_(image),
  nbLayers_(0),
  nbDataBlocks_(0),
  nbCenterLayers_(0) {
        
}
        
Ref<AztecDetectorResult> Detector::detect() {
  Ref<Point> pCenter = getMatrixCenter();
            
  std::vector<Ref<Point> > bullEyeCornerPoints = getBullEyeCornerPoints(pCenter);
            
  extractParameters(bullEyeCornerPoints);
  
  ArrayRef< Ref<ResultPoint> > corners = getMatrixCornerPoints(bullEyeCornerPoints);
            
  Ref<BitMatrix> bits =
    sampleGrid(image_,
               corners[shift_%4],
               corners[(shift_+3)%4],
               corners[(shift_+2)%4],
               corners[(shift_+1)%4]);
            
  // std::printf("------------\ndetected: compact:%s, nbDataBlocks:%d, nbLayers:%d\n------------\n",compact_?"YES":"NO", nbDataBlocks_, nbLayers_);
            
  return Ref<AztecDetectorResult>(new AztecDetectorResult(bits, corners, compact_, nbDataBlocks_, nbLayers_));
}
        
void Detector::extractParameters(std::vector<Ref<Point> > bullEyeCornerPoints) {
  int twoCenterLayers = 2 * nbCenterLayers_;
  // get the bits around the bull's eye
  Ref<BitArray> resab = sampleLine(bullEyeCornerPoints[0], bullEyeCornerPoints[1], twoCenterLayers+1);
  Ref<BitArray> resbc = sampleLine(bullEyeCornerPoints[1], bullEyeCornerPoints[2], twoCenterLayers+1);
  Ref<BitArray> rescd = sampleLine(bullEyeCornerPoints[2], bullEyeCornerPoints[3], twoCenterLayers+1);
  Ref<BitArray> resda = sampleLine(bullEyeCornerPoints[3], bullEyeCornerPoints[0], twoCenterLayers+1);
        
  // determin the orientation of the matrix
  if (resab->get(0) && resab->get(twoCenterLayers)) {
    shift_ = 0;
  } else if (resbc->get(0) && resbc->get(twoCenterLayers)) {
    shift_ = 1;
  } else if (rescd->get(0) && rescd->get(twoCenterLayers)) {
    shift_ = 2;
  } else if (resda->get(0) && resda->get(twoCenterLayers)) {
    shift_ = 3;
  } else {
    // std::printf("could not detemine orientation\n");
    throw ReaderException("could not determine orientation");
  }
            
  //d      a
  //
  //c      b
            
  //flatten the bits in a single array
  Ref<BitArray> parameterData(new BitArray(compact_?28:40));
  Ref<BitArray> shiftedParameterData(new BitArray(compact_?28:40));
            
  if (compact_) {
    for (int i = 0; i < 7; i++) {
      if (resab->get(2+i)) shiftedParameterData->set(i);
      if (resbc->get(2+i)) shiftedParameterData->set(i+7);
      if (rescd->get(2+i)) shiftedParameterData->set(i+14);
      if (resda->get(2+i)) shiftedParameterData->set(i+21);
    }
    for (int i = 0; i < 28; i++) {
      if (shiftedParameterData->get((i+shift_*7)%28)) parameterData->set(i);
    }
                
  } else {
    for (int i = 0; i < 11; i++) {
      if (i < 5) {
        if (resab->get(2+i)) shiftedParameterData->set(i);
        if (resbc->get(2+i)) shiftedParameterData->set(i+10);
        if (rescd->get(2+i)) shiftedParameterData->set(i+20);
        if (resda->get(2+i)) shiftedParameterData->set(i+30);
      }
      if (i > 5) {
        if (resab->get(2+i)) shiftedParameterData->set(i-1);
        if (resbc->get(2+i)) shiftedParameterData->set(i+9);
        if (rescd->get(2+i)) shiftedParameterData->set(i+19);
        if (resda->get(2+i)) shiftedParameterData->set(i+29);
      }
    }
    for (int i = 0; i < 40; i++) {
      if (shiftedParameterData->get((i+shift_*10)%40)) parameterData->set(i);
    }
  }
            
  correctParameterData(parameterData, compact_);
            
  getParameters(parameterData);
}
        
ArrayRef< Ref<ResultPoint> >
Detector::getMatrixCornerPoints(std::vector<Ref<Point> > bullEyeCornerPoints) {
  float ratio = (2 * nbLayers_ + (nbLayers_ > 4 ? 1 : 0) + (nbLayers_ - 4) / 8) / (2.0f * nbCenterLayers_);
            
  int dx = bullEyeCornerPoints[0]->getX() - bullEyeCornerPoints[2]->getX();
  dx += dx > 0 ? 1 : -1;
  int dy = bullEyeCornerPoints[0]->getY() - bullEyeCornerPoints[2]->getY();
  dy += dy > 0 ? 1 : -1;
            
  int targetcx = MathUtils::round(bullEyeCornerPoints[2]->getX() - ratio * dx);
  int targetcy = MathUtils::round(bullEyeCornerPoints[2]->getY() - ratio * dy);
            
  int targetax = MathUtils::round(bullEyeCornerPoints[0]->getX() + ratio * dx);
  int targetay = MathUtils::round(bullEyeCornerPoints[0]->getY() + ratio * dy);
            
  dx = bullEyeCornerPoints[1]->getX() - bullEyeCornerPoints[3]->getX();
  dx += dx > 0 ? 1 : -1;
  dy = bullEyeCornerPoints[1]->getY() - bullEyeCornerPoints[3]->getY();
  dy += dy > 0 ? 1 : -1;
            
  int targetdx = MathUtils::round(bullEyeCornerPoints[3]->getX() - ratio * dx);
  int targetdy = MathUtils::round(bullEyeCornerPoints[3]->getY() - ratio * dy);
  int targetbx = MathUtils::round(bullEyeCornerPoints[1]->getX() + ratio * dx);
  int targetby = MathUtils::round(bullEyeCornerPoints[1]->getY() + ratio * dy);
            
  if (!isValid(targetax, targetay) ||
      !isValid(targetbx, targetby) ||

      !isValid(targetcx, targetcy) ||
      !isValid(targetdx, targetdy)) {
    throw ReaderException("matrix extends over image bounds");
  }
  Array< Ref<ResultPoint> >* array = new Array< Ref<ResultPoint> >();
  vector< Ref<ResultPoint> >& returnValue (array->values());
  returnValue.push_back(Ref<ResultPoint>(new ResultPoint(float(targetax), float(targetay))));
  returnValue.push_back(Ref<ResultPoint>(new ResultPoint(float(targetbx), float(targetby))));
  returnValue.push_back(Ref<ResultPoint>(new ResultPoint(float(targetcx), float(targetcy))));
  returnValue.push_back(Ref<ResultPoint>(new ResultPoint(float(targetdx), float(targetdy))));
  return ArrayRef< Ref<ResultPoint> >(array);
}
        
void Detector::correctParameterData(Ref<zxing::BitArray> parameterData, bool compact) {
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
            
  ArrayRef<int> parameterWords(new Array<int>(numCodewords));
            
  int codewordSize = 4;
  for (int i = 0; i < numCodewords; i++) {
    int flag = 1;
    for (int j = 1; j <= codewordSize; j++) {
      if (parameterData->get(codewordSize*i + codewordSize - j)) {
        parameterWords[i] += flag;
      }
      flag <<= 1;
    }
  }
                        
  try {
    // std::printf("parameter data reed solomon\n");
    ReedSolomonDecoder rsDecoder(GenericGF::AZTEC_PARAM);
    rsDecoder.decode(parameterWords, numECCodewords);
  } catch (ReedSolomonException const& ignored) {
    (void)ignored;
    // std::printf("reed solomon decoding failed\n");
    throw ReaderException("failed to decode parameter data");
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
}
        
std::vector<Ref<Point> > Detector::getBullEyeCornerPoints(Ref<zxing::aztec::Point> pCenter) {
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
      float q = distance(poutd, pouta) * nbCenterLayers_ / (distance(pind, pina) * (nbCenterLayers_ + 2));
      if (q < 0.75 || q > 1.25 || !isWhiteOrBlackRectangle(pouta, poutb, poutc, poutd)) {
        break;
      }
    }
                
    pina = pouta;
    pinb = poutb;
    pinc = poutc;
    pind = poutd;
                
    color = !color;
  }
            
  if (nbCenterLayers_ != 5 && nbCenterLayers_ != 7) {
    throw ReaderException("encountered wrong bullseye ring count");
  }
            
  compact_ = nbCenterLayers_ == 5;
            
            
            
  float ratio = 0.75f*2 / (2*nbCenterLayers_-3);
            
  int dx = pina->getX() - pind->getX();
  int dy = pina->getY() - pinc->getY();
            
  int targetcx = MathUtils::round(pinc->getX() - ratio * dx);
  int targetcy = MathUtils::round(pinc->getY() - ratio * dy);
  int targetax = MathUtils::round(pina->getX() + ratio * dx);
  int targetay = MathUtils::round(pina->getY() + ratio * dy);
            
  dx = pinb->getX() - pind->getX();
  dy = pinb->getY() - pind->getY();
            
  int targetdx = MathUtils::round(pind->getX() - ratio * dx);
  int targetdy = MathUtils::round(pind->getY() - ratio * dy);
  int targetbx = MathUtils::round(pinb->getX() + ratio * dx);
  int targetby = MathUtils::round(pinb->getY() + ratio * dy);
            
  if (!isValid(targetax, targetay) ||
      !isValid(targetbx, targetby) ||
      !isValid(targetcx, targetcy) ||
      !isValid(targetdx, targetdy)) {
    throw ReaderException("bullseye extends over image bounds");
  }
            
  std::vector<Ref<Point> > returnValue;
  returnValue.push_back(Ref<Point>(new Point(targetax, targetay)));
  returnValue.push_back(Ref<Point>(new Point(targetbx, targetby)));
  returnValue.push_back(Ref<Point>(new Point(targetcx, targetcy)));
  returnValue.push_back(Ref<Point>(new Point(targetdx, targetdy)));
            
  return returnValue;
            
}
        
Ref<Point> Detector::getMatrixCenter() {
  Ref<ResultPoint> pointA, pointB, pointC, pointD;
  try {
                
    std::vector<Ref<ResultPoint> > cornerPoints = WhiteRectangleDetector(image_).detect();
    pointA = cornerPoints[0];
    pointB = cornerPoints[1];
    pointC = cornerPoints[2];
    pointD = cornerPoints[3];
                
  } catch (NotFoundException const& e) {
    (void)e;
                
    int cx = image_->getWidth() / 2;
    int cy = image_->getHeight() / 2;
                
    pointA = getFirstDifferent(Ref<Point>(new Point(cx+7, cy-7)), false,  1, -1)->toResultPoint();
    pointB = getFirstDifferent(Ref<Point>(new Point(cx+7, cy+7)), false,  1,  1)->toResultPoint();
    pointC = getFirstDifferent(Ref<Point>(new Point(cx-7, cy+7)), false, -1, -1)->toResultPoint();
    pointD = getFirstDifferent(Ref<Point>(new Point(cx-7, cy-7)), false, -1, -1)->toResultPoint();
                                      
  }
            
  int cx = MathUtils::round((pointA->getX() + pointD->getX() + pointB->getX() + pointC->getX()) / 4.0f);
  int cy = MathUtils::round((pointA->getY() + pointD->getY() + pointB->getY() + pointC->getY()) / 4.0f);
            
  try {
                
    std::vector<Ref<ResultPoint> > cornerPoints = WhiteRectangleDetector(image_, 15, cx, cy).detect();
    pointA = cornerPoints[0];
    pointB = cornerPoints[1];
    pointC = cornerPoints[2];
    pointD = cornerPoints[3];
                
  } catch (NotFoundException const& e) {
    (void)e;
                
    pointA = getFirstDifferent(Ref<Point>(new Point(cx+7, cy-7)), false,  1, -1)->toResultPoint();
    pointB = getFirstDifferent(Ref<Point>(new Point(cx+7, cy+7)), false,  1,  1)->toResultPoint();
    pointC = getFirstDifferent(Ref<Point>(new Point(cx-7, cy+7)), false, -1, 1)->toResultPoint();
    pointD = getFirstDifferent(Ref<Point>(new Point(cx-7, cy-7)), false, -1, -1)->toResultPoint();
                
  }
            
  cx = MathUtils::round((pointA->getX() + pointD->getX() + pointB->getX() + pointC->getX()) / 4.0f);
  cy = MathUtils::round((pointA->getY() + pointD->getY() + pointB->getY() + pointC->getY()) / 4.0f);
            
  return Ref<Point>(new Point(cx, cy));
            
}
        
Ref<BitMatrix> Detector::sampleGrid(Ref<zxing::BitMatrix> image,
                                    Ref<zxing::ResultPoint> topLeft,
                                    Ref<zxing::ResultPoint> bottomLeft,
                                    Ref<zxing::ResultPoint> bottomRight,
                                    Ref<zxing::ResultPoint> topRight) {
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
                            dimension - 0.5f,
                            0.5f,
                            dimension - 0.5f,
                            dimension - 0.5f,
                            0.5f,
                            dimension - 0.5f,
                            topLeft->getX(),
                            topLeft->getY(),
                            topRight->getX(),
                            topRight->getY(),
                            bottomRight->getX(),
                            bottomRight->getY(),
                            bottomLeft->getX(),
                            bottomLeft->getY());
}
        
void Detector::getParameters(Ref<zxing::BitArray> parameterData) {
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
    if (parameterData->get(i)) {
      nbLayers_++;
    }
  }
            
  for (int i = nbBitsForNbLayers; i < nbBitsForNbLayers + nbBitsForNbDatablocks; i++) {
    nbDataBlocks_ <<= 1;
    if (parameterData->get(i)) {
      nbDataBlocks_++;
    }
  }
            
  nbLayers_++;
  nbDataBlocks_++;
}
        
Ref<BitArray> Detector::sampleLine(Ref<zxing::aztec::Point> p1, Ref<zxing::aztec::Point> p2, int size) {
  Ref<BitArray> res(new BitArray(size));
            
  float d = distance(p1, p2);
  float moduleSize = d / (size-1);
  float dx = moduleSize * float(p2->getX() - p1->getX())/d;
  float dy = moduleSize * float(p2->getY() - p1->getY())/d;
  
  float px = float(p1->getX());
  float py = float(p1->getY());
            
  for (int i = 0; i < size; i++) {
    if (image_->get(MathUtils::round(px), MathUtils::round(py))) res->set(i);
    px+=dx;
    py+=dy;
  }
            
  return res;
}
        
bool Detector::isWhiteOrBlackRectangle(Ref<zxing::aztec::Point> p1,
                                       Ref<zxing::aztec::Point> p2,
                                       Ref<zxing::aztec::Point> p3,
                                       Ref<zxing::aztec::Point> p4) {
  int corr = 3;
            
  p1 = new Point(p1->getX() - corr, p1->getY() + corr);
  p2 = new Point(p2->getX() - corr, p2->getY() - corr);
  p3 = new Point(p3->getX() + corr, p3->getY() - corr);
  p4 = new Point(p4->getX() + corr, p4->getY() + corr);
            
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
        
int Detector::getColor(Ref<zxing::aztec::Point> p1, Ref<zxing::aztec::Point> p2) {
  float d = distance(p1, p2);
            
  float dx = (p2->getX() - p1->getX()) / d;
  float dy = (p2->getY() - p1->getY()) / d;
            
  int error = 0;
            
  float px = float(p1->getX());
  float py = float(p1->getY());
            
  bool colorModel = image_->get(p1->getX(), p1->getY());
            
  for (int i = 0; i < d; i++) {
    px += dx;
    py += dy;
    if (image_->get(MathUtils::round(px), MathUtils::round(py)) != colorModel) {
      error ++;
    }
  }
            
  float errRatio = (float)error/d;
            
            
  if (errRatio > 0.1f && errRatio < 0.9f) {
    return 0;
  }
            
  return (errRatio <= 0.1) == colorModel ? 1 : -1;
}
        
Ref<Point> Detector::getFirstDifferent(Ref<zxing::aztec::Point> init, bool color, int dx, int dy) {
  int x = init->getX() + dx;
  int y = init->getY() + dy;
            
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
        
float Detector::distance(Ref<zxing::aztec::Point> a, Ref<zxing::aztec::Point> b) {
  return sqrtf((float)((a->getX() - b->getX()) * (a->getX() - b->getX()) + (a->getY() - b->getY()) * (a->getY() - b->getY())));
}
