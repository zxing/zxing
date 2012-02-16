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

package com.google.zxing.pdf417.decoder.ec;

import com.google.zxing.ChecksumException;

/**
 * <p>Incomplete implementation of PDF417 error correction. For now, only detects errors.</p>
 *
 * @author Sean Owen
 * @see com.google.zxing.common.reedsolomon.ReedSolomonDecoder
 */
public final class ErrorCorrection {

  private final ModulusGF field;

  public ErrorCorrection() {
    this.field = ModulusGF.PDF417_GF;
  }

  public void decode(int[] received, int numECCodewords) throws ChecksumException {
    ModulusPoly poly = new ModulusPoly(field, received);
    int[] syndromeCoefficients = new int[numECCodewords];
    boolean noError = true;
    for (int i = 0; i < numECCodewords; i++) {
      int eval = poly.evaluateAt(field.exp(i + 1));
      syndromeCoefficients[syndromeCoefficients.length - 1 - i] = eval;
      if (eval != 0) {
        noError = false;
      }
    }
    if (!noError) {
      throw ChecksumException.getChecksumInstance();
    }
    // TODO actually correct errors!
  }

}
