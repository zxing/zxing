/*
 * Copyright 2007 Google Inc.
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

package com.google.zxing.qrcode.detector;

import com.google.zxing.MonochromeBitmapSource;
import com.google.zxing.ReaderException;
import com.google.zxing.ResultPoint;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.Version;

/**
 * @author srowen@google.com (Sean Owen)
 */
public final class Detector {

  private final MonochromeBitmapSource image;

  public Detector(MonochromeBitmapSource image) {
    this.image = image;
  }

  public DetectorResult detect() throws ReaderException {

    MonochromeBitmapSource image = this.image;

    FinderPatternFinder finder = new FinderPatternFinder(image);
    FinderPatternInfo info = finder.find();

    FinderPattern topLeft = info.getTopLeft();
    FinderPattern topRight = info.getTopRight();
    FinderPattern bottomLeft = info.getBottomLeft();

    float moduleSize = calculateModuleSize(topLeft, topRight, bottomLeft);
    int dimension = computeDimension(topLeft, topRight, bottomLeft, moduleSize);
    Version provisionalVersion = Version.getProvisionalVersionForDimension(dimension);
    int modulesBetweenFPCenters = provisionalVersion.getDimensionForVersion() - 7;

    // Guess where a "bottom right" finder pattern would have been
    float bottomRightX = topRight.getX() - topLeft.getX() + bottomLeft.getX();
    float bottomRightY = topRight.getY() - topLeft.getY() + bottomLeft.getY();

    AlignmentPattern alignmentPattern = null;
    // Anything above version 1 has an alignment pattern
    if (provisionalVersion.getAlignmentPatternCenters().length > 0) {

      // Estimate that alignment pattern is closer by 3 modules
      // from "bottom right" to known top left location
      float correctionToTopLeft = 1.0f - 3.0f / (float) modulesBetweenFPCenters;
      int estAlignmentX = (int) (topLeft.getX() + correctionToTopLeft * (bottomRightX - topLeft.getX()));
      int estAlignmentY = (int) (topLeft.getY() + correctionToTopLeft * (bottomRightY - topLeft.getY()));

      // Kind of arbitrary -- expand search radius before giving up
      for (int i = 4; i <= 16; i <<= 1) {
        try {
          alignmentPattern = findAlignmentInRegion(moduleSize,
              estAlignmentX,
              estAlignmentY,
              (float) i);
          break;
        } catch (ReaderException de) {
          // try next round
        }
      }
      if (alignmentPattern == null) {
        throw new ReaderException("Could not find alignment pattern");
      }

    }

    GridSampler sampler = GridSampler.getInstance();
    BitMatrix bits = sampler.sampleGrid(image,
        topLeft,
        topRight,
        bottomLeft,
        alignmentPattern,
        dimension);

    /*
    try {
      BufferedImage outImage =
          new BufferedImage(dimension,
                            dimension,
                            BufferedImage.TYPE_BYTE_BINARY);
      for (int i = 0; i < dimension; i++) {
        for (int j = 0; j < dimension; j++) {
          if (bits.get(i, j)) {
            outImage.setRGB(j, i, 0xFF000000);
          } else {
            outImage.setRGB(j, i, 0xFFFFFFFF);
          }
        }
      }
      ImageIO.write(outImage, "PNG",
          new File("/home/srowen/out.png"));
    } catch (IOException ioe) {
      ioe.printStackTrace();
    }
     */

    ResultPoint[] points;
    if (alignmentPattern == null) {
      points = new ResultPoint[] { bottomLeft, topLeft, topRight };      
    } else {
      points = new ResultPoint[] { bottomLeft, topLeft, topRight, alignmentPattern };
    }
    return new DetectorResult(bits, points);
  }

  private static int computeDimension(ResultPoint topLeft,
                                      ResultPoint topRight,
                                      ResultPoint bottomLeft,
                                      float moduleSize)
      throws ReaderException {
    int tltrCentersDimension =
        round(FinderPatternFinder.distance(topLeft, topRight) / moduleSize);
    int tlblCentersDimension =
        round(FinderPatternFinder.distance(topLeft, bottomLeft) / moduleSize);
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
        throw new ReaderException("Bad dimension: " + dimension);
    }
    return dimension;
  }

  private float calculateModuleSize(ResultPoint topLeft,
                                    ResultPoint topRight,
                                    ResultPoint bottomLeft) {
    // Take the average
    return (calculateModuleSizeOneWay(topLeft, topRight) +
            calculateModuleSizeOneWay(topLeft, bottomLeft)) / 2.0f;
  }

  private float calculateModuleSizeOneWay(ResultPoint pattern,
                                          ResultPoint otherPattern) {
    float moduleSizeEst1 = sizeOfBlackWhiteBlackRunBothWays((int) pattern.getX(),
                                                            (int) pattern.getY(),
                                                            (int) otherPattern.getX(),
                                                            (int) otherPattern.getY());
    float moduleSizeEst2 = sizeOfBlackWhiteBlackRunBothWays((int) otherPattern.getX(),
                                                            (int) otherPattern.getY(),
                                                            (int) pattern.getX(),
                                                            (int) pattern.getY());
    if (Float.isNaN(moduleSizeEst1)) {
      return moduleSizeEst2;
    }
    if (Float.isNaN(moduleSizeEst2)) {
      return moduleSizeEst1;
    }
    // Average them, and divide by 7 since we've counted the width of 3 black modules,
    // and 1 white and 1 black module on either side. Ergo, divide sum by 14.
    return (moduleSizeEst1 + moduleSizeEst2) / 14.0f;
  }

  private float sizeOfBlackWhiteBlackRunBothWays(int fromX, int fromY, int toX, int toY) {
    float result = sizeOfBlackWhiteBlackRun(fromX, fromY, toX, toY);
    result += sizeOfBlackWhiteBlackRun(fromX, fromY, fromX - (toX - fromX), fromY - (toY - fromY));
    return result - 1.0f; // -1 because we counted the middle pixel twice
  }

  private float sizeOfBlackWhiteBlackRun(int fromX, int fromY, int toX, int toY) {
    // Mild variant of Bresenham's algorithm;
    // see http://en.wikipedia.org/wiki/Bresenham's_line_algorithm
    boolean steep = Math.abs(toY - fromY) > Math.abs(toX - fromX);
    if (steep) {
      int temp = fromX;
      fromX = fromY;
      fromY = temp;
      temp = toX;
      toX = toY;
      toY = temp;
    }

    int dx = Math.abs(toX - fromX);
    int dy = Math.abs(toY - fromY);
    int error = -dx >> 1;
    int ystep = fromY < toY ? 1 : -1;
    int xstep = fromX < toX ? 1 : -1;
    int state = 0; // In black pixels, looking for white, first or second time
    for (int x = fromX, y = fromY; x != toX; x += xstep) {

      int realX = steep ? y : x;
      int realY = steep ? x : y;
      if (state == 1) { // In white pixels, looking for black
        if (image.isBlack(realX, realY)) {
          state++;
        }
      } else {
        if (!image.isBlack(realX, realY)) {
          state++;
        }
      }

      if (state == 3) { // Found black, white, black, and stumbled back onto white; done
        int diffX = x - fromX;
        int diffY = y - fromY;
        return (float) Math.sqrt((double) (diffX * diffX + diffY * diffY));
      }
      error += dy;
      if (error > 0) {
        y += ystep;
        error -= dx;
      }
    }
    // Hmm, couldn't find all of what we wanted -- don't know
    return Float.NaN;
  }

  private AlignmentPattern findAlignmentInRegion(float overallEstModuleSize,
                                                 int estAlignmentX,
                                                 int estAlignmentY,
                                                 float allowanceFactor)
      throws ReaderException {
    // Look for an alignment pattern (3 modules in size) around where it
    // should be
    int allowance = (int) (allowanceFactor * overallEstModuleSize);
    int alignmentAreaLeftX = Math.max(0, estAlignmentX - allowance);
    int alignmentAreaRightX = Math.min(image.getWidth() - 1,
        estAlignmentX + allowance);
    int alignmentAreaTopY = Math.max(0, estAlignmentY - allowance);
    int alignmentAreaBottomY = Math.min(image.getHeight() - 1,
        estAlignmentY + allowance);

    AlignmentPatternFinder alignmentFinder =
        new AlignmentPatternFinder(
            image,
            alignmentAreaLeftX,
            alignmentAreaTopY,
            alignmentAreaRightX - alignmentAreaLeftX,
            alignmentAreaBottomY - alignmentAreaTopY,
            overallEstModuleSize);
    return alignmentFinder.find();
  }

  /**
   * Ends up being a bit faster than Math.round()
   */
  private static int round(float d) {
    return (int) (d + 0.5f);
  }

}
