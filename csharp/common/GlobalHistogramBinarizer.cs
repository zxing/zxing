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
using System;
using Binarizer = com.google.zxing.Binarizer;
using LuminanceSource = com.google.zxing.LuminanceSource;
using ReaderException = com.google.zxing.ReaderException;
namespace com.google.zxing.common
{
	
	/// <summary> This Binarizer implementation uses the old ZXing global histogram approach. It is suitable
	/// for low-end mobile devices which don't have enough CPU or memory to use a local thresholding
	/// algorithm. However, because it picks a global black point, it cannot handle difficult shadows
	/// and gradients.
	/// 
	/// Faster mobile devices and all desktop applications should probably use HybridBinarizer instead.
	/// 
	/// </summary>
	/// <author>  dswitkin@google.com (Daniel Switkin)
	/// </author>
	/// <author>  Sean Owen
	/// </author>
	/// <author>www.Redivivus.in (suraj.supekar@redivivus.in) - Ported from ZXING Java Source 
	/// </author>
	public class GlobalHistogramBinarizer:Binarizer
	{
		override public BitMatrix BlackMatrix
		{
			// Does not sharpen the data, as this call is intended to only be used by 2D Readers.
			
			get
			{
				LuminanceSource source = LuminanceSource;
                // Redivivus.in Java to c# Porting update
                // 30/01/2010 
                // Added
                // START
                sbyte[] localLuminances;
				//END
                
                int width = source.Width;
				int height = source.Height;
				BitMatrix matrix = new BitMatrix(width, height);
				
				// Quickly calculates the histogram by sampling four rows from the image. This proved to be
				// more robust on the blackbox tests than sampling a diagonal as we used to do.
				initArrays(width);
				int[] localBuckets = buckets;
				for (int y = 1; y < 5; y++)
				{
					int row = height * y / 5;
                    // Redivivus.in Java to c# Porting update
                    // 30/01/2010 
                    // Commented & Added
                    // START
                    //sbyte[] localLuminances = source.getRow(row, luminances);
                    localLuminances = source.getRow(row, luminances);
                    // END
					int right = (width << 2) / 5;
					for (int x = width / 5; x < right; x++)
					{
						int pixel = localLuminances[x] & 0xff;
						localBuckets[pixel >> LUMINANCE_SHIFT]++;
					}
				}
				int blackPoint = estimateBlackPoint(localBuckets);
				
				// We delay reading the entire image luminance until the black point estimation succeeds.
				// Although we end up reading four rows twice, it is consistent with our motto of
				// "fail quickly" which is necessary for continuous scanning.

				localLuminances = source.Matrix; // Govinda : Removed sbyte []
				for (int y = 0; y < height; y++)
				{
					int offset = y * width;
					for (int x = 0; x < width; x++)
					{
						int pixel = localLuminances[offset + x] & 0xff;
						if (pixel < blackPoint)
						{
							matrix.set_Renamed(x, y);
						}
					}
				}
				
				return matrix;
			}
			
		}
		
		private const int LUMINANCE_BITS = 5;
		//UPGRADE_NOTE: Final was removed from the declaration of 'LUMINANCE_SHIFT '. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
		private static readonly int LUMINANCE_SHIFT = 8 - LUMINANCE_BITS;
		//UPGRADE_NOTE: Final was removed from the declaration of 'LUMINANCE_BUCKETS '. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
		private static readonly int LUMINANCE_BUCKETS = 1 << LUMINANCE_BITS;
		
		private sbyte[] luminances = null;
		private int[] buckets = null;
		
		public GlobalHistogramBinarizer(LuminanceSource source):base(source)
		{
		}
		
		// Applies simple sharpening to the row data to improve performance of the 1D Readers.
		public override BitArray getBlackRow(int y, BitArray row)
		{
			LuminanceSource source = LuminanceSource;
			int width = source.Width;
			if (row == null || row.Size < width)
			{
				row = new BitArray(width);
			}
			else
			{
				row.clear();
			}
			
			initArrays(width);
			sbyte[] localLuminances = source.getRow(y, luminances);
			int[] localBuckets = buckets;
			for (int x = 0; x < width; x++)
			{
				int pixel = localLuminances[x] & 0xff;
				localBuckets[pixel >> LUMINANCE_SHIFT]++;
			}
			int blackPoint = estimateBlackPoint(localBuckets);
			
			int left = localLuminances[0] & 0xff;
			int center = localLuminances[1] & 0xff;
			for (int x = 1; x < width - 1; x++)
			{
				int right = localLuminances[x + 1] & 0xff;
				// A simple -1 4 -1 box filter with a weight of 2.
				int luminance = ((center << 2) - left - right) >> 1;
				if (luminance < blackPoint)
				{
					row.set_Renamed(x);
				}
				left = center;
				center = right;
			}
			return row;
		}
		
		public override Binarizer createBinarizer(LuminanceSource source)
		{
			return new GlobalHistogramBinarizer(source);
		}
		
		private void  initArrays(int luminanceSize)
		{
			if (luminances == null || luminances.Length < luminanceSize)
			{
				luminances = new sbyte[luminanceSize];
			}
			if (buckets == null)
			{
				buckets = new int[LUMINANCE_BUCKETS];
			}
			else
			{
				for (int x = 0; x < LUMINANCE_BUCKETS; x++)
				{
					buckets[x] = 0;
				}
			}
		}
		
		private static int estimateBlackPoint(int[] buckets)
		{
			// Find the tallest peak in the histogram.
			int numBuckets = buckets.Length;
			int maxBucketCount = 0;
			int firstPeak = 0;
			int firstPeakSize = 0;
			for (int x = 0; x < numBuckets; x++)
			{
				if (buckets[x] > firstPeakSize)
				{
					firstPeak = x;
					firstPeakSize = buckets[x];
				}
				if (buckets[x] > maxBucketCount)
				{
					maxBucketCount = buckets[x];
				}
			}
			
			// Find the second-tallest peak which is somewhat far from the tallest peak.
			int secondPeak = 0;
			int secondPeakScore = 0;
			for (int x = 0; x < numBuckets; x++)
			{
				int distanceToBiggest = x - firstPeak;
				// Encourage more distant second peaks by multiplying by square of distance.
				int score = buckets[x] * distanceToBiggest * distanceToBiggest;
				if (score > secondPeakScore)
				{
					secondPeak = x;
					secondPeakScore = score;
				}
			}
			
			// Make sure firstPeak corresponds to the black peak.
			if (firstPeak > secondPeak)
			{
				int temp = firstPeak;
				firstPeak = secondPeak;
				secondPeak = temp;
			}
			
			// If there is too little contrast in the image to pick a meaningful black point, throw rather
			// than waste time trying to decode the image, and risk false positives.
			// TODO: It might be worth comparing the brightest and darkest pixels seen, rather than the
			// two peaks, to determine the contrast.
			if (secondPeak - firstPeak <= numBuckets >> 4)
			{
				throw ReaderException.Instance;
			}
			
			// Find a valley between them that is low and closer to the white peak.
			int bestValley = secondPeak - 1;
			int bestValleyScore = - 1;
			for (int x = secondPeak - 1; x > firstPeak; x--)
			{
				int fromFirst = x - firstPeak;
				int score = fromFirst * fromFirst * (secondPeak - x) * (maxBucketCount - buckets[x]);
				if (score > bestValleyScore)
				{
					bestValley = x;
					bestValleyScore = score;
				}
			}
			
			return bestValley << LUMINANCE_SHIFT;
		}
	}
}