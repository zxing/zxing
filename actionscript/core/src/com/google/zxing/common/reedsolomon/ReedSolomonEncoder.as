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
	  public  class ReedSolomonEncoder
    { 
    	import com.google.zxing.common.flexdatatypes.ArrayList;
    	import com.google.zxing.common.flexdatatypes.IllegalArgumentException;
    	
          private var Field:GF256 ;
          private var cachedGenerators:ArrayList;

          public function ReedSolomonEncoder(field:GF256 ) {
            if (GF256.QR_CODE_FIELD != field) {
              throw new IllegalArgumentException("Only QR Code is supported at this time");
            }
            this.Field = field;
            this.cachedGenerators = new ArrayList();
            cachedGenerators.Add(new GF256Poly(field, [ 1 ]));
          }

          private function buildGenerator(degree:int):GF256Poly {
            if (degree >= cachedGenerators.Count) {
                var lastGenerator:GF256Poly = cachedGenerators.getObjectByIndex(cachedGenerators.Count - 1) as GF256Poly;
              for (var d:int = cachedGenerators.Count; d <= degree; d++)
              {
                  var nextGenerator:GF256Poly = lastGenerator.multiply(new GF256Poly(Field, [ 1, Field.exp(d - 1)]));
                cachedGenerators.Add(nextGenerator);
                lastGenerator = nextGenerator;
              }
            }
            return (cachedGenerators.getObjectByIndex(degree) as GF256Poly);    
          }

          public function encode( toEncode:Array,  ecBytes:int) :void{
            if (ecBytes == 0) {
              throw new IllegalArgumentException("No error correction bytes");
            }
            var dataBytes:int = toEncode.length - ecBytes;
            if (dataBytes <= 0) {
              throw new IllegalArgumentException("No data bytes provided");
            }
            var generator:GF256Poly = buildGenerator(ecBytes);
            var infoCoefficients:Array = new Array(dataBytes);
            //System.Array.Copy(toEncode, 0, infoCoefficients, 0, dataBytes);
            for (var ii:int=0;ii<dataBytes;ii++)
            {
            	infoCoefficients[ii] = toEncode[ii];
            }
            
            var info:GF256Poly  = new GF256Poly(this.Field, infoCoefficients);
            info = info.multiplyByMonomial(ecBytes, 1);
            var  remainder:GF256Poly = info.divide(generator)[1];
            var coefficients:Array = remainder.getCoefficients();
            var numZeroCoefficients:int = ecBytes - coefficients.length;
            for (var i:int = 0; i < numZeroCoefficients; i++) {
              toEncode[dataBytes + i] = 0;
            }
            //System.Array.Copy(coefficients, 0, toEncode, dataBytes + numZeroCoefficients, coefficients.length);
            for(var jj:int=0;jj < coefficients.length;jj++)
            {
            	toEncode[dataBytes + numZeroCoefficients + jj] = coefficients[jj]; 
            }
            
          }
    
    }
}