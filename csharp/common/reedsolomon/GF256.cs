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
using System;
namespace com.google.zxing.common.reedsolomon
{
	
	/// <summary> <p>This class contains utility methods for performing mathematical operations over
	/// the Galois Field GF(256). Operations use a given primitive polynomial in calculations.</p>
	/// 
	/// <p>Throughout this package, elements of GF(256) are represented as an <code>int</code>
	/// for convenience and speed (but at the cost of memory).
	/// Only the bottom 8 bits are really used.</p>
	/// 
	/// </summary>
	/// <author>  Sean Owen
	/// </author>
	/// <author>www.Redivivus.in (suraj.supekar@redivivus.in) - Ported from ZXING Java Source 
	/// </author>
	public sealed class GF256
	{
		internal GF256Poly Zero
		{
			get
			{
				return zero;
			}
			
		}
		internal GF256Poly One
		{
			get
			{
				return one;
			}
			
		}
		
		//UPGRADE_NOTE: Final was removed from the declaration of 'QR_CODE_FIELD '. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
		public static readonly GF256 QR_CODE_FIELD = new GF256(0x011D); // x^8 + x^4 + x^3 + x^2 + 1
		//UPGRADE_NOTE: Final was removed from the declaration of 'DATA_MATRIX_FIELD '. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
		public static readonly GF256 DATA_MATRIX_FIELD = new GF256(0x012D); // x^8 + x^5 + x^3 + x^2 + 1
		
		//UPGRADE_NOTE: Final was removed from the declaration of 'expTable '. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
		private int[] expTable;
		//UPGRADE_NOTE: Final was removed from the declaration of 'logTable '. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
		private int[] logTable;
		//UPGRADE_NOTE: Final was removed from the declaration of 'zero '. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
		private GF256Poly zero;
		//UPGRADE_NOTE: Final was removed from the declaration of 'one '. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
		private GF256Poly one;
		
		/// <summary> Create a representation of GF(256) using the given primitive polynomial.
		/// 
		/// </summary>
		/// <param name="primitive">irreducible polynomial whose coefficients are represented by
		/// the bits of an int, where the least-significant bit represents the constant
		/// coefficient
		/// </param>
		private GF256(int primitive)
		{
			expTable = new int[256];
			logTable = new int[256];
			int x = 1;
			for (int i = 0; i < 256; i++)
			{
				expTable[i] = x;
				x <<= 1; // x = x * 2; we're assuming the generator alpha is 2
				if (x >= 0x100)
				{
					x ^= primitive;
				}
			}
			for (int i = 0; i < 255; i++)
			{
				logTable[expTable[i]] = i;
			}
			// logTable[0] == 0 but this should never be used
			zero = new GF256Poly(this, new int[]{0});
			one = new GF256Poly(this, new int[]{1});
		}
		
		/// <returns> the monomial representing coefficient * x^degree
		/// </returns>
		internal GF256Poly buildMonomial(int degree, int coefficient)
		{
			if (degree < 0)
			{
				throw new System.ArgumentException();
			}
			if (coefficient == 0)
			{
				return zero;
			}
			int[] coefficients = new int[degree + 1];
			coefficients[0] = coefficient;
			return new GF256Poly(this, coefficients);
		}
		
		/// <summary> Implements both addition and subtraction -- they are the same in GF(256).
		/// 
		/// </summary>
		/// <returns> sum/difference of a and b
		/// </returns>
		internal static int addOrSubtract(int a, int b)
		{
			return a ^ b;
		}
		
		/// <returns> 2 to the power of a in GF(256)
		/// </returns>
		internal int exp(int a)
		{
			return expTable[a];
		}
		
		/// <returns> base 2 log of a in GF(256)
		/// </returns>
		internal int log(int a)
		{
			if (a == 0)
			{
				throw new System.ArgumentException();
			}
			return logTable[a];
		}
		
		/// <returns> multiplicative inverse of a
		/// </returns>
		internal int inverse(int a)
		{
			if (a == 0)
			{
				throw new System.ArithmeticException();
			}
			return expTable[255 - logTable[a]];
		}
		
		/// <param name="a">
		/// </param>
		/// <param name="b">
		/// </param>
		/// <returns> product of a and b in GF(256)
		/// </returns>
		internal int multiply(int a, int b)
		{
			if (a == 0 || b == 0)
			{
				return 0;
			}
			if (a == 1)
			{
				return b;
			}
			if (b == 1)
			{
				return a;
			}
			return expTable[(logTable[a] + logTable[b]) % 255];
		}
	}
}