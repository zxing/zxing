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

import com.google.zxing.common.PerspectiveTransform;

public final class PDF417PerspectiveTransform extends PerspectiveTransform {

  private final float x0, y0, x2, y2, x0p, y0p, x1p, y1p, x2p, y2p, x3p, y3p;

  float widthFrom;
  float heightFrom;

  float topWidthTo;
  float bottomWidthTo;
  float leftHeightTo;
  float rightHeightTo;

  public PDF417PerspectiveTransform(float x0,
                                    float y0,
                                    float x2,
                                    float y2,
                                    float x0p,
                                    float y0p,
                                    float x1p,
                                    float y1p,
                                    float x2p,
                                    float y2p,
                                    float x3p,
                                    float y3p) {
    this.x0 = x0;
    this.x2 = x2 - 1;
    this.y0 = y0;
    this.y2 = y2 - 1;

    this.x0p = x0p;
    this.x1p = x1p;
    this.x2p = x2p;
    this.x3p = x3p;

    this.y0p = y0p;
    this.y1p = y1p;
    this.y2p = y2p;
    this.y3p = y3p;

    init();
  }

  private void init() {
    widthFrom = x2 - x0;
    heightFrom = y2 - y0;

    topWidthTo = x1p - x0p;
    bottomWidthTo = x2p - x3p;
    leftHeightTo = y3p - y0p;
    rightHeightTo = y2p - y1p;
  }

  public void transformPoints(float[] points) {
    int max = points.length;
    for (int i = 0; i < max; i += 2) {
      float deltaX = points[i] - x0;
      float deltaY = points[i + 1] - y0;

      points[i] = 
          ((x0p + deltaX * topWidthTo / widthFrom) * (heightFrom - deltaY) + (x3p + deltaX * bottomWidthTo / widthFrom) * deltaY) /
          heightFrom;
      points[i + 1] = y0p + points[i + 1];
    }
  }
}
