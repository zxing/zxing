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
using DecodeHintType = com.google.zxing.DecodeHintType;
using ReaderException = com.google.zxing.ReaderException;
using Result = com.google.zxing.Result;
using ResultPoint = com.google.zxing.ResultPoint;
using BitArray = com.google.zxing.common.BitArray;
namespace com.google.zxing.oned
{
	
	/// <summary> <p>Implements decoding of the ITF format.</p>
	/// 
	/// <p>"ITF" stands for Interleaved Two of Five. This Reader will scan ITF barcode with 6, 10 or 14
	/// digits. The checksum is optional and is not applied by this Reader. The consumer of the decoded
	/// value will have to apply a checksum if required.</p>
	/// 
	/// <p><a href="http://en.wikipedia.org/wiki/Interleaved_2_of_5">http://en.wikipedia.org/wiki/Interleaved_2_of_5</a>
	/// is a great reference for Interleaved 2 of 5 information.</p>
	/// 
	/// </summary>
	/// <author>  kevin.osullivan@sita.aero, SITA Lab.
	/// </author>
	/// <author>www.Redivivus.in (suraj.supekar@redivivus.in) - Ported from ZXING Java Source 
	/// </author>
	public sealed class ITFReader:OneDReader
	{
		
		//UPGRADE_NOTE: Final was removed from the declaration of 'MAX_AVG_VARIANCE '. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
		//UPGRADE_WARNING: Data types in Visual C# might be different.  Verify the accuracy of narrowing conversions. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1042'"
		private static readonly int MAX_AVG_VARIANCE = (int) (PATTERN_MATCH_RESULT_SCALE_FACTOR * 0.42f);
		//UPGRADE_NOTE: Final was removed from the declaration of 'MAX_INDIVIDUAL_VARIANCE '. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
		//UPGRADE_WARNING: Data types in Visual C# might be different.  Verify the accuracy of narrowing conversions. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1042'"
		private static readonly int MAX_INDIVIDUAL_VARIANCE = (int) (PATTERN_MATCH_RESULT_SCALE_FACTOR * 0.8f);
		
		private const int W = 3; // Pixel width of a wide line
		private const int N = 1; // Pixed width of a narrow line
		
		//UPGRADE_NOTE: Final was removed from the declaration of 'DEFAULT_ALLOWED_LENGTHS'. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
		private static readonly int[] DEFAULT_ALLOWED_LENGTHS = new int[]{6, 10, 14, 44};
		
		// Stores the actual narrow line width of the image being decoded.
		private int narrowLineWidth = - 1;
		
		/// <summary> Start/end guard pattern.
		/// 
		/// Note: The end pattern is reversed because the row is reversed before
		/// searching for the END_PATTERN
		/// </summary>
		//UPGRADE_NOTE: Final was removed from the declaration of 'START_PATTERN '. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
		private static readonly int[] START_PATTERN = new int[]{N, N, N, N};
		//UPGRADE_NOTE: Final was removed from the declaration of 'END_PATTERN_REVERSED '. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
		private static readonly int[] END_PATTERN_REVERSED = new int[]{N, N, W};
		
		/// <summary> Patterns of Wide / Narrow lines to indicate each digit</summary>
		//UPGRADE_NOTE: Final was removed from the declaration of 'PATTERNS '. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
		private static readonly int[][] PATTERNS = new int[][]{new int[]{N, N, W, W, N}, new int[]{W, N, N, N, W}, new int[]{N, W, N, N, W}, new int[]{W, W, N, N, N}, new int[]{N, N, W, N, W}, new int[]{W, N, W, N, N}, new int[]{N, W, W, N, N}, new int[]{N, N, N, W, W}, new int[]{W, N, N, W, N}, new int[]{N, W, N, W, N}};
		
		public override Result decodeRow(int rowNumber, BitArray row, System.Collections.Hashtable hints)
		{
			
			// Find out where the Middle section (payload) starts & ends
			int[] startRange = decodeStart(row);
			int[] endRange = decodeEnd(row);
			
			System.Text.StringBuilder result = new System.Text.StringBuilder(20);
			decodeMiddle(row, startRange[1], endRange[0], result);
			System.String resultString = result.ToString();
			
			int[] allowedLengths = null;
			if (hints != null)
			{
				allowedLengths = (int[]) hints[DecodeHintType.ALLOWED_LENGTHS];
			}
			if (allowedLengths == null)
			{
				allowedLengths = DEFAULT_ALLOWED_LENGTHS;
			}
			
			// To avoid false positives with 2D barcodes (and other patterns), make
			// an assumption that the decoded string must be 6, 10 or 14 digits.
			int length = resultString.Length;
			bool lengthOK = false;
			for (int i = 0; i < allowedLengths.Length; i++)
			{
				if (length == allowedLengths[i])
				{
					lengthOK = true;
					break;
				}
			}
			if (!lengthOK)
			{
				throw ReaderException.Instance;
			}
			
			//UPGRADE_WARNING: Data types in Visual C# might be different.  Verify the accuracy of narrowing conversions. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1042'"
			return new Result(resultString, null, new ResultPoint[]{new ResultPoint(startRange[1], (float) rowNumber), new ResultPoint(endRange[0], (float) rowNumber)}, BarcodeFormat.ITF);
		}
		
		/// <param name="row">         row of black/white values to search
		/// </param>
		/// <param name="payloadStart">offset of start pattern
		/// </param>
		/// <param name="resultString">{@link StringBuffer} to append decoded chars to
		/// </param>
		/// <throws>  ReaderException if decoding could not complete successfully </throws>
		private static void  decodeMiddle(BitArray row, int payloadStart, int payloadEnd, System.Text.StringBuilder resultString)
		{
			
			// Digits are interleaved in pairs - 5 black lines for one digit, and the
			// 5
			// interleaved white lines for the second digit.
			// Therefore, need to scan 10 lines and then
			// split these into two arrays
			int[] counterDigitPair = new int[10];
			int[] counterBlack = new int[5];
			int[] counterWhite = new int[5];
			
			while (payloadStart < payloadEnd)
			{
				
				// Get 10 runs of black/white.
				recordPattern(row, payloadStart, counterDigitPair);
				// Split them into each array
				for (int k = 0; k < 5; k++)
				{
					int twoK = k << 1;
					counterBlack[k] = counterDigitPair[twoK];
					counterWhite[k] = counterDigitPair[twoK + 1];
				}
				
				int bestMatch = decodeDigit(counterBlack);
				resultString.Append((char) ('0' + bestMatch));
				bestMatch = decodeDigit(counterWhite);
				resultString.Append((char) ('0' + bestMatch));
				
				for (int i = 0; i < counterDigitPair.Length; i++)
				{
					payloadStart += counterDigitPair[i];
				}
			}
		}
		
		/// <summary> Identify where the start of the middle / payload section starts.
		/// 
		/// </summary>
		/// <param name="row">row of black/white values to search
		/// </param>
		/// <returns> Array, containing index of start of 'start block' and end of
		/// 'start block'
		/// </returns>
		/// <throws>  ReaderException </throws>
		internal int[] decodeStart(BitArray row)
		{
			int endStart = skipWhiteSpace(row);
			int[] startPattern = findGuardPattern(row, endStart, START_PATTERN);
			
			// Determine the width of a narrow line in pixels. We can do this by
			// getting the width of the start pattern and dividing by 4 because its
			// made up of 4 narrow lines.
			this.narrowLineWidth = (startPattern[1] - startPattern[0]) >> 2;
			
			validateQuietZone(row, startPattern[0]);
			
			return startPattern;
		}
		
		/// <summary> The start & end patterns must be pre/post fixed by a quiet zone. This
		/// zone must be at least 10 times the width of a narrow line.  Scan back until
		/// we either get to the start of the barcode or match the necessary number of
		/// quiet zone pixels.
		/// 
		/// Note: Its assumed the row is reversed when using this method to find
		/// quiet zone after the end pattern.
		/// 
		/// ref: http://www.barcode-1.net/i25code.html
		/// 
		/// </summary>
		/// <param name="row">bit array representing the scanned barcode.
		/// </param>
		/// <param name="startPattern">index into row of the start or end pattern.
		/// </param>
		/// <throws>  ReaderException if the quiet zone cannot be found, a ReaderException is thrown. </throws>
		private void  validateQuietZone(BitArray row, int startPattern)
		{
			
			int quietCount = this.narrowLineWidth * 10; // expect to find this many pixels of quiet zone
			
			for (int i = startPattern - 1; quietCount > 0 && i >= 0; i--)
			{
				if (row.get_Renamed(i))
				{
					break;
				}
				quietCount--;
			}
			if (quietCount != 0)
			{
				// Unable to find the necessary number of quiet zone pixels.
				throw ReaderException.Instance;
			}
		}
		
		/// <summary> Skip all whitespace until we get to the first black line.
		/// 
		/// </summary>
		/// <param name="row">row of black/white values to search
		/// </param>
		/// <returns> index of the first black line.
		/// </returns>
		/// <throws>  ReaderException Throws exception if no black lines are found in the row </throws>
		private static int skipWhiteSpace(BitArray row)
		{
			int width = row.Size;
			int endStart = 0;
			while (endStart < width)
			{
				if (row.get_Renamed(endStart))
				{
					break;
				}
				endStart++;
			}
			if (endStart == width)
			{
				throw ReaderException.Instance;
			}
			
			return endStart;
		}
		
		/// <summary> Identify where the end of the middle / payload section ends.
		/// 
		/// </summary>
		/// <param name="row">row of black/white values to search
		/// </param>
		/// <returns> Array, containing index of start of 'end block' and end of 'end
		/// block'
		/// </returns>
		/// <throws>  ReaderException </throws>
		
		internal int[] decodeEnd(BitArray row)
		{
			
			// For convenience, reverse the row and then
			// search from 'the start' for the end block
			row.reverse();
			try
			{
				int endStart = skipWhiteSpace(row);
				int[] endPattern = findGuardPattern(row, endStart, END_PATTERN_REVERSED);
				
				// The start & end patterns must be pre/post fixed by a quiet zone. This
				// zone must be at least 10 times the width of a narrow line.
				// ref: http://www.barcode-1.net/i25code.html
				validateQuietZone(row, endPattern[0]);
				
				// Now recalculate the indices of where the 'endblock' starts & stops to
				// accommodate
				// the reversed nature of the search
				int temp = endPattern[0];
				endPattern[0] = row.Size - endPattern[1];
				endPattern[1] = row.Size - temp;
				
				return endPattern;
			}
			finally
			{
				// Put the row back the right way.
				row.reverse();
			}
		}
		
		/// <param name="row">      row of black/white values to search
		/// </param>
		/// <param name="rowOffset">position to start search
		/// </param>
		/// <param name="pattern">  pattern of counts of number of black and white pixels that are
		/// being searched for as a pattern
		/// </param>
		/// <returns> start/end horizontal offset of guard pattern, as an array of two
		/// ints
		/// </returns>
		/// <throws>  ReaderException if pattern is not found </throws>
		private static int[] findGuardPattern(BitArray row, int rowOffset, int[] pattern)
		{
			
			// TODO: This is very similar to implementation in UPCEANReader. Consider if they can be
			// merged to a single method.
			int patternLength = pattern.Length;
			int[] counters = new int[patternLength];
			int width = row.Size;
			bool isWhite = false;
			
			int counterPosition = 0;
			int patternStart = rowOffset;
			for (int x = rowOffset; x < width; x++)
			{
				bool pixel = row.get_Renamed(x);
				if (pixel ^ isWhite)
				{
					counters[counterPosition]++;
				}
				else
				{
					if (counterPosition == patternLength - 1)
					{
						if (patternMatchVariance(counters, pattern, MAX_INDIVIDUAL_VARIANCE) < MAX_AVG_VARIANCE)
						{
							return new int[]{patternStart, x};
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
		
		/// <summary> Attempts to decode a sequence of ITF black/white lines into single
		/// digit.
		/// 
		/// </summary>
		/// <param name="counters">the counts of runs of observed black/white/black/... values
		/// </param>
		/// <returns> The decoded digit
		/// </returns>
		/// <throws>  ReaderException if digit cannot be decoded </throws>
		private static int decodeDigit(int[] counters)
		{
			
			int bestVariance = MAX_AVG_VARIANCE; // worst variance we'll accept
			int bestMatch = - 1;
			int max = PATTERNS.Length;
			for (int i = 0; i < max; i++)
			{
				int[] pattern = PATTERNS[i];
				int variance = patternMatchVariance(counters, pattern, MAX_INDIVIDUAL_VARIANCE);
				if (variance < bestVariance)
				{
					bestVariance = variance;
					bestMatch = i;
				}
			}
			if (bestMatch >= 0)
			{
				return bestMatch;
			}
			else
			{
				throw ReaderException.Instance;
			}
		}
	}
}