/*
* Copyright 2009 ZXing authors
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
using BarcodeFormat = com.google.zxing.BarcodeFormat;
using WriterException = com.google.zxing.WriterException;
using ByteMatrix = com.google.zxing.common.ByteMatrix;
namespace com.google.zxing.oned
{
	
	
	/// <summary> This object renders an EAN13 code as a ByteMatrix 2D array of greyscale
	/// values.
	/// 
	/// </summary>
	/// <author>  aripollak@gmail.com (Ari Pollak)
	/// </author>
	/// <author>www.Redivivus.in (suraj.supekar@redivivus.in) - Ported from ZXING Java Source 
	/// </author>
	public sealed class EAN13Writer:UPCEANWriter
	{
		
		private const int codeWidth = 3 + (7 * 6) + 5 + (7 * 6) + 3; // end guard
		
		public override ByteMatrix encode(System.String contents, BarcodeFormat format, int width, int height, System.Collections.Hashtable hints)
		{
			if (format != BarcodeFormat.EAN_13)
			{
				throw new System.ArgumentException("Can only encode EAN_13, but got " + format);
			}
			
			return base.encode(contents, format, width, height, hints);
		}
		
		public override sbyte[] encode(System.String contents)
		{
			if (contents.Length != 13)
			{
				throw new System.ArgumentException("Requested contents should be 13 digits long, but got " + contents.Length);
			}
			
			int firstDigit = System.Int32.Parse(contents.Substring(0, (1) - (0)));
			int parities = EAN13Reader.FIRST_DIGIT_ENCODINGS[firstDigit];
			sbyte[] result = new sbyte[codeWidth];
			int pos = 0;
			
			pos += appendPattern(result, pos, UPCEANReader.START_END_PATTERN, 1);
			
			// See {@link #EAN13Reader} for a description of how the first digit & left bars are encoded
			for (int i = 1; i <= 6; i++)
			{
				int digit = System.Int32.Parse(contents.Substring(i, (i + 1) - (i)));
				if ((parities >> (6 - i) & 1) == 1)
				{
					digit += 10;
				}
				pos += appendPattern(result, pos, UPCEANReader.L_AND_G_PATTERNS[digit], 0);
			}
			
			pos += appendPattern(result, pos, UPCEANReader.MIDDLE_PATTERN, 0);
			
			for (int i = 7; i <= 12; i++)
			{
				int digit = System.Int32.Parse(contents.Substring(i, (i + 1) - (i)));
				pos += appendPattern(result, pos, UPCEANReader.L_PATTERNS[digit], 1);
			}
			pos += appendPattern(result, pos, UPCEANReader.START_END_PATTERN, 1);
			
			return result;
		}
	}
}