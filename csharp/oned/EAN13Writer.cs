using System;
using System.Collections.Generic;

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

namespace com.google.zxing.oned
{

	using BarcodeFormat = com.google.zxing.BarcodeFormat;
	using EncodeHintType = com.google.zxing.EncodeHintType;
	using FormatException = com.google.zxing.FormatException;
	using WriterException = com.google.zxing.WriterException;
	using BitMatrix = com.google.zxing.common.BitMatrix;


	/// <summary>
	/// This object renders an EAN13 code as a <seealso cref="BitMatrix"/>.
	/// 
	/// @author aripollak@gmail.com (Ari Pollak)
	/// </summary>
	public sealed class EAN13Writer : UPCEANWriter
	{

	  private const int CODE_WIDTH = 3 + (7 * 6) + 5 + (7 * 6) + 3; // end guard -  right bars -  middle guard -  left bars -  start guard

//JAVA TO C# CONVERTER WARNING: Method 'throws' clauses are not available in .NET:
//ORIGINAL LINE: public com.google.zxing.common.BitMatrix encode(String contents, com.google.zxing.BarcodeFormat format, int width, int height, java.util.Map<com.google.zxing.EncodeHintType,?> hints) throws com.google.zxing.WriterException
      public override BitMatrix encode(string contents, BarcodeFormat format, int width, int height, IDictionary<EncodeHintType, object> hints)
	  {
		if (format != BarcodeFormat.EAN_13)
		{
		  throw new System.ArgumentException("Can only encode EAN_13, but got " + format);
		}

		return base.encode(contents, format, width, height, hints);
	  }

	  public override bool[] encode(string contents)
	  {
		if (contents.Length != 13)
		{
		  throw new System.ArgumentException("Requested contents should be 13 digits long, but got " + contents.Length);
		}
		try
		{
		  if (!UPCEANReader.checkStandardUPCEANChecksum(contents))
		  {
			throw new System.ArgumentException("Contents do not pass checksum");
		  }
		}
		catch (FormatException fe)
		{
		  throw new System.ArgumentException("Illegal contents");
		}

		int firstDigit = Convert.ToInt32(contents.Substring(0, 1));
		int parities = EAN13Reader.FIRST_DIGIT_ENCODINGS[firstDigit];
		bool[] result = new bool[CODE_WIDTH];
		int pos = 0;

		pos += appendPattern(result, pos, UPCEANReader.START_END_PATTERN, true);

		// See {@link #EAN13Reader} for a description of how the first digit & left bars are encoded
		for (int i = 1; i <= 6; i++)
		{
		  int digit = Convert.ToInt32(contents.Substring(i, 1));
		  if ((parities >> (6 - i) & 1) == 1)
		  {
			digit += 10;
		  }
		  pos += appendPattern(result, pos, UPCEANReader.L_AND_G_PATTERNS[digit], false);
		}

		pos += appendPattern(result, pos, UPCEANReader.MIDDLE_PATTERN, false);

		for (int i = 7; i <= 12; i++)
		{
		  int digit = Convert.ToInt32(contents.Substring(i, 1));
		  pos += appendPattern(result, pos, UPCEANReader.L_PATTERNS[digit], true);
		}
		pos += appendPattern(result, pos, UPCEANReader.START_END_PATTERN, true);

		return result;
	  }

	}

}