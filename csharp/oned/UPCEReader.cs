using System.Collections;
using System.Text;

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

namespace com.google.zxing.oned
{

	using BarcodeFormat = com.google.zxing.BarcodeFormat;
	using ChecksumException = com.google.zxing.ChecksumException;
	using FormatException = com.google.zxing.FormatException;
	using NotFoundException = com.google.zxing.NotFoundException;
	using BitArray = com.google.zxing.common.BitArray;

	/// <summary>
	/// <p>Implements decoding of the UPC-E format.</p>
	/// <p/>
	/// <p><a href="http://www.barcodeisland.com/upce.phtml">This</a> is a great reference for
	/// UPC-E information.</p>
	/// 
	/// @author Sean Owen
	/// </summary>
	public sealed class UPCEReader : UPCEANReader
	{

	  /// <summary>
	  /// The pattern that marks the middle, and end, of a UPC-E pattern.
	  /// There is no "second half" to a UPC-E barcode.
	  /// </summary>
	  private static readonly int[] MIDDLE_END_PATTERN = {1, 1, 1, 1, 1, 1};

	  /// <summary>
	  /// See <seealso cref="#L_AND_G_PATTERNS"/>; these values similarly represent patterns of
	  /// even-odd parity encodings of digits that imply both the number system (0 or 1)
	  /// used, and the check digit.
	  /// </summary>
	  private static readonly int[][] NUMSYS_AND_CHECK_DIGIT_PATTERNS = {new int[] {0x38, 0x34, 0x32, 0x31, 0x2C, 0x26, 0x23, 0x2A, 0x29, 0x25}, new int[] {0x07, 0x0B, 0x0D, 0x0E, 0x13, 0x19, 0x1C, 0x15, 0x16, 0x1A}};

	  private readonly int[] decodeMiddleCounters;

	  public UPCEReader()
	  {
		decodeMiddleCounters = new int[4];
	  }

//JAVA TO C# CONVERTER WARNING: Method 'throws' clauses are not available in .NET:
//ORIGINAL LINE: protected int decodeMiddle(com.google.zxing.common.BitArray row, int[] startRange, StringBuilder result) throws com.google.zxing.NotFoundException
	  protected internal override int decodeMiddle(BitArray row, int[] startRange, StringBuilder result)
	  {
		int[] counters = decodeMiddleCounters;
		counters[0] = 0;
		counters[1] = 0;
		counters[2] = 0;
		counters[3] = 0;
		int end = row.Size;
		int rowOffset = startRange[1];

		int lgPatternFound = 0;

		for (int x = 0; x < 6 && rowOffset < end; x++)
		{
		  int bestMatch = decodeDigit(row, counters, rowOffset, L_AND_G_PATTERNS);
		  result.Append((char)('0' + bestMatch % 10));
		  foreach (int counter in counters)
		  {
			rowOffset += counter;
		  }
		  if (bestMatch >= 10)
		  {
			lgPatternFound |= 1 << (5 - x);
		  }
		}

		determineNumSysAndCheckDigit(result, lgPatternFound);

		return rowOffset;
	  }

//JAVA TO C# CONVERTER WARNING: Method 'throws' clauses are not available in .NET:
//ORIGINAL LINE: protected int[] decodeEnd(com.google.zxing.common.BitArray row, int endStart) throws com.google.zxing.NotFoundException
	  protected internal override int[] decodeEnd(BitArray row, int endStart)
	  {
		return findGuardPattern(row, endStart, true, MIDDLE_END_PATTERN);
	  }

//JAVA TO C# CONVERTER WARNING: Method 'throws' clauses are not available in .NET:
//ORIGINAL LINE: protected boolean checkChecksum(String s) throws com.google.zxing.FormatException, com.google.zxing.ChecksumException
	  protected internal override bool checkChecksum(string s)
	  {
		return base.checkChecksum(convertUPCEtoUPCA(s));
	  }

//JAVA TO C# CONVERTER WARNING: Method 'throws' clauses are not available in .NET:
//ORIGINAL LINE: private static void determineNumSysAndCheckDigit(StringBuilder resultString, int lgPatternFound) throws com.google.zxing.NotFoundException
	  private static void determineNumSysAndCheckDigit(StringBuilder resultString, int lgPatternFound)
	  {

		for (int numSys = 0; numSys <= 1; numSys++)
		{
		  for (int d = 0; d < 10; d++)
		  {
			if (lgPatternFound == NUMSYS_AND_CHECK_DIGIT_PATTERNS[numSys][d])
			{
			  resultString.Insert(0, (char)('0' + numSys));
			  resultString.Append((char)('0' + d));
			  return;
			}
		  }
		}
		throw NotFoundException.NotFoundInstance;
	  }

	  internal override BarcodeFormat BarcodeFormat
	  {
		  get
		  {
			return BarcodeFormat.UPC_E;
		  }
	  }

	  /// <summary>
	  /// Expands a UPC-E value back into its full, equivalent UPC-A code value.
	  /// </summary>
	  /// <param name="upce"> UPC-E code as string of digits </param>
	  /// <returns> equivalent UPC-A code as string of digits </returns>
	  public static string convertUPCEtoUPCA(string upce)
	  {
		char[] upceChars = new char[6];
		upce.CopyTo(1, upceChars, 0, 6);
		StringBuilder result = new StringBuilder(12);
		result.Append(upce[0]);
		char lastChar = upceChars[5];
		switch (lastChar)
		{
		  case '0':
		  case '1':
		  case '2':
			result.Append(upceChars, 0, 2);
			result.Append(lastChar);
			result.Append("0000");
			result.Append(upceChars, 2, 3);
			break;
		  case '3':
			result.Append(upceChars, 0, 3);
			result.Append("00000");
			result.Append(upceChars, 3, 2);
			break;
		  case '4':
			result.Append(upceChars, 0, 4);
			result.Append("00000");
			result.Append(upceChars[4]);
			break;
		  default:
			result.Append(upceChars, 0, 5);
			result.Append("0000");
			result.Append(lastChar);
			break;
		}
		result.Append(upce[7]);
		return result.ToString();
	  }

	}

}