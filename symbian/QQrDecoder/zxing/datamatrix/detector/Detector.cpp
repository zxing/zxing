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

#include <zxing/common/GridSampler.h>
#include <zxing/datamatrix/detector/Detector.h>
#include <cmath>
#include <sstream>
#include <cstdlib>

namespace zxing {
namespace datamatrix {

using namespace std;

ResultPointsAndTransitions::ResultPointsAndTransitions() : to_(), from_(), transitions_(0) {
  Ref<CornerPoint> ref(new CornerPoint(0,0));
  from_ = ref;
  to_ = ref;
}

ResultPointsAndTransitions::ResultPointsAndTransitions(Ref<CornerPoint> from, Ref<CornerPoint> to, int transitions) :
  to_(to), from_(from), transitions_(transitions) {
}

Ref<CornerPoint> ResultPointsAndTransitions::getFrom() {
      return from_;
}

Ref<CornerPoint> ResultPointsAndTransitions::getTo() {
      return to_;
}

int ResultPointsAndTransitions::getTransitions() {
      return transitions_;
}

Detector::Detector(Ref<BitMatrix> image) : image_(image) { }

Ref<BitMatrix> Detector::getImage() {
   return image_;
}

Ref<DetectorResult> Detector::detect() {
    Ref<MonochromeRectangleDetector> rectangleDetector_(new MonochromeRectangleDetector(image_));
    std::vector<Ref<CornerPoint> > cornerPoints = rectangleDetector_->detect();
    Ref<CornerPoint> pointA = cornerPoints[0];
    Ref<CornerPoint> pointB = cornerPoints[1];
    Ref<CornerPoint> pointC = cornerPoints[2];
    Ref<CornerPoint> pointD = cornerPoints[3];

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
    Ref<CornerPoint> maybeTopLeft;
    Ref<CornerPoint> bottomLeft;
    Ref<CornerPoint> maybeBottomRight;
  if (lSideOne->getFrom()->equals(lSideOne->getTo())) {
    bottomLeft = lSideOne->getFrom();
    maybeTopLeft = lSideTwo->getFrom();
    maybeBottomRight = lSideTwo->getTo();
  }
  else if (lSideOne->getFrom()->equals(lSideTwo->getFrom())) {
    bottomLeft = lSideOne->getFrom();
    maybeTopLeft = lSideOne->getTo();
    maybeBottomRight = lSideTwo->getTo();
  }
  else if (lSideOne->getFrom()->equals(lSideTwo->getTo())) {
    bottomLeft = lSideOne->getFrom();
    maybeTopLeft = lSideOne->getTo();
    maybeBottomRight = lSideTwo->getFrom();
  }
  else if (lSideOne->getTo()->equals(lSideTwo->getFrom())) {
    bottomLeft = lSideOne->getTo();
    maybeTopLeft = lSideOne->getFrom();
    maybeBottomRight = lSideTwo->getTo();
  }
  else if (lSideOne->getTo()->equals(lSideTwo->getTo())) {
    bottomLeft = lSideOne->getTo();
    maybeTopLeft = lSideOne->getFrom();
    maybeBottomRight = lSideTwo->getFrom();
  }
  else {
    bottomLeft = lSideTwo->getFrom();
    maybeTopLeft = lSideOne->getTo();
    maybeBottomRight = lSideOne->getFrom();
  }

    // Bottom left is correct but top left and bottom right might be switched
    std::vector<Ref<CornerPoint> > corners(3);
    corners[0].reset(maybeTopLeft);
    corners[1].reset(bottomLeft);
    corners[2].reset(maybeBottomRight);
    // Use the dot product trick to sort them out
    orderBestPatterns(corners);

    // Now we know which is which:
    Ref<CornerPoint> bottomRight(corners[0]);
    bottomLeft = corners[1];
    Ref<CornerPoint> topLeft(corners[2]);

    // Which point didn't we find in relation to the "L" sides? that's the top right corner
    Ref<CornerPoint> topRight;
    if (!(pointA->equals(bottomRight) || pointA->equals(bottomLeft) || pointA->equals(topLeft))) {
      topRight = pointA;
    } else if (!(pointB->equals(bottomRight) || pointB->equals(bottomLeft) || pointB->equals(topLeft))) {
      topRight = pointB;
    } else if (!(pointC->equals(bottomRight) || pointC->equals(bottomLeft) || pointC->equals(topLeft))) {
      topRight = pointC;
    } else {
      topRight = pointD;
    }

  float topRightX = (bottomRight->getX() - bottomLeft->getX()) + topLeft->getX();
  float topRightY = (bottomRight->getY() - bottomLeft->getY()) + topLeft->getY();
  Ref<CornerPoint> topR(new CornerPoint(topRightX,topRightY));

    // Next determine the dimension by tracing along the top or right side and counting black/white
    // transitions. Since we start inside a black module, we should see a number of transitions
    // equal to 1 less than the code dimension. Well, actually 2 less, because we are going to
    // end on a black module:
    // The top right point is actually the corner of a module, which is one of the two black modules
    // adjacent to the white module at the top right. Tracing to that corner from either the top left
    // or bottom right should work here. The number of transitions could be higher than it should be
    // due to noise. So we try both and take the min.
  int dimension = min(transitionsBetween(topLeft, topRight)->getTransitions(),
                             transitionsBetween(bottomRight, topRight)->getTransitions());
    if ((dimension & 0x01) == 1) {
      // it can't be odd, so, round... up?
      dimension++;
    }
    dimension += 2;

    Ref<PerspectiveTransform> transform = createTransform(topLeft, topR, bottomLeft, bottomRight, dimension);
    Ref<BitMatrix> bits(sampleGrid(image_, dimension, transform));
    std::vector<Ref<ResultPoint> > points(4);
    points[0].reset(pointA);
    points[1].reset(pointB);
    points[2].reset(pointC);
    points[3].reset(pointD);
    Ref<DetectorResult> detectorResult(new DetectorResult(bits, points, transform));
    return detectorResult;
}

Ref<ResultPointsAndTransitions> Detector::transitionsBetween(Ref<CornerPoint> from, Ref<CornerPoint> to) {
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

Ref<PerspectiveTransform> Detector::createTransform(Ref<ResultPoint> topLeft, Ref<ResultPoint> topRight, Ref <
    ResultPoint > bottomLeft, Ref<ResultPoint> bottomRight, int dimension) {

  Ref<PerspectiveTransform> transform(PerspectiveTransform::quadrilateralToQuadrilateral(
        0.0f,
        0.0f,
        dimension,
        0.0f,
        dimension,
        dimension,
        0.0f,
        dimension,
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

Ref<BitMatrix> Detector::sampleGrid(Ref<BitMatrix> image, int dimension, Ref<PerspectiveTransform> transform) {
  GridSampler &sampler = GridSampler::getInstance();
  return sampler.sampleGrid(image, dimension, transform);
}

void Detector::insertionSort(std::vector<Ref<ResultPointsAndTransitions> > &vector) {
    int max = vector.size();
  bool swapped = true;
     Ref<ResultPointsAndTransitions> value;
    Ref<ResultPointsAndTransitions> valueB;
  do {
    swapped = false;
      for (int i = 1; i < max; i++) {
        value = vector[i-1];
      if (compare(value, (valueB = vector[i])) > 0) {
      swapped = true;
      vector[i-1].reset(valueB);
        vector[i].reset(value);
      }
    }
  } while (swapped);
}
void Detector::orderBestPatterns(std::vector<Ref<CornerPoint> > &patterns) {
    // Find distances between pattern centers
    float zeroOneDistance = distance(patterns[0]->getX(), patterns[1]->getX(),patterns[0]->getY(), patterns[1]->getY());
    float oneTwoDistance = distance(patterns[1]->getX(), patterns[2]->getX(),patterns[1]->getY(), patterns[2]->getY());
    float zeroTwoDistance = distance(patterns[0]->getX(), patterns[2]->getX(),patterns[0]->getY(), patterns[2]->getY());

    Ref<CornerPoint> pointA, pointB, pointC;
    // Assume one closest to other two is B; A and C will just be guesses at first
    if (oneTwoDistance >= zeroOneDistance && oneTwoDistance >= zeroTwoDistance) {
      pointB = patterns[0];
      pointA = patterns[1];
      pointC = patterns[2];
    } else if (zeroTwoDistance >= oneTwoDistance && zeroTwoDistance >= zeroOneDistance) {
      pointB = patterns[1];
      pointA = patterns[0];
      pointC = patterns[2];
    } else {
      pointB = patterns[2];
      pointA = patterns[0];
      pointC = patterns[1];
    }

    // Use cross product to figure out whether A and C are correct or flipped.
    // This asks whether BC x BA has a positive z component, which is the arrangement
    // we want for A, B, C. If it's negative, then we've got it flipped around and
    // should swap A and C.
    if (crossProductZ(pointA, pointB, pointC) < 0.0f) {
      Ref<CornerPoint> temp = pointA;
      pointA = pointC;
      pointC = temp;
    }

    patterns[0] = pointA;
    patterns[1] = pointB;
    patterns[2] = pointC;
}

float Detector::distance(float x1, float x2, float y1, float y2) {
    float xDiff = x1 - x2;
    float yDiff = y1 - y2;
    return (float) sqrt((double) (xDiff * xDiff + yDiff * yDiff));
  }

int Detector::compare(Ref<ResultPointsAndTransitions> a, Ref<ResultPointsAndTransitions> b) {
      return a->getTransitions() - b->getTransitions();
    }

float Detector::crossProductZ(Ref<ResultPoint> pointA, Ref<ResultPoint> pointB, Ref<ResultPoint> pointC) {
    float bX = pointB->getX();
    float bY = pointB->getY();
    return ((pointC->getX() - bX) * (pointA->getY() - bY)) - ((pointC->getY() - bY) * (pointA->getX() - bX));
  }
}
}
