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
	
	/// <summary> This class implements a local thresholding algorithm, which while slower than the
	/// GlobalHistogramBinarizer, is fairly efficient for what it does. It is designed for
	/// high frequency images of barcodes with black data on white backgrounds. For this application,
	/// it does a much better job than a global blackpoint with severe shadows and gradients.
	/// However it tends to produce artifacts on lower frequency images and is therefore not
	/// a good general purpose binarizer for uses outside ZXing.
	/// 
	/// This class extends GlobalHistogramBinarizer, using the older histogram approach for 1D readers,
	/// and the newer local approach for 2D readers. 1D decoding using a per-row histogram is already
	/// inherently local, and only fails for horizontal gradients. We can revisit that problem later,
	/// but for now it was not a win to use local blocks for 1D.
	/// 
	/// This Binarizer is the default for the unit tests and the recommended class for library users.
	/// 
	/// </summary>
	/// <author>  dswitkin@google.com (Daniel Switkin)
	/// </author>
	/// <author>www.Redivivus.in (suraj.supekar@redivivus.in) - Ported from ZXING Java Source 
	/// </author>
	public sealed class HybridBinarizer:GlobalHistogramBinarizer
	{
		override public BitMatrix BlackMatrix
		{
			get
			{
				binarizeEntireImage();
				return matrix;
			}
			
		}
		
		// This class uses 5x5 blocks to compute local luminance, where each block is 8x8 pixels.
		// So this is the smallest dimension in each axis we can accept.
		private const int MINIMUM_DIMENSION = 40;
		
		private BitMatrix matrix = null;
		
		public HybridBinarizer(LuminanceSource source):base(source)
		{
		}
		
		public override Binarizer createBinarizer(LuminanceSource source)
		{
			return new HybridBinarizer(source);
		}
		
		// Calculates the final BitMatrix once for all requests. This could be called once from the
		// constructor instead, but there are some advantages to doing it lazily, such as making
		// profiling easier, and not doing heavy lifting when callers don't expect it.
		private void  binarizeEntireImage()
		{
			if (matrix == null)
			{
				LuminanceSource source = LuminanceSource;
				if (source.Width >= MINIMUM_DIMENSION && source.Height >= MINIMUM_DIMENSION)
				{
					sbyte[] luminances = source.Matrix;
					int width = source.Width;
					int height = source.Height;
					int subWidth = width >> 3;
					int subHeight = height >> 3;
					int[][] blackPoints = calculateBlackPoints(luminances, subWidth, subHeight, width);
					
					matrix = new BitMatrix(width, height);
					calculateThresholdForBlock(luminances, subWidth, subHeight, width, blackPoints, matrix);
				}
				else
				{
					// If the image is too small, fall back to the global histogram approach.
					matrix = base.BlackMatrix;
				}
			}
		}
		
		// For each 8x8 block in the image, calculate the average black point using a 5x5 grid
		// of the blocks around it. Also handles the corner cases, but will ignore up to 7 pixels
		// on the right edge and 7 pixels at the bottom of the image if the overall dimensions are not
		// multiples of eight. In practice, leaving those pixels white does not seem to be a problem.
		private static void  calculateThresholdForBlock(sbyte[] luminances, int subWidth, int subHeight, int stride, int[][] blackPoints, BitMatrix matrix)
		{
			for (int y = 0; y < subHeight; y++)
			{
				for (int x = 0; x < subWidth; x++)
				{
					int left = (x > 1)?x:2;
					left = (left < subWidth - 2)?left:subWidth - 3;
					int top = (y > 1)?y:2;
					top = (top < subHeight - 2)?top:subHeight - 3;
					int sum = 0;
					for (int z = - 2; z <= 2; z++)
					{
						int[] blackRow = blackPoints[top + z];
						sum += blackRow[left - 2];
						sum += blackRow[left - 1];
						sum += blackRow[left];
						sum += blackRow[left + 1];
						sum += blackRow[left + 2];
					}
					int average = sum / 25;
					threshold8x8Block(luminances, x << 3, y << 3, average, stride, matrix);
				}
			}
		}
		
		// Applies a single threshold to an 8x8 block of pixels.
		private static void  threshold8x8Block(sbyte[] luminances, int xoffset, int yoffset, int threshold, int stride, BitMatrix matrix)
		{
			for (int y = 0; y < 8; y++)
			{
				int offset = (yoffset + y) * stride + xoffset;
				for (int x = 0; x < 8; x++)
				{
					int pixel = luminances[offset + x] & 0xff;
					if (pixel < threshold)
					{
						matrix.set_Renamed(xoffset + x, yoffset + y);
					}
				}
			}
		}
		
		// Calculates a single black point for each 8x8 block of pixels and saves it away.
		private static int[][] calculateBlackPoints(sbyte[] luminances, int subWidth, int subHeight, int stride)
		{
			int[][] blackPoints = new int[subHeight][];
			for (int i = 0; i < subHeight; i++)
			{
				blackPoints[i] = new int[subWidth];
			}
			for (int y = 0; y < subHeight; y++)
			{
				for (int x = 0; x < subWidth; x++)
				{
					int sum = 0;
					int min = 255;
					int max = 0;
					for (int yy = 0; yy < 8; yy++)
					{
						int offset = ((y << 3) + yy) * stride + (x << 3);
						for (int xx = 0; xx < 8; xx++)
						{
							int pixel = luminances[offset + xx] & 0xff;
							sum += pixel;
							if (pixel < min)
							{
								min = pixel;
							}
							if (pixel > max)
							{
								max = pixel;
							}
						}
					}
					
					// If the contrast is inadequate, use half the minimum, so that this block will be
					// treated as part of the white background, but won't drag down neighboring blocks
					// too much.
					int average = (max - min > 24)?(sum >> 6):(min >> 1);
					blackPoints[y][x] = average;
				}
			}
			return blackPoints;
		}
	}
}