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
using BinaryBitmap = com.google.zxing.BinaryBitmap;
using DecodeHintType = com.google.zxing.DecodeHintType;
using Reader = com.google.zxing.Reader;
using ReaderException = com.google.zxing.ReaderException;
using Result = com.google.zxing.Result;
using ResultMetadataType = com.google.zxing.ResultMetadataType;
using ResultPoint = com.google.zxing.ResultPoint;
using BitArray = com.google.zxing.common.BitArray;
namespace com.google.zxing.oned
{
	
	/// <summary> Encapsulates functionality and implementation that is common to all families
	/// of one-dimensional barcodes.
	/// 
	/// </summary>
	/// <author>  dswitkin@google.com (Daniel Switkin)
	/// </author>
	/// <author>  Sean Owen
	/// </author>
	/// <author>www.Redivivus.in (suraj.supekar@redivivus.in) - Ported from ZXING Java Source 
	/// </author>
	public abstract class OneDReader : Reader
	{
		
		private const int INTEGER_MATH_SHIFT = 8;
		//UPGRADE_NOTE: Final was removed from the declaration of 'PATTERN_MATCH_RESULT_SCALE_FACTOR '. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
		internal static readonly int PATTERN_MATCH_RESULT_SCALE_FACTOR = 1 << INTEGER_MATH_SHIFT;
		
		public virtual Result decode(BinaryBitmap image)
		{
			return decode(image, null);
		}
		
		// Note that we don't try rotation without the try harder flag, even if rotation was supported.
		public virtual Result decode(BinaryBitmap image, System.Collections.Hashtable hints)
		{
			try
			{
				return doDecode(image, hints);
			}
			catch (ReaderException re)
			{
				bool tryHarder = hints != null && hints.ContainsKey(DecodeHintType.TRY_HARDER);
				if (tryHarder && image.RotateSupported)
				{
					BinaryBitmap rotatedImage = image.rotateCounterClockwise();
					Result result = doDecode(rotatedImage, hints);
					// Record that we found it rotated 90 degrees CCW / 270 degrees CW
					System.Collections.Hashtable metadata = result.ResultMetadata;
					int orientation = 270;
					if (metadata != null && metadata.ContainsKey(ResultMetadataType.ORIENTATION))
					{
						// But if we found it reversed in doDecode(), add in that result here:
						orientation = (orientation + ((System.Int32) metadata[ResultMetadataType.ORIENTATION])) % 360;
					}
					result.putMetadata(ResultMetadataType.ORIENTATION, (System.Object) orientation);
					// Update result points
					ResultPoint[] points = result.ResultPoints;
					int height = rotatedImage.Height;
					for (int i = 0; i < points.Length; i++)
					{
						points[i] = new ResultPoint(height - points[i].Y - 1, points[i].X);
					}
					return result;
				}
				else
				{
					throw re;
				}
			}
		}
		
		/// <summary> We're going to examine rows from the middle outward, searching alternately above and below the
		/// middle, and farther out each time. rowStep is the number of rows between each successive
		/// attempt above and below the middle. So we'd scan row middle, then middle - rowStep, then
		/// middle + rowStep, then middle - (2 * rowStep), etc.
		/// rowStep is bigger as the image is taller, but is always at least 1. We've somewhat arbitrarily
		/// decided that moving up and down by about 1/16 of the image is pretty good; we try more of the
		/// image if "trying harder".
		/// 
		/// </summary>
		/// <param name="image">The image to decode
		/// </param>
		/// <param name="hints">Any hints that were requested
		/// </param>
		/// <returns> The contents of the decoded barcode
		/// </returns>
		/// <throws>  ReaderException Any spontaneous errors which occur </throws>
		private Result doDecode(BinaryBitmap image, System.Collections.Hashtable hints)
		{
			int width = image.Width;
			int height = image.Height;
			BitArray row = new BitArray(width);
			
			int middle = height >> 1;
			bool tryHarder = hints != null && hints.ContainsKey(DecodeHintType.TRY_HARDER);
			int rowStep = System.Math.Max(1, height >> (tryHarder?7:4));
			int maxLines;
			if (tryHarder)
			{
				maxLines = height; // Look at the whole image, not just the center
			}
			else
			{
				maxLines = 9; // Nine rows spaced 1/16 apart is roughly the middle half of the image
			}
			
			for (int x = 0; x < maxLines; x++)
			{
				
				// Scanning from the middle out. Determine which row we're looking at next:
				int rowStepsAboveOrBelow = (x + 1) >> 1;
				bool isAbove = (x & 0x01) == 0; // i.e. is x even?
				int rowNumber = middle + rowStep * (isAbove?rowStepsAboveOrBelow:- rowStepsAboveOrBelow);
				if (rowNumber < 0 || rowNumber >= height)
				{
					// Oops, if we run off the top or bottom, stop
					break;
				}
				
				// Estimate black point for this row and load it:
				try
				{
					row = image.getBlackRow(rowNumber, row);
				}
				catch (ReaderException)
				{
					continue;
				}
				
				// While we have the image data in a BitArray, it's fairly cheap to reverse it in place to
				// handle decoding upside down barcodes.
				for (int attempt = 0; attempt < 2; attempt++)
				{
					if (attempt == 1)
					{
						// trying again?
						row.reverse(); // reverse the row and continue
						// This means we will only ever draw result points *once* in the life of this method
						// since we want to avoid drawing the wrong points after flipping the row, and,
						// don't want to clutter with noise from every single row scan -- just the scans
						// that start on the center line.
						if (hints != null && hints.ContainsKey(DecodeHintType.NEED_RESULT_POINT_CALLBACK))
						{
							System.Collections.Hashtable newHints = System.Collections.Hashtable.Synchronized(new System.Collections.Hashtable()); // Can't use clone() in J2ME
							System.Collections.IEnumerator hintEnum = hints.Keys.GetEnumerator();
							//UPGRADE_TODO: Method 'java.util.Enumeration.hasMoreElements' was converted to 'System.Collections.IEnumerator.MoveNext' which has a different behavior. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1073_javautilEnumerationhasMoreElements'"
							while (hintEnum.MoveNext())
							{
								//UPGRADE_TODO: Method 'java.util.Enumeration.nextElement' was converted to 'System.Collections.IEnumerator.Current' which has a different behavior. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1073_javautilEnumerationnextElement'"
								System.Object key = hintEnum.Current;
								if (!key.Equals(DecodeHintType.NEED_RESULT_POINT_CALLBACK))
								{
									newHints[key] = hints[key];
								}
							}
							hints = newHints;
						}
					}
					try
					{
						// Look for a barcode
						Result result = decodeRow(rowNumber, row, hints);
						// We found our barcode
						if (attempt == 1)
						{
							// But it was upside down, so note that
							result.putMetadata(ResultMetadataType.ORIENTATION, (System.Object) 180);
							// And remember to flip the result points horizontally.
							ResultPoint[] points = result.ResultPoints;
							points[0] = new ResultPoint(width - points[0].X - 1, points[0].Y);
							points[1] = new ResultPoint(width - points[1].X - 1, points[1].Y);
						}
						return result;
					}
					catch (ReaderException)
					{
						// continue -- just couldn't decode this row
					}
				}
			}
			
			throw ReaderException.Instance;
		}
		
		/// <summary> Records the size of successive runs of white and black pixels in a row, starting at a given point.
		/// The values are recorded in the given array, and the number of runs recorded is equal to the size
		/// of the array. If the row starts on a white pixel at the given start point, then the first count
		/// recorded is the run of white pixels starting from that point; likewise it is the count of a run
		/// of black pixels if the row begin on a black pixels at that point.
		/// 
		/// </summary>
		/// <param name="row">row to count from
		/// </param>
		/// <param name="start">offset into row to start at
		/// </param>
		/// <param name="counters">array into which to record counts
		/// </param>
		/// <throws>  ReaderException if counters cannot be filled entirely from row before running out </throws>
		/// <summary>  of pixels
		/// </summary>
		internal static void  recordPattern(BitArray row, int start, int[] counters)
		{
			int numCounters = counters.Length;
			for (int i = 0; i < numCounters; i++)
			{
				counters[i] = 0;
			}
			int end = row.Size;
			if (start >= end)
			{
				throw ReaderException.Instance;
			}
			bool isWhite = !row.get_Renamed(start);
			int counterPosition = 0;
			int i2 = start;
			while (i2 < end)
			{
				bool pixel = row.get_Renamed(i2);
				if (pixel ^ isWhite)
				{
					// that is, exactly one is true
					counters[counterPosition]++;
				}
				else
				{
					counterPosition++;
					if (counterPosition == numCounters)
					{
						break;
					}
					else
					{
						counters[counterPosition] = 1;
						isWhite ^= true; // isWhite = !isWhite;
					}
				}
				i2++;
			}
			// If we read fully the last section of pixels and filled up our counters -- or filled
			// the last counter but ran off the side of the image, OK. Otherwise, a problem.
			if (!(counterPosition == numCounters || (counterPosition == numCounters - 1 && i2 == end)))
			{
				throw ReaderException.Instance;
			}
		}
		
		/// <summary> Determines how closely a set of observed counts of runs of black/white values matches a given
		/// target pattern. This is reported as the ratio of the total variance from the expected pattern
		/// proportions across all pattern elements, to the length of the pattern.
		/// 
		/// </summary>
		/// <param name="counters">observed counters
		/// </param>
		/// <param name="pattern">expected pattern
		/// </param>
		/// <param name="maxIndividualVariance">The most any counter can differ before we give up
		/// </param>
		/// <returns> ratio of total variance between counters and pattern compared to total pattern size,
		/// where the ratio has been multiplied by 256. So, 0 means no variance (perfect match); 256 means
		/// the total variance between counters and patterns equals the pattern length, higher values mean
		/// even more variance
		/// </returns>
		internal static int patternMatchVariance(int[] counters, int[] pattern, int maxIndividualVariance)
		{
			int numCounters = counters.Length;
			int total = 0;
			int patternLength = 0;
			for (int i = 0; i < numCounters; i++)
			{
				total += counters[i];
				patternLength += pattern[i];
			}
			if (total < patternLength)
			{
				// If we don't even have one pixel per unit of bar width, assume this is too small
				// to reliably match, so fail:
				return System.Int32.MaxValue;
			}
			// We're going to fake floating-point math in integers. We just need to use more bits.
			// Scale up patternLength so that intermediate values below like scaledCounter will have
			// more "significant digits"
			int unitBarWidth = (total << INTEGER_MATH_SHIFT) / patternLength;
			maxIndividualVariance = (maxIndividualVariance * unitBarWidth) >> INTEGER_MATH_SHIFT;
			
			int totalVariance = 0;
			for (int x = 0; x < numCounters; x++)
			{
				int counter = counters[x] << INTEGER_MATH_SHIFT;
				int scaledPattern = pattern[x] * unitBarWidth;
				int variance = counter > scaledPattern?counter - scaledPattern:scaledPattern - counter;
				if (variance > maxIndividualVariance)
				{
					return System.Int32.MaxValue;
				}
				totalVariance += variance;
			}
			return totalVariance / total;
		}
		
		/// <summary> <p>Attempts to decode a one-dimensional barcode format given a single row of
		/// an image.</p>
		/// 
		/// </summary>
		/// <param name="rowNumber">row number from top of the row
		/// </param>
		/// <param name="row">the black/white pixel data of the row
		/// </param>
		/// <param name="hints">decode hints
		/// </param>
		/// <returns> {@link Result} containing encoded string and start/end of barcode
		/// </returns>
		/// <throws>  ReaderException if an error occurs or barcode cannot be found </throws>
		public abstract Result decodeRow(int rowNumber, BitArray row, System.Collections.Hashtable hints);
	}
}
