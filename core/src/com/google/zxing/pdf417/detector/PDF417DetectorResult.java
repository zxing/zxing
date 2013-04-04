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

import com.google.zxing.ResultPoint;
import com.google.zxing.common.BitMatrix;

/**
 * Ideally, this class would extend DetectorResult. This is currently not possible, because TransformableBitMatrix doesn't have a
 * common interface with BitMatrix. See comment in TransformableBitMatrix.
 *
 * @author Guenther Grau
 */
public class PDF417DetectorResult {

  private final BitMatrix bits;
  private final ResultPoint[] points;
  private final float codewordWidth;

  public PDF417DetectorResult(BitMatrix bits, ResultPoint[] points, float codewordWidth) {
    this.bits = bits;
    this.points = points;
    this.codewordWidth = codewordWidth;
  }

  public final BitMatrix getBits() {
    return bits;
  }

  public final ResultPoint[] getPoints() {
    return points;
  }

  public float getCodewordWidth() {
    return codewordWidth;
  }
}