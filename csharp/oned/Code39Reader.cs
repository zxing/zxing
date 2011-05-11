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
using BarcodeFormat = com.google.zxing.BarcodeFormat;
using ReaderException = com.google.zxing.ReaderException;
using Result = com.google.zxing.Result;
using ResultPoint = com.google.zxing.ResultPoint;
using BitArray = com.google.zxing.common.BitArray;
namespace com.google.zxing.oned
{
	
	/// <summary> <p>Decodes Code 39 barcodes. This does not support "Full ASCII Code 39" yet.</p>
	/// 
	/// </summary>
	/// <author>  Sean Owen
	/// </author>
	/// <author>www.Redivivus.in (suraj.supekar@redivivus.in) - Ported from ZXING Java Source 
	/// </author>
	public sealed class Code39Reader:OneDReader
	{
		
		internal const System.String ALPHABET_STRING = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ-. *$/+%";
		//UPGRADE_NOTE: Final was removed from the declaration of 'ALPHABET '. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
		private static readonly char[] ALPHABET = ALPHABET_STRING.ToCharArray();
		
		/// <summary> These represent the encodings of characters, as patterns of wide and narrow bars.
		/// The 9 least-significant bits of each int correspond to the pattern of wide and narrow,
		/// with 1s representing "wide" and 0s representing narrow.
		/// </summary>
		//UPGRADE_NOTE: Final was removed from the declaration of 'CHARACTER_ENCODINGS'. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
		internal static readonly int[] CHARACTER_ENCODINGS = new int[]{0x034, 0x121, 0x061, 0x160, 0x031, 0x130, 0x070, 0x025, 0x124, 0x064, 0x109, 0x049, 0x148, 0x019, 0x118, 0x058, 0x00D, 0x10C, 0x04C, 0x01C, 0x103, 0x043, 0x142, 0x013, 0x112, 0x052, 0x007, 0x106, 0x046, 0x016, 0x181, 0x0C1, 0x1C0, 0x091, 0x190, 0x0D0, 0x085, 0x184, 0x0C4, 0x094, 0x0A8, 0x0A2, 0x08A, 0x02A};
		
		//UPGRADE_NOTE: Final was removed from the declaration of 'ASTERISK_ENCODING '. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
		private static readonly int ASTERISK_ENCODING = CHARACTER_ENCODINGS[39];
		
		//UPGRADE_NOTE: Final was removed from the declaration of 'usingCheckDigit '. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
		private bool usingCheckDigit;
		//UPGRADE_NOTE: Final was removed from the declaration of 'extendedMode '. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
		private bool extendedMode;
		
		/// <summary> Creates a reader that assumes all encoded data is data, and does not treat the final
		/// character as a check digit. It will not decoded "extended Code 39" sequences.
		/// </summary>
		public Code39Reader()
		{
			usingCheckDigit = false;
			extendedMode = false;
		}
		
		/// <summary> Creates a reader that can be configured to check the last character as a check digit.
		/// It will not decoded "extended Code 39" sequences.
		/// 
		/// </summary>
		/// <param name="usingCheckDigit">if true, treat the last data character as a check digit, not
		/// data, and verify that the checksum passes.
		/// </param>
		public Code39Reader(bool usingCheckDigit)
		{
			this.usingCheckDigit = usingCheckDigit;
			this.extendedMode = false;
		}
		
		/// <summary> Creates a reader that can be configured to check the last character as a check digit,
		/// or optionally attempt to decode "extended Code 39" sequences that are used to encode
		/// the full ASCII character set.
		/// 
		/// </summary>
		/// <param name="usingCheckDigit">if true, treat the last data character as a check digit, not
		/// data, and verify that the checksum passes.
		/// </param>
		/// <param name="extendedMode">if true, will attempt to decode extended Code 39 sequences in the
		/// text.
		/// </param>
		public Code39Reader(bool usingCheckDigit, bool extendedMode)
		{
			this.usingCheckDigit = usingCheckDigit;
			this.extendedMode = extendedMode;
		}
		
		public override Result decodeRow(int rowNumber, BitArray row, System.Collections.Hashtable hints)
		{
			
			int[] start = findAsteriskPattern(row);
			int nextStart = start[1];
			int end = row.Size;
			
			// Read off white space
			while (nextStart < end && !row.get_Renamed(nextStart))
			{
				nextStart++;
			}
			
			System.Text.StringBuilder result = new System.Text.StringBuilder(20);
			int[] counters = new int[9];
			char decodedChar;
			int lastStart;
			do 
			{
				recordPattern(row, nextStart, counters);
				int pattern = toNarrowWidePattern(counters);
				if (pattern < 0)
				{
					throw ReaderException.Instance;
				}
				decodedChar = patternToChar(pattern);
				result.Append(decodedChar);
				lastStart = nextStart;
				for (int i = 0; i < counters.Length; i++)
				{
					nextStart += counters[i];
				}
				// Read off white space
				while (nextStart < end && !row.get_Renamed(nextStart))
				{
					nextStart++;
				}
			}
			while (decodedChar != '*');
			result.Remove(result.Length - 1, 1); // remove asterisk
			
			// Look for whitespace after pattern:
			int lastPatternSize = 0;
			for (int i = 0; i < counters.Length; i++)
			{
				lastPatternSize += counters[i];
			}
			int whiteSpaceAfterEnd = nextStart - lastStart - lastPatternSize;
			// If 50% of last pattern size, following last pattern, is not whitespace, fail
			// (but if it's whitespace to the very end of the image, that's OK)
			if (nextStart != end && whiteSpaceAfterEnd / 2 < lastPatternSize)
			{
				throw ReaderException.Instance;
			}
			
			if (usingCheckDigit)
			{
				int max = result.Length - 1;
				int total = 0;
				for (int i = 0; i < max; i++)
				{
					total += ALPHABET_STRING.IndexOf((System.Char) result[i]);
				}
				if (total % 43 != ALPHABET_STRING.IndexOf((System.Char) result[max]))
				{
					throw ReaderException.Instance;
				}
				result.Remove(max, 1);
			}
			
			System.String resultString = result.ToString();
			if (extendedMode)
			{
				resultString = decodeExtended(resultString);
			}
			
			if (resultString.Length == 0)
			{
				// Almost surely a false positive
				throw ReaderException.Instance;
			}
			
			//UPGRADE_WARNING: Data types in Visual C# might be different.  Verify the accuracy of narrowing conversions. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1042'"
			float left = (float) (start[1] + start[0]) / 2.0f;
			//UPGRADE_WARNING: Data types in Visual C# might be different.  Verify the accuracy of narrowing conversions. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1042'"
			float right = (float) (nextStart + lastStart) / 2.0f;
			//UPGRADE_WARNING: Data types in Visual C# might be different.  Verify the accuracy of narrowing conversions. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1042'"
			return new Result(resultString, null, new ResultPoint[]{new ResultPoint(left, (float) rowNumber), new ResultPoint(right, (float) rowNumber)}, BarcodeFormat.CODE_39);
		}
		
		private static int[] findAsteriskPattern(BitArray row)
		{
			int width = row.Size;
			int rowOffset = 0;
			while (rowOffset < width)
			{
				if (row.get_Renamed(rowOffset))
				{
					break;
				}
				rowOffset++;
			}
			
			int counterPosition = 0;
			int[] counters = new int[9];
			int patternStart = rowOffset;
			bool isWhite = false;
			int patternLength = counters.Length;
			
			for (int i = rowOffset; i < width; i++)
			{
				bool pixel = row.get_Renamed(i);
				if (pixel ^ isWhite)
				{
					counters[counterPosition]++;
				}
				else
				{
					if (counterPosition == patternLength - 1)
					{
						if (toNarrowWidePattern(counters) == ASTERISK_ENCODING)
						{
							// Look for whitespace before start pattern, >= 50% of width of start pattern
							if (row.isRange(System.Math.Max(0, patternStart - (i - patternStart) / 2), patternStart, false))
							{
								return new int[]{patternStart, i};
							}
						}
						patternStart += counters[0] + counters[1];
						for (int y = 2; y < patternLength; y++)
						{
							counters[y - 2] = counters[y];
						}
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
			throw ReaderException.Instance;
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
				int minCounter = System.Int32.MaxValue;
				for (int i = 0; i < numCounters; i++)
				{
					int counter = counters[i];
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
					if (counters[i] > maxNarrowCounter)
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
						if (counters[i] > maxNarrowCounter)
						{
							wideCounters--;
							// totalWideCountersWidth = 3 * average, so this checks if counter >= 3/2 * average
							if ((counter << 1) >= totalWideCountersWidth)
							{
								return - 1;
							}
						}
					}
					return pattern;
				}
			}
			while (wideCounters > 3);
			return - 1;
		}
		
		private static char patternToChar(int pattern)
		{
			for (int i = 0; i < CHARACTER_ENCODINGS.Length; i++)
			{
				if (CHARACTER_ENCODINGS[i] == pattern)
				{
					return ALPHABET[i];
				}
			}
			throw ReaderException.Instance;
		}
		
		private static System.String decodeExtended(System.String encoded)
		{
			int length = encoded.Length;
			System.Text.StringBuilder decoded = new System.Text.StringBuilder(length);
			for (int i = 0; i < length; i++)
			{
				char c = encoded[i];
				if (c == '+' || c == '$' || c == '%' || c == '/')
				{
					char next = encoded[i + 1];
					char decodedChar = '\x0000';
					switch (c)
					{
						
						case '+': 
							// +A to +Z map to a to z
							if (next >= 'A' && next <= 'Z')
							{
								decodedChar = (char) (next + 32);
							}
							else
							{
								throw ReaderException.Instance;
							}
							break;
						
						case '$': 
							// $A to $Z map to control codes SH to SB
							if (next >= 'A' && next <= 'Z')
							{
								decodedChar = (char) (next - 64);
							}
							else
							{
								throw ReaderException.Instance;
							}
							break;
						
						case '%': 
							// %A to %E map to control codes ESC to US
							if (next >= 'A' && next <= 'E')
							{
								decodedChar = (char) (next - 38);
							}
							else if (next >= 'F' && next <= 'W')
							{
								decodedChar = (char) (next - 11);
							}
							else
							{
								throw ReaderException.Instance;
							}
							break;
						
						case '/': 
							// /A to /O map to ! to , and /Z maps to :
							if (next >= 'A' && next <= 'O')
							{
								decodedChar = (char) (next - 32);
							}
							else if (next == 'Z')
							{
								decodedChar = ':';
							}
							else
							{
								throw ReaderException.Instance;
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
