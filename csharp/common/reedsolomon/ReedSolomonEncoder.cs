using System;
using System.Collections.Generic;

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

namespace com.google.zxing.common.reedsolomon
{


	/// <summary>
	/// <p>Implements Reed-Solomon enbcoding, as the name implies.</p>
	/// 
	/// @author Sean Owen
	/// @author William Rucklidge
	/// </summary>
	public sealed class ReedSolomonEncoder
	{

	  private readonly GenericGF field;
	  private readonly IList<GenericGFPoly> cachedGenerators;

	  public ReedSolomonEncoder(GenericGF field)
	  {
		if (!GenericGF.QR_CODE_FIELD_256.Equals(field))
		{
		  throw new System.ArgumentException("Only QR Code is supported at this time");
		}
		this.field = field;
		this.cachedGenerators = new List<GenericGFPoly>();
		cachedGenerators.Add(new GenericGFPoly(field, new int[]{1}));
	  }

	  private GenericGFPoly buildGenerator(int degree)
	  {
		if (degree >= cachedGenerators.Count)
		{
		  GenericGFPoly lastGenerator = cachedGenerators[cachedGenerators.Count - 1];
		  for (int d = cachedGenerators.Count; d <= degree; d++)
		  {
			GenericGFPoly nextGenerator = lastGenerator.multiply(new GenericGFPoly(field, new int[] {1, field.exp(d - 1)}));
			cachedGenerators.Add(nextGenerator);
			lastGenerator = nextGenerator;
		  }
		}
		return cachedGenerators[degree];
	  }

	  public void encode(int[] toEncode, int ecBytes)
	  {
		if (ecBytes == 0)
		{
		  throw new System.ArgumentException("No error correction bytes");
		}
		int dataBytes = toEncode.Length - ecBytes;
		if (dataBytes <= 0)
		{
		  throw new System.ArgumentException("No data bytes provided");
		}
		GenericGFPoly generator = buildGenerator(ecBytes);
		int[] infoCoefficients = new int[dataBytes];
		Array.Copy(toEncode, 0, infoCoefficients, 0, dataBytes);
		GenericGFPoly info = new GenericGFPoly(field, infoCoefficients);
		info = info.multiplyByMonomial(ecBytes, 1);
		GenericGFPoly remainder = info.divide(generator)[1];
		int[] coefficients = remainder.Coefficients;
		int numZeroCoefficients = ecBytes - coefficients.Length;
		for (int i = 0; i < numZeroCoefficients; i++)
		{
		  toEncode[dataBytes + i] = 0;
		}
		Array.Copy(coefficients, 0, toEncode, dataBytes + numZeroCoefficients, coefficients.Length);
	  }

	}

}