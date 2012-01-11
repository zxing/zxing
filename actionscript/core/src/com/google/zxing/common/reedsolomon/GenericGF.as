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

/**
 * <p>This class contains utility methods for performing mathematical operations over
 * the Galois Fields. Operations use a given primitive polynomial in calculations.</p>
 *
 * <p>Throughout this package, elements of the GF are represented as an <code>int</code>
 * for convenience and speed (but at the cost of memory).
 * </p>
 *
 * @author Sean Owen
 * @author David Olivier
 */
public class GenericGF 
{

  public static var  AZTEC_DATA_12:GenericGF = new GenericGF(0x1069, 4096); // x^12 + x^6 + x^5 + x^3 + 1
  public static var  AZTEC_DATA_10:GenericGF = new GenericGF(0x409, 1024); // x^10 + x^3 + 1
  public static var  AZTEC_DATA_6:GenericGF = new GenericGF(0x43, 64); // x^6 + x + 1
  public static var  AZTEC_PARAM:GenericGF = new GenericGF(0x13, 16); // x^4 + x + 1
  public static var  QR_CODE_FIELD_256:GenericGF = new GenericGF(0x011D, 256); // x^8 + x^4 + x^3 + x^2 + 1
  public static var  DATA_MATRIX_FIELD_256:GenericGF = new GenericGF(0x012D, 256); // x^8 + x^5 + x^3 + x^2 + 1
  public static var  AZTEC_DATA_8:GenericGF = DATA_MATRIX_FIELD_256;

  private static var INITIALIZATION_THRESHOLD:int = 0;

  private var expTable:Array;
  private var logTable:Array;
  private var zero:GenericGFPoly;
  private var one:GenericGFPoly;
  public var size:int;
  public var primitive:int;
  private var initialized:Boolean = false;

  /**
   * Create a representation of GF(size) using the given primitive polynomial.
   *
   * @param primitive irreducible polynomial whose coefficients are represented by
   *  the bits of an int, where the least-significant bit represents the constant
   *  coefficient
   */
  public function GenericGF(primitive:int, size:int) {
  	this.primitive = primitive;
    this.size = size;
    
    if (size <= INITIALIZATION_THRESHOLD){
    	initialize();
    }
  }

  private function initialize():void{
    expTable = new Array(size);
    logTable = new Array(size);
    var x:int = 1;
    for (var i:int = 0; i < size; i++) {
      expTable[i] = x;
      x <<= 1; // x = x * 2; we're assuming the generator alpha is 2
      if (x >= size) {
        x ^= primitive;
        x &= size-1;
      }
    }
    for (var ii:int = 0; ii < size-1; ii++) {
      logTable[expTable[ii]] = ii;
    }
    // logTable[0] == 0 but this should never be used
    zero = new GenericGFPoly(this, [0]);
    one = new GenericGFPoly(this, [1]);
    initialized = true;
  }
  
  private function checkInit():void{
  	if (!initialized) {
      initialize();
    }
  }
  
  public function getZero():GenericGFPoly {
  	checkInit();
  	
    return zero;
  }

  public function getOne():GenericGFPoly {
  	checkInit();
  	
    return one;
  }

  /**
   * @return the monomial representing coefficient * x^degree
   */
  public function buildMonomial(degree:int, coefficient:int):GenericGFPoly {
  	checkInit();
  	
    if (degree < 0) {
      throw new IllegalArgumentException();
    }
    if (coefficient == 0) {
      return zero;
    }
    var coefficients:Array = new Array(degree + 1);
    coefficients[0] = coefficient;
    return new GenericGFPoly(this, coefficients);
  }

  /**
   * Implements both addition and subtraction -- they are the same in GF(size).
   *
   * @return sum/difference of a and b
   */
  public static function addOrSubtract(a:int, b:int):int {
    return a ^ b;
  }

  /**
   * @return 2 to the power of a in GF(size)
   */
  public function exp(a:int):int {
  	checkInit();
  	
    return expTable[a];
  }

  /**
   * @return base 2 log of a in GF(size)
   */
  public function log(a:int):int {
  	checkInit();
  	
    if (a == 0) {
      throw new IllegalArgumentException();
    }
    return logTable[a];
  }

  /**
   * @return multiplicative inverse of a
   */
  public function inverse(a:int):int {
  	checkInit();
  	
    if (a == 0) {
      throw new Error("arithmetic exception");
    }
    return expTable[size - logTable[a] - 1];
  }

  /**
   * @param a
   * @param b
   * @return product of a and b in GF(size)
   */
  public function multiply(a:int, b:int):int {
  	checkInit();
  	
    if (a == 0 || b == 0) {
      return 0;
    }
    
    if (a<0 || b <0 || a>=size || b >=size){
    	a++;
    }
    
    var logSum:int = logTable[a] + logTable[b];
    var result:int = expTable[(logSum % size) + Math.floor(logSum / size)];
    return result;
  }

  public function getSize():int{
  	return size;
  }
  
  public function Equals(other:GenericGF):Boolean
  {
  	return ((this.primitive == other.primitive) && (this.size == other.size));
  }
  
}
}