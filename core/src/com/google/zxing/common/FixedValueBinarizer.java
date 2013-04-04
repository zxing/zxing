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

import com.google.zxing.Binarizer;
import com.google.zxing.LuminanceSource;
import com.google.zxing.NotFoundException;

<<<<<<< Upstream, based on origin/master
/**
 * @author Guenther Grau
 */
public class FixedValueBinarizer extends HybridBinarizer {

  LuminanceSource source;

  public FixedValueBinarizer(LuminanceSource source) {
    super(source);
    this.source = source;
  }

  // Does not sharpen the data, as this call is intended to only be used by 2D Readers.
  @Override
  public BitMatrix getBlackMatrix() throws NotFoundException {
    AdjustableBitMatrix bitMatrix = new AdjustableBitMatrix(source);
    // FIXME calculate default black point 
    bitMatrix.setBlackpoint(95);
    return bitMatrix;
  }

  @Override
  public Binarizer createBinarizer(LuminanceSource source) {
    return new FixedValueBinarizer(source);
=======
public class FixedValueBinarizer extends GlobalHistogramBinarizer {

  private int blackPoint = -1;
  LuminanceSource source;

  public FixedValueBinarizer(LuminanceSource source) {
    super(source);
    this.source = source;
  }

  // Does not sharpen the data, as this call is intended to only be used by 2D Readers.
  @Override
  public BitMatrix getBlackMatrix() throws NotFoundException {
    return new AdjustableBitMatrix(source);
  }

  @Override
  public Binarizer createBinarizer(LuminanceSource source) {
    return new FixedValueBinarizer(source);
  }

  public int getBlackPoint() {
    return blackPoint;
  }

  public void setBlackPoint(int blackPoint) {
    this.blackPoint = blackPoint;
>>>>>>> 84c0c47 work in progress for a new PDF417 decoder
  }
}
