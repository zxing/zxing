using System.Collections.Generic;

/*
 * Copyright 2010 ZXing authors
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
	using WriterException = com.google.zxing.WriterException;
	using BitMatrix = com.google.zxing.common.BitMatrix;


	/// <summary>
	/// This object renders a CODE39 code as a <seealso cref="BitMatrix"/>.
	/// 
	/// @author erik.barbara@gmail.com (Erik Barbara)
	/// </summary>
	public sealed class Code39Writer : OneDimensionalCodeWriter
	{

//JAVA TO C# CONVERTER WARNING: Method 'throws' clauses are not available in .NET:
//ORIGINAL LINE: public com.google.zxing.common.BitMatrix encode(String contents, com.google.zxing.BarcodeFormat format, int width, int height, java.util.Map<com.google.zxing.EncodeHintType,?> hints) throws com.google.zxing.WriterException
	  public override BitMatrix encode(string contents, BarcodeFormat format, int width, int height, IDictionary<EncodeHintType,object> hints)
	  {
		if (format != BarcodeFormat.CODE_39)
		{
		  throw new System.ArgumentException("Can only encode CODE_39, but got " + format);
		}
		return base.encode(contents, format, width, height, hints);
	  }

	  public override bool[] encode(string contents)
	  {
		int length = contents.Length;
		if (length > 80)
		{
		  throw new System.ArgumentException("Requested contents should be less than 80 digits long, but got " + length);
		}

		int[] widths = new int[9];
		int codeWidth = 24 + 1 + length;
		for (int i = 0; i < length; i++)
		{
		  int indexInString = Code39Reader.ALPHABET_STRING.IndexOf(contents[i]);
		  toIntArray(Code39Reader.CHARACTER_ENCODINGS[indexInString], widths);
		  foreach (int width in widths)
		  {
			codeWidth += width;
		  }
		}
		bool[] result = new bool[codeWidth];
		toIntArray(Code39Reader.CHARACTER_ENCODINGS[39], widths);
		int pos = appendPattern(result, 0, widths, true);
		int[] narrowWhite = {1};
		pos += appendPattern(result, pos, narrowWhite, false);
		//append next character to bytematrix
		for (int i = length - 1; i >= 0; i--)
		{
		  int indexInString = Code39Reader.ALPHABET_STRING.IndexOf(contents[i]);
		  toIntArray(Code39Reader.CHARACTER_ENCODINGS[indexInString], widths);
		  pos += appendPattern(result, pos, widths, true);
		  pos += appendPattern(result, pos, narrowWhite, false);
		}
		toIntArray(Code39Reader.CHARACTER_ENCODINGS[39], widths);
		pos += appendPattern(result, pos, widths, true);
		return result;
	  }

	  private static void toIntArray(int a, int[] toReturn)
	  {
		for (int i = 0; i < 9; i++)
		{
		  int temp = a & (1 << i);
		  toReturn[i] = temp == 0 ? 1 : 2;
		}
	  }

	}
}