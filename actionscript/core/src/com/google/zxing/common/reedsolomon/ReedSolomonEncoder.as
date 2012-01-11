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

package com.google.zxing.common.reedsolomon
{

import com.google.zxing.common.flexdatatypes.ArrayList;
import com.google.zxing.common.flexdatatypes.IllegalArgumentException;
import com.google.zxing.common.flexdatatypes.Utils;
/**
 * <p>Implements Reed-Solomon enbcoding, as the name implies.</p>
 *
 * @author Sean Owen
 * @author William Rucklidge
 */
public class ReedSolomonEncoder 
{

  private var field:GenericGF;
  private var cachedGenerators:ArrayList;

  public function ReedSolomonEncoder(field:GenericGF ) {
    if (!GenericGF.QR_CODE_FIELD_256.Equals(field)) {
      throw new IllegalArgumentException("Only QR Code is supported at this time");
    }
    this.field = field;
    this.cachedGenerators = new ArrayList();
    cachedGenerators.addElement(new GenericGFPoly(field, [ 1 ]));
  }

  private function buildGenerator(degree:int):GenericGFPoly {
    if (degree >= cachedGenerators.size()) {
      var lastGenerator:GenericGFPoly= (cachedGenerators.elementAt(cachedGenerators.size() - 1) as GenericGFPoly);
      for (var d:int = cachedGenerators.size(); d <= degree; d++) {
        var nextGenerator:GenericGFPoly = lastGenerator.multiply(new GenericGFPoly(field, [ 1, field.exp(d - 1) ]));
        cachedGenerators.addElement(nextGenerator);
        lastGenerator = nextGenerator;
      }
    }
    return (cachedGenerators.elementAt(degree) as GenericGFPoly)    
  }

  public function encode(toEncode:Array, ecBytes:int):void 
  {
    if (ecBytes == 0) 
	{
      throw new IllegalArgumentException("No error correction bytes");
    }
    var dataBytes:int = toEncode.length - ecBytes;
    if (dataBytes <= 0) 
	{
      throw new IllegalArgumentException("No data bytes provided");
    }
    var generator:GenericGFPoly = buildGenerator(ecBytes);
    var infoCoefficients:Array = new Array(dataBytes);
    Utils.arraycopy(toEncode, 0, infoCoefficients, 0, dataBytes);
    var info:GenericGFPoly = new GenericGFPoly(field, infoCoefficients);
    info = info.multiplyByMonomial(ecBytes, 1);
    var remainder:GenericGFPoly = info.divide(generator)[1];
    var coefficients:Array = remainder.getCoefficients();
    var numZeroCoefficients:int = ecBytes - coefficients.length;
    for (var i:int = 0; i < numZeroCoefficients; i++) 
	{
      toEncode[dataBytes + i] = 0;
    }
    Utils.arraycopy(coefficients, 0, toEncode, dataBytes + numZeroCoefficients, coefficients.length);
  }

}
}