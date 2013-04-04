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
 * This class supports the rotation of a bit matrix. To keep it simple it only supports rotation 180 degrees.
 * It could be made more generic and sophisticated to support cropping and/or arbitrary transformations. I'll leave this as an exercise
 * to the reader ;-)
 * 
 * @author Guenther Grau
 */
public class RotationTransformer implements CoordinateTransformer {

  private boolean rotate;

  @Override
  public int getX(int x, int y, int width, int height) {
    if (!rotate) {
      return x;
    }
    return width - x - 1;
  }

  @Override
  public int getY(int x, int y, int width, int height) {
    if (!rotate) {
      return y;
    }
    return height - y - 1;
  }

  public boolean isRotate() {
    return rotate;
  }

  public void setRotate(boolean rotate) {
    this.rotate = rotate;
  }
}
