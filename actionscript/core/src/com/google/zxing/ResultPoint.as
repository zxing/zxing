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

package com.google.zxing
{
/**
 * <p>Encapsulates a point of interest in an image containing a barcode. Typically, this
 * would be the location of a finder pattern or the corner of the barcode, for example.</p>
 *
 * @author Sean Owen
 */
 import com.google.zxing.common.flexdatatypes.StringBuilder;
 
    public class ResultPoint
    {
        protected var x:Number;
  		protected var y:Number;

  		public function ResultPoint(x:Number, y:Number) 
  		{
    		this.x = x;
    		this.y = y;
  		}

  		public final function getX():Number 
  		{
    		return x;
  		}

  		public final function getY():Number 
  		{
    		return y;
  		}

  		public function equals(other:Object ):Boolean 
  		{
    		if (other is ResultPoint) 
    		{
       			var otherPoint:ResultPoint = other as ResultPoint;
      			return x == otherPoint.x && y == otherPoint.y;
    		}
    		return false;
  		}
  		
  		/* no default method to determine a hashcode for Number in Actionscript

  		public function hashCode():int 
  		{
    		return 31 * identityHashCode(x) + identityHashCode(y);
  		}
		*/
 		public function  toString():String 
 		{
    		var result:StringBuilder = new StringBuilder(25);
		    result.Append('(');
		    result.Append(x);
		    result.Append(',');
		    result.Append(y);
		    result.Append(')');
		    return result.toString();
 		 }

		  /**
		   * <p>Orders an array of three ResultPoints in an order [A,B,C] such that AB < AC and
		   * BC < AC and the angle between BC and BA is less than 180 degrees.
		   */
		  public static function orderBestPatterns(patterns:Array):void 
		  {
		
		    // Find distances between pattern centers
		    var zeroOneDistance:Number = distance(patterns[0], patterns[1]);
		    var oneTwoDistance:Number = distance(patterns[1], patterns[2]);
		    var zeroTwoDistance:Number = distance(patterns[0], patterns[2]);
		
		    var pointA:ResultPoint;
		    var pointB:ResultPoint;
		    var pointC:ResultPoint;
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
		    if (crossProductZ(pointA, pointB, pointC) < 0.0) {
		      var temp:ResultPoint = pointA;
		      pointA = pointC;
		      pointC = temp;
		    }
		
		    patterns[0] = pointA;
		    patterns[1] = pointB;
		    patterns[2] = pointC;
		  }


		  /**
		   * @return distance between two points
		   */
		  public static function distance(pattern1:ResultPoint, pattern2:ResultPoint):Number 
		  {
		    var xDiff:Number = pattern1.getX() - pattern2.getX();
		    var yDiff:Number = pattern1.getY() - pattern2.getY();
		    return Math.sqrt(xDiff * xDiff + yDiff * yDiff);
		  }
		
		  /**
		   * Returns the z component of the cross product between vectors BC and BA.
		   */
		  public static function crossProductZ(pointA:ResultPoint, pointB:ResultPoint, pointC:ResultPoint):Number 
		  {
		    var bX:Number = pointB.x;
		    var bY:Number = pointB.y;
		    return ((pointC.x - bX) * (pointA.y - bY)) - ((pointC.y - bY) * (pointA.x - bX));
		  }


  	}
}