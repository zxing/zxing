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

package com.google.zxing.common.reedsolomon;

import java.util.Vector;

/**
 * <p>Implements Reed-Solomon enbcoding, as the name implies.</p>
 *
 * @author Sean Owen
 * @author William Rucklidge
 */
public final class ReedSolomonEncoder {

  private final GenericGF field;
  private final Vector cachedGenerators;

  public ReedSolomonEncoder(GenericGF field) {
    if (!GenericGF.QR_CODE_FIELD_256.equals(field)) {
      throw new IllegalArgumentException("Only QR Code is supported at this time");
    }
    this.field = field;
    this.cachedGenerators = new Vector();
    cachedGenerators.addElement(new GenericGFPoly(field, new int[] { 1 }));
  }

  private GenericGFPoly buildGenerator(int degree) {
    if (degree >= cachedGenerators.size()) {
      GenericGFPoly lastGenerator = (GenericGFPoly) cachedGenerators.elementAt(cachedGenerators.size() - 1);
      for (int d = cachedGenerators.size(); d <= degree; d++) {
        GenericGFPoly nextGenerator = lastGenerator.multiply(new GenericGFPoly(field, new int[] { 1, field.exp(d - 1) }));
        cachedGenerators.addElement(nextGenerator);
        lastGenerator = nextGenerator;
      }
    }
    return (GenericGFPoly) cachedGenerators.elementAt(degree);    
  }

  public void encode(int[] toEncode, int ecBytes) {
    if (ecBytes == 0) {
      throw new IllegalArgumentException("No error correction bytes");
    }
    int dataBytes = toEncode.length - ecBytes;
    if (dataBytes <= 0) {
      throw new IllegalArgumentException("No data bytes provided");
    }
    GenericGFPoly generator = buildGenerator(ecBytes);
    int[] infoCoefficients = new int[dataBytes];
    System.arraycopy(toEncode, 0, infoCoefficients, 0, dataBytes);
    GenericGFPoly info = new GenericGFPoly(field, infoCoefficients);
    info = info.multiplyByMonomial(ecBytes, 1);
    GenericGFPoly remainder = info.divide(generator)[1];
    int[] coefficients = remainder.getCoefficients();
    int numZeroCoefficients = ecBytes - coefficients.length;
    for (int i = 0; i < numZeroCoefficients; i++) {
      toEncode[dataBytes + i] = 0;
    }
    System.arraycopy(coefficients, 0, toEncode, dataBytes + numZeroCoefficients, coefficients.length);
  }

}
