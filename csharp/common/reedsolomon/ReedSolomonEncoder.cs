/*
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

using System;
using System.Collections;

namespace com.google.zxing.common.reedsolomon
{
    public sealed class ReedSolomonEncoder
    { 
          private GF256 Field;
          private ArrayList  cachedGenerators;

          public ReedSolomonEncoder(GF256 field) {
            if (!GF256.QR_CODE_FIELD.Equals(field)) {
              throw new ArgumentException("Only QR Code is supported at this time");
            }
            this.Field = field;
            this.cachedGenerators = new ArrayList();
            cachedGenerators.Add(new GF256Poly(field, new int[] { 1 }));
          }

          private GF256Poly buildGenerator(int degree) {
            if (degree >= cachedGenerators.Count) {
                GF256Poly lastGenerator = (GF256Poly)cachedGenerators[(cachedGenerators.Count - 1)];
              for (int d = cachedGenerators.Count; d <= degree; d++)
              {
                  GF256Poly nextGenerator = lastGenerator.multiply(new GF256Poly(Field, new int[] { 1, Field.exp(d - 1) }));
                cachedGenerators.Add(nextGenerator);
                lastGenerator = nextGenerator;
              }
            }
            return (GF256Poly) cachedGenerators[(degree)];    
          }

          public void encode(int[] toEncode, int ecBytes) {
            if (ecBytes == 0) {
              throw new ArgumentException("No error correction bytes");
            }
            int dataBytes = toEncode.Length - ecBytes;
            if (dataBytes <= 0) {
              throw new ArgumentException("No data bytes provided");
            }
            GF256Poly generator = buildGenerator(ecBytes);
            int[] infoCoefficients = new int[dataBytes];
            System.Array.Copy(toEncode, 0, infoCoefficients, 0, dataBytes);
            GF256Poly info = new GF256Poly(this.Field, infoCoefficients);
            info = info.multiplyByMonomial(ecBytes, 1);
            GF256Poly remainder = info.divide(generator)[1];
            int[] coefficients = remainder.getCoefficients();
            int numZeroCoefficients = ecBytes - coefficients.Length;
            for (int i = 0; i < numZeroCoefficients; i++) {
              toEncode[dataBytes + i] = 0;
            }
            System.Array.Copy(coefficients, 0, toEncode, dataBytes + numZeroCoefficients, coefficients.Length);
          }
    
    }
}