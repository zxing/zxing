/*
 * Copyright 2009 ZXing authors
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

package com.google.zxing.common;

import com.google.zxing.BlackPointEstimationMethod;
import com.google.zxing.MonochromeBitmapSource;
import com.google.zxing.ReaderException;

/**
 * Encapulates a cropped region of another {@link com.google.zxing.MonochromeBitmapSource}.
 *
 * @author Sean Owen
 */
public final class CroppedMonochromeBitmapSource implements MonochromeBitmapSource {

  private final MonochromeBitmapSource delegate;
  private final int left;
  private final int top;
  private final int right;
  private final int bottom;

  /**
   * Creates an instance that uses only a region of the given image as a source of pixels to decode.
   *
   * @param delegate image to decode a region of
   * @param left x coordinate of leftmost pixels to decode
   * @param top y coordinate of topmost pixels to decode
   * @param right one more than the x coordinate of rightmost pixels to decode, i.e. we will decode
   *  pixels whose x coordinate is in [left,right)
   * @param bottom likewise, one more than the y coordinate of the bottommost pixels to decode
   */
  public CroppedMonochromeBitmapSource(MonochromeBitmapSource delegate,
                                       int left, int top, int right, int bottom) {
    this.delegate = delegate;
    this.left = left;
    this.top = top;
    this.right = right;
    this.bottom = bottom;
  }

  public boolean isBlack(int x, int y) {
    return delegate.isBlack(left + x, top + y);
  }

  public BitArray getBlackRow(int y, BitArray row, int startX, int getWidth) {
    return delegate.getBlackRow(top + y, row, left + startX, getWidth);
  }

  public BitArray getBlackColumn(int x, BitArray column, int startY, int getHeight) {
    return delegate.getBlackColumn(left + x, column, top + startY, getHeight);
  }

  public int getHeight() {
    return bottom - top;
  }

  public int getWidth() {
    return right - left;
  }

  public void estimateBlackPoint(BlackPointEstimationMethod method, int argument)
      throws ReaderException {
    // Hmm, the delegate will probably base this on the whole image though...
    delegate.estimateBlackPoint(method, argument);
  }

  public BlackPointEstimationMethod getLastEstimationMethod() {
    return delegate.getLastEstimationMethod();
  }

  public MonochromeBitmapSource rotateCounterClockwise() {
    MonochromeBitmapSource rotated = delegate.rotateCounterClockwise();
    return new CroppedMonochromeBitmapSource(rotated,
                                             top,
                                             delegate.getWidth() - right,
                                             delegate.getHeight() - bottom,
                                             left);
  }

  public boolean isRotateSupported() {
    return delegate.isRotateSupported();
  }

  public int getLuminance(int x, int y) {
    return delegate.getLuminance(x, y);
  }

  public int[] getLuminanceRow(int y, int[] row) {
    return delegate.getLuminanceRow(y, row);
  }

  public int[] getLuminanceColumn(int x, int[] column) {
    return delegate.getLuminanceColumn(x, column);
  }

}
