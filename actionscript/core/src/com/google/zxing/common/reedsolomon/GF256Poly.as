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
package com.google.zxing.common.reedsolomon
{
	    /**
 * <p>Represents a polynomial whose coefficients are elements of GF(256).
 * Instances of this class are immutable.</p>
 *
 * <p>Much credit is due to William Rucklidge since portions of this code are an indirect
 * port of his C++ Reed-Solomon implementation.</p>
 *
 * @author Sean Owen
 */

    public  class GF256Poly
    { 
    	  import com.google.zxing.common.flexdatatypes.StringBuilder;
    	  import com.google.zxing.common.flexdatatypes.IllegalArgumentException;
    	  
          private var field:GF256;
          private var coefficients:Array ;

          /**
           * @param field the {@link GF256} instance representing the field to use
           * to perform computations
           * @param coefficients coefficients as ints representing elements of GF(256), arranged
           * from most significant (highest-power term) coefficient to least significant
           * @throws Error if argument is null or empty,
           * or if leading coefficient is 0 and this is not a
           * constant polynomial (that is, it is not the monomial "0")
           */
          public function GF256Poly( field:GF256, coefficients:Array) {
            if (coefficients == null || coefficients.length == 0) {
              throw new IllegalArgumentException("common : reedsolomon : GFPoly : constructor input parameters invalid");
            }
            this.field = field;
            var coefficientsLength:int = coefficients.length;
            if (coefficientsLength > 1 && coefficients[0] == 0) {
              // Leading term must be non-zero for anything except the constant polynomial "0"
              var firstNonZero:int = 1;
              while (firstNonZero < coefficientsLength && coefficients[firstNonZero] == 0) {
                firstNonZero++;
              }
              if (firstNonZero == coefficientsLength) {
                this.coefficients = field.getZero().coefficients;
              } else {
                this.coefficients = new Array(coefficientsLength - firstNonZero);
                
                //System.Array.Copy(coefficients,firstNonZero,this.coefficients,0,this.coefficients.length);
                var ctr:int=0;
                for (var i:int = firstNonZero;i<coefficients.length;i++)
                {
                	this.coefficients[ctr] = coefficients[i];
                	ctr++;
                }
                
                
              }
            } else {
              this.coefficients = coefficients;
            }
          }

          public function getCoefficients():Array
          {
            return coefficients;
          }

          /**
           * @return degree of this polynomial
           */
          public function getDegree():int
          {
            return coefficients.length - 1;
          }

          /**
           * @return true iff this polynomial is the monomial "0"
           */
          public function isZero():Boolean
          {
            return coefficients[0] == 0;
          }

          /**
           * @return coefficient of x^degree term in this polynomial
           */
          public function getCoefficient(degree:int):int
          {
            return coefficients[coefficients.length - 1 - degree];
          }

          /**
           * @return evaluation of this polynomial at a given point
           */
          public function evaluateAt(a:int):int
          {
            if (a == 0) {
              // Just return the x^0 coefficient
              return getCoefficient(0);
            }
            var size:int = coefficients.length;
            var result:int = 0;

            if (a == 1) {
              // Just the sum of the coefficients
              result = 0;
              for (var i2:int = 0; i2 < size; i2++) {
                result = GF256.addOrSubtract(result, coefficients[i2]);
              }
              return result;
            }

            result = coefficients[0];
            for (var i:int = 1; i < size; i++) {
              result = GF256.addOrSubtract(field.multiply(a, result), coefficients[i]);
            }
            return result;
          }

          public function addOrSubtract(other:GF256Poly):GF256Poly
          {
            if (field != other.field) {
              throw new IllegalArgumentException("common : reedsolomon : GFPoly : GF256Polys do not have same GF256 field");
            }
            if (isZero()) {
              return other;
            }
            if (other.isZero()) {
              return this;
            }

            var smallerCoefficients:Array = this.coefficients;
            var largerCoefficients:Array = other.coefficients;
            if (smallerCoefficients.length > largerCoefficients.length) {
              var temp:Array = smallerCoefficients;
              smallerCoefficients = largerCoefficients;
              largerCoefficients = temp;
            }
            var sumDiff:Array = new Array(largerCoefficients.length);
            var lengthDiff:int = largerCoefficients.length - smallerCoefficients.length;
            // Copy high-order terms only found in higher-degree polynomial's coefficients
            //System.Array.Copy(largerCoefficients, 0, sumDiff, 0, lengthDiff);
            for (var ii:int=0;ii<lengthDiff;ii++)
            {
            	sumDiff[ii] = largerCoefficients[ii];
            }

            for (var i:int = lengthDiff; i < largerCoefficients.length; i++) {
              sumDiff[i] = GF256.addOrSubtract(smallerCoefficients[i - lengthDiff], largerCoefficients[i]);
            }

            return new GF256Poly(field, sumDiff);
          }

          
          public function multiply(other:Object):GF256Poly
          {
          	if (other is GF256Poly) { return multiply_GF256Poly(other as GF256Poly);}
          	else if (other is int) { return multiply_int(other as int);}
          	else { new IllegalArgumentException('common : reedsolomon : GFPoly : GF256Poly : multiply : unknown type of other'); }
          	return null;
          }
          
          public function multiply_GF256Poly(other:GF256Poly ):GF256Poly
          {
            if (field != other.field) {
              throw new IllegalArgumentException("common : reedsolomon : GFPoly : GF256Polys do not have same GF256 field");
            }
            if (isZero() || other.isZero()) {
              return field.getZero();
            }
            var aCoefficients:Array = this.coefficients;
            var aLength:int = aCoefficients.length;
            var bCoefficients:Array = other.coefficients;
            var bLength:int = bCoefficients.length;
            var product:Array = new Array(aLength + bLength - 1);
            for (var i:int = 0; i < aLength; i++) {
              var aCoeff:int = aCoefficients[i];
              for (var j:int = 0; j < bLength; j++) {
                product[i + j] = GF256.addOrSubtract(product[i + j],
                    field.multiply(aCoeff, bCoefficients[j]));
              }
            }
            return new GF256Poly(field, product);
          }

          public function multiply_int(scalar:int):GF256Poly
          {
            if (scalar == 0) {
              return field.getZero();
            }
            if (scalar == 1) {
              return this;
            }
            var size:int = coefficients.length;
            var product:Array = new Array(size);
            for (var i:int = 0; i < size; i++) {
              product[i] = field.multiply(coefficients[i], scalar);
            }
            return new GF256Poly(field, product);
          }

          public function multiplyByMonomial(degree:int, coefficient:int):GF256Poly
          {
            if (degree < 0) {
              throw new IllegalArgumentException("common : reedsolomon : GFPoly : degree less then 0");
            }
            if (coefficient == 0) {
              return field.getZero();
            }
            var size:int = coefficients.length;
            var product:Array = new Array(size + degree);
            for (var i:int = 0; i < size; i++) {
              product[i] = field.multiply(coefficients[i], coefficient);
            }
            return new GF256Poly(field, product);
          }

          public function divide(other:GF256Poly):Array
          {
            if (field != other.field) {
              throw new IllegalArgumentException("common : reedsolomon : GFPoly : GF256Polys do not have same GF256 field");
            }
            if (other.isZero()) {
              throw new IllegalArgumentException("common : reedsolomon : GFPoly : Divide by 0");
            }

            var quotient:GF256Poly  = field.getZero();
            var remainder:GF256Poly  = this;

            var denominatorLeadingTerm:int = other.getCoefficient(other.getDegree());
            var inverseDenominatorLeadingTerm:int = field.inverse(denominatorLeadingTerm);

            while (remainder.getDegree() >= other.getDegree() && !remainder.isZero()) {
              var degreeDifference:int = remainder.getDegree() - other.getDegree();
              var scale:int = field.multiply(remainder.getCoefficient(remainder.getDegree()), inverseDenominatorLeadingTerm);
              var term:GF256Poly = other.multiplyByMonomial(degreeDifference, scale);
              var iterationQuotient:GF256Poly = field.buildMonomial(degreeDifference, scale);
              quotient = quotient.addOrSubtract(iterationQuotient);
              remainder = remainder.addOrSubtract(term);
            }

            return [quotient, remainder];
          }

          public function toString():String {
              var result:StringBuilder = new StringBuilder(8 * getDegree());
            for (var degree:int = getDegree(); degree >= 0; degree--) {
              var coefficient:int = getCoefficient(degree);
              if (coefficient != 0) {
                if (coefficient < 0) {
                  result.Append(" - ");
                  coefficient = -coefficient;
                } else {
                  if (result.length > 0) {
                    result.Append(" + ");
                  }
                }
                if (degree == 0 || coefficient != 1) {
                  var alphaPower:int = field.log(coefficient);
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
        public function Equals(other:GF256Poly):Boolean
        {
        	var result:Boolean = false;
        	if (this.field == other.field)
        	{
        		if (this.coefficients.Equals(other.coefficients))
        		{
        			result = true;
        		}
        	}          
        	return result;       	
        } 
    }

}