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
#include <cmath>
#include <sstream>
#include <cstdlib>

namespace zxing {
namespace qrcode {

using namespace std;

Detector::Detector(Ref<BitMatrix> image) :
    image_(image) {
}

Ref<BitMatrix> Detector::getImage() {
   return image_;
}

Ref<DetectorResult> Detector::detect() {
  FinderPatternFinder finder(image_);
  Ref<FinderPatternInfo> info(finder.find());

  Ref<FinderPattern> topLeft(info->getTopLeft());
  Ref<FinderPattern> topRight(info->getTopRight());
  Ref<FinderPattern> bottomLeft(info->getBottomLeft());

  float moduleSize = calculateModuleSize(topLeft, topRight, bottomLeft);
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
      } catch (zxing::ReaderException re) {
        // try next round
      }
    }
    if (alignmentPattern == 0) {
      // Try anyway
    }

  }

  Ref<PerspectiveTransform> transform = createTransform(topLeft, topRight, bottomLeft, alignmentPattern, dimension);
  Ref<BitMatrix> bits(sampleGrid(image_, dimension, transform));
  std::vector<Ref<ResultPoint> > points(alignmentPattern == 0 ? 3 : 4);
  points[0].reset(bottomLeft);
  points[1].reset(topLeft);
  points[2].reset(topRight);
  if (alignmentPattern != 0) {
    points[3].reset(alignmentPattern);
  }

  Ref<DetectorResult> result(new DetectorResult(bits, points, transform));
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
    sourceBottomRightX = sourceBottomRightY = dimMinusThree - 3.0f;
  } else {
    // Don't have an alignment pattern, just make up the bottom-right point
    bottomRightX = (topRight->getX() - topLeft->getX()) + bottomLeft->getX();
    bottomRightY = (topRight->getY() - topLeft->getY()) + bottomLeft->getY();
    sourceBottomRightX = sourceBottomRightY = dimMinusThree;
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
  int tltrCentersDimension = int(FinderPatternFinder::distance(topLeft, topRight) / moduleSize + 0.5f);
  int tlblCentersDimension = int(FinderPatternFinder::distance(topLeft, bottomLeft) / moduleSize + 0.5f);
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
  if (isnan(moduleSizeEst1)) {
    return moduleSizeEst2;
  }
  if (isnan(moduleSizeEst2)) {
    return moduleSizeEst1;
  }
  // Average them, and divide by 7 since we've counted the width of 3 black modules,
  // and 1 white and 1 black module on either side. Ergo, divide sum by 14.
  return (moduleSizeEst1 + moduleSizeEst2) / 14.0f;
}

float Detector::sizeOfBlackWhiteBlackRunBothWays(int fromX, int fromY, int toX, int toY) {

  float result = sizeOfBlackWhiteBlackRun(fromX, fromY, toX, toY);


  // Now count other way -- don't run off image though of course
  int otherToX = fromX - (toX - fromX);
  if (otherToX < 0) {
    // "to" should the be the first value not included, so, the first value off
    // the edge is -1
    otherToX = -1;
  } else if (otherToX >= (int)image_->getWidth()) {
    otherToX = image_->getWidth();
  }
  int otherToY = fromY - (toY - fromY);
  if (otherToY < 0) {
    otherToY = -1;
  } else if (otherToY >= (int)image_->getHeight()) {
    otherToY = image_->getHeight();
  }
  result += sizeOfBlackWhiteBlackRun(fromX, fromY, otherToX, otherToY);
  return result - 1.0f; // -1 because we counted the middle pixel twice
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
  int ystep = fromY < toY ? 1 : -1;
  int xstep = fromX < toX ? 1 : -1;
  int state = 0; // In black pixels, looking for white, first or second time
  for (int x = fromX, y = fromY; x != toX; x += xstep) {

    int realX = steep ? y : x;
    int realY = steep ? x : y;
    if (state == 1) { // In white pixels, looking for black
      if (image_->get(realX, realY)) {
        state++;
      }
    } else {
      if (!image_->get(realX, realY)) {
        state++;
      }
    }

    if (state == 3) { // Found black, white, black, and stumbled back onto white; done
      int diffX = x - fromX;
      int diffY = y - fromY;
      return (float)sqrt((double)(diffX * diffX + diffY * diffY));
    }
    error += dy;
    if (error > 0) {
      y += ystep;
      error -= dx;
    }
  }
  int diffX = toX - fromX;
  int diffY = toY - fromY;
  return (float)sqrt((double)(diffX * diffX + diffY * diffY));
}

Ref<AlignmentPattern> Detector::findAlignmentInRegion(float overallEstModuleSize, int estAlignmentX, int estAlignmentY,
    float allowanceFactor) {
  // Look for an alignment pattern (3 modules in size) around where it
  // should be
  int allowance = (int)(allowanceFactor * overallEstModuleSize);
  int alignmentAreaLeftX = max(0, estAlignmentX - allowance);
  int alignmentAreaRightX = min((int)(image_->getWidth() - 1), estAlignmentX + allowance);
  int alignmentAreaTopY = max(0, estAlignmentY - allowance);
  int alignmentAreaBottomY = min((int)(image_->getHeight() - 1), estAlignmentY + allowance);

  AlignmentPatternFinder alignmentFinder(image_, alignmentAreaLeftX, alignmentAreaTopY, alignmentAreaRightX
                                         - alignmentAreaLeftX, alignmentAreaBottomY - alignmentAreaTopY, overallEstModuleSize);
  return alignmentFinder.find();
}

}
}
