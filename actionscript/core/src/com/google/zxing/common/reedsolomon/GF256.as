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
 * <p>This class contains utility methods for performing mathematical operations over
 * the Galois Field GF(256). Operations use a given primitive polynomial in calculations.</p>
 *
 * <p>Throughout this package, elements of GF(256) are represented as an <code>int</code>
 * for convenience and speed (but at the cost of memory).
 * Only the bottom 8 bits are really used.</p>
 *
 * @author Sean Owen
 */
    public class GF256
    { 
    	import com.google.zxing.common.flexdatatypes.IllegalArgumentException;
    	
          public static var  QR_CODE_FIELD:GF256 = new GF256(0x011D); // x^8 + x^4 + x^3 + x^2 + 1
          public static var DATA_MATRIX_FIELD:GF256 = new GF256(0x012D); // x^8 + x^5 + x^3 + x^2 + 1

          public var expTable:Array;
          public var logTable:Array;
          private var zero:GF256Poly;
          private var one:GF256Poly;

          /**
           * Create a representation of GF(256) using the given primitive polynomial.
           *
           * @param primitive irreducible polynomial whose coefficients are represented by
           *  the bits of an int, where the least-significant bit represents the constant
           *  coefficient
           */
          public function GF256(primitive:int) {
            expTable = new Array(256);
            logTable = new Array(256);
            var x:int = 1;
            for (var i:int = 0; i < 256; i++) {
              expTable[i] = x;
              x <<= 1; // x = x * 2; we're assuming the generator alpha is 2
              if (x >= 0x100) {
                x ^= primitive;
              }
            }
            for (var i2:int = 0; i2 < 255; i2++) {
              logTable[expTable[i2]] = i2;
            }
            // logTable[0] == 0 but this should never be used
            zero = new GF256Poly(this, [0]);
            one = new GF256Poly(this, [1]);
          }

          public function getZero():GF256Poly {
            return zero;
          }

          public function getOne():GF256Poly
          {
            return one;
          }

          /**
           * @return the monomial representing coefficient * x^degree
           */
          public function buildMonomial(degree:int, coefficient:int):GF256Poly
          {
            if (degree < 0) {
              throw new IllegalArgumentException("common : reedsolomon : gf256 : buildnominal");
            }
            if (coefficient == 0) {
              return zero;
            }
            var  coefficients:Array = new Array(degree + 1);
            coefficients[0] = coefficient;
            return new GF256Poly(this, coefficients);
          }

          /**
           * Implements both addition and subtraction -- they are the same in GF(256).
           *
           * @return sum/difference of a and b
           */
          public static function addOrSubtract(a:int, b:int):int {
            return a ^ b;
          }

          /**
           * @return 2 to the power of a in GF(256)
           */
		          public function exp(a:int):int
          {
            return expTable[a];
          }

          /**
           * @return base 2 log of a in GF(256)
           */
          public function log(a:int):int
          {
            if (a == 0) {
              throw new IllegalArgumentException("common : reedsolomon : gf256 : log : a == 0");
            }
            return logTable[a];
          }

          /**
           * @return multiplicative inverse of a
           */
          public function inverse(a:int):int
          {
            if (a == 0) {
              throw new IllegalArgumentException("GF256:inverse: a cannot be 0");
            }
            return expTable[255 - logTable[a]];
          }

          /**
           * @param a
           * @param b
           * @return product of a and b in GF(256)
           */
          public function multiply(a:int, b:int):int
          {
            if (a == 0 || b == 0) {
              return 0;
            }
            if (a == 1) {
              return b;
            }
            if (b == 1) {
              return a;
            }
            return expTable[(logTable[a] + logTable[b]) % 255];
          }

		
		public function Equals(other:GF256):Boolean
		{
			if (expTable != other.expTable) { return false; }
          	if (logTable != other.logTable) { return false; }
          	if (zero != other.getZero()) { return false; }
          	if (one != other.getOne()) { return false; }
          	return true;
		}  
		
    
    }

}