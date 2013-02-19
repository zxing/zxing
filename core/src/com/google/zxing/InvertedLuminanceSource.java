/*
 * Copyright 2013 ZXing authors
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

package com.google.zxing;

/**
 * A wrapper implementation of {@link LuminanceSource} which inverts the luminances it returns -- black becomes
 * white and vice versa, and each value becomes (255-value).
 * 
 * @author Sean Owen
 */
public final class InvertedLuminanceSource extends LuminanceSource {

  private final LuminanceSource delegate;

  public InvertedLuminanceSource(LuminanceSource delegate) {
    super(delegate.getWidth(), delegate.getHeight());
    this.delegate = delegate;
  }

  @Override
  public byte[] getRow(int y, byte[] row) {
    row = delegate.getRow(y, row);
    int width = getWidth();
    for (int i = 0; i < width; i++) {
      row[i] = (byte) (255 - (row[i] & 0xFF));
    }
    return row;
  }

  @Override
  public byte[] getMatrix() {
    byte[] matrix = delegate.getMatrix();
    int length = getWidth() * getHeight();
    byte[] invertedMatrix = new byte[length];
    for (int i = 0; i < length; i++) {
      invertedMatrix[i] = (byte) (255 - (matrix[i] & 0xFF));
    }
    return invertedMatrix;
  }
  
  @Override
  public boolean isCropSupported() {
    return delegate.isCropSupported();
  }

  @Override
  public LuminanceSource crop(int left, int top, int width, int height) {
    return new InvertedLuminanceSource(delegate.crop(left, top, width, height));
  }

  @Override
  public boolean isRotateSupported() {
    return delegate.isRotateSupported();
  }

  /**
   * @return original delegate {@link LuminanceSource} since invert undoes itself
   */
  @Override
  public LuminanceSource invert() {
    return delegate;
  }

  @Override
  public LuminanceSource rotateCounterClockwise() {
    return new InvertedLuminanceSource(delegate.rotateCounterClockwise());
  }

  @Override
  public LuminanceSource rotateCounterClockwise45() {
    return new InvertedLuminanceSource(delegate.rotateCounterClockwise45());
  }

}
