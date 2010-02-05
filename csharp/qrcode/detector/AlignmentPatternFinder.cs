/*
* Copyright 2007 ZXing authors
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
using ReaderException = com.google.zxing.ReaderException;
using ResultPoint = com.google.zxing.ResultPoint;
using ResultPointCallback = com.google.zxing.ResultPointCallback;
using BitMatrix = com.google.zxing.common.BitMatrix;
namespace com.google.zxing.qrcode.detector
{
	
	/// <summary> <p>This class attempts to find alignment patterns in a QR Code. Alignment patterns look like finder
	/// patterns but are smaller and appear at regular intervals throughout the image.</p>
	/// 
	/// <p>At the moment this only looks for the bottom-right alignment pattern.</p>
	/// 
	/// <p>This is mostly a simplified copy of {@link FinderPatternFinder}. It is copied,
	/// pasted and stripped down here for maximum performance but does unfortunately duplicate
	/// some code.</p>
	/// 
	/// <p>This class is thread-safe but not reentrant. Each thread must allocate its own object.
	/// 
	/// </summary>
	/// <author>  Sean Owen
	/// </author>
	/// <author>www.Redivivus.in (suraj.supekar@redivivus.in) - Ported from ZXING Java Source 
	/// </author>
	sealed class AlignmentPatternFinder
	{
		
		//UPGRADE_NOTE: Final was removed from the declaration of 'image '. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
		private BitMatrix image;
		//UPGRADE_NOTE: Final was removed from the declaration of 'possibleCenters '. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
		private System.Collections.ArrayList possibleCenters;
		//UPGRADE_NOTE: Final was removed from the declaration of 'startX '. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
		private int startX;
		//UPGRADE_NOTE: Final was removed from the declaration of 'startY '. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
		private int startY;
		//UPGRADE_NOTE: Final was removed from the declaration of 'width '. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
		private int width;
		//UPGRADE_NOTE: Final was removed from the declaration of 'height '. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
		private int height;
		//UPGRADE_NOTE: Final was removed from the declaration of 'moduleSize '. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
		private float moduleSize;
		//UPGRADE_NOTE: Final was removed from the declaration of 'crossCheckStateCount '. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
		private int[] crossCheckStateCount;
		//UPGRADE_NOTE: Final was removed from the declaration of 'resultPointCallback '. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
		private ResultPointCallback resultPointCallback;
		
		/// <summary> <p>Creates a finder that will look in a portion of the whole image.</p>
		/// 
		/// </summary>
		/// <param name="image">image to search
		/// </param>
		/// <param name="startX">left column from which to start searching
		/// </param>
		/// <param name="startY">top row from which to start searching
		/// </param>
		/// <param name="width">width of region to search
		/// </param>
		/// <param name="height">height of region to search
		/// </param>
		/// <param name="moduleSize">estimated module size so far
		/// </param>
		internal AlignmentPatternFinder(BitMatrix image, int startX, int startY, int width, int height, float moduleSize, ResultPointCallback resultPointCallback)
		{
			this.image = image;
			this.possibleCenters = System.Collections.ArrayList.Synchronized(new System.Collections.ArrayList(5));
			this.startX = startX;
			this.startY = startY;
			this.width = width;
			this.height = height;
			this.moduleSize = moduleSize;
			this.crossCheckStateCount = new int[3];
			this.resultPointCallback = resultPointCallback;
		}
		
		/// <summary> <p>This method attempts to find the bottom-right alignment pattern in the image. It is a bit messy since
		/// it's pretty performance-critical and so is written to be fast foremost.</p>
		/// 
		/// </summary>
		/// <returns> {@link AlignmentPattern} if found
		/// </returns>
		/// <throws>  ReaderException if not found </throws>
		internal AlignmentPattern find()
		{
			int startX = this.startX;
			int height = this.height;
			int maxJ = startX + width;
			int middleI = startY + (height >> 1);
			// We are looking for black/white/black modules in 1:1:1 ratio;
			// this tracks the number of black/white/black modules seen so far
			int[] stateCount = new int[3];
			for (int iGen = 0; iGen < height; iGen++)
			{
				// Search from middle outwards
				int i = middleI + ((iGen & 0x01) == 0?((iGen + 1) >> 1):- ((iGen + 1) >> 1));
				stateCount[0] = 0;
				stateCount[1] = 0;
				stateCount[2] = 0;
				int j = startX;
				// Burn off leading white pixels before anything else; if we start in the middle of
				// a white run, it doesn't make sense to count its length, since we don't know if the
				// white run continued to the left of the start point
				while (j < maxJ && !image.get_Renamed(j, i))
				{
					j++;
				}
				int currentState = 0;
				while (j < maxJ)
				{
					if (image.get_Renamed(j, i))
					{
						// Black pixel
						if (currentState == 1)
						{
							// Counting black pixels
							stateCount[currentState]++;
						}
						else
						{
							// Counting white pixels
							if (currentState == 2)
							{
								// A winner?
								if (foundPatternCross(stateCount))
								{
									// Yes
									AlignmentPattern confirmed = handlePossibleCenter(stateCount, i, j);
									if (confirmed != null)
									{
										return confirmed;
									}
								}
								stateCount[0] = stateCount[2];
								stateCount[1] = 1;
								stateCount[2] = 0;
								currentState = 1;
							}
							else
							{
								stateCount[++currentState]++;
							}
						}
					}
					else
					{
						// White pixel
						if (currentState == 1)
						{
							// Counting black pixels
							currentState++;
						}
						stateCount[currentState]++;
					}
					j++;
				}
				if (foundPatternCross(stateCount))
				{
					AlignmentPattern confirmed = handlePossibleCenter(stateCount, i, maxJ);
					if (confirmed != null)
					{
						return confirmed;
					}
				}
			}
			
			// Hmm, nothing we saw was observed and confirmed twice. If we had
			// any guess at all, return it.
			if (!(possibleCenters.Count == 0))
			{
				return (AlignmentPattern) possibleCenters[0];
			}
			
			throw ReaderException.Instance;
		}
		
		/// <summary> Given a count of black/white/black pixels just seen and an end position,
		/// figures the location of the center of this black/white/black run.
		/// </summary>
		private static float centerFromEnd(int[] stateCount, int end)
		{
			//UPGRADE_WARNING: Data types in Visual C# might be different.  Verify the accuracy of narrowing conversions. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1042'"
			return (float) (end - stateCount[2]) - stateCount[1] / 2.0f;
		}
		
		/// <param name="stateCount">count of black/white/black pixels just read
		/// </param>
		/// <returns> true iff the proportions of the counts is close enough to the 1/1/1 ratios
		/// used by alignment patterns to be considered a match
		/// </returns>
		private bool foundPatternCross(int[] stateCount)
		{
			float moduleSize = this.moduleSize;
			float maxVariance = moduleSize / 2.0f;
			for (int i = 0; i < 3; i++)
			{
				if (System.Math.Abs(moduleSize - stateCount[i]) >= maxVariance)
				{
					return false;
				}
			}
			return true;
		}
		
		/// <summary> <p>After a horizontal scan finds a potential alignment pattern, this method
		/// "cross-checks" by scanning down vertically through the center of the possible
		/// alignment pattern to see if the same proportion is detected.</p>
		/// 
		/// </summary>
		/// <param name="startI">row where an alignment pattern was detected
		/// </param>
		/// <param name="centerJ">center of the section that appears to cross an alignment pattern
		/// </param>
		/// <param name="maxCount">maximum reasonable number of modules that should be
		/// observed in any reading state, based on the results of the horizontal scan
		/// </param>
		/// <returns> vertical center of alignment pattern, or {@link Float#NaN} if not found
		/// </returns>
		private float crossCheckVertical(int startI, int centerJ, int maxCount, int originalStateCountTotal)
		{
			BitMatrix image = this.image;
			
			int maxI = image.Height;
			int[] stateCount = crossCheckStateCount;
			stateCount[0] = 0;
			stateCount[1] = 0;
			stateCount[2] = 0;
			
			// Start counting up from center
			int i = startI;
			while (i >= 0 && image.get_Renamed(centerJ, i) && stateCount[1] <= maxCount)
			{
				stateCount[1]++;
				i--;
			}
			// If already too many modules in this state or ran off the edge:
			if (i < 0 || stateCount[1] > maxCount)
			{
				return System.Single.NaN;
			}
			while (i >= 0 && !image.get_Renamed(centerJ, i) && stateCount[0] <= maxCount)
			{
				stateCount[0]++;
				i--;
			}
			if (stateCount[0] > maxCount)
			{
				return System.Single.NaN;
			}
			
			// Now also count down from center
			i = startI + 1;
			while (i < maxI && image.get_Renamed(centerJ, i) && stateCount[1] <= maxCount)
			{
				stateCount[1]++;
				i++;
			}
			if (i == maxI || stateCount[1] > maxCount)
			{
				return System.Single.NaN;
			}
			while (i < maxI && !image.get_Renamed(centerJ, i) && stateCount[2] <= maxCount)
			{
				stateCount[2]++;
				i++;
			}
			if (stateCount[2] > maxCount)
			{
				return System.Single.NaN;
			}
			
			int stateCountTotal = stateCount[0] + stateCount[1] + stateCount[2];
			if (5 * System.Math.Abs(stateCountTotal - originalStateCountTotal) >= 2 * originalStateCountTotal)
			{
				return System.Single.NaN;
			}
			
			return foundPatternCross(stateCount)?centerFromEnd(stateCount, i):System.Single.NaN;
		}
		
		/// <summary> <p>This is called when a horizontal scan finds a possible alignment pattern. It will
		/// cross check with a vertical scan, and if successful, will see if this pattern had been
		/// found on a previous horizontal scan. If so, we consider it confirmed and conclude we have
		/// found the alignment pattern.</p>
		/// 
		/// </summary>
		/// <param name="stateCount">reading state module counts from horizontal scan
		/// </param>
		/// <param name="i">row where alignment pattern may be found
		/// </param>
		/// <param name="j">end of possible alignment pattern in row
		/// </param>
		/// <returns> {@link AlignmentPattern} if we have found the same pattern twice, or null if not
		/// </returns>
		private AlignmentPattern handlePossibleCenter(int[] stateCount, int i, int j)
		{
			int stateCountTotal = stateCount[0] + stateCount[1] + stateCount[2];
			float centerJ = centerFromEnd(stateCount, j);
			//UPGRADE_WARNING: Data types in Visual C# might be different.  Verify the accuracy of narrowing conversions. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1042'"
			float centerI = crossCheckVertical(i, (int) centerJ, 2 * stateCount[1], stateCountTotal);
			if (!System.Single.IsNaN(centerI))
			{
				//UPGRADE_WARNING: Data types in Visual C# might be different.  Verify the accuracy of narrowing conversions. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1042'"
				float estimatedModuleSize = (float) (stateCount[0] + stateCount[1] + stateCount[2]) / 3.0f;
				int max = possibleCenters.Count;
				for (int index = 0; index < max; index++)
				{
					AlignmentPattern center = (AlignmentPattern) possibleCenters[index];
					// Look for about the same center and module size:
					if (center.aboutEquals(estimatedModuleSize, centerI, centerJ))
					{
						return new AlignmentPattern(centerJ, centerI, estimatedModuleSize);
					}
				}
				// Hadn't found this before; save it
				ResultPoint point = new AlignmentPattern(centerJ, centerI, estimatedModuleSize);
				possibleCenters.Add(point);
				if (resultPointCallback != null)
				{
					resultPointCallback.foundPossibleResultPoint(point);
				}
			}
			return null;
		}
	}
}