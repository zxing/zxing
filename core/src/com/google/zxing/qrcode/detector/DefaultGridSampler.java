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
import com.google.zxing.common.BitMatrix;

/**
 * @author srowen@google.com (Sean Owen)
 */
public final class DefaultGridSampler extends GridSampler {

  protected BitMatrix sampleGrid(MonochromeBitmapSource image,
                                 FinderPattern topLeft,
                                 FinderPattern topRight,
                                 FinderPattern bottomLeft,
                                 AlignmentPattern alignmentPattern,
                                 int dimension) throws ReaderException {
    float bottomRightX;
    float bottomRightY;
    if (alignmentPattern != null) {
      bottomRightX = alignmentPattern.getX();
      bottomRightY = alignmentPattern.getY();
    } else {
      // Don't have an alignment pattern, just make up the bottom-right point
      bottomRightX = (topRight.getX() - topLeft.getX()) + bottomLeft.getX();
      bottomRightY = (topRight.getY() - topLeft.getY()) + bottomLeft.getY();
    }

    float dimMinusThree = (float) dimension - 3.5f;
    JAIPerspectiveTransform transform = JAIPerspectiveTransform.getQuadToQuad(
        3.5f,
        3.5f,
        dimMinusThree,
        3.5f,
        3.5f,
        dimMinusThree,
        dimMinusThree - 3.0f,
        dimMinusThree - 3.0f,
        topLeft.getX(),
        topLeft.getY(),
        topRight.getX(),
        topRight.getY(),
        bottomLeft.getX(),
        bottomLeft.getY(),
        bottomRightX,
        bottomRightY);

    BitMatrix bits = new BitMatrix(dimension);
    float[] points = new float[dimension << 1];
    for (int i = 0; i < dimension; i++) {
      int max = points.length;
      float iValue = (float) i + 0.5f;
      for (int j = 0; j < max; j += 2) {
        points[j] = (float) (j >> 1) + 0.5f;
        points[j + 1] = iValue;
      }
      transform.transform(points);
      // Quick check to see if points transformed to something inside the image;
      // sufficent to check the endpoints
      checkEndpoint(image, points);
      for (int j = 0; j < dimension; j++) {
        int offset = j << 1;
        if (image.isBlack((int) points[offset], (int) points[offset + 1])) {
          // Black(-ish) pixel
          bits.set(i, j);
        }
      }
    }
    return bits;
  }

}