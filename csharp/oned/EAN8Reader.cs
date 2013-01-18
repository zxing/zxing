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
	using NotFoundException = com.google.zxing.NotFoundException;
	using BitArray = com.google.zxing.common.BitArray;

	/// <summary>
	/// <p>Implements decoding of the EAN-8 format.</p>
	/// 
	/// @author Sean Owen
	/// </summary>
	public sealed class EAN8Reader : UPCEANReader
	{

	  private readonly int[] decodeMiddleCounters;

	  public EAN8Reader()
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

		for (int x = 0; x < 4 && rowOffset < end; x++)
		{
		  int bestMatch = decodeDigit(row, counters, rowOffset, L_PATTERNS);
		  result.Append((char)('0' + bestMatch));
		  foreach (int counter in counters)
		  {
			rowOffset += counter;
		  }
		}

		int[] middleRange = findGuardPattern(row, rowOffset, true, MIDDLE_PATTERN);
		rowOffset = middleRange[1];

		for (int x = 0; x < 4 && rowOffset < end; x++)
		{
		  int bestMatch = decodeDigit(row, counters, rowOffset, L_PATTERNS);
		  result.Append((char)('0' + bestMatch));
		  foreach (int counter in counters)
		  {
			rowOffset += counter;
		  }
		}

		return rowOffset;
	  }

	  internal override BarcodeFormat BarcodeFormat
	  {
		  get
		  {
			return BarcodeFormat.EAN_8;
		  }
	  }

	}

}