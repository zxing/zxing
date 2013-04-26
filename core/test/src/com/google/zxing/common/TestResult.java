/*
 * Copyright 2008 ZXing authors
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

final class TestResult {

  private final int mustPassCount;
  private final int tryHarderCount;
  private final int maxMisreads;
  private final int maxTryHarderMisreads;
  private final float rotation;

  TestResult(int mustPassCount, int tryHarderCount, int maxMisreads, int maxTryHarderMisreads, float rotation) {
    this.mustPassCount = mustPassCount;
    this.tryHarderCount = tryHarderCount;
    this.maxMisreads = maxMisreads;
    this.maxTryHarderMisreads = maxTryHarderMisreads;
    this.rotation = rotation;
  }

  int getMustPassCount() {
    return mustPassCount;
  }

  int getTryHarderCount() {
    return tryHarderCount;
  }
  
  int getMaxMisreads() {
    return maxMisreads;
  }
  
  int getMaxTryHarderMisreads() {
    return maxTryHarderMisreads;
  }

  float getRotation() {
    return rotation;
  }

}
