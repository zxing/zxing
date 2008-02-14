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

/**
 * <p>Encapsulates information about finder patterns in an image, including the location of
 * the three finder patterns, and their estimated module size.</p>
 *
 * @author srowen@google.com (Sean Owen)
 */
final class FinderPatternInfo {

  private final float rawEstimatedModuleSize;
  private final FinderPattern bottomLeft;
  private final FinderPattern topLeft;
  private final FinderPattern topRight;

  FinderPatternInfo(float rawEstimatedModuleSize,
                    FinderPattern[] patternCenters) {
    this.rawEstimatedModuleSize = rawEstimatedModuleSize;
    this.bottomLeft = patternCenters[0];
    this.topLeft = patternCenters[1];
    this.topRight = patternCenters[2];
  }

  float getRawEstimatedModuleSize() {
    return rawEstimatedModuleSize;
  }

  FinderPattern getBottomLeft() {
    return bottomLeft;
  }

  FinderPattern getTopLeft() {
    return topLeft;
  }

  FinderPattern getTopRight() {
    return topRight;
  }

}
