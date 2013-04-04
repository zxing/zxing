/*
 * Copyright 2007 ZXing authors
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

package com.google.zxing.pdf417.detector;

import com.google.zxing.NotFoundException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.DefaultGridSampler;
import com.google.zxing.common.PerspectiveTransform;

/**
 * @author Guenther Grau
 */
public final class PDF417GridSampler extends DefaultGridSampler {

  @Override
  public BitMatrix sampleGrid(BitMatrix image, int dimensionX, int dimensionY, float p1ToX, float p1ToY, float p2ToX, float p2ToY,
      float p3ToX, float p3ToY, float p4ToX, float p4ToY, float p1FromX, float p1FromY, float p2FromX, float p2FromY, float p3FromX,
      float p3FromY, float p4FromX, float p4FromY) throws NotFoundException {

    PerspectiveTransform transform = new PDF417PerspectiveTransform(p1ToX, p1ToY, p3ToX, p3ToY, p1FromX, p1FromY, p2FromX, p2FromY,
        p3FromX, p3FromY, p4FromX, p4FromY);

    return sampleGrid(image, dimensionX, dimensionY, transform);
  }

  @Override
  public BitMatrix sampleGrid(BitMatrix image, int dimensionX, int dimensionY, PerspectiveTransform transform) throws NotFoundException {
    if (dimensionX <= 0 || dimensionY <= 0) {
      throw NotFoundException.getNotFoundInstance();
    }
    BitMatrix bits = new BitMatrix(dimensionX, dimensionY);
    //bits.setModuleWidth(image.getModuleWidth());
    final int coordinateCount = dimensionX << 1;
    float[] points = new float[coordinateCount];
    for (int y = 0; y < dimensionY; y++) {
      for (int x = 0; x < coordinateCount; x += 2) {
        points[x] = x >> 1;
        points[x + 1] = y;
      }
      transform.transformPoints(points);
      // Quick check to see if points transformed to something inside the image;
      // sufficient to check the endpoints
      checkAndNudgePoints(image, points);
      try {
        for (int x = 0; x < coordinateCount; x += 2) {
          if (image.get(Math.round(points[x]), Math.round(points[x + 1]))) {
            // Black(-ish) pixel
            bits.set(x >> 1, y);
          }
        }
      } catch (ArrayIndexOutOfBoundsException aioobe) {
        // This feels wrong, but, sometimes if the finder patterns are misidentified, the resulting
        // transform gets "twisted" such that it maps a straight line of points to a set of points
        // whose endpoints are in bounds, but others are not. There is probably some mathematical
        // way to detect this about the transformation that I don't know yet.
        // This results in an ugly runtime exception despite our clever checks above -- can't have
        // that. We could check each point's coordinates but that feels duplicative. We settle for
        // catching and wrapping ArrayIndexOutOfBoundsException.
        aioobe.printStackTrace();
        throw NotFoundException.getNotFoundInstance();
      }
    }
    return bits;
  }

}
