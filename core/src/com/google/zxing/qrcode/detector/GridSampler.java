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

  private static final String DEFAULT_IMPL_CLASS = "com.google.zxing.qrcode.detector.DefaultGridSampler";

  private static String gridSamplerClassName = DEFAULT_IMPL_CLASS;
  private static GridSampler gridSampler;

  /**
   * <p>Sets the (fully-qualified) name of the implementation of {@link GridSampler} which will be
   * returned from {@link #getInstance()}.</p>
   *
   * @param className {@link GridSampler} implementation to instantiate
   */
  public static void setGridSamplerClassName(String className) {
    if (className == null) {
      throw new IllegalArgumentException();
    }
    gridSamplerClassName = className;
  }

  /**
   * @return the current implementation of {@link GridSampler}, instantiating one if one does
   *  not already exist. The class which is instantied may be set by
   *  {@link #setGridSamplerClassName(String)}
   */
  public static GridSampler getInstance() {
    if (gridSampler == null) {
      // We don't need to synchronize this -- don't really care if two threads initialize at once.
      // The second one will win.
      try {
        Class gridSamplerClass = Class.forName(gridSamplerClassName);
        gridSampler = (GridSampler) gridSamplerClass.newInstance();
      } catch (ClassNotFoundException cnfe) {
        // The exceptions below would represent bad programming errors;
        // For J2ME we're punting them out with RuntimeException
        throw new RuntimeException(cnfe.toString());
      } catch (IllegalAccessException iae) {
        throw new RuntimeException(iae.toString());
      } catch (InstantiationException ie) {
        throw new RuntimeException(ie.toString());
      }
    }
    return gridSampler;
  }

  /**
   * <p>Given an image, locations of a QR Code's finder patterns and bottom-right alignment pattern,
   * and the presumed dimension in modules of the QR Code, implemntations of this method extract
   * the QR Code from the image by sampling the points in the image which should correspond to the
   * modules of the QR Code.</p>
   *
   * @param image image to sample
   * @param topLeft top-left finder pattern location
   * @param topRight top-right finder pattern location
   * @param bottomLeft bottom-left finder pattern location
   * @param alignmentPattern bottom-right alignment pattern location
   * @param dimension dimension of QR Code
   * @return {@link BitMatrix} representing QR Code's modules
   * @throws ReaderException if QR Code cannot be reasonably sampled -- for example if the location
   *  of the finder patterns imply a transformation that would require sampling off the image
   */
  protected abstract BitMatrix sampleGrid(MonochromeBitmapSource image,
                                          FinderPattern topLeft,
                                          FinderPattern topRight,
                                          FinderPattern bottomLeft,
                                          AlignmentPattern alignmentPattern,
                                          int dimension) throws ReaderException;

  /**
   * <p>Checks a set of points that have been transformed to sample points on an image against
   * the image's dimensions to see if the endpoints are even within the image.
   * This method actually only checks the endpoints since the points are assumed to lie
   * on a line.</p>
   *
   * <p>This method will actually "nudge" the endpoints back onto the image if they are found to be barely
   * (less than 1 pixel) off the image. This accounts for imperfect detection of finder patterns in an image
   * where the QR Code runs all the way to the image border.</p>
   *
   * @param image image into which the points should map
   * @param points actual points in x1,y1,...,xn,yn form
   * @throws ReaderException if an endpoint is lies outside the image boundaries
   */
  protected static void checkEndpoint(MonochromeBitmapSource image, float[] points) throws ReaderException {
    int width = image.getWidth();
    int height = image.getHeight();
    checkOneEndpoint(points, (int) points[0], (int) points[1], width, height);
    checkOneEndpoint(points, (int) points[points.length - 2], (int) points[points.length - 1], width, height);
  }

  private static void checkOneEndpoint(float[] points, int x, int y, int width, int height) throws ReaderException {
    if (x < -1 || x > width || y < -1 || y > height) {
      throw new ReaderException("Transformed point out of bounds at " + x + ',' + y);
    }
    if (x == -1) {
      points[0] = 0.0f;
    }
    if (y == -1) {
      points[1] = 0.0f;
    }
    if (x == width) {
      points[0] = width - 1;
    }
    if (y == height) {
      points[1] = height - 1;
    }
  }

}
