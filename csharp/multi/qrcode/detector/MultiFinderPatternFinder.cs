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
using DecodeHintType = com.google.zxing.DecodeHintType;
using ReaderException = com.google.zxing.ReaderException;
using ResultPoint = com.google.zxing.ResultPoint;
using ResultPointCallback = com.google.zxing.ResultPointCallback;
using Collections = com.google.zxing.common.Collections;
using Comparator = com.google.zxing.common.Comparator;
using BitMatrix = com.google.zxing.common.BitMatrix;
using FinderPattern = com.google.zxing.qrcode.detector.FinderPattern;
using FinderPatternFinder = com.google.zxing.qrcode.detector.FinderPatternFinder;
using FinderPatternInfo = com.google.zxing.qrcode.detector.FinderPatternInfo;
namespace com.google.zxing.multi.qrcode.detector
{
	
	/// <summary> <p>This class attempts to find finder patterns in a QR Code. Finder patterns are the square
	/// markers at three corners of a QR Code.</p>
	/// 
	/// <p>This class is thread-safe but not reentrant. Each thread must allocate its own object.
	/// 
	/// <p>In contrast to {@link FinderPatternFinder}, this class will return an array of all possible
	/// QR code locations in the image.</p>
	/// 
	/// <p>Use the TRY_HARDER hint to ask for a more thorough detection.</p>
	/// 
	/// </summary>
	/// <author>  Sean Owen
	/// </author>
	/// <author>  Hannes Erven
	/// </author>
	/// <author>www.Redivivus.in (suraj.supekar@redivivus.in) - Ported from ZXING Java Source 
	/// </author>


	sealed class MultiFinderPatternFinder:FinderPatternFinder
	{
		
		//UPGRADE_NOTE: Final was removed from the declaration of 'EMPTY_RESULT_ARRAY '. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
		private static readonly FinderPatternInfo[] EMPTY_RESULT_ARRAY = new FinderPatternInfo[0];
		
		// TODO MIN_MODULE_COUNT and MAX_MODULE_COUNT would be great hints to ask the user for
		// since it limits the number of regions to decode
		
		// max. legal count of modules per QR code edge (177)
		private const float MAX_MODULE_COUNT_PER_EDGE = 180;
		// min. legal count per modules per QR code edge (11)
		private const float MIN_MODULE_COUNT_PER_EDGE = 9;
		
		/// <summary> More or less arbitrary cutoff point for determining if two finder patterns might belong
		/// to the same code if they differ less than DIFF_MODSIZE_CUTOFF_PERCENT percent in their
		/// estimated modules sizes.
		/// </summary>
		private const float DIFF_MODSIZE_CUTOFF_PERCENT = 0.05f;
		
		/// <summary> More or less arbitrary cutoff point for determining if two finder patterns might belong
		/// to the same code if they differ less than DIFF_MODSIZE_CUTOFF pixels/module in their
		/// estimated modules sizes.
		/// </summary>
		private const float DIFF_MODSIZE_CUTOFF = 0.5f;
		
		
		/// <summary> A comparator that orders FinderPatterns by their estimated module size.</summary>
		private class ModuleSizeComparator : Comparator
		{
			public int compare(System.Object center1, System.Object center2)
			{
				float value_Renamed = ((FinderPattern) center2).EstimatedModuleSize - ((FinderPattern) center1).EstimatedModuleSize;
				return value_Renamed < 0.0?- 1:(value_Renamed > 0.0?1:0);
			}
		}
		
		/// <summary> <p>Creates a finder that will search the image for three finder patterns.</p>
		/// 
		/// </summary>
		/// <param name="image">image to search
		/// </param>
		internal MultiFinderPatternFinder(BitMatrix image):base(image)
		{
		}
		
		internal MultiFinderPatternFinder(BitMatrix image, ResultPointCallback resultPointCallback):base(image, resultPointCallback)
		{
		}
		
		/// <returns> the 3 best {@link FinderPattern}s from our list of candidates. The "best" are
		/// those that have been detected at least {@link #CENTER_QUORUM} times, and whose module
		/// size differs from the average among those patterns the least
		/// </returns>
		/// <throws>  ReaderException if 3 such finder patterns do not exist </throws>
		private FinderPattern[][] selectBestPatterns()
		{
			System.Collections.ArrayList possibleCenters = PossibleCenters;
			int size = possibleCenters.Count;
			
			if (size < 3)
			{
				// Couldn't find enough finder patterns
				throw ReaderException.Instance;
			}
			
			/*
			* Begin HE modifications to safely detect multiple codes of equal size
			*/
			if (size == 3)
			{
				return new FinderPattern[][]{new FinderPattern[]{(FinderPattern) possibleCenters[0], (FinderPattern) possibleCenters[1], (FinderPattern) possibleCenters[2]}};
			}
			
			// Sort by estimated module size to speed up the upcoming checks
			Collections.insertionSort(possibleCenters, new ModuleSizeComparator());
			
			/*
			* Now lets start: build a list of tuples of three finder locations that
			*  - feature similar module sizes
			*  - are placed in a distance so the estimated module count is within the QR specification
			*  - have similar distance between upper left/right and left top/bottom finder patterns
			*  - form a triangle with 90° angle (checked by comparing top right/bottom left distance
			*    with pythagoras)
			*
			* Note: we allow each point to be used for more than one code region: this might seem
			* counterintuitive at first, but the performance penalty is not that big. At this point,
			* we cannot make a good quality decision whether the three finders actually represent
			* a QR code, or are just by chance layouted so it looks like there might be a QR code there.
			* So, if the layout seems right, lets have the decoder try to decode.     
			*/
			
			System.Collections.ArrayList results = System.Collections.ArrayList.Synchronized(new System.Collections.ArrayList(10)); // holder for the results
			
			for (int i1 = 0; i1 < (size - 2); i1++)
			{
				FinderPattern p1 = (FinderPattern) possibleCenters[i1];
				if (p1 == null)
				{
					continue;
				}
				
				for (int i2 = i1 + 1; i2 < (size - 1); i2++)
				{
					FinderPattern p2 = (FinderPattern) possibleCenters[i2];
					if (p2 == null)
					{
						continue;
					}
					
					// Compare the expected module sizes; if they are really off, skip
					float vModSize12 = (p1.EstimatedModuleSize - p2.EstimatedModuleSize) / (System.Math.Min(p1.EstimatedModuleSize, p2.EstimatedModuleSize));
					float vModSize12A = System.Math.Abs(p1.EstimatedModuleSize - p2.EstimatedModuleSize);
					if (vModSize12A > DIFF_MODSIZE_CUTOFF && vModSize12 >= DIFF_MODSIZE_CUTOFF_PERCENT)
					{
						// break, since elements are ordered by the module size deviation there cannot be
						// any more interesting elements for the given p1.
						break;
					}
					
					for (int i3 = i2 + 1; i3 < size; i3++)
					{
						FinderPattern p3 = (FinderPattern) possibleCenters[i3];
						if (p3 == null)
						{
							continue;
						}
						
						// Compare the expected module sizes; if they are really off, skip
						float vModSize23 = (p2.EstimatedModuleSize - p3.EstimatedModuleSize) / (System.Math.Min(p2.EstimatedModuleSize, p3.EstimatedModuleSize));
						float vModSize23A = System.Math.Abs(p2.EstimatedModuleSize - p3.EstimatedModuleSize);
						if (vModSize23A > DIFF_MODSIZE_CUTOFF && vModSize23 >= DIFF_MODSIZE_CUTOFF_PERCENT)
						{
							// break, since elements are ordered by the module size deviation there cannot be
							// any more interesting elements for the given p1.
							break;
						}
						
						FinderPattern[] test = new FinderPattern[]{p1, p2, p3};
						ResultPoint.orderBestPatterns(test);
						
						// Calculate the distances: a = topleft-bottomleft, b=topleft-topright, c = diagonal
						FinderPatternInfo info = new FinderPatternInfo(test);
						float dA = ResultPoint.distance(info.TopLeft, info.BottomLeft);
						float dC = ResultPoint.distance(info.TopRight, info.BottomLeft);
						float dB = ResultPoint.distance(info.TopLeft, info.TopRight);
						
						// Check the sizes
						float estimatedModuleCount = ((dA + dB) / p1.EstimatedModuleSize) / 2;
						if (estimatedModuleCount > MAX_MODULE_COUNT_PER_EDGE || estimatedModuleCount < MIN_MODULE_COUNT_PER_EDGE)
						{
							continue;
						}
						
						// Calculate the difference of the edge lengths in percent
						float vABBC = System.Math.Abs(((dA - dB) / System.Math.Min(dA, dB)));
						if (vABBC >= 0.1f)
						{
							continue;
						}
						
						// Calculate the diagonal length by assuming a 90° angle at topleft
						//UPGRADE_WARNING: Data types in Visual C# might be different.  Verify the accuracy of narrowing conversions. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1042'"
						float dCpy = (float) System.Math.Sqrt(dA * dA + dB * dB);
						// Compare to the real distance in %
						float vPyC = System.Math.Abs(((dC - dCpy) / System.Math.Min(dC, dCpy)));
						
						if (vPyC >= 0.1f)
						{
							continue;
						}
						
						// All tests passed!
						results.Add(test);
					} // end iterate p3
				} // end iterate p2
			} // end iterate p1
			
			if (!(results.Count == 0))
			{
				FinderPattern[][] resultArray = new FinderPattern[results.Count][];
				for (int i = 0; i < results.Count; i++)
				{
					resultArray[i] = (FinderPattern[]) results[i];
				}
				return resultArray;
			}
			
			// Nothing found!
			throw ReaderException.Instance;
		}
		
		public FinderPatternInfo[] findMulti(System.Collections.Hashtable hints)
		{
			bool tryHarder = hints != null && hints.ContainsKey(DecodeHintType.TRY_HARDER);
			BitMatrix image = Image;
			int maxI = image.Height;
			int maxJ = image.Width;
			// We are looking for black/white/black/white/black modules in
			// 1:1:3:1:1 ratio; this tracks the number of such modules seen so far
			
			// Let's assume that the maximum version QR Code we support takes up 1/4 the height of the
			// image, and then account for the center being 3 modules in size. This gives the smallest
			// number of pixels the center could be, so skip this often. When trying harder, look for all
			// QR versions regardless of how dense they are.
			//UPGRADE_WARNING: Data types in Visual C# might be different.  Verify the accuracy of narrowing conversions. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1042'"
			int iSkip = (int) (maxI / (MAX_MODULES * 4.0f) * 3);
			if (iSkip < MIN_SKIP || tryHarder)
			{
				iSkip = MIN_SKIP;
			}
			
			int[] stateCount = new int[5];
			for (int i = iSkip - 1; i < maxI; i += iSkip)
			{
				// Get a row of black/white values
				stateCount[0] = 0;
				stateCount[1] = 0;
				stateCount[2] = 0;
				stateCount[3] = 0;
				stateCount[4] = 0;
				int currentState = 0;
				for (int j = 0; j < maxJ; j++)
				{
					if (image.get_Renamed(j, i))
					{
						// Black pixel
						if ((currentState & 1) == 1)
						{
							// Counting white pixels
							currentState++;
						}
						stateCount[currentState]++;
					}
					else
					{
						// White pixel
						if ((currentState & 1) == 0)
						{
							// Counting black pixels
							if (currentState == 4)
							{
								// A winner?
								if (foundPatternCross(stateCount))
								{
									// Yes
									bool confirmed = handlePossibleCenter(stateCount, i, j);
									if (!confirmed)
									{
										do 
										{
											// Advance to next black pixel
											j++;
										}
										while (j < maxJ && !image.get_Renamed(j, i));
										j--; // back up to that last white pixel
									}
									// Clear state to start looking again
									currentState = 0;
									stateCount[0] = 0;
									stateCount[1] = 0;
									stateCount[2] = 0;
									stateCount[3] = 0;
									stateCount[4] = 0;
								}
								else
								{
									// No, shift counts back by two
									stateCount[0] = stateCount[2];
									stateCount[1] = stateCount[3];
									stateCount[2] = stateCount[4];
									stateCount[3] = 1;
									stateCount[4] = 0;
									currentState = 3;
								}
							}
							else
							{
								stateCount[++currentState]++;
							}
						}
						else
						{
							// Counting white pixels
							stateCount[currentState]++;
						}
					}
				} // for j=...
				
				if (foundPatternCross(stateCount))
				{
					handlePossibleCenter(stateCount, i, maxJ);
				} // end if foundPatternCross
			} // for i=iSkip-1 ...
			FinderPattern[][] patternInfo = selectBestPatterns();
			System.Collections.ArrayList result = System.Collections.ArrayList.Synchronized(new System.Collections.ArrayList(10));
			for (int i = 0; i < patternInfo.Length; i++)
			{
				FinderPattern[] pattern = patternInfo[i];
				ResultPoint.orderBestPatterns(pattern);
				result.Add(new FinderPatternInfo(pattern));
			}
			
			if ((result.Count == 0))
			{
				return EMPTY_RESULT_ARRAY;
			}
			else
			{
				FinderPatternInfo[] resultArray = new FinderPatternInfo[result.Count];
				for (int i = 0; i < result.Count; i++)
				{
					resultArray[i] = (FinderPatternInfo) result[i];
				}
				return resultArray;
			}
		}
	}
}