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

package com.google.zxing.common;

/**
 * This class supports the transformation of a bit matrix. Ideally, we would define a BitMatrix interface which the current BitMatrix
 * class would implement. The TransformableBitMatrix class would also implement the BitMatrix interface and delegate the calls to 
 * a bitMatrix member transforming the coordinates of the in and output parameters, respectively.
 * 
 * @author Guenther Grau
 */
public class TransformableBitMatrix {

  private final AdjustableBitMatrix bitMatrix;
  private final CoordinateTransformer t;
  private final int width;
  private final int height;
  private final int blackpoint = 127;

  public TransformableBitMatrix(AdjustableBitMatrix bitMatrix, CoordinateTransformer coordinateTransformer) {
    this.bitMatrix = bitMatrix;
    this.t = coordinateTransformer;
    width = bitMatrix.getWidth();
    height = bitMatrix.getHeight();
  }

  public boolean get(int x, int y) {
    try {
      return bitMatrix.get(t.getX(x, y, width, height), t.getY(x, y, width, height));
    } catch (ArrayIndexOutOfBoundsException e) {
      e.printStackTrace();
    }
    return true;
  }

  public void set(int x, int y) {
    bitMatrix.set(t.getX(x, y, width, height), t.getY(x, y, width, height));
  }

  public void flip(int x, int y) {
    bitMatrix.flip(t.getX(x, y, width, height), t.getY(x, y, width, height));
  }

  public void clear() {
    bitMatrix.clear();
  }

  public int getHeight() {
    return height;
  }

  public int getWidth() {
    return width;
  }

  public int getBlackpoint() {
    return blackpoint;
  }

  public void setBlackpoint(int blackpoint) {
    if (bitMatrix instanceof AdjustableBitMatrix) {
      bitMatrix.setBlackpoint(blackpoint);
    }
  }
}
