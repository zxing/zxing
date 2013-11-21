/*
 * Copyright 2012 ZXing authors
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
 * Simply encapsulates a width and height.
 */
public final class Dimension {

  private final int width;
  private final int height;
  
  public Dimension(int width, int height) {
    if (width < 0 || height < 0) {
      throw new IllegalArgumentException();
    }
    this.width = width;
    this.height = height;
  }

  public int getWidth() {
    return width;
  }

  public int getHeight() {
    return height;
  }

  @Override 
  public boolean equals(Object other) {
    if (other instanceof Dimension) {
      Dimension d = (Dimension) other;
      return width == d.width && height == d.height;
    }
    return false;
  }

  @Override 
  public int hashCode() {
      return width * 32713 + height;
  }

  @Override
  public String toString() {
    return width + "x" + height;
  }

}
