using System;
using System.Collections;
using System.Collections.Generic;
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
	using DecodeHintType = com.google.zxing.DecodeHintType;
	using FormatException = com.google.zxing.FormatException;
	using NotFoundException = com.google.zxing.NotFoundException;
	using Result = com.google.zxing.Result;
	using ResultPoint = com.google.zxing.ResultPoint;
	using BitArray = com.google.zxing.common.BitArray;


	/// <summary>
	/// <p>Decodes Code 128 barcodes.</p>
	/// 
	/// @author Sean Owen
	/// </summary>
	public sealed class Code128Reader : OneDReader
	{

	  internal static readonly int[][] CODE_PATTERNS = {new int[] {2, 1, 2, 2, 2, 2}, new int[] {2, 2, 2, 1, 2, 2}, new int[] {2, 2, 2, 2, 2, 1}, new int[] {1, 2, 1, 2, 2, 3}, new int[] {1, 2, 1, 3, 2, 2}, new int[] {1, 3, 1, 2, 2, 2}, new int[] {1, 2, 2, 2, 1, 3}, new int[] {1, 2, 2, 3, 1, 2}, new int[] {1, 3, 2, 2, 1, 2}, new int[] {2, 2, 1, 2, 1, 3}, new int[] {2, 2, 1, 3, 1, 2}, new int[] {2, 3, 1, 2, 1, 2}, new int[] {1, 1, 2, 2, 3, 2}, new int[] {1, 2, 2, 1, 3, 2}, new int[] {1, 2, 2, 2, 3, 1}, new int[] {1, 1, 3, 2, 2, 2}, new int[] {1, 2, 3, 1, 2, 2}, new int[] {1, 2, 3, 2, 2, 1}, new int[] {2, 2, 3, 2, 1, 1}, new int[] {2, 2, 1, 1, 3, 2}, new int[] {2, 2, 1, 2, 3, 1}, new int[] {2, 1, 3, 2, 1, 2}, new int[] {2, 2, 3, 1, 1, 2}, new int[] {3, 1, 2, 1, 3, 1}, new int[] {3, 1, 1, 2, 2, 2}, new int[] {3, 2, 1, 1, 2, 2}, new int[] {3, 2, 1, 2, 2, 1}, new int[] {3, 1, 2, 2, 1, 2}, new int[] {3, 2, 2, 1, 1, 2}, new int[] {3, 2, 2, 2, 1, 1}, new int[] {2, 1, 2, 1, 2, 3}, new int[] {2, 1, 2, 3, 2, 1}, new int[] {2, 3, 2, 1, 2, 1}, new int[] {1, 1, 1, 3, 2, 3}, new int[] {1, 3, 1, 1, 2, 3}, new int[] {1, 3, 1, 3, 2, 1}, new int[] {1, 1, 2, 3, 1, 3}, new int[] {1, 3, 2, 1, 1, 3}, new int[] {1, 3, 2, 3, 1, 1}, new int[] {2, 1, 1, 3, 1, 3}, new int[] {2, 3, 1, 1, 1, 3}, new int[] {2, 3, 1, 3, 1, 1}, new int[] {1, 1, 2, 1, 3, 3}, new int[] {1, 1, 2, 3, 3, 1}, new int[] {1, 3, 2, 1, 3, 1}, new int[] {1, 1, 3, 1, 2, 3}, new int[] {1, 1, 3, 3, 2, 1}, new int[] {1, 3, 3, 1, 2, 1}, new int[] {3, 1, 3, 1, 2, 1}, new int[] {2, 1, 1, 3, 3, 1}, new int[] {2, 3, 1, 1, 3, 1}, new int[] {2, 1, 3, 1, 1, 3}, new int[] {2, 1, 3, 3, 1, 1}, new int[] {2, 1, 3, 1, 3, 1}, new int[] {3, 1, 1, 1, 2, 3}, new int[] {3, 1, 1, 3, 2, 1}, new int[] {3, 3, 1, 1, 2, 1}, new int[] {3, 1, 2, 1, 1, 3}, new int[] {3, 1, 2, 3, 1, 1}, new int[] {3, 3, 2, 1, 1, 1}, new int[] {3, 1, 4, 1, 1, 1}, new int[] {2, 2, 1, 4, 1, 1}, new int[] {4, 3, 1, 1, 1, 1}, new int[] {1, 1, 1, 2, 2, 4}, new int[] {1, 1, 1, 4, 2, 2}, new int[] {1, 2, 1, 1, 2, 4}, new int[] {1, 2, 1, 4, 2, 1}, new int[] {1, 4, 1, 1, 2, 2}, new int[] {1, 4, 1, 2, 2, 1}, new int[] {1, 1, 2, 2, 1, 4}, new int[] {1, 1, 2, 4, 1, 2}, new int[] {1, 2, 2, 1, 1, 4}, new int[] {1, 2, 2, 4, 1, 1}, new int[] {1, 4, 2, 1, 1, 2}, new int[] {1, 4, 2, 2, 1, 1}, new int[] {2, 4, 1, 2, 1, 1}, new int[] {2, 2, 1, 1, 1, 4}, new int[] {4, 1, 3, 1, 1, 1}, new int[] {2, 4, 1, 1, 1, 2}, new int[] {1, 3, 4, 1, 1, 1}, new int[] {1, 1, 1, 2, 4, 2}, new int[] {1, 2, 1, 1, 4, 2}, new int[] {1, 2, 1, 2, 4, 1}, new int[] {1, 1, 4, 2, 1, 2}, new int[] {1, 2, 4, 1, 1, 2}, new int[] {1, 2, 4, 2, 1, 1}, new int[] {4, 1, 1, 2, 1, 2}, new int[] {4, 2, 1, 1, 1, 2}, new int[] {4, 2, 1, 2, 1, 1}, new int[] {2, 1, 2, 1, 4, 1}, new int[] {2, 1, 4, 1, 2, 1}, new int[] {4, 1, 2, 1, 2, 1}, new int[] {1, 1, 1, 1, 4, 3}, new int[] {1, 1, 1, 3, 4, 1}, new int[] {1, 3, 1, 1, 4, 1}, new int[] {1, 1, 4, 1, 1, 3}, new int[] {1, 1, 4, 3, 1, 1}, new int[] {4, 1, 1, 1, 1, 3}, new int[] {4, 1, 1, 3, 1, 1}, new int[] {1, 1, 3, 1, 4, 1}, new int[] {1, 1, 4, 1, 3, 1}, new int[] {3, 1, 1, 1, 4, 1}, new int[] {4, 1, 1, 1, 3, 1}, new int[] {2, 1, 1, 4, 1, 2}, new int[] {2, 1, 1, 2, 1, 4}, new int[] {2, 1, 1, 2, 3, 2}, new int[] {2, 3, 3, 1, 1, 1, 2}};

	  private static readonly int MAX_AVG_VARIANCE = (int)(PATTERN_MATCH_RESULT_SCALE_FACTOR * 0.25f);
	  private static readonly int MAX_INDIVIDUAL_VARIANCE = (int)(PATTERN_MATCH_RESULT_SCALE_FACTOR * 0.7f);

	  private const int CODE_SHIFT = 98;

	  private const int CODE_CODE_C = 99;
	  private const int CODE_CODE_B = 100;
	  private const int CODE_CODE_A = 101;

	  private const int CODE_FNC_1 = 102;
	  private const int CODE_FNC_2 = 97;
	  private const int CODE_FNC_3 = 96;
	  private const int CODE_FNC_4_A = 101;
	  private const int CODE_FNC_4_B = 100;

	  private const int CODE_START_A = 103;
	  private const int CODE_START_B = 104;
	  private const int CODE_START_C = 105;
	  private const int CODE_STOP = 106;

//JAVA TO C# CONVERTER WARNING: Method 'throws' clauses are not available in .NET:
//ORIGINAL LINE: private static int[] findStartPattern(com.google.zxing.common.BitArray row) throws com.google.zxing.NotFoundException
	  private static int[] findStartPattern(BitArray row)
	  {
		int width = row.Size;
		int rowOffset = row.getNextSet(0);

		int counterPosition = 0;
		int[] counters = new int[6];
		int patternStart = rowOffset;
		bool isWhite = false;
		int patternLength = counters.Length;

		for (int i = rowOffset; i < width; i++)
		{
		  if (row.get(i) ^ isWhite)
		  {
			counters[counterPosition]++;
		  }
		  else
		  {
			if (counterPosition == patternLength - 1)
			{
			  int bestVariance = MAX_AVG_VARIANCE;
			  int bestMatch = -1;
			  for (int startCode = CODE_START_A; startCode <= CODE_START_C; startCode++)
			  {
				int variance = patternMatchVariance(counters, CODE_PATTERNS[startCode], MAX_INDIVIDUAL_VARIANCE);
				if (variance < bestVariance)
				{
				  bestVariance = variance;
				  bestMatch = startCode;
				}
			  }
			  // Look for whitespace before start pattern, >= 50% of width of start pattern
			  if (bestMatch >= 0 && row.isRange(Math.Max(0, patternStart - (i - patternStart) / 2), patternStart, false))
			  {
				return new int[]{patternStart, i, bestMatch};
			  }
			  patternStart += counters[0] + counters[1];
			  Array.Copy(counters, 2, counters, 0, patternLength - 2);
			  counters[patternLength - 2] = 0;
			  counters[patternLength - 1] = 0;
			  counterPosition--;
			}
			else
			{
			  counterPosition++;
			}
			counters[counterPosition] = 1;
			isWhite = !isWhite;
		  }
		}
		throw NotFoundException.NotFoundInstance;
	  }

//JAVA TO C# CONVERTER WARNING: Method 'throws' clauses are not available in .NET:
//ORIGINAL LINE: private static int decodeCode(com.google.zxing.common.BitArray row, int[] counters, int rowOffset) throws com.google.zxing.NotFoundException
	  private static int decodeCode(BitArray row, int[] counters, int rowOffset)
	  {
		recordPattern(row, rowOffset, counters);
		int bestVariance = MAX_AVG_VARIANCE; // worst variance we'll accept
		int bestMatch = -1;
		for (int d = 0; d < CODE_PATTERNS.Length; d++)
		{
		  int[] pattern = CODE_PATTERNS[d];
		  int variance = patternMatchVariance(counters, pattern, MAX_INDIVIDUAL_VARIANCE);
		  if (variance < bestVariance)
		  {
			bestVariance = variance;
			bestMatch = d;
		  }
		}
		// TODO We're overlooking the fact that the STOP pattern has 7 values, not 6.
		if (bestMatch >= 0)
		{
		  return bestMatch;
		}
		else
		{
		  throw NotFoundException.NotFoundInstance;
		}
	  }

//JAVA TO C# CONVERTER WARNING: Method 'throws' clauses are not available in .NET:
//ORIGINAL LINE: public com.google.zxing.Result decodeRow(int rowNumber, com.google.zxing.common.BitArray row, java.util.Map<com.google.zxing.DecodeHintType,?> hints) throws com.google.zxing.NotFoundException, com.google.zxing.FormatException, com.google.zxing.ChecksumException
      public override Result decodeRow(int rowNumber, BitArray row, IDictionary<DecodeHintType, object> hints)
	  {

		int[] startPatternInfo = findStartPattern(row);
		int startCode = startPatternInfo[2];
		int codeSet;
		switch (startCode)
		{
		  case CODE_START_A:
			codeSet = CODE_CODE_A;
			break;
		  case CODE_START_B:
			codeSet = CODE_CODE_B;
			break;
		  case CODE_START_C:
			codeSet = CODE_CODE_C;
			break;
		  default:
			throw FormatException.FormatInstance;
		}

		bool done = false;
		bool isNextShifted = false;

		StringBuilder result = new StringBuilder(20);
		IList<sbyte> rawCodes = new List<sbyte>(20);

		int lastStart = startPatternInfo[0];
		int nextStart = startPatternInfo[1];
		int[] counters = new int[6];

		int lastCode = 0;
		int code = 0;
		int checksumTotal = startCode;
		int multiplier = 0;
		bool lastCharacterWasPrintable = true;

		while (!done)
		{

		  bool unshift = isNextShifted;
		  isNextShifted = false;

		  // Save off last code
		  lastCode = code;

		  // Decode another code from image
		  code = decodeCode(row, counters, nextStart);

		  rawCodes.Add((sbyte) code);

		  // Remember whether the last code was printable or not (excluding CODE_STOP)
		  if (code != CODE_STOP)
		  {
			lastCharacterWasPrintable = true;
		  }

		  // Add to checksum computation (if not CODE_STOP of course)
		  if (code != CODE_STOP)
		  {
			multiplier++;
			checksumTotal += multiplier * code;
		  }

		  // Advance to where the next code will to start
		  lastStart = nextStart;
		  foreach (int counter in counters)
		  {
			nextStart += counter;
		  }

		  // Take care of illegal start codes
		  switch (code)
		  {
			case CODE_START_A:
			case CODE_START_B:
			case CODE_START_C:
			  throw FormatException.FormatInstance;
		  }

		  switch (codeSet)
		  {

			case CODE_CODE_A:
			  if (code < 64)
			  {
				result.Append((char)(' ' + code));
			  }
			  else if (code < 96)
			  {
				result.Append((char)(code - 64));
			  }
			  else
			  {
				// Don't let CODE_STOP, which always appears, affect whether whether we think the last
				// code was printable or not.
				if (code != CODE_STOP)
				{
				  lastCharacterWasPrintable = false;
				}
				switch (code)
				{
				  case CODE_FNC_1:
				  case CODE_FNC_2:
				  case CODE_FNC_3:
				  case CODE_FNC_4_A:
					// do nothing?
					break;
				  case CODE_SHIFT:
					isNextShifted = true;
					codeSet = CODE_CODE_B;
					break;
				  case CODE_CODE_B:
					codeSet = CODE_CODE_B;
					break;
				  case CODE_CODE_C:
					codeSet = CODE_CODE_C;
					break;
				  case CODE_STOP:
					done = true;
					break;
				}
			  }
			  break;
			case CODE_CODE_B:
			  if (code < 96)
			  {
				result.Append((char)(' ' + code));
			  }
			  else
			  {
				if (code != CODE_STOP)
				{
				  lastCharacterWasPrintable = false;
				}
				switch (code)
				{
				  case CODE_FNC_1:
				  case CODE_FNC_2:
				  case CODE_FNC_3:
				  case CODE_FNC_4_B:
					// do nothing?
					break;
				  case CODE_SHIFT:
					isNextShifted = true;
					codeSet = CODE_CODE_A;
					break;
				  case CODE_CODE_A:
					codeSet = CODE_CODE_A;
					break;
				  case CODE_CODE_C:
					codeSet = CODE_CODE_C;
					break;
				  case CODE_STOP:
					done = true;
					break;
				}
			  }
			  break;
			case CODE_CODE_C:
			  if (code < 100)
			  {
				if (code < 10)
				{
				  result.Append('0');
				}
				result.Append(code);
			  }
			  else
			  {
				if (code != CODE_STOP)
				{
				  lastCharacterWasPrintable = false;
				}
				switch (code)
				{
				  case CODE_FNC_1:
					// do nothing?
					break;
				  case CODE_CODE_A:
					codeSet = CODE_CODE_A;
					break;
				  case CODE_CODE_B:
					codeSet = CODE_CODE_B;
					break;
				  case CODE_STOP:
					done = true;
					break;
				}
			  }
			  break;
		  }

		  // Unshift back to another code set if we were shifted
		  if (unshift)
		  {
			codeSet = codeSet == CODE_CODE_A ? CODE_CODE_B : CODE_CODE_A;
		  }

		}

		// Check for ample whitespace following pattern, but, to do this we first need to remember that
		// we fudged decoding CODE_STOP since it actually has 7 bars, not 6. There is a black bar left
		// to read off. Would be slightly better to properly read. Here we just skip it:
		nextStart = row.getNextUnset(nextStart);
		if (!row.isRange(nextStart, Math.Min(row.Size, nextStart + (nextStart - lastStart) / 2), false))
		{
		  throw NotFoundException.NotFoundInstance;
		}

		// Pull out from sum the value of the penultimate check code
		checksumTotal -= multiplier * lastCode;
		// lastCode is the checksum then:
		if (checksumTotal % 103 != lastCode)
		{
		  throw ChecksumException.ChecksumInstance;
		}

		// Need to pull out the check digits from string
		int resultLength = result.Length;
		if (resultLength == 0)
		{
		  // false positive
		  throw NotFoundException.NotFoundInstance;
		}

		// Only bother if the result had at least one character, and if the checksum digit happened to
		// be a printable character. If it was just interpreted as a control code, nothing to remove.
		if (resultLength > 0 && lastCharacterWasPrintable)
		{
		  if (codeSet == CODE_CODE_C)
		  {
			result.Remove(resultLength - 2, 2);
		  }
		  else
		  {
			result.Remove(resultLength - 1, 1);
		  }
		}

		float left = (float)(startPatternInfo[1] + startPatternInfo[0]) / 2.0f;
		float right = (float)(nextStart + lastStart) / 2.0f;

		int rawCodesSize = rawCodes.Count;
		sbyte[] rawBytes = new sbyte[rawCodesSize];
		for (int i = 0; i < rawCodesSize; i++)
		{
		  rawBytes[i] = rawCodes[i];
		}

		return new Result(result.ToString(), rawBytes, new ResultPoint[]{new ResultPoint(left, (float) rowNumber), new ResultPoint(right, (float) rowNumber)}, BarcodeFormat.CODE_128);

	  }

	}

}