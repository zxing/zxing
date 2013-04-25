// -*- mode:c++; tab-width:2; indent-tabs-mode:nil; c-basic-offset:2 -*-
/*
 *  Detector.cpp
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

#include <zxing/qrcode/detector/Detector.h>
#include <zxing/qrcode/detector/FinderPatternFinder.h>
#include <zxing/qrcode/detector/FinderPattern.h>
#include <zxing/qrcode/detector/AlignmentPattern.h>
#include <zxing/qrcode/detector/AlignmentPatternFinder.h>
#include <zxing/qrcode/Version.h>
#include <zxing/common/GridSampler.h>
#include <zxing/DecodeHints.h>
#include <zxing/common/detector/MathUtils.h>
#include <sstream>
#include <cstdlib>

using std::ostringstream;
using std::abs;
using std::min;
using std::max;
using zxing::qrcode::Detector;
using zxing::Ref;
using zxing::BitMatrix;
using zxing::ResultPointCallback;
using zxing::DetectorResult;
using zxing::PerspectiveTransform;
using zxing::qrcode::AlignmentPattern;
using zxing::common::detector::MathUtils;

// VC++
using zxing::DecodeHints;
using zxing::qrcode::FinderPatternFinder;
using zxing::qrcode::FinderPatternInfo;
using zxing::ResultPoint;

Detector::Detector(Ref<BitMatrix> image) :
  image_(image) {
}

Ref<BitMatrix> Detector::getImage() const {
  return image_;
}

Ref<ResultPointCallback> Detector::getResultPointCallback() const {
  return callback_;
}

Ref<DetectorResult> Detector::detect(DecodeHints const& hints) {
  callback_ = hints.getResultPointCallback();
  FinderPatternFinder finder(image_, hints.getResultPointCallback());
  Ref<FinderPatternInfo> info(finder.find(hints));
  return processFinderPatternInfo(info);
}

Ref<DetectorResult> Detector::processFinderPatternInfo(Ref<FinderPatternInfo> info){
  Ref<FinderPattern> topLeft(info->getTopLeft());
  Ref<FinderPattern> topRight(info->getTopRight());
  Ref<FinderPattern> bottomLeft(info->getBottomLeft());

  float moduleSize = calculateModuleSize(topLeft, topRight, bottomLeft);
  if (moduleSize < 1.0f) {
    throw zxing::ReaderException("bad module size");
  }
  int dimension = computeDimension(topLeft, topRight, bottomLeft, moduleSize);
  Version *provisionalVersion = Version::getProvisionalVersionForDimension(dimension);
  int modulesBetweenFPCenters = provisionalVersion->getDimensionForVersion() - 7;

  Ref<AlignmentPattern> alignmentPattern;
  // Anything above version 1 has an alignment pattern
  if (provisionalVersion->getAlignmentPatternCenters().size() > 0) {


    // Guess where a "bottom right" finder pattern would have been
    float bottomRightX = topRight->getX() - topLeft->getX() + bottomLeft->getX();
    float bottomRightY = topRight->getY() - topLeft->getY() + bottomLeft->getY();


    // Estimate that alignment pattern is closer by 3 modules
    // from "bottom right" to known top left location
    float correctionToTopLeft = 1.0f - 3.0f / (float)modulesBetweenFPCenters;
    int estAlignmentX = (int)(topLeft->getX() + correctionToTopLeft * (bottomRightX - topLeft->getX()));
    int estAlignmentY = (int)(topLeft->getY() + correctionToTopLeft * (bottomRightY - topLeft->getY()));


    // Kind of arbitrary -- expand search radius before giving up
    for (int i = 4; i <= 16; i <<= 1) {
      try {
        alignmentPattern = findAlignmentInRegion(moduleSize, estAlignmentX, estAlignmentY, (float)i);
        break;
      } catch (zxing::ReaderException const& re) {
        (void)re;
        // try next round
      }
    }
    if (alignmentPattern == 0) {
      // Try anyway
    }

  }

  Ref<PerspectiveTransform> transform = createTransform(topLeft, topRight, bottomLeft, alignmentPattern, dimension);
  Ref<BitMatrix> bits(sampleGrid(image_, dimension, transform));
  ArrayRef< Ref<ResultPoint> > points(new Array< Ref<ResultPoint> >(alignmentPattern == 0 ? 3 : 4));
  points[0].reset(bottomLeft);
  points[1].reset(topLeft);
  points[2].reset(topRight);
  if (alignmentPattern != 0) {
    points[3].reset(alignmentPattern);
  }

  Ref<DetectorResult> result(new DetectorResult(bits, points));
  return result;
}

Ref<PerspectiveTransform> Detector::createTransform(Ref<ResultPoint> topLeft, Ref<ResultPoint> topRight, Ref <
                                                    ResultPoint > bottomLeft, Ref<ResultPoint> alignmentPattern, int dimension) {

  float dimMinusThree = (float)dimension - 3.5f;
  float bottomRightX;
  float bottomRightY;
  float sourceBottomRightX;
  float sourceBottomRightY;
  if (alignmentPattern != 0) {
    bottomRightX = alignmentPattern->getX();
    bottomRightY = alignmentPattern->getY();
    sourceBottomRightX = dimMinusThree - 3.0f;
    sourceBottomRightY = sourceBottomRightX;
  } else {
    // Don't have an alignment pattern, just make up the bottom-right point
    bottomRightX = (topRight->getX() - topLeft->getX()) + bottomLeft->getX();
    bottomRightY = (topRight->getY() - topLeft->getY()) + bottomLeft->getY();
    sourceBottomRightX = dimMinusThree;
    sourceBottomRightY = dimMinusThree;
  }

  Ref<PerspectiveTransform> transform(PerspectiveTransform::quadrilateralToQuadrilateral(3.5f, 3.5f, dimMinusThree, 3.5f, sourceBottomRightX,
                                                                                         sourceBottomRightY, 3.5f, dimMinusThree, topLeft->getX(), topLeft->getY(), topRight->getX(),
                                                                                         topRight->getY(), bottomRightX, bottomRightY, bottomLeft->getX(), bottomLeft->getY()));

  return transform;
}

Ref<BitMatrix> Detector::sampleGrid(Ref<BitMatrix> image, int dimension, Ref<PerspectiveTransform> transform) {
  GridSampler &sampler = GridSampler::getInstance();
  return sampler.sampleGrid(image, dimension, transform);
}

int Detector::computeDimension(Ref<ResultPoint> topLeft, Ref<ResultPoint> topRight, Ref<ResultPoint> bottomLeft,
                               float moduleSize) {
  int tltrCentersDimension =
    MathUtils::round(ResultPoint::distance(topLeft, topRight) / moduleSize);
  int tlblCentersDimension =
    MathUtils::round(ResultPoint::distance(topLeft, bottomLeft) / moduleSize);
  int dimension = ((tltrCentersDimension + tlblCentersDimension) >> 1) + 7;
  switch (dimension & 0x03) { // mod 4
  case 0:
    dimension++;
    break;
    // 1? do nothing
  case 2:
    dimension--;
    break;
  case 3:
    ostringstream s;
    s << "Bad dimension: " << dimension;
    throw zxing::ReaderException(s.str().c_str());
  }
  return dimension;
}

float Detector::calculateModuleSize(Ref<ResultPoint> topLeft, Ref<ResultPoint> topRight, Ref<ResultPoint> bottomLeft) {
  // Take the average
  return (calculateModuleSizeOneWay(topLeft, topRight) + calculateModuleSizeOneWay(topLeft, bottomLeft)) / 2.0f;
}

float Detector::calculateModuleSizeOneWay(Ref<ResultPoint> pattern, Ref<ResultPoint> otherPattern) {
  float moduleSizeEst1 = sizeOfBlackWhiteBlackRunBothWays((int)pattern->getX(), (int)pattern->getY(),
                                                          (int)otherPattern->getX(), (int)otherPattern->getY());
  float moduleSizeEst2 = sizeOfBlackWhiteBlackRunBothWays((int)otherPattern->getX(), (int)otherPattern->getY(),
                                                          (int)pattern->getX(), (int)pattern->getY());
  if (zxing::isnan(moduleSizeEst1)) {
    return moduleSizeEst2;
  }
  if (zxing::isnan(moduleSizeEst2)) {
    return moduleSizeEst1;
  }
  // Average them, and divide by 7 since we've counted the width of 3 black modules,
  // and 1 white and 1 black module on either side. Ergo, divide sum by 14.
  return (moduleSizeEst1 + moduleSizeEst2) / 14.0f;
}

float Detector::sizeOfBlackWhiteBlackRunBothWays(int fromX, int fromY, int toX, int toY) {

  float result = sizeOfBlackWhiteBlackRun(fromX, fromY, toX, toY);

  // Now count other way -- don't run off image though of course
  float scale = 1.0f;
  int otherToX = fromX - (toX - fromX);
  if (otherToX < 0) {
    scale = (float) fromX / (float) (fromX - otherToX);
    otherToX = 0;
  } else if (otherToX >= (int)image_->getWidth()) {
    scale = (float) (image_->getWidth() - 1 - fromX) / (float) (otherToX - fromX);
    otherToX = image_->getWidth() - 1;
  }
  int otherToY = (int) (fromY - (toY - fromY) * scale);

  scale = 1.0f;
  if (otherToY < 0) {
    scale = (float) fromY / (float) (fromY - otherToY);
    otherToY = 0;
  } else if (otherToY >= (int)image_->getHeight()) {
    scale = (float) (image_->getHeight() - 1 - fromY) / (float) (otherToY - fromY);
    otherToY = image_->getHeight() - 1;
  }
  otherToX = (int) (fromX + (otherToX - fromX) * scale);

  result += sizeOfBlackWhiteBlackRun(fromX, fromY, otherToX, otherToY);

  // Middle pixel is double-counted this way; subtract 1
  return result - 1.0f;
}

float Detector::sizeOfBlackWhiteBlackRun(int fromX, int fromY, int toX, int toY) {
  // Mild variant of Bresenham's algorithm;
  // see http://en.wikipedia.org/wiki/Bresenham's_line_algorithm
  bool steep = abs(toY - fromY) > abs(toX - fromX);
  if (steep) {
    int temp = fromX;
    fromX = fromY;
    fromY = temp;
    temp = toX;
    toX = toY;
    toY = temp;
  }

  int dx = abs(toX - fromX);
  int dy = abs(toY - fromY);
  int error = -dx >> 1;
  int xstep = fromX < toX ? 1 : -1;
  int ystep = fromY < toY ? 1 : -1;

  // In black pixels, looking for white, first or second time.
  int state = 0;
  // Loop up until x == toX, but not beyond
  int xLimit = toX + xstep;
  for (int x = fromX, y = fromY; x != xLimit; x += xstep) {
    int realX = steep ? y : x;
    int realY = steep ? x : y;

    // Does current pixel mean we have moved white to black or vice versa?
    if (!((state == 1) ^ image_->get(realX, realY))) {
      if (state == 2) {
        return MathUtils::distance(x, y, fromX, fromY);
      }
      state++;
    }

    error += dy;
    if (error > 0) {
      if (y == toY) {
        break;
      }
      y += ystep;
      error -= dx;
    }
  }
  // Found black-white-black; give the benefit of the doubt that the next pixel outside the image
  // is "white" so this last point at (toX+xStep,toY) is the right ending. This is really a
  // small approximation; (toX+xStep,toY+yStep) might be really correct. Ignore this.
  if (state == 2) {
    return MathUtils::distance(toX + xstep, toY, fromX, fromY);
  }
  // else we didn't find even black-white-black; no estimate is really possible
  return nan();
}

Ref<AlignmentPattern> Detector::findAlignmentInRegion(float overallEstModuleSize, int estAlignmentX, int estAlignmentY,
                                                      float allowanceFactor) {
  // Look for an alignment pattern (3 modules in size) around where it
  // should be
  int allowance = (int)(allowanceFactor * overallEstModuleSize);
  int alignmentAreaLeftX = max(0, estAlignmentX - allowance);
  int alignmentAreaRightX = min((int)(image_->getWidth() - 1), estAlignmentX + allowance);
  if (alignmentAreaRightX - alignmentAreaLeftX < overallEstModuleSize * 3) {
    throw zxing::ReaderException("region too small to hold alignment pattern");
  }
  int alignmentAreaTopY = max(0, estAlignmentY - allowance);
  int alignmentAreaBottomY = min((int)(image_->getHeight() - 1), estAlignmentY + allowance);
  if (alignmentAreaBottomY - alignmentAreaTopY < overallEstModuleSize * 3) {
    throw zxing::ReaderException("region too small to hold alignment pattern");
  }

  AlignmentPatternFinder alignmentFinder(image_, alignmentAreaLeftX, alignmentAreaTopY, alignmentAreaRightX
                                         - alignmentAreaLeftX, alignmentAreaBottomY - alignmentAreaTopY, overallEstModuleSize, callback_);
  return alignmentFinder.find();
}
