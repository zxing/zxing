using System;
using System.Text;

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

namespace com.google.zxing
{

	using MathUtils = com.google.zxing.common.detector.MathUtils;

	/// <summary>
	/// <p>Encapsulates a point of interest in an image containing a barcode. Typically, this
	/// would be the location of a finder pattern or the corner of the barcode, for example.</p>
	/// 
	/// @author Sean Owen
	/// </summary>
	public class ResultPoint
	{

	  private readonly float x;
	  private readonly float y;

	  public ResultPoint(float x, float y)
	  {
		this.x = x;
		this.y = y;
	  }

	  public float X
	  {
		  get
		  {
			return x;
		  }
	  }

	  public float Y
	  {
		  get
		  {
			return y;
		  }
	  }

	  public override bool Equals(object other)
	  {
		if (other is ResultPoint)
		{
		  ResultPoint otherPoint = (ResultPoint) other;
		  return x == otherPoint.x && y == otherPoint.y;
		}
		return false;
	  }

      public override int GetHashCode()
      {
          //return 31 * float.floatToIntBits(x) + float.floatToIntBits(y);

          int xbits = BitConverter.ToInt32(BitConverter.GetBytes(x), 0);
          int ybits = BitConverter.ToInt32(BitConverter.GetBytes(y), 0);
          return 31 * xbits + ybits;
      }

	  public override string ToString()
	  {
		StringBuilder result = new StringBuilder(25);
		result.Append('(');
		result.Append(x);
		result.Append(',');
		result.Append(y);
		result.Append(')');
		return result.ToString();
	  }

	  /// <summary>
	  /// <p>Orders an array of three ResultPoints in an order [A,B,C] such that AB < AC and
	  /// BC < AC and the angle between BC and BA is less than 180 degrees.
	  /// </summary>
	  public static void orderBestPatterns(ResultPoint[] patterns)
	  {

		// Find distances between pattern centers
		float zeroOneDistance = distance(patterns[0], patterns[1]);
		float oneTwoDistance = distance(patterns[1], patterns[2]);
		float zeroTwoDistance = distance(patterns[0], patterns[2]);

		ResultPoint pointA;
		ResultPoint pointB;
		ResultPoint pointC;
		// Assume one closest to other two is B; A and C will just be guesses at first
		if (oneTwoDistance >= zeroOneDistance && oneTwoDistance >= zeroTwoDistance)
		{
		  pointB = patterns[0];
		  pointA = patterns[1];
		  pointC = patterns[2];
		}
		else if (zeroTwoDistance >= oneTwoDistance && zeroTwoDistance >= zeroOneDistance)
		{
		  pointB = patterns[1];
		  pointA = patterns[0];
		  pointC = patterns[2];
		}
		else
		{
		  pointB = patterns[2];
		  pointA = patterns[0];
		  pointC = patterns[1];
		}

		// Use cross product to figure out whether A and C are correct or flipped.
		// This asks whether BC x BA has a positive z component, which is the arrangement
		// we want for A, B, C. If it's negative, then we've got it flipped around and
		// should swap A and C.
		if (crossProductZ(pointA, pointB, pointC) < 0.0f)
		{
		  ResultPoint temp = pointA;
		  pointA = pointC;
		  pointC = temp;
		}

		patterns[0] = pointA;
		patterns[1] = pointB;
		patterns[2] = pointC;
	  }


	  /// <returns> distance between two points </returns>
	  public static float distance(ResultPoint pattern1, ResultPoint pattern2)
	  {
		return MathUtils.distance(pattern1.x, pattern1.y, pattern2.x, pattern2.y);
	  }

	  /// <summary>
	  /// Returns the z component of the cross product between vectors BC and BA.
	  /// </summary>
	  private static float crossProductZ(ResultPoint pointA, ResultPoint pointB, ResultPoint pointC)
	  {
		float bX = pointB.x;
		float bY = pointB.y;
		return ((pointC.x - bX) * (pointA.y - bY)) - ((pointC.y - bY) * (pointA.x - bX));
	  }


	}

}