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
    using com.google.zxing.common;
    

	/// <summary>
	/// <p>Decodes Code 39 barcodes. This does not support "Full ASCII Code 39" yet.</p>
	/// 
	/// @author Sean Owen </summary>
	/// <seealso cref= Code93Reader </seealso>
	public sealed class Code39Reader : OneDReader
	{

	  internal const string ALPHABET_STRING = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ-. *$/+%";
	  private static readonly char[] ALPHABET = ALPHABET_STRING.ToCharArray();

	  /// <summary>
	  /// These represent the encodings of characters, as patterns of wide and narrow bars.
	  /// The 9 least-significant bits of each int correspond to the pattern of wide and narrow,
	  /// with 1s representing "wide" and 0s representing narrow.
	  /// </summary>
	  internal static readonly int[] CHARACTER_ENCODINGS = {0x034, 0x121, 0x061, 0x160, 0x031, 0x130, 0x070, 0x025, 0x124, 0x064, 0x109, 0x049, 0x148, 0x019, 0x118, 0x058, 0x00D, 0x10C, 0x04C, 0x01C, 0x103, 0x043, 0x142, 0x013, 0x112, 0x052, 0x007, 0x106, 0x046, 0x016, 0x181, 0x0C1, 0x1C0, 0x091, 0x190, 0x0D0, 0x085, 0x184, 0x0C4, 0x094, 0x0A8, 0x0A2, 0x08A, 0x02A};

	  private static readonly int ASTERISK_ENCODING = CHARACTER_ENCODINGS[39];

	  private readonly bool usingCheckDigit;
	  private readonly bool extendedMode;
	  private readonly StringBuilder decodeRowResult;
	  private readonly int[] counters;

	  /// <summary>
	  /// Creates a reader that assumes all encoded data is data, and does not treat the final
	  /// character as a check digit. It will not decoded "extended Code 39" sequences.
	  /// </summary>
	  public Code39Reader() : this(false)
	  {
	  }

	  /// <summary>
	  /// Creates a reader that can be configured to check the last character as a check digit.
	  /// It will not decoded "extended Code 39" sequences.
	  /// </summary>
	  /// <param name="usingCheckDigit"> if true, treat the last data character as a check digit, not
	  /// data, and verify that the checksum passes. </param>
	  public Code39Reader(bool usingCheckDigit) : this(usingCheckDigit, false)
	  {
	  }

	  /// <summary>
	  /// Creates a reader that can be configured to check the last character as a check digit,
	  /// or optionally attempt to decode "extended Code 39" sequences that are used to encode
	  /// the full ASCII character set.
	  /// </summary>
	  /// <param name="usingCheckDigit"> if true, treat the last data character as a check digit, not
	  /// data, and verify that the checksum passes. </param>
	  /// <param name="extendedMode"> if true, will attempt to decode extended Code 39 sequences in the
	  /// text. </param>
	  public Code39Reader(bool usingCheckDigit, bool extendedMode)
	  {
		this.usingCheckDigit = usingCheckDigit;
		this.extendedMode = extendedMode;
		decodeRowResult = new StringBuilder(20);
		counters = new int[9];
	  }

//JAVA TO C# CONVERTER WARNING: Method 'throws' clauses are not available in .NET:
//ORIGINAL LINE: public com.google.zxing.Result decodeRow(int rowNumber, com.google.zxing.common.BitArray row, java.util.Map<com.google.zxing.DecodeHintType,?> hints) throws com.google.zxing.NotFoundException, com.google.zxing.ChecksumException, com.google.zxing.FormatException
      public override Result decodeRow(int rowNumber, BitArray row, IDictionary<DecodeHintType, object> hints)
	  {

		int[] theCounters = counters;
		theCounters.Fill(0);
		StringBuilder result = decodeRowResult;
		result.Length = 0;

		int[] start = findAsteriskPattern(row, theCounters);
		// Read off white space    
		int nextStart = row.getNextSet(start[1]);
		int end = row.Size;

		char decodedChar;
		int lastStart;
		do
		{
		  recordPattern(row, nextStart, theCounters);
		  int pattern = toNarrowWidePattern(theCounters);
		  if (pattern < 0)
		  {
			throw NotFoundException.NotFoundInstance;
		  }
		  decodedChar = patternToChar(pattern);
		  result.Append(decodedChar);
		  lastStart = nextStart;
		  foreach (int counter in theCounters)
		  {
			nextStart += counter;
		  }
		  // Read off white space
		  nextStart = row.getNextSet(nextStart);
		} while (decodedChar != '*');
		result.Length = result.Length - 1; // remove asterisk

		// Look for whitespace after pattern:
		int lastPatternSize = 0;
		foreach (int counter in theCounters)
		{
		  lastPatternSize += counter;
		}
		int whiteSpaceAfterEnd = nextStart - lastStart - lastPatternSize;
		// If 50% of last pattern size, following last pattern, is not whitespace, fail
		// (but if it's whitespace to the very end of the image, that's OK)
		if (nextStart != end && (whiteSpaceAfterEnd >> 1) < lastPatternSize)
		{
		  throw NotFoundException.NotFoundInstance;
		}

		if (usingCheckDigit)
		{
		  int max = result.Length - 1;
		  int total = 0;
		  for (int i = 0; i < max; i++)
		  {
			total += ALPHABET_STRING.IndexOf(decodeRowResult[i]);
		  }
		  if (result[max] != ALPHABET[total % 43])
		  {
			throw ChecksumException.ChecksumInstance;
		  }
		  result.Length = max;
		}

		if (result.Length == 0)
		{
		  // false positive
		  throw NotFoundException.NotFoundInstance;
		}

		string resultString;
		if (extendedMode)
		{
		  resultString = decodeExtended(result.ToString());
		}
		else
		{
		  resultString = result.ToString();
		}

		float left = (float)(start[1] + start[0]) / 2.0f;
		float right = (float)(nextStart + lastStart) / 2.0f;
		return new Result(resultString, null, new ResultPoint[]{new ResultPoint(left, (float) rowNumber), new ResultPoint(right, (float) rowNumber)}, BarcodeFormat.CODE_39);

	  }

//JAVA TO C# CONVERTER WARNING: Method 'throws' clauses are not available in .NET:
//ORIGINAL LINE: private static int[] findAsteriskPattern(com.google.zxing.common.BitArray row, int[] counters) throws com.google.zxing.NotFoundException
	  private static int[] findAsteriskPattern(BitArray row, int[] counters)
	  {
		int width = row.Size;
		int rowOffset = row.getNextSet(0);

		int counterPosition = 0;
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
			  // Look for whitespace before start pattern, >= 50% of width of start pattern
			  if (toNarrowWidePattern(counters) == ASTERISK_ENCODING && row.isRange(Math.Max(0, patternStart - ((i - patternStart) >> 1)), patternStart, false))
			  {
				return new int[]{patternStart, i};
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

	  // For efficiency, returns -1 on failure. Not throwing here saved as many as 700 exceptions
	  // per image when using some of our blackbox images.
	  private static int toNarrowWidePattern(int[] counters)
	  {
		int numCounters = counters.Length;
		int maxNarrowCounter = 0;
		int wideCounters;
		do
		{
		  int minCounter = int.MaxValue;
		  foreach (int counter in counters)
		  {
			if (counter < minCounter && counter > maxNarrowCounter)
			{
			  minCounter = counter;
			}
		  }
		  maxNarrowCounter = minCounter;
		  wideCounters = 0;
		  int totalWideCountersWidth = 0;
		  int pattern = 0;
		  for (int i = 0; i < numCounters; i++)
		  {
			int counter = counters[i];
			if (counter > maxNarrowCounter)
			{
			  pattern |= 1 << (numCounters - 1 - i);
			  wideCounters++;
			  totalWideCountersWidth += counter;
			}
		  }
		  if (wideCounters == 3)
		  {
			// Found 3 wide counters, but are they close enough in width?
			// We can perform a cheap, conservative check to see if any individual
			// counter is more than 1.5 times the average:
			for (int i = 0; i < numCounters && wideCounters > 0; i++)
			{
			  int counter = counters[i];
			  if (counter > maxNarrowCounter)
			  {
				wideCounters--;
				// totalWideCountersWidth = 3 * average, so this checks if counter >= 3/2 * average
				if ((counter << 1) >= totalWideCountersWidth)
				{
				  return -1;
				}
			  }
			}
			return pattern;
		  }
		} while (wideCounters > 3);
		return -1;
	  }

//JAVA TO C# CONVERTER WARNING: Method 'throws' clauses are not available in .NET:
//ORIGINAL LINE: private static char patternToChar(int pattern) throws com.google.zxing.NotFoundException
	  private static char patternToChar(int pattern)
	  {
		for (int i = 0; i < CHARACTER_ENCODINGS.Length; i++)
		{
		  if (CHARACTER_ENCODINGS[i] == pattern)
		  {
			return ALPHABET[i];
		  }
		}
		throw NotFoundException.NotFoundInstance;
	  }

//JAVA TO C# CONVERTER WARNING: Method 'throws' clauses are not available in .NET:
//ORIGINAL LINE: private static String decodeExtended(CharSequence encoded) throws com.google.zxing.FormatException
	  private static string decodeExtended(string encoded)
	  {
		int length = encoded.Length;
		StringBuilder decoded = new StringBuilder(length);
		for (int i = 0; i < length; i++)
		{
		  char c = encoded[i];
		  if (c == '+' || c == '$' || c == '%' || c == '/')
		  {
			char next = encoded[i + 1];
			char decodedChar = '\0';
			switch (c)
			{
			  case '+':
				// +A to +Z map to a to z
				if (next >= 'A' && next <= 'Z')
				{
				  decodedChar = (char)(next + 32);
				}
				else
				{
				  throw FormatException.FormatInstance;
				}
				break;
			  case '$':
				// $A to $Z map to control codes SH to SB
				if (next >= 'A' && next <= 'Z')
				{
				  decodedChar = (char)(next - 64);
				}
				else
				{
				  throw FormatException.FormatInstance;
				}
				break;
			  case '%':
				// %A to %E map to control codes ESC to US
				if (next >= 'A' && next <= 'E')
				{
				  decodedChar = (char)(next - 38);
				}
				else if (next >= 'F' && next <= 'W')
				{
				  decodedChar = (char)(next - 11);
				}
				else
				{
				  throw FormatException.FormatInstance;
				}
				break;
			  case '/':
				// /A to /O map to ! to , and /Z maps to :
				if (next >= 'A' && next <= 'O')
				{
				  decodedChar = (char)(next - 32);
				}
				else if (next == 'Z')
				{
				  decodedChar = ':';
				}
				else
				{
				  throw FormatException.FormatInstance;
				}
				break;
			}
			decoded.Append(decodedChar);
			// bump up i again since we read two characters
			i++;
		  }
		  else
		  {
			decoded.Append(c);
		  }
		}
		return decoded.ToString();
	  }

	}

}