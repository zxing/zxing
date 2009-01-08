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
using System.Text;
namespace com.google.zxing.common.reedsolomon
{

    /// <summary> <p>Represents a polynomial whose coefficients are elements of GF(256).
    /// Instances of this class are immutable.</p>
    /// 
    /// <p>Much credit is due to William Rucklidge since portions of this code are an indirect
    /// port of his C++ Reed-Solomon implementation.</p>
    /// 
    /// </summary>
    /// <author>  srowen@google.com (Sean Owen)
    /// </author>
    public sealed class GF256Poly
    { 
          private GF256 field;
          private int[] coefficients;

          /**
           * @param field the {@link GF256} instance representing the field to use
           * to perform computations
           * @param coefficients coefficients as ints representing elements of GF(256), arranged
           * from most significant (highest-power term) coefficient to least significant
           * @throws ArgumentException if argument is null or empty,
           * or if leading coefficient is 0 and this is not a
           * constant polynomial (that is, it is not the monomial "0")
           */
          public GF256Poly(GF256 field, int[] coefficients) {
            if (coefficients == null || coefficients.Length == 0) {
              throw new ArgumentException();
            }
            this.field = field;
            int coefficientsLength = coefficients.Length;
            if (coefficientsLength > 1 && coefficients[0] == 0) {
              // Leading term must be non-zero for anything except the constant polynomial "0"
              int firstNonZero = 1;
              while (firstNonZero < coefficientsLength && coefficients[firstNonZero] == 0) {
                firstNonZero++;
              }
              if (firstNonZero == coefficientsLength) {
                this.coefficients = field.getZero().coefficients;
              } else {
                this.coefficients = new int[coefficientsLength - firstNonZero];
                System.Array.Copy(coefficients,firstNonZero,this.coefficients,0,this.coefficients.Length);
              }
            } else {
              this.coefficients = coefficients;
            }
          }

          public int[] getCoefficients()
          {
            return coefficients;
          }

          /**
           * @return degree of this polynomial
           */
          public int getDegree()
          {
            return coefficients.Length - 1;
          }

          /**
           * @return true iff this polynomial is the monomial "0"
           */
          public bool isZero()
          {
            return coefficients[0] == 0;
          }

          /**
           * @return coefficient of x^degree term in this polynomial
           */
          public int getCoefficient(int degree)
          {
            return coefficients[coefficients.Length - 1 - degree];
          }

          /**
           * @return evaluation of this polynomial at a given point
           */
          public int evaluateAt(int a)
          {
            if (a == 0) {
              // Just return the x^0 coefficient
              return getCoefficient(0);
            }
            int size = coefficients.Length;
            int result = 0;

            if (a == 1) {
              // Just the sum of the coefficients
              result = 0;
              for (int i = 0; i < size; i++) {
                result = GF256.addOrSubtract(result, coefficients[i]);
              }
              return result;
            }

            result = coefficients[0];
            for (int i = 1; i < size; i++) {
              result = GF256.addOrSubtract(field.multiply(a, result), coefficients[i]);
            }
            return result;
          }

          public GF256Poly addOrSubtract(GF256Poly other)
          {
            if (!field.Equals(other.field)) {
              throw new ArgumentException("GF256Polys do not have same GF256 field");
            }
            if (isZero()) {
              return other;
            }
            if (other.isZero()) {
              return this;
            }

            int[] smallerCoefficients = this.coefficients;
            int[] largerCoefficients = other.coefficients;
            if (smallerCoefficients.Length > largerCoefficients.Length) {
              int[] temp = smallerCoefficients;
              smallerCoefficients = largerCoefficients;
              largerCoefficients = temp;
            }
            int[] sumDiff = new int[largerCoefficients.Length];
            int lengthDiff = largerCoefficients.Length - smallerCoefficients.Length;
            // Copy high-order terms only found in higher-degree polynomial's coefficients
            System.Array.Copy(largerCoefficients, 0, sumDiff, 0, lengthDiff);

            for (int i = lengthDiff; i < largerCoefficients.Length; i++) {
              sumDiff[i] = GF256.addOrSubtract(smallerCoefficients[i - lengthDiff], largerCoefficients[i]);
            }

            return new GF256Poly(field, sumDiff);
          }

          public GF256Poly multiply(GF256Poly other)
          {
            if (!field.Equals(other.field)) {
              throw new ArgumentException("GF256Polys do not have same GF256 field");
            }
            if (isZero() || other.isZero()) {
              return field.getZero();
            }
            int[] aCoefficients = this.coefficients;
            int aLength = aCoefficients.Length;
            int[] bCoefficients = other.coefficients;
            int bLength = bCoefficients.Length;
            int[] product = new int[aLength + bLength - 1];
            for (int i = 0; i < aLength; i++) {
              int aCoeff = aCoefficients[i];
              for (int j = 0; j < bLength; j++) {
                product[i + j] = GF256.addOrSubtract(product[i + j],
                    field.multiply(aCoeff, bCoefficients[j]));
              }
            }
            return new GF256Poly(field, product);
          }

          public GF256Poly multiply(int scalar)
          {
            if (scalar == 0) {
              return field.getZero();
            }
            if (scalar == 1) {
              return this;
            }
            int size = coefficients.Length;
            int[] product = new int[size];
            for (int i = 0; i < size; i++) {
              product[i] = field.multiply(coefficients[i], scalar);
            }
            return new GF256Poly(field, product);
          }

          public GF256Poly multiplyByMonomial(int degree, int coefficient)
          {
            if (degree < 0) {
              throw new ArgumentException();
            }
            if (coefficient == 0) {
              return field.getZero();
            }
            int size = coefficients.Length;
            int[] product = new int[size + degree];
            for (int i = 0; i < size; i++) {
              product[i] = field.multiply(coefficients[i], coefficient);
            }
            return new GF256Poly(field, product);
          }

          public GF256Poly[] divide(GF256Poly other)
          {
            if (!field.Equals(other.field)) {
              throw new ArgumentException("GF256Polys do not have same GF256 field");
            }
            if (other.isZero()) {
              throw new ArgumentException("Divide by 0");
            }

            GF256Poly quotient = field.getZero();
            GF256Poly remainder = this;

            int denominatorLeadingTerm = other.getCoefficient(other.getDegree());
            int inverseDenominatorLeadingTerm = field.inverse(denominatorLeadingTerm);

            while (remainder.getDegree() >= other.getDegree() && !remainder.isZero()) {
              int degreeDifference = remainder.getDegree() - other.getDegree();
              int scale = field.multiply(remainder.getCoefficient(remainder.getDegree()), inverseDenominatorLeadingTerm);
              GF256Poly term = other.multiplyByMonomial(degreeDifference, scale);
              GF256Poly iterationQuotient = field.buildMonomial(degreeDifference, scale);
              quotient = quotient.addOrSubtract(iterationQuotient);
              remainder = remainder.addOrSubtract(term);
            }

            return new GF256Poly[] { quotient, remainder };
          }

          public String toString() {
              StringBuilder result = new StringBuilder(8 * getDegree());
            for (int degree = getDegree(); degree >= 0; degree--) {
              int coefficient = getCoefficient(degree);
              if (coefficient != 0) {
                if (coefficient < 0) {
                  result.Append(" - ");
                  coefficient = -coefficient;
                } else {
                  if (result.Length > 0) {
                    result.Append(" + ");
                  }
                }
                if (degree == 0 || coefficient != 1) {
                  int alphaPower = field.log(coefficient);
                  if (alphaPower == 0) {
                    result.Append('1');
                  } else if (alphaPower == 1) {
                    result.Append('a');
                  } else {
                    result.Append("a^");
                    result.Append(alphaPower);
                  }
                }
                if (degree != 0) {
                  if (degree == 1) {
                    result.Append('x');
                  } else {
                    result.Append("x^");
                    result.Append(degree);
                  }
                }
              }
            }
            return result.ToString();
          }
        
    }
}