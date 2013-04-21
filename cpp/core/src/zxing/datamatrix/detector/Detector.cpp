// -*- mode:c++; tab-width:2; indent-tabs-mode:nil; c-basic-offset:2 -*-
/*
 *  Detector.cpp
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

#include <map>
#include <zxing/ResultPoint.h>
#include <zxing/common/GridSampler.h>
#include <zxing/datamatrix/detector/Detector.h>
#include <zxing/common/detector/MathUtils.h>
#include <zxing/NotFoundException.h>
#include <sstream>
#include <cstdlib>

using std::abs;
using zxing::Ref;
using zxing::BitMatrix;
using zxing::ResultPoint;
using zxing::DetectorResult;
using zxing::PerspectiveTransform;
using zxing::NotFoundException;
using zxing::datamatrix::Detector;
using zxing::datamatrix::ResultPointsAndTransitions;
using zxing::common::detector::MathUtils;

namespace {
  typedef std::map<Ref<ResultPoint>, int> PointMap;
  void increment(PointMap& table, Ref<ResultPoint> const& key) {
    int& value = table[key];
    value += 1;
  }
}

ResultPointsAndTransitions::ResultPointsAndTransitions() {
  Ref<ResultPoint> ref(new ResultPoint(0, 0));
  from_ = ref;
  to_ = ref;
  transitions_ = 0;
}

ResultPointsAndTransitions::ResultPointsAndTransitions(Ref<ResultPoint> from, Ref<ResultPoint> to,
                                                       int transitions)
  : to_(to), from_(from), transitions_(transitions) {
}

Ref<ResultPoint> ResultPointsAndTransitions::getFrom() {
  return from_;
}

Ref<ResultPoint> ResultPointsAndTransitions::getTo() {
  return to_;
}

int ResultPointsAndTransitions::getTransitions() {
  return transitions_;
}

Detector::Detector(Ref<BitMatrix> image)
  : image_(image) {
}

Ref<BitMatrix> Detector::getImage() {
  return image_;
}

Ref<DetectorResult> Detector::detect() {
  Ref<WhiteRectangleDetector> rectangleDetector_(new WhiteRectangleDetector(image_));
  std::vector<Ref<ResultPoint> > ResultPoints = rectangleDetector_->detect();
  Ref<ResultPoint> pointA = ResultPoints[0];
  Ref<ResultPoint> pointB = ResultPoints[1];
  Ref<ResultPoint> pointC = ResultPoints[2];
  Ref<ResultPoint> pointD = ResultPoints[3];

  // Point A and D are across the diagonal from one another,
  // as are B and C. Figure out which are the solid black lines
  // by counting transitions
  std::vector<Ref<ResultPointsAndTransitions> > transitions(4);
  transitions[0].reset(transitionsBetween(pointA, pointB));
  transitions[1].reset(transitionsBetween(pointA, pointC));
  transitions[2].reset(transitionsBetween(pointB, pointD));
  transitions[3].reset(transitionsBetween(pointC, pointD));
  insertionSort(transitions);

  // Sort by number of transitions. First two will be the two solid sides; last two
  // will be the two alternating black/white sides
  Ref<ResultPointsAndTransitions> lSideOne(transitions[0]);
  Ref<ResultPointsAndTransitions> lSideTwo(transitions[1]);

  // Figure out which point is their intersection by tallying up the number of times we see the
  // endpoints in the four endpoints. One will show up twice.
  typedef std::map<Ref<ResultPoint>, int> PointMap;
  PointMap pointCount;
  increment(pointCount, lSideOne->getFrom());
  increment(pointCount, lSideOne->getTo());
  increment(pointCount, lSideTwo->getFrom());
  increment(pointCount, lSideTwo->getTo());

  // Figure out which point is their intersection by tallying up the number of times we see the
  // endpoints in the four endpoints. One will show up twice.
  Ref<ResultPoint> maybeTopLeft;
  Ref<ResultPoint> bottomLeft;
  Ref<ResultPoint> maybeBottomRight;
  for (PointMap::const_iterator entry = pointCount.begin(), end = pointCount.end(); entry != end; ++entry) {
    Ref<ResultPoint> const& point = entry->first;
    int value = entry->second;
    if (value == 2) {
      bottomLeft = point; // this is definitely the bottom left, then -- end of two L sides
    } else {
      // Otherwise it's either top left or bottom right -- just assign the two arbitrarily now
      if (maybeTopLeft == 0) {
        maybeTopLeft = point;
      } else {
        maybeBottomRight = point;
      }
    }
  }

  if (maybeTopLeft == 0 || bottomLeft == 0 || maybeBottomRight == 0) {
    throw NotFoundException();
  }

  // Bottom left is correct but top left and bottom right might be switched
  std::vector<Ref<ResultPoint> > corners(3);
  corners[0].reset(maybeTopLeft);
  corners[1].reset(bottomLeft);
  corners[2].reset(maybeBottomRight);

  // Use the dot product trick to sort them out
  ResultPoint::orderBestPatterns(corners);

  // Now we know which is which:
  Ref<ResultPoint> bottomRight(corners[0]);
  bottomLeft = corners[1];
  Ref<ResultPoint> topLeft(corners[2]);

  // Which point didn't we find in relation to the "L" sides? that's the top right corner
  Ref<ResultPoint> topRight;
  if (!(pointA->equals(bottomRight) || pointA->equals(bottomLeft) || pointA->equals(topLeft))) {
    topRight = pointA;
  } else if (!(pointB->equals(bottomRight) || pointB->equals(bottomLeft)
               || pointB->equals(topLeft))) {
    topRight = pointB;
  } else if (!(pointC->equals(bottomRight) || pointC->equals(bottomLeft)
               || pointC->equals(topLeft))) {
    topRight = pointC;
  } else {
    topRight = pointD;
  }

  // Next determine the dimension by tracing along the top or right side and counting black/white
  // transitions. Since we start inside a black module, we should see a number of transitions
  // equal to 1 less than the code dimension. Well, actually 2 less, because we are going to
  // end on a black module:

  // The top right point is actually the corner of a module, which is one of the two black modules
  // adjacent to the white module at the top right. Tracing to that corner from either the top left
  // or bottom right should work here.

  int dimensionTop = transitionsBetween(topLeft, topRight)->getTransitions();
  int dimensionRight = transitionsBetween(bottomRight, topRight)->getTransitions();

  //dimensionTop++;
  if ((dimensionTop & 0x01) == 1) {
    // it can't be odd, so, round... up?
    dimensionTop++;
  }
  dimensionTop += 2;

  //dimensionRight++;
  if ((dimensionRight & 0x01) == 1) {
    // it can't be odd, so, round... up?
    dimensionRight++;
  }
  dimensionRight += 2;

  Ref<BitMatrix> bits;
  Ref<PerspectiveTransform> transform;
  Ref<ResultPoint> correctedTopRight;


  // Rectanguar symbols are 6x16, 6x28, 10x24, 10x32, 14x32, or 14x44. If one dimension is more
  // than twice the other, it's certainly rectangular, but to cut a bit more slack we accept it as
  // rectangular if the bigger side is at least 7/4 times the other:
  if (4 * dimensionTop >= 7 * dimensionRight || 4 * dimensionRight >= 7 * dimensionTop) {
    // The matrix is rectangular
    correctedTopRight = correctTopRightRectangular(bottomLeft, bottomRight, topLeft, topRight,
                                                   dimensionTop, dimensionRight);
    if (correctedTopRight == NULL) {
      correctedTopRight = topRight;
    }

    dimensionTop = transitionsBetween(topLeft, correctedTopRight)->getTransitions();
    dimensionRight = transitionsBetween(bottomRight, correctedTopRight)->getTransitions();

    if ((dimensionTop & 0x01) == 1) {
      // it can't be odd, so, round... up?
      dimensionTop++;
    }

    if ((dimensionRight & 0x01) == 1) {
      // it can't be odd, so, round... up?
      dimensionRight++;
    }

    transform = createTransform(topLeft, correctedTopRight, bottomLeft, bottomRight, dimensionTop,
                                dimensionRight);
    bits = sampleGrid(image_, dimensionTop, dimensionRight, transform);

  } else {
    // The matrix is square
    int dimension = min(dimensionRight, dimensionTop);

    // correct top right point to match the white module
    correctedTopRight = correctTopRight(bottomLeft, bottomRight, topLeft, topRight, dimension);
    if (correctedTopRight == NULL) {
      correctedTopRight = topRight;
    }

    // Redetermine the dimension using the corrected top right point
    int dimensionCorrected = std::max(transitionsBetween(topLeft, correctedTopRight)->getTransitions(),
                                      transitionsBetween(bottomRight, correctedTopRight)->getTransitions());
    dimensionCorrected++;
    if ((dimensionCorrected & 0x01) == 1) {
      dimensionCorrected++;
    }

    transform = createTransform(topLeft, correctedTopRight, bottomLeft, bottomRight,
                                dimensionCorrected, dimensionCorrected);
    bits = sampleGrid(image_, dimensionCorrected, dimensionCorrected, transform);
  }

  ArrayRef< Ref<ResultPoint> > points (new Array< Ref<ResultPoint> >(4));
  points[0].reset(topLeft);
  points[1].reset(bottomLeft);
  points[2].reset(correctedTopRight);
  points[3].reset(bottomRight);
  Ref<DetectorResult> detectorResult(new DetectorResult(bits, points));
  return detectorResult;
}

/**
 * Calculates the position of the white top right module using the output of the rectangle detector
 * for a rectangular matrix
 */
Ref<ResultPoint> Detector::correctTopRightRectangular(Ref<ResultPoint> bottomLeft,
                                                      Ref<ResultPoint> bottomRight, Ref<ResultPoint> topLeft, Ref<ResultPoint> topRight,
                                                      int dimensionTop, int dimensionRight) {

  float corr = distance(bottomLeft, bottomRight) / (float) dimensionTop;
  int norm = distance(topLeft, topRight);
  float cos = (topRight->getX() - topLeft->getX()) / norm;
  float sin = (topRight->getY() - topLeft->getY()) / norm;

  Ref<ResultPoint> c1(
    new ResultPoint(topRight->getX() + corr * cos, topRight->getY() + corr * sin));

  corr = distance(bottomLeft, topLeft) / (float) dimensionRight;
  norm = distance(bottomRight, topRight);
  cos = (topRight->getX() - bottomRight->getX()) / norm;
  sin = (topRight->getY() - bottomRight->getY()) / norm;

  Ref<ResultPoint> c2(
    new ResultPoint(topRight->getX() + corr * cos, topRight->getY() + corr * sin));

  if (!isValid(c1)) {
    if (isValid(c2)) {
      return c2;
    }
    return Ref<ResultPoint>(NULL);
  }
  if (!isValid(c2)) {
    return c1;
  }

  int l1 = abs(dimensionTop - transitionsBetween(topLeft, c1)->getTransitions())
    + abs(dimensionRight - transitionsBetween(bottomRight, c1)->getTransitions());
  int l2 = abs(dimensionTop - transitionsBetween(topLeft, c2)->getTransitions())
    + abs(dimensionRight - transitionsBetween(bottomRight, c2)->getTransitions());

  return l1 <= l2 ? c1 : c2;
}

/**
 * Calculates the position of the white top right module using the output of the rectangle detector
 * for a square matrix
 */
Ref<ResultPoint> Detector::correctTopRight(Ref<ResultPoint> bottomLeft,
                                           Ref<ResultPoint> bottomRight, Ref<ResultPoint> topLeft, Ref<ResultPoint> topRight,
                                           int dimension) {

  float corr = distance(bottomLeft, bottomRight) / (float) dimension;
  int norm = distance(topLeft, topRight);
  float cos = (topRight->getX() - topLeft->getX()) / norm;
  float sin = (topRight->getY() - topLeft->getY()) / norm;

  Ref<ResultPoint> c1(
    new ResultPoint(topRight->getX() + corr * cos, topRight->getY() + corr * sin));

  corr = distance(bottomLeft, topLeft) / (float) dimension;
  norm = distance(bottomRight, topRight);
  cos = (topRight->getX() - bottomRight->getX()) / norm;
  sin = (topRight->getY() - bottomRight->getY()) / norm;

  Ref<ResultPoint> c2(
    new ResultPoint(topRight->getX() + corr * cos, topRight->getY() + corr * sin));

  if (!isValid(c1)) {
    if (isValid(c2)) {
      return c2;
    }
    return Ref<ResultPoint>(NULL);
  }
  if (!isValid(c2)) {
    return c1;
  }

  int l1 = abs(
    transitionsBetween(topLeft, c1)->getTransitions()
    - transitionsBetween(bottomRight, c1)->getTransitions());
  int l2 = abs(
    transitionsBetween(topLeft, c2)->getTransitions()
    - transitionsBetween(bottomRight, c2)->getTransitions());

  return l1 <= l2 ? c1 : c2;
}

bool Detector::isValid(Ref<ResultPoint> p) {
  return p->getX() >= 0 && p->getX() < image_->getWidth() && p->getY() > 0
    && p->getY() < image_->getHeight();
}

int Detector::distance(Ref<ResultPoint> a, Ref<ResultPoint> b) {
  return MathUtils::round(ResultPoint::distance(a, b));
}

Ref<ResultPointsAndTransitions> Detector::transitionsBetween(Ref<ResultPoint> from,
                                                             Ref<ResultPoint> to) {
  // See QR Code Detector, sizeOfBlackWhiteBlackRun()
  int fromX = (int) from->getX();
  int fromY = (int) from->getY();
  int toX = (int) to->getX();
  int toY = (int) to->getY();
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
  int transitions = 0;
  bool inBlack = image_->get(steep ? fromY : fromX, steep ? fromX : fromY);
  for (int x = fromX, y = fromY; x != toX; x += xstep) {
    bool isBlack = image_->get(steep ? y : x, steep ? x : y);
    if (isBlack != inBlack) {
      transitions++;
      inBlack = isBlack;
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
  Ref<ResultPointsAndTransitions> result(new ResultPointsAndTransitions(from, to, transitions));
  return result;
}

Ref<PerspectiveTransform> Detector::createTransform(Ref<ResultPoint> topLeft,
                                                    Ref<ResultPoint> topRight, Ref<ResultPoint> bottomLeft, Ref<ResultPoint> bottomRight,
                                                    int dimensionX, int dimensionY) {

  Ref<PerspectiveTransform> transform(
    PerspectiveTransform::quadrilateralToQuadrilateral(
      0.5f,
      0.5f,
      dimensionX - 0.5f,
      0.5f,
      dimensionX - 0.5f,
      dimensionY - 0.5f,
      0.5f,
      dimensionY - 0.5f,
      topLeft->getX(),
      topLeft->getY(),
      topRight->getX(),
      topRight->getY(),
      bottomRight->getX(),
      bottomRight->getY(),
      bottomLeft->getX(),
      bottomLeft->getY()));
  return transform;
}

Ref<BitMatrix> Detector::sampleGrid(Ref<BitMatrix> image, int dimensionX, int dimensionY,
                                    Ref<PerspectiveTransform> transform) {
  GridSampler &sampler = GridSampler::getInstance();
  return sampler.sampleGrid(image, dimensionX, dimensionY, transform);
}

void Detector::insertionSort(std::vector<Ref<ResultPointsAndTransitions> > &vector) {
  int max = vector.size();
  bool swapped = true;
  Ref<ResultPointsAndTransitions> value;
  Ref<ResultPointsAndTransitions> valueB;
  do {
    swapped = false;
    for (int i = 1; i < max; i++) {
      value = vector[i - 1];
      if (compare(value, (valueB = vector[i])) > 0){
        swapped = true;
        vector[i - 1].reset(valueB);
        vector[i].reset(value);
      }
    }
  } while (swapped);
}

int Detector::compare(Ref<ResultPointsAndTransitions> a, Ref<ResultPointsAndTransitions> b) {
  return a->getTransitions() - b->getTransitions();
}
