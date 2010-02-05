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
using System;
namespace com.google.zxing.common.reedsolomon
{
	
	/// <summary> <p>Implements Reed-Solomon enbcoding, as the name implies.</p>
	/// 
	/// </summary>
	/// <author>  Sean Owen
	/// </author>
	/// <author>  William Rucklidge
	/// </author>
	/// <author>www.Redivivus.in (suraj.supekar@redivivus.in) - Ported from ZXING Java Source 
	/// </author>
	public sealed class ReedSolomonEncoder
	{
		
		//UPGRADE_NOTE: Final was removed from the declaration of 'field '. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
		private GF256 field;
		//UPGRADE_NOTE: Final was removed from the declaration of 'cachedGenerators '. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
		private System.Collections.ArrayList cachedGenerators;
		
		public ReedSolomonEncoder(GF256 field)
		{
			if (!GF256.QR_CODE_FIELD.Equals(field))
			{
				throw new System.ArgumentException("Only QR Code is supported at this time");
			}
			this.field = field;
			this.cachedGenerators = System.Collections.ArrayList.Synchronized(new System.Collections.ArrayList(10));
			cachedGenerators.Add(new GF256Poly(field, new int[]{1}));
		}
		
		private GF256Poly buildGenerator(int degree)
		{
			if (degree >= cachedGenerators.Count)
			{
				GF256Poly lastGenerator = (GF256Poly) cachedGenerators[cachedGenerators.Count - 1];
				for (int d = cachedGenerators.Count; d <= degree; d++)
				{
					GF256Poly nextGenerator = lastGenerator.multiply(new GF256Poly(field, new int[]{1, field.exp(d - 1)}));
					cachedGenerators.Add(nextGenerator);
					lastGenerator = nextGenerator;
				}
			}
			return (GF256Poly) cachedGenerators[degree];
		}
		
		public void  encode(int[] toEncode, int ecBytes)
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
			GF256Poly generator = buildGenerator(ecBytes);
			int[] infoCoefficients = new int[dataBytes];
			Array.Copy(toEncode, 0, infoCoefficients, 0, dataBytes);
			GF256Poly info = new GF256Poly(field, infoCoefficients);
			info = info.multiplyByMonomial(ecBytes, 1);
			GF256Poly remainder = info.divide(generator)[1];
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