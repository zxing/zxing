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

package com.google.zxing.client.android;

import android.graphics.Matrix;
import com.google.zxing.MonochromeBitmapSource;
import com.google.zxing.ReaderException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.GridSampler;

/**
 * Implementation based on Android's
 * {@link Matrix#setPolyToPoly(float[], int, float[], int, int)}
 * class, which should offer faster performance for these matrix
 * operations.
 * 
 * @author srowen@google.com (Sean Owen)
 */
public final class AndroidGraphicsGridSampler extends GridSampler {

  @Override
  public BitMatrix sampleGrid(MonochromeBitmapSource image,
                              int dimension,
                              float p1ToX, float p1ToY,
                              float p2ToX, float p2ToY,
                              float p3ToX, float p3ToY,
                              float p4ToX, float p4ToY,
                              float p1FromX, float p1FromY,
                              float p2FromX, float p2FromY,
                              float p3FromX, float p3FromY,
                              float p4FromX, float p4FromY) throws ReaderException {

    Matrix transformMatrix = new Matrix();
    boolean succeeded = transformMatrix.setPolyToPoly(
      new float[] { p1FromX, p1FromY, p2FromX, p2FromY, p3FromX, p3FromY, p4FromX, p4FromY },
      0,
      new float[] { p1ToX, p1ToY, p2ToX, p2ToY, p3ToX, p3ToY, p4ToX, p4ToY },
      0,
      4
    );
    if (!succeeded) {
      throw new ReaderException("Could not establish transformation matrix");
    }

    BitMatrix bits = new BitMatrix(dimension);
    float[] points = new float[dimension << 1];
    for (int i = 0; i < dimension; i++) {
      int max = points.length;
      float iValue = (float) i + 0.5f;
      for (int j = 0; j < max; j += 2) {
        points[j] = (float) (j >> 1) + 0.5f;
        points[j + 1] = iValue;
      }
      transformMatrix.mapPoints(points);
      // Quick check to see if points transformed to something inside the image;
      // sufficent to check the endpoints
      checkAndNudgePoints(image, points);
      for (int j = 0; j < max; j += 2) {
        if (image.isBlack((int) points[j], (int) points[j + 1])) {
          // Black(-ish) pixel
          bits.set(i, j >> 1);
        }
      }
    }
    return bits;
  }

}
