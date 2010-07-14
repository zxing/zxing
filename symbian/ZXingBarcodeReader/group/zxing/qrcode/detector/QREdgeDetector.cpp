/*
 *  QREdgeDetector.cpp
 *  zxing
 *
 *  Created by Ralf Kistner on 7/12/2009.
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

#include <zxing/qrcode/detector/QREdgeDetector.h>
#include <zxing/common/EdgeDetector.h>
#include <cstdlib>

using namespace std;

namespace zxing {
namespace qrcode {

static const float patternEdgeThreshold = 2;
static const int patternEdgeWidth = 3;
static const float patternEdgeSearchRatio = 1.1;
static const int patternEdgeSkip = 2;

static const float accurateEdgeThreshold = 3.3;
static const int accurateEdgeWidth = 7;
static const int accurateEdgeSkip = 2;

static Point guessLastPattern(Point topLeft, Point topRight, Point bottomLeft) {
  return Point(topRight.x - topLeft.x + bottomLeft.x, topRight.y - topLeft.y + bottomLeft.y);
}

static Point rp(Ref<ResultPoint> rp) {
  return Point(rp->getX(), rp->getY());
}

QREdgeDetector::QREdgeDetector(Ref<BitMatrix> image) : Detector(image) { }

Ref<PerspectiveTransform> QREdgeDetector::createTransform(Ref<ResultPoint> topLeft, Ref<ResultPoint> topRight, Ref <
      ResultPoint > bottomLeft, Ref<ResultPoint> alignmentPattern, int dimension) {

  if(alignmentPattern == NULL) {
    Point corner = findCorner(*Detector::getImage(), rp(topLeft), rp(topRight), rp(bottomLeft), dimension);
    return get1CornerTransform(rp(topLeft), rp(topRight), rp(bottomLeft), corner, dimension);
  } else {
    return Detector::createTransform(topLeft, topRight, bottomLeft, alignmentPattern, dimension);
  }
}




Point QREdgeDetector::findCorner(const BitMatrix& image, Point topLeft, Point topRight, Point bottomLeft, int dimension) {
  Point bottomRight = guessLastPattern(topLeft, topRight, bottomLeft);

  Line bottomEst = findPatternEdge(image, bottomLeft, topLeft, bottomRight, false);
  Line rightEst = findPatternEdge(image, topRight, topLeft, bottomRight, true);

  //return EdgeDetector::intersection(bottomEst, rightEst);

  Line bottom = EdgeDetector::findLine(image, bottomEst, false, accurateEdgeWidth, accurateEdgeThreshold, accurateEdgeSkip);
  Line right = EdgeDetector::findLine(image, rightEst, true, accurateEdgeWidth, accurateEdgeThreshold, accurateEdgeSkip);


  return EdgeDetector::intersection(bottom, right);
}

Line QREdgeDetector::findPatternEdge(const BitMatrix& image, Point pattern, Point opposite, Point direction, bool invert) {
  Point start = endOfReverseBlackWhiteBlackRun(image, pattern, opposite);

  float dx = pattern.x - start.x;
  float dy = pattern.y - start.y;
  float dist = sqrt(dx*dx + dy*dy);

  float dirX = direction.x - pattern.x;
  float dirY = direction.y - pattern.y;
  float dirSize = sqrt(dirX*dirX + dirY*dirY);

  float nx = dirX/dirSize;
  float ny = dirY/dirSize;

  float search = dist * patternEdgeSearchRatio;
  Point a(start.x + nx*search, start.y + ny*search);
  Point b(start.x - nx*search, start.y - ny*search);

  return EdgeDetector::findLine(image, Line(a, b), invert, patternEdgeWidth, patternEdgeThreshold, patternEdgeSkip);
}


Ref<PerspectiveTransform> QREdgeDetector::get1CornerTransform(Point topLeft, Point topRight, Point bottomLeft, Point corner, int dimension) {
  float dimMinusThree = (float) dimension - 3.5f;

  Ref<PerspectiveTransform> transform(PerspectiveTransform::quadrilateralToQuadrilateral(3.5f, 3.5f, dimMinusThree, 3.5f, dimension,
                                      dimension, 3.5f, dimMinusThree, topLeft.x, topLeft.y, topRight.x,
                                      topRight.y, corner.x, corner.y, bottomLeft.x, bottomLeft.y));

  return transform;
}

// Adapted from "sizeOfBlackWhiteBlackRun" in zxing::qrcode::Detector
Point QREdgeDetector::endOfReverseBlackWhiteBlackRun(const BitMatrix& image, Point from, Point to) {
  int fromX = (int)from.x;
  int fromY = (int)from.y;
  int toX = (int)to.x;
  int toY = (int)to.y;

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
  int ystep = fromY < toY ? -1 : 1;
  int xstep = fromX < toX ? -1 : 1;
  int state = 0; // In black pixels, looking for white, first or second time

  // In case there are no points, prepopulate to from
  int realX = fromX;
  int realY = fromY;
  for (int x = fromX, y = fromY; x != toX; x += xstep) {
    realX = steep ? y : x;
    realY = steep ? x : y;

    if(realX < 0 || realY < 0 || realX >= (int)image.getWidth() || realY >= (int)image.getHeight())
      break;

    if (state == 1) { // In white pixels, looking for black
      if (image.get(realX, realY)) {
        state++;
      }
    } else {
      if (!image.get(realX, realY)) {
        state++;
      }
    }

    if (state == 3) { // Found black, white, black, and stumbled back onto white; done
      return Point(realX, realY);
    }
    error += dy;
    if (error > 0) {
      y += ystep;
      error -= dx;
    }
  }

  // B-W-B run not found, return the last point visited.
  return Point(realX, realY);
}

} // namespace qrcode
} // namespace zxing
