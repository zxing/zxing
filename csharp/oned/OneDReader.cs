using System;
using System.Collections;
using System.Collections.Generic;

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

	using BinaryBitmap = com.google.zxing.BinaryBitmap;
	using ChecksumException = com.google.zxing.ChecksumException;
	using DecodeHintType = com.google.zxing.DecodeHintType;
	using FormatException = com.google.zxing.FormatException;
	using NotFoundException = com.google.zxing.NotFoundException;
	using Reader = com.google.zxing.Reader;
	using ReaderException = com.google.zxing.ReaderException;
	using Result = com.google.zxing.Result;
	using ResultMetadataType = com.google.zxing.ResultMetadataType;
	using ResultPoint = com.google.zxing.ResultPoint;
	using BitArray = com.google.zxing.common.BitArray;


	/// <summary>
	/// Encapsulates functionality and implementation that is common to all families
	/// of one-dimensional barcodes.
	/// 
	/// @author dswitkin@google.com (Daniel Switkin)
	/// @author Sean Owen
	/// </summary>
	public abstract class OneDReader : com.google.zxing.Reader
	{

	  protected internal const int INTEGER_MATH_SHIFT = 8;
	  protected internal static readonly int PATTERN_MATCH_RESULT_SCALE_FACTOR = 1 << INTEGER_MATH_SHIFT;

//JAVA TO C# CONVERTER WARNING: Method 'throws' clauses are not available in .NET:
//ORIGINAL LINE: public com.google.zxing.Result decode(com.google.zxing.BinaryBitmap image) throws com.google.zxing.NotFoundException, com.google.zxing.FormatException
	  public virtual Result decode(BinaryBitmap image)
	  {
		return decode(image, null);
	  }

	  // Note that we don't try rotation without the try harder flag, even if rotation was supported.
//JAVA TO C# CONVERTER WARNING: Method 'throws' clauses are not available in .NET:
//ORIGINAL LINE: public com.google.zxing.Result decode(com.google.zxing.BinaryBitmap image, java.util.Map<com.google.zxing.DecodeHintType,?> hints) throws com.google.zxing.NotFoundException, com.google.zxing.FormatException
      public virtual Result decode(BinaryBitmap image, IDictionary<DecodeHintType, object> hints)
	  {
		try
		{
		  return doDecode(image, hints);
		}
		catch (NotFoundException nfe)
		{
		  bool tryHarder = hints != null && hints.ContainsKey(DecodeHintType.TRY_HARDER);
		  if (tryHarder && image.RotateSupported)
		  {
			BinaryBitmap rotatedImage = image.rotateCounterClockwise();
			Result result = doDecode(rotatedImage, hints);
			// Record that we found it rotated 90 degrees CCW / 270 degrees CW
//JAVA TO C# CONVERTER TODO TASK: Java wildcard generics are not converted to .NET:
//ORIGINAL LINE: java.util.Map<com.google.zxing.ResultMetadataType,?> metadata = result.getResultMetadata();
			IDictionary<ResultMetadataType, object> metadata = result.ResultMetadata;
			int orientation = 270;
			if (metadata != null && metadata.ContainsKey(ResultMetadataType.ORIENTATION))
			{
			  // But if we found it reversed in doDecode(), add in that result here:
			  orientation = (orientation + (int) metadata[ResultMetadataType.ORIENTATION]) % 360;
			}
			result.putMetadata(ResultMetadataType.ORIENTATION, orientation);
			// Update result points
			ResultPoint[] points = result.ResultPoints;
			if (points != null)
			{
			  int height = rotatedImage.Height;
			  for (int i = 0; i < points.Length; i++)
			  {
				points[i] = new ResultPoint(height - points[i].Y - 1, points[i].X);
			  }
			}
			return result;
		  }
		  else
		  {
			throw nfe;
		  }
		}
	  }

	  public virtual void reset()
	  {
		// do nothing
	  }

	  /// <summary>
	  /// We're going to examine rows from the middle outward, searching alternately above and below the
	  /// middle, and farther out each time. rowStep is the number of rows between each successive
	  /// attempt above and below the middle. So we'd scan row middle, then middle - rowStep, then
	  /// middle + rowStep, then middle - (2 * rowStep), etc.
	  /// rowStep is bigger as the image is taller, but is always at least 1. We've somewhat arbitrarily
	  /// decided that moving up and down by about 1/16 of the image is pretty good; we try more of the
	  /// image if "trying harder".
	  /// </summary>
	  /// <param name="image"> The image to decode </param>
	  /// <param name="hints"> Any hints that were requested </param>
	  /// <returns> The contents of the decoded barcode </returns>
	  /// <exception cref="NotFoundException"> Any spontaneous errors which occur </exception>
//JAVA TO C# CONVERTER WARNING: Method 'throws' clauses are not available in .NET:
//ORIGINAL LINE: private com.google.zxing.Result doDecode(com.google.zxing.BinaryBitmap image, java.util.Map<com.google.zxing.DecodeHintType,?> hints) throws com.google.zxing.NotFoundException
      private Result doDecode(BinaryBitmap image, IDictionary<DecodeHintType, object> hints)
	  {
		int width = image.Width;
		int height = image.Height;
		BitArray row = new BitArray(width);

		int middle = height >> 1;
		bool tryHarder = hints != null && hints.ContainsKey(DecodeHintType.TRY_HARDER);
		int rowStep = Math.Max(1, height >> (tryHarder ? 8 : 5));
		int maxLines;
		if (tryHarder)
		{
		  maxLines = height; // Look at the whole image, not just the center
		}
		else
		{
		  maxLines = 15; // 15 rows spaced 1/32 apart is roughly the middle half of the image
		}

		for (int x = 0; x < maxLines; x++)
		{

		  // Scanning from the middle out. Determine which row we're looking at next:
		  int rowStepsAboveOrBelow = (x + 1) >> 1;
		  bool isAbove = (x & 0x01) == 0; // i.e. is x even?
		  int rowNumber = middle + rowStep * (isAbove ? rowStepsAboveOrBelow : -rowStepsAboveOrBelow);
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
		  catch (NotFoundException nfe)
		  {
			continue;
		  }

		  // While we have the image data in a BitArray, it's fairly cheap to reverse it in place to
		  // handle decoding upside down barcodes.
		  for (int attempt = 0; attempt < 2; attempt++)
		  {
			if (attempt == 1) // trying again?
			{
			  row.reverse(); // reverse the row and continue
			  // This means we will only ever draw result points *once* in the life of this method
			  // since we want to avoid drawing the wrong points after flipping the row, and,
			  // don't want to clutter with noise from every single row scan -- just the scans
			  // that start on the center line.
			  if (hints != null && hints.ContainsKey(DecodeHintType.NEED_RESULT_POINT_CALLBACK))
			  {
                //IDictionary<DecodeHintType, object> newHints = new EnumMap<DecodeHintType, object>(typeof(DecodeHintType));
			      Dictionary<DecodeHintType,object> newHints = new Dictionary<DecodeHintType,object>();
//JAVA TO C# CONVERTER TODO TASK: There is no .NET Dictionary equivalent to the Java 'putAll' method:
                //newHints.putAll(hints);
                //foreach (DecodeHintType dht in Enum.GetValues(typeof(DecodeHintType)))
                //{
                //    newHints.Add(dht,dht);
                //}
			    foreach (KeyValuePair<DecodeHintType,object> kvp in hints)
			    {
			        newHints.Add(kvp.Key,kvp.Value);
			    }

                if (newHints.ContainsKey(DecodeHintType.NEED_RESULT_POINT_CALLBACK))
			    {
			        newHints.Remove(DecodeHintType.NEED_RESULT_POINT_CALLBACK);
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
				result.putMetadata(ResultMetadataType.ORIENTATION, 180);
				// And remember to flip the result points horizontally.
				ResultPoint[] points = result.ResultPoints;
				if (points != null)
				{
				  points[0] = new ResultPoint(width - points[0].X - 1, points[0].Y);
				  points[1] = new ResultPoint(width - points[1].X - 1, points[1].Y);
				}
			  }
			  return result;
			}
			catch (ReaderException re)
			{
			  // continue -- just couldn't decode this row
			}
		  }
		}

		throw NotFoundException.NotFoundInstance;
	  }

	  /// <summary>
	  /// Records the size of successive runs of white and black pixels in a row, starting at a given point.
	  /// The values are recorded in the given array, and the number of runs recorded is equal to the size
	  /// of the array. If the row starts on a white pixel at the given start point, then the first count
	  /// recorded is the run of white pixels starting from that point; likewise it is the count of a run
	  /// of black pixels if the row begin on a black pixels at that point.
	  /// </summary>
	  /// <param name="row"> row to count from </param>
	  /// <param name="start"> offset into row to start at </param>
	  /// <param name="counters"> array into which to record counts </param>
	  /// <exception cref="NotFoundException"> if counters cannot be filled entirely from row before running out
	  ///  of pixels </exception>
//JAVA TO C# CONVERTER WARNING: Method 'throws' clauses are not available in .NET:
//ORIGINAL LINE: protected static void recordPattern(com.google.zxing.common.BitArray row, int start, int[] counters) throws com.google.zxing.NotFoundException
	  protected internal static void recordPattern(BitArray row, int start, int[] counters)
	  {
		int numCounters = counters.Length;
        //Arrays.fill(counters, 0, numCounters, 0);
        counters.Fill(0);
		int end = row.Size;
		if (start >= end)
		{
		  throw NotFoundException.NotFoundInstance;
		}
		bool isWhite = !row.get(start);
		int counterPosition = 0;
		int i = start;
		while (i < end)
		{
		  if (row.get(i) ^ isWhite) // that is, exactly one is true
		  {
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
			  isWhite = !isWhite;
			}
		  }
		  i++;
		}
		// If we read fully the last section of pixels and filled up our counters -- or filled
		// the last counter but ran off the side of the image, OK. Otherwise, a problem.
		if (!(counterPosition == numCounters || (counterPosition == numCounters - 1 && i == end)))
		{
		  throw NotFoundException.NotFoundInstance;
		}
	  }

//JAVA TO C# CONVERTER WARNING: Method 'throws' clauses are not available in .NET:
//ORIGINAL LINE: protected static void recordPatternInReverse(com.google.zxing.common.BitArray row, int start, int[] counters) throws com.google.zxing.NotFoundException
	  protected internal static void recordPatternInReverse(BitArray row, int start, int[] counters)
	  {
		// This could be more efficient I guess
		int numTransitionsLeft = counters.Length;
		bool last = row.get(start);
		while (start > 0 && numTransitionsLeft >= 0)
		{
		  if (row.get(--start) != last)
		  {
			numTransitionsLeft--;
			last = !last;
		  }
		}
		if (numTransitionsLeft >= 0)
		{
		  throw NotFoundException.NotFoundInstance;
		}
		recordPattern(row, start + 1, counters);
	  }

	  /// <summary>
	  /// Determines how closely a set of observed counts of runs of black/white values matches a given
	  /// target pattern. This is reported as the ratio of the total variance from the expected pattern
	  /// proportions across all pattern elements, to the length of the pattern.
	  /// </summary>
	  /// <param name="counters"> observed counters </param>
	  /// <param name="pattern"> expected pattern </param>
	  /// <param name="maxIndividualVariance"> The most any counter can differ before we give up </param>
	  /// <returns> ratio of total variance between counters and pattern compared to total pattern size,
	  ///  where the ratio has been multiplied by 256. So, 0 means no variance (perfect match); 256 means
	  ///  the total variance between counters and patterns equals the pattern length, higher values mean
	  ///  even more variance </returns>
	  protected internal static int patternMatchVariance(int[] counters, int[] pattern, int maxIndividualVariance)
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
		  return int.MaxValue;
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
		  int variance = counter > scaledPattern ? counter - scaledPattern : scaledPattern - counter;
		  if (variance > maxIndividualVariance)
		  {
			return int.MaxValue;
		  }
		  totalVariance += variance;
		}
		return totalVariance / total;
	  }

	  /// <summary>
	  /// <p>Attempts to decode a one-dimensional barcode format given a single row of
	  /// an image.</p>
	  /// </summary>
	  /// <param name="rowNumber"> row number from top of the row </param>
	  /// <param name="row"> the black/white pixel data of the row </param>
	  /// <param name="hints"> decode hints </param>
	  /// <returns> <seealso cref="Result"/> containing encoded string and start/end of barcode </returns>
	  /// <exception cref="NotFoundException"> if an error occurs or barcode cannot be found </exception>
//JAVA TO C# CONVERTER WARNING: Method 'throws' clauses are not available in .NET:
//ORIGINAL LINE: public abstract com.google.zxing.Result decodeRow(int rowNumber, com.google.zxing.common.BitArray row, java.util.Map<com.google.zxing.DecodeHintType,?> hints) throws com.google.zxing.NotFoundException, com.google.zxing.ChecksumException, com.google.zxing.FormatException;
      public abstract Result decodeRow(int rowNumber, BitArray row, IDictionary<DecodeHintType, object> hints);
	}

}