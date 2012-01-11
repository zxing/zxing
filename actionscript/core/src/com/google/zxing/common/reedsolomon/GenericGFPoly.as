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

import com.google.zxing.common.flexdatatypes.IllegalArgumentException;
import com.google.zxing.common.flexdatatypes.StringBuilder;
import com.google.zxing.common.flexdatatypes.Utils;
/**
 * <p>Represents a polynomial whose coefficients are elements of a GF.
 * Instances of this class are immutable.</p>
 *
 * <p>Much credit is due to William Rucklidge since portions of this code are an indirect
 * port of his C++ Reed-Solomon implementation.</p>
 *
 * @author Sean Owen
 */
public class GenericGFPoly {

  public var field:GenericGF;
  public var coefficients:Array;

  /**
   * @param field the {@link GenericGF} instance representing the field to use
   * to perform computations
   * @param coefficients coefficients as ints representing elements of GF(size), arranged
   * from most significant (highest-power term) coefficient to least significant
   * @throws IllegalArgumentException if argument is null or empty,
   * or if leading coefficient is 0 and this is not a
   * constant polynomial (that is, it is not the monomial "0")
   */
  public function GenericGFPoly(field:GenericGF, coefficients:Array) {
    if (coefficients == null || coefficients.length == 0) {
      throw new IllegalArgumentException();
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
        Utils.arraycopy(coefficients,firstNonZero,this.coefficients,0,this.coefficients.length);
      }
    } else 
	{
      this.coefficients = coefficients;
    }
  }

  public function getCoefficients():Array {
    return coefficients;
  }

  /**
   * @return degree of this polynomial
   */
  public function getDegree():int {
    return coefficients.length - 1;
  }

  /**
   * @return true iff this polynomial is the monomial "0"
   */
  public function isZero():Boolean {
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
  public function evaluateAt(a:int):int {
    if (a == 0) {
      // Just return the x^0 coefficient
      return getCoefficient(0);
    }
    var size:int = coefficients.length;
    if (a == 1) {
      // Just the sum of the coefficients
      var result:int = 0;
      for (var i:int = 0; i < size; i++) {
        result = GenericGF.addOrSubtract(result, coefficients[i]);
      }
      return result;
    }
    result = coefficients[0];
    for (i = 1; i < size; i++) {
      result = GenericGF.addOrSubtract(field.multiply(a, result), coefficients[i]);
    }
    return result;
  }

  public function addOrSubtract(other:GenericGFPoly):GenericGFPoly {
    if (!field.Equals(other.field)) {
      throw new IllegalArgumentException("GenericGFPolys do not have same GenericGF field");
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
    Utils.arraycopy(largerCoefficients, 0, sumDiff, 0, lengthDiff);

    for (var i:int = lengthDiff; i < largerCoefficients.length; i++) {
      sumDiff[i] = GenericGF.addOrSubtract(smallerCoefficients[i - lengthDiff], largerCoefficients[i]);
    }

    return new GenericGFPoly(field, sumDiff);
  }

  public function multiply(other:*):GenericGFPoly 
  {
  	if (other is int) { return this.multiply_scalar(other as int);}
  	other = (other as GenericGFPoly);
    if (!field.Equals(other.field)) {
      throw new IllegalArgumentException("GenericGFPolys do not have same GenericGF field");
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
        product[i + j] = GenericGF.addOrSubtract(product[i + j],
            field.multiply(aCoeff, bCoefficients[j]));
      }
    }
    return new GenericGFPoly(field, product);
  }

  public function multiply_scalar(scalar:int):GenericGFPoly {
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
    return new GenericGFPoly(field, product);
  }

  public function multiplyByMonomial(degree:int, coefficient:int):GenericGFPoly {
    if (degree < 0) {
      throw new IllegalArgumentException();
    }
    if (coefficient == 0) {
      return field.getZero();
    }
    var size:int = coefficients.length;
    var product:Array = new Array(size + degree);
    for (var i:int = 0; i < size; i++) 
	{
      product[i] = field.multiply(coefficients[i], coefficient);
    }
    return new GenericGFPoly(field, product);
  }

  public function divide(other:GenericGFPoly):Array 
  {
    if (!field.Equals(other.field)) 
	{
      throw new IllegalArgumentException("GenericGFPolys do not have same GenericGF field");
    }
    if (other.isZero()) 
	{
      throw new IllegalArgumentException("Divide by 0");
    }

    var quotient:GenericGFPoly = field.getZero();
    var remainder:GenericGFPoly  = this;

    var denominatorLeadingTerm:int = other.getCoefficient(other.getDegree());
    var inverseDenominatorLeadingTerm:int = field.inverse(denominatorLeadingTerm);

    while (remainder.getDegree() >= other.getDegree() && !remainder.isZero()) {
      var degreeDifference:int = remainder.getDegree() - other.getDegree();
      var scale:int = field.multiply(remainder.getCoefficient(remainder.getDegree()), inverseDenominatorLeadingTerm);
      var term:GenericGFPoly = other.multiplyByMonomial(degreeDifference, scale);
      var iterationQuotient:GenericGFPoly = field.buildMonomial(degreeDifference, scale);
      quotient = quotient.addOrSubtract(iterationQuotient);
      remainder = remainder.addOrSubtract(term);
    }

    return [ quotient, remainder ];
  }

  public function toString():String 
  {
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
    return result.toString();
  }

	public function Equals(other:GenericGFPoly):Boolean
	{
		
		if (this.field == other.field)
		{
			if (this.coefficients.length == other.coefficients.length)
			{
				for (var i:int=0;i<this.coefficients.length;i++)
				{
					if (other.coefficients.indexOf(this.coefficients[i]) == -1) 
					{ 
					   return false; 
					}
				}
				return true;
			}
		}
		
		return false;
		
	}
}
}
