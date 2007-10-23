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
 * Implementations of this class can, given locations of finder patterns for a QR code in an
 * image, sample the right points in the image to reconstruct the QR code, accounting for
 * perspective distortion. It is abstracted since it is relatively expensive and should be allowed
 * to take advantage of platform-specific optimized implementations, like Sun's Java Advanced
 * Imaging library, but which may not be available in other environments such as J2ME, and vice
 * versa.
 *
 * The implementation used can be controlled by calling {@link #setGridSamplerClassName(String)}
 * with the name of a class which implements this interface.
 *
 * @author srowen@google.com (Sean Owen)
 */
public abstract class GridSampler {

  private static final String DEFAULT_IMPL_CLASS =
      "com.google.zxing.qrcode.detector.DefaultGridSampler";

  private static String gridSamplerClassName = DEFAULT_IMPL_CLASS;
  private static GridSampler gridSampler;

  public static void setGridSamplerClassName(String className) {
    if (className == null) {
      throw new IllegalArgumentException();
    }
    gridSamplerClassName = className;
  }

  public static GridSampler getInstance() {
    if (gridSampler == null) {
      try {
        Class gridSamplerClass = Class.forName(gridSamplerClassName);
        gridSampler = (GridSampler) gridSamplerClass.newInstance();
      } catch (ClassNotFoundException cnfe) {
        throw new RuntimeException(cnfe.toString());
      } catch (IllegalAccessException iae) {
        throw new RuntimeException(iae.toString());
      } catch (InstantiationException ie) {
        throw new RuntimeException(ie.toString());
      }
    }
    return gridSampler;
  }

  protected abstract BitMatrix sampleGrid(MonochromeBitmapSource image,
                                          FinderPattern topLeft,
                                          FinderPattern topRight,
                                          FinderPattern bottomLeft,
                                          AlignmentPattern alignmentPattern,
                                          int dimension) throws ReaderException;

  protected static void checkEndpoint(MonochromeBitmapSource image, float[] points)
      throws ReaderException {
    int x = (int) points[0];
    int y = (int) points[1];
    if (x < 0 || x >= image.getWidth() || y < 0 || y >= image.getHeight()) {
      throw new ReaderException("Transformed point out of bounds at " + x + ',' + y);
    }
    x = (int) points[points.length - 2];
    y = (int) points[points.length - 1];
    if (x < 0 || x >= image.getWidth() || y < 0 || y >= image.getHeight()) {
      throw new ReaderException("Transformed point out of bounds at " + x + ',' + y);
    }
  }

}
