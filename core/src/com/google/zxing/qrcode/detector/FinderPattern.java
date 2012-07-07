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

package com.google.zxing.qrcode.detector;

import com.google.zxing.ResultPoint;

/**
 * <p>Encapsulates a finder pattern, which are the three square patterns found in
 * the corners of QR Codes. It also encapsulates a count of similar finder patterns,
 * as a convenience to the finder's bookkeeping.</p>
 *
 * @author Sean Owen
 */
public final class FinderPattern extends ResultPoint {

  private final float estimatedModuleSize;
  private int count;

  FinderPattern(float posX, float posY, float estimatedModuleSize) {
    this(posX, posY, estimatedModuleSize, 1);
  }

  private FinderPattern(float posX, float posY, float estimatedModuleSize, int count) {
    super(posX, posY);
    this.estimatedModuleSize = estimatedModuleSize;
    this.count = count;
  }

  public float getEstimatedModuleSize() {
    return estimatedModuleSize;
  }

  int getCount() {
    return count;
  }

  void incrementCount() {
    this.count++;
  }

  /**
   * <p>Determines if this finder pattern "about equals" a finder pattern at the stated
   * position and size -- meaning, it is at nearly the same center with nearly the same size.</p>
   */
  boolean aboutEquals(float moduleSize, float i, float j) {
    if (Math.abs(i - getY()) <= moduleSize && Math.abs(j - getX()) <= moduleSize) {
      float moduleSizeDiff = Math.abs(moduleSize - estimatedModuleSize);
      return moduleSizeDiff <= 1.0f || moduleSizeDiff <= estimatedModuleSize;
    }
    return false;
  }

  /**
   * Combines this object's current estimate of a finder pattern position and module size
   * with a new estimate. It returns a new {@code FinderPattern} containing a weighted average
   * based on count.
   */
  FinderPattern combineEstimate(float i, float j, float newModuleSize) {
    int combinedCount = count + 1;
    float combinedX = (count * getX() + j) / combinedCount;
    float combinedY = (count * getY() + i) / combinedCount;
    float combinedModuleSize = (count * estimatedModuleSize + newModuleSize) / combinedCount;
    return new FinderPattern(combinedX, combinedY, combinedModuleSize, combinedCount);
  }

}
