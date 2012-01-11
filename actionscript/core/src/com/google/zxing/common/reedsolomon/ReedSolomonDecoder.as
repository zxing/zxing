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
 * <p>Implements Reed-Solomon decoding, as the name implies.</p>
 *
 * <p>The algorithm will not be explained here, but the following references were helpful
 * in creating this implementation:</p>
 *
 * <ul>
 * <li>Bruce Maggs.
 * <a href="http://www.cs.cmu.edu/afs/cs.cmu.edu/project/pscico-guyb/realworld/www/rs_decode.ps">
 * "Decoding Reed-Solomon Codes"</a> (see discussion of Forney's Formula)</li>
 * <li>J.I. Hall. <a href="www.mth.msu.edu/~jhall/classes/codenotes/GRS.pdf">
 * "Chapter 5. Generalized Reed-Solomon Codes"</a>
 * (see discussion of Euclidean algorithm)</li>
 * </ul>
 *
 * <p>Much credit is due to William Rucklidge since portions of this code are an indirect
 * port of his C++ Reed-Solomon implementation.</p>
 *
 * @author Sean Owen
 * @author William Rucklidge
 * @author sanfordsquires
 */
public class ReedSolomonDecoder {

  private var  field:GenericGF;

  public function ReedSolomonDecoder(field:GenericGF) 
  {
    this.field = field;
  }

  /**
   * <p>Decodes given set of received codewords, which include both data and error-correction
   * codewords. Really, this means it uses Reed-Solomon to detect and correct errors, in-place,
   * in the input.</p>
   *
   * @param received data and error-correction codewords
   * @param twoS number of error-correction codewords available
   * @throws ReedSolomonException if decoding fails for any reason
   */
  public function decode(received:Array, twoS:int):void 
  {
  	/* debug  */
  	/*received = [66,102,135,71,71,3,162,242,246,118,246,246,118,198,82,230,54,246,210,246,119,119,66,246,227,
                       247,83,214,38,199,86,86,230,150,198,82,230,54,246,208,236,17,236,17,236,17,236,17,236,17,
					   236,17,236,17,236,69,165,146,99,159,55,25,86,244,208,192,209,50,8,174];
    twoS = 15;		
  	*/
  	/* debug */
  	
    var poly:GenericGFPoly = new GenericGFPoly(field, received);
    var syndromeCoefficients:Array = new Array(twoS);
    var dataMatrix:Boolean = field.Equals(GenericGF.DATA_MATRIX_FIELD_256);
    var noError:Boolean = true;
    for (var i:int = 0; i < twoS; i++) {
      // Thanks to sanfordsquires for this fix:
      var eval:int = poly.evaluateAt(field.exp(dataMatrix ? i + 1 : i));
      syndromeCoefficients[syndromeCoefficients.length - 1 - i] = eval;
      if (eval != 0) {
        noError = false;
      }
    }
    if (noError) {
      return;
    }
    var syndrome:GenericGFPoly = new GenericGFPoly(field, syndromeCoefficients);
    var sigmaOmega:Array = runEuclideanAlgorithm(field.buildMonomial(twoS, 1), syndrome, twoS);
    var sigma:GenericGFPoly = sigmaOmega[0];
    var omega:GenericGFPoly = sigmaOmega[1];
    var errorLocations:Array = findErrorLocations(sigma);
    var errorMagnitudes:Array = findErrorMagnitudes(omega, errorLocations, dataMatrix);
    for (i = 0; i < errorLocations.length; i++) {
      var position:int = received.length - 1 - field.log(errorLocations[i]);
      if (position < 0) {
        throw new ReedSolomonException("Bad error location");
      }
      received[position] = GenericGF.addOrSubtract(received[position], errorMagnitudes[i]);
    }
  }

  private function runEuclideanAlgorithm(a:GenericGFPoly, b:GenericGFPoly, R:int):Array
  {
    // Assume a's degree is >= b's
    if (a.getDegree() < b.getDegree()) {
      var temp:GenericGFPoly  = a;
      a = b;
      b = temp;
    }

    var rLast:GenericGFPoly = a;
    var r:GenericGFPoly = b;
    var sLast:GenericGFPoly = field.getOne();
    var s:GenericGFPoly = field.getZero();
    var tLast:GenericGFPoly = field.getZero();
    var t:GenericGFPoly = field.getOne();

    // Run Euclidean algorithm until r's degree is less than R/2
    while (r.getDegree() >= R / 2) {
      var rLastLast:GenericGFPoly = rLast;
      var sLastLast:GenericGFPoly = sLast;
      var tLastLast:GenericGFPoly = tLast;
      rLast = r;
      sLast = s;
      tLast = t;

      // Divide rLastLast by rLast, with quotient in q and remainder in r
      if (rLast.isZero()) 
	  {
        // Oops, Euclidean algorithm already terminated?
        throw new ReedSolomonException("r_{i-1} was zero");
      }
      r = rLastLast;
      var q:GenericGFPoly = field.getZero();
      var denominatorLeadingTerm:int = rLast.getCoefficient(rLast.getDegree());
      var dltInverse:int = field.inverse(denominatorLeadingTerm);
      while (r.getDegree() >= rLast.getDegree() && !r.isZero()) {
        var degreeDiff:int = r.getDegree() - rLast.getDegree();
        var scale:int = field.multiply(r.getCoefficient(r.getDegree()), dltInverse);
        q = q.addOrSubtract(field.buildMonomial(degreeDiff, scale));
        r = r.addOrSubtract(rLast.multiplyByMonomial(degreeDiff, scale));
      }

      s = q.multiply(sLast).addOrSubtract(sLastLast);
      t = q.multiply(tLast).addOrSubtract(tLastLast);
    }

    var sigmaTildeAtZero:int = t.getCoefficient(0);
    if (sigmaTildeAtZero == 0) {
      throw new ReedSolomonException("sigmaTilde(0) was zero");
    }

    var inverse:int = field.inverse(sigmaTildeAtZero);
    var sigma:GenericGFPoly = t.multiply(inverse);
    var omega:GenericGFPoly = r.multiply(inverse);
    return [sigma, omega];
  }

  private function findErrorLocations(errorLocator:GenericGFPoly ) :Array {
    // This is a direct application of Chien's search
    var numErrors:int = errorLocator.getDegree();
    if (numErrors == 1) 
	{ // shortcut
      return [errorLocator.getCoefficient(1) ];
    }
    var result:Array = new Array(numErrors);
    var e:int = 0;
    for (var i:int = 1; i < field.getSize() && e < numErrors; i++) 
	{
      if (errorLocator.evaluateAt(i) == 0) 
	  {
        result[e] = field.inverse(i);
        e++;
      }
    }
    if (e != numErrors) {
      throw new ReedSolomonException("Error locator degree does not match number of roots");
    }
    return result;
  }

  private function findErrorMagnitudes(errorEvaluator:GenericGFPoly, errorLocations:Array, dataMatrix:Boolean):Array {
    // This is directly applying Forney's Formula
    var s:int = errorLocations.length;
    var result:Array = new Array(s);
    for (var i:int = 0; i < s; i++) {
      var xiInverse:int = field.inverse(errorLocations[i]);
      var denominator:int = 1;
      for (var j:int = 0; j < s; j++) {
        if (i != j) 
        {
          //denominator = field.multiply(denominator, GenericGF.addOrSubtract(1, field.multiply(errorLocations[j], xiInverse)));
          // Above should work but fails on some Apple and Linux JDKs due to a Hotspot bug.
          // Below is a funny-looking workaround from Steven Parkes
          var term:int = field.multiply(errorLocations[j], xiInverse);
          var termPlus1:int = (term & 0x1) == 0 ? term | 1 : term & ~1;
          denominator = field.multiply(denominator, termPlus1);
        }
      }
      result[i] = field.multiply(errorEvaluator.evaluateAt(xiInverse),field.inverse(denominator));
      // Thanks to sanfordsquires for this fix:
      if (dataMatrix) 
	  {
        result[i] = field.multiply(result[i], xiInverse);
      }
    }
    return result;
  }

}
}