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
using ReaderException = com.google.zxing.ReaderException;
using ResultPoint = com.google.zxing.ResultPoint;
using BitMatrix = com.google.zxing.common.BitMatrix;
using Collections = com.google.zxing.common.Collections;
using Comparator = com.google.zxing.common.Comparator;
using DetectorResult = com.google.zxing.common.DetectorResult;
using GridSampler = com.google.zxing.common.GridSampler;
using MonochromeRectangleDetector = com.google.zxing.common.detector.MonochromeRectangleDetector;
namespace com.google.zxing.datamatrix.detector
{
	
	/// <summary> <p>Encapsulates logic that can detect a Data Matrix Code in an image, even if the Data Matrix Code
	/// is rotated or skewed, or partially obscured.</p>
	/// 
	/// </summary>
	/// <author>  Sean Owen
	/// </author>
	/// <author>www.Redivivus.in (suraj.supekar@redivivus.in) - Ported from ZXING Java Source 
	/// </author>
	public sealed class Detector
	{
		
		//private static final int MAX_MODULES = 32;
		
		// Trick to avoid creating new Integer objects below -- a sort of crude copy of
		// the Integer.valueOf(int) optimization added in Java 5, not in J2ME
		//UPGRADE_NOTE: Final was removed from the declaration of 'INTEGERS '. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
		private static readonly System.Int32[] INTEGERS = new System.Int32[]{0, 1, 2, 3, 4};
		// No, can't use valueOf()
		
		//UPGRADE_NOTE: Final was removed from the declaration of 'image '. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
		private BitMatrix image;
		//UPGRADE_NOTE: Final was removed from the declaration of 'rectangleDetector '. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
		private MonochromeRectangleDetector rectangleDetector;
		
		public Detector(BitMatrix image)
		{
			this.image = image;
			rectangleDetector = new MonochromeRectangleDetector(image);
		}
		
		/// <summary> <p>Detects a Data Matrix Code in an image.</p>
		/// 
		/// </summary>
		/// <returns> {@link DetectorResult} encapsulating results of detecting a QR Code
		/// </returns>
		/// <throws>  ReaderException if no Data Matrix Code can be found </throws>
		public DetectorResult detect()
		{
			
			ResultPoint[] cornerPoints = rectangleDetector.detect();
			ResultPoint pointA = cornerPoints[0];
			ResultPoint pointB = cornerPoints[1];
			ResultPoint pointC = cornerPoints[2];
			ResultPoint pointD = cornerPoints[3];
			
			// Point A and D are across the diagonal from one another,
			// as are B and C. Figure out which are the solid black lines
			// by counting transitions
			System.Collections.ArrayList transitions = System.Collections.ArrayList.Synchronized(new System.Collections.ArrayList(4));
			transitions.Add(transitionsBetween(pointA, pointB));
			transitions.Add(transitionsBetween(pointA, pointC));
			transitions.Add(transitionsBetween(pointB, pointD));
			transitions.Add(transitionsBetween(pointC, pointD));
			Collections.insertionSort(transitions, new ResultPointsAndTransitionsComparator());
			
			// Sort by number of transitions. First two will be the two solid sides; last two
			// will be the two alternating black/white sides
			ResultPointsAndTransitions lSideOne = (ResultPointsAndTransitions) transitions[0];
			ResultPointsAndTransitions lSideTwo = (ResultPointsAndTransitions) transitions[1];
			
			// Figure out which point is their intersection by tallying up the number of times we see the
			// endpoints in the four endpoints. One will show up twice.
			System.Collections.Hashtable pointCount = System.Collections.Hashtable.Synchronized(new System.Collections.Hashtable());
			increment(pointCount, lSideOne.From);
			increment(pointCount, lSideOne.To);
			increment(pointCount, lSideTwo.From);
			increment(pointCount, lSideTwo.To);
			
			ResultPoint maybeTopLeft = null;
			ResultPoint bottomLeft = null;
			ResultPoint maybeBottomRight = null;
			System.Collections.IEnumerator points = pointCount.Keys.GetEnumerator();
			//UPGRADE_TODO: Method 'java.util.Enumeration.hasMoreElements' was converted to 'System.Collections.IEnumerator.MoveNext' which has a different behavior. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1073_javautilEnumerationhasMoreElements'"
			while (points.MoveNext())
			{
				//UPGRADE_TODO: Method 'java.util.Enumeration.nextElement' was converted to 'System.Collections.IEnumerator.Current' which has a different behavior. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1073_javautilEnumerationnextElement'"
				ResultPoint point = (ResultPoint) points.Current;
				System.Int32 value_Renamed = (System.Int32) pointCount[point];
				if (value_Renamed == 2)
				{
					bottomLeft = point; // this is definitely the bottom left, then -- end of two L sides
				}
				else
				{
					// Otherwise it's either top left or bottom right -- just assign the two arbitrarily now
					if (maybeTopLeft == null)
					{
						maybeTopLeft = point;
					}
					else
					{
						maybeBottomRight = point;
					}
				}
			}
			
			if (maybeTopLeft == null || bottomLeft == null || maybeBottomRight == null)
			{
				throw ReaderException.Instance;
			}
			
			// Bottom left is correct but top left and bottom right might be switched
			ResultPoint[] corners = new ResultPoint[]{maybeTopLeft, bottomLeft, maybeBottomRight};
			// Use the dot product trick to sort them out
			ResultPoint.orderBestPatterns(corners);
			
			// Now we know which is which:
			ResultPoint bottomRight = corners[0];
			bottomLeft = corners[1];
			ResultPoint topLeft = corners[2];
			
			// Which point didn't we find in relation to the "L" sides? that's the top right corner
			ResultPoint topRight;
			if (!pointCount.ContainsKey(pointA))
			{
				topRight = pointA;
			}
			else if (!pointCount.ContainsKey(pointB))
			{
				topRight = pointB;
			}
			else if (!pointCount.ContainsKey(pointC))
			{
				topRight = pointC;
			}
			else
			{
				topRight = pointD;
			}
			
			// Next determine the dimension by tracing along the top or right side and counting black/white
			// transitions. Since we start inside a black module, we should see a number of transitions
			// equal to 1 less than the code dimension. Well, actually 2 less, because we are going to
			// end on a black module:
			
			// The top right point is actually the corner of a module, which is one of the two black modules
			// adjacent to the white module at the top right. Tracing to that corner from either the top left
			// or bottom right should work here. The number of transitions could be higher than it should be
			// due to noise. So we try both and take the min.
			
			int dimension = System.Math.Min(transitionsBetween(topLeft, topRight).Transitions, transitionsBetween(bottomRight, topRight).Transitions);
			if ((dimension & 0x01) == 1)
			{
				// it can't be odd, so, round... up?
				dimension++;
			}
			dimension += 2;
			
			BitMatrix bits = sampleGrid(image, topLeft, bottomLeft, bottomRight, dimension);
			return new DetectorResult(bits, new ResultPoint[]{pointA, pointB, pointC, pointD});
		}
		
		/// <summary> Increments the Integer associated with a key by one.</summary>
		private static void  increment(System.Collections.Hashtable table, ResultPoint key)
		{
            //System.Int32 value_Renamed = (System.Int32) table[key];
            ////UPGRADE_TODO: The 'System.Int32' structure does not have an equivalent to NULL. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1291'"
            //table[key] = value_Renamed == null?INTEGERS[1]:INTEGERS[value_Renamed + 1];
            // Redivivus.in Java to c# Porting update
            // 30/01/2010 
            // Added
            // START
            System.Int32 value_Renamed = 0;
            try
            {
                if (table.Count > 0)
                    value_Renamed = (System.Int32)table[key];
            }
            catch
            {
                value_Renamed = 0;
            }
            table[key] = value_Renamed == 0 ? INTEGERS[1] : INTEGERS[value_Renamed + 1];
            //END
		}
		
		private static BitMatrix sampleGrid(BitMatrix image, ResultPoint topLeft, ResultPoint bottomLeft, ResultPoint bottomRight, int dimension)
		{
			
			// We make up the top right point for now, based on the others.
			// TODO: we actually found a fourth corner above and figured out which of two modules
			// it was the corner of. We could use that here and adjust for perspective distortion.
			float topRightX = (bottomRight.X - bottomLeft.X) + topLeft.X;
			float topRightY = (bottomRight.Y - bottomLeft.Y) + topLeft.Y;
			
			// Note that unlike in the QR Code sampler, we didn't find the center of modules, but the
			// very corners. So there is no 0.5f here; 0.0f is right.
			GridSampler sampler = GridSampler.Instance;
			return sampler.sampleGrid(image, dimension, 0.0f, 0.0f, dimension, 0.0f, dimension, dimension, 0.0f, dimension, topLeft.X, topLeft.Y, topRightX, topRightY, bottomRight.X, bottomRight.Y, bottomLeft.X, bottomLeft.Y);
		}
		
		/// <summary> Counts the number of black/white transitions between two points, using something like Bresenham's algorithm.</summary>
		private ResultPointsAndTransitions transitionsBetween(ResultPoint from, ResultPoint to)
		{
			// See QR Code Detector, sizeOfBlackWhiteBlackRun()
			//UPGRADE_WARNING: Data types in Visual C# might be different.  Verify the accuracy of narrowing conversions. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1042'"
			int fromX = (int) from.X;
			//UPGRADE_WARNING: Data types in Visual C# might be different.  Verify the accuracy of narrowing conversions. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1042'"
			int fromY = (int) from.Y;
			//UPGRADE_WARNING: Data types in Visual C# might be different.  Verify the accuracy of narrowing conversions. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1042'"
			int toX = (int) to.X;
			//UPGRADE_WARNING: Data types in Visual C# might be different.  Verify the accuracy of narrowing conversions. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1042'"
			int toY = (int) to.Y;
			bool steep = System.Math.Abs(toY - fromY) > System.Math.Abs(toX - fromX);
			if (steep)
			{
				int temp = fromX;
				fromX = fromY;
				fromY = temp;
				temp = toX;
				toX = toY;
				toY = temp;
			}
			
			int dx = System.Math.Abs(toX - fromX);
			int dy = System.Math.Abs(toY - fromY);
			int error = - dx >> 1;
			int ystep = fromY < toY?1:- 1;
			int xstep = fromX < toX?1:- 1;
			int transitions = 0;
			bool inBlack = image.get_Renamed(steep?fromY:fromX, steep?fromX:fromY);
			for (int x = fromX, y = fromY; x != toX; x += xstep)
			{
				bool isBlack = image.get_Renamed(steep?y:x, steep?x:y);
				if (isBlack != inBlack)
				{
					transitions++;
					inBlack = isBlack;
				}
				error += dy;
				if (error > 0)
				{
					if (y == toY)
					{
						break;
					}
					y += ystep;
					error -= dx;
				}
			}
			return new ResultPointsAndTransitions(from, to, transitions);
		}
		
		/// <summary> Simply encapsulates two points and a number of transitions between them.</summary>
		private class ResultPointsAndTransitions
		{
			public ResultPoint From
			{
				get
				{
					return from;
				}
				
			}
			public ResultPoint To
			{
				get
				{
					return to;
				}
				
			}
			public int Transitions
			{
				get
				{
					return transitions;
				}
				
			}
			//UPGRADE_NOTE: Final was removed from the declaration of 'from '. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
			private ResultPoint from;
			//UPGRADE_NOTE: Final was removed from the declaration of 'to '. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
			private ResultPoint to;
			//UPGRADE_NOTE: Final was removed from the declaration of 'transitions '. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
			private int transitions;
			internal ResultPointsAndTransitions(ResultPoint from, ResultPoint to, int transitions)
			{
				this.from = from;
				this.to = to;
				this.transitions = transitions;
			}
			public override System.String ToString()
			{
				return from + "/" + to + '/' + transitions;
			}
		}
		
		/// <summary> Orders ResultPointsAndTransitions by number of transitions, ascending.</summary>
		private class ResultPointsAndTransitionsComparator : Comparator
		{
			public int compare(System.Object o1, System.Object o2)
			{
				return ((ResultPointsAndTransitions) o1).Transitions - ((ResultPointsAndTransitions) o2).Transitions;
			}
		}
	}
}