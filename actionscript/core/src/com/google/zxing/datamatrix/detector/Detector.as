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
package com.google.zxing.datamatrix.detector
{
	import com.google.zxing.ReaderException;
	import com.google.zxing.ResultPoint;
	import com.google.zxing.common.BitArray;
	import com.google.zxing.common.BitMatrix;
	import com.google.zxing.common.DetectorResult;
	import com.google.zxing.common.GridSampler;
	import com.google.zxing.common.detector.MonochromeRectangleDetector;
	import com.google.zxing.common.detector.WhiteRectangleDetector;
	import com.google.zxing.common.flexdatatypes.ArrayList;
	import com.google.zxing.common.flexdatatypes.HashTable;
	
   /**
     * <p>Encapsulates logic that can detect a Data Matrix Code in an image, even if the Data Matrix Code
     * is rotated or skewed, or partially obscured.</p>
     *
     * @author Sean Owen
     */
    public class Detector
    {
    	  import com.google.zxing.common.flexdatatypes.HashTable;
    	  import com.google.zxing.common.BitMatrix;
          import com.google.zxing.common.DecoderResult;
          import com.google.zxing.common.DetectorResult;
          import com.google.zxing.common.flexdatatypes.ArrayList;
          import com.google.zxing.common.BitArray;
          import com.google.zxing.common.GridSampler;
          import com.google.zxing.ResultPoint;
          import com.google.zxing.datamatrix.detector.ResultPointsAndTransitionsComparator;
          import com.google.zxing.common.detector.MonochromeRectangleDetector;
          
          
          //private static var MAX_MODULES:int = 32;

          // Trick to avoid creating new int objects below -- a sort of crude copy of
          // the int.valueOf(int) optimization added in Java 5, not in J2ME
            private static var INTEGERS:Array =[ 0,1,2,3,4 ];
          

          private  var image:BitMatrix ;
          private var  rectangleDetector:WhiteRectangleDetector;
          
          public function Detector(image:BitMatrix) 
          {
            this.image = image;
            rectangleDetector = new WhiteRectangleDetector(image);
          }

          /**
           * <p>Detects a Data Matrix Code in an image.</p>
           *
           * @return {@link DetectorResult} encapsulating results of detecting a QR Code
           * @throws ReaderException if no Data Matrix Code can be found
           */
          public function detect():DetectorResult 
		  {

    
    		var cornerPoints:Array = rectangleDetector.detect();
            var pointA:ResultPoint = cornerPoints[0]
            var pointB:ResultPoint = cornerPoints[1]
            var pointC:ResultPoint = cornerPoints[2]
            var pointD:ResultPoint = cornerPoints[3]


            // Point A and D are across the diagonal from one another,
            // as are B and C. Figure out which are the solid black lines
            // by counting transitions
            var transitions:ArrayList = new ArrayList(4);
            transitions.Add(transitionsBetween(pointA, pointB));
            transitions.Add(transitionsBetween(pointA, pointC));
            transitions.Add(transitionsBetween(pointB, pointD));
            transitions.Add(transitionsBetween(pointC, pointD));
            transitions.sort_ResultPointsAndTransitionsComparator();
            //Collections.insertionSort(transitions, new ResultPointsAndTransitionsComparator());

            // Sort by number of transitions. First two will be the two solid sides; last two
            // will be the two alternating black/white sides
            var lSideOne:ResultPointsAndTransitions = ( transitions.getObjectByIndex(0) as ResultPointsAndTransitions);
            var lSideTwo:ResultPointsAndTransitions = ( transitions.getObjectByIndex(1) as ResultPointsAndTransitions);

            // Figure out which point is their intersection by tallying up the number of times we see the
            // endpoints in the four endpoints. One will show up twice.
            var  pointCount:HashTable = new HashTable();
            increment(pointCount, lSideOne.getFrom());
            increment(pointCount, lSideOne.getTo());
            increment(pointCount, lSideTwo.getFrom());
            increment(pointCount, lSideTwo.getTo());

            var maybeTopLeft:ResultPoint = null;
            var bottomLeft:ResultPoint = null;
            var maybeBottomRight:ResultPoint = null;
			var size:int = pointCount.getSize();
			for (var ii:int=0;ii<size;ii++)
            {
              var point:ResultPoint = pointCount.getKeyByIndex(ii) as ResultPoint;// resultpoints are used as keys
              var value:int = Math.floor(pointCount.getValueByIndex(ii) as Number);
              if (value == 2) 
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
              throw new ReaderException("Detector : detect : maybeTopLeft or bottomLeft or maybeBottomRight == null");
            }

            // Bottom left is correct but top left and bottom right might be switched
            var corners:Array = [maybeTopLeft, bottomLeft, maybeBottomRight];
            // Use the dot product trick to sort them out
            ResultPoint.orderBestPatterns(corners);

            // Now we know which is which:
            var bottomRight:ResultPoint = corners[0];
            bottomLeft = corners[1];
            var topLeft:ResultPoint = corners[2];

            // Which point didn't we find in relation to the "L" sides? that's the top right corner
            var topRight:ResultPoint;
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
    // or bottom right should work here.
    
    var dimensionTop:int = transitionsBetween(topLeft, topRight).getTransitions();
    var dimensionRight:int = transitionsBetween(bottomRight, topRight).getTransitions();
    
    if ((dimensionTop & 0x01) == 1) {
      // it can't be odd, so, round... up?
      dimensionTop++;
    }
    dimensionTop += 2;
    
    if ((dimensionRight & 0x01) == 1) {
      // it can't be odd, so, round... up?
      dimensionRight++;
    }
    dimensionRight += 2;

    var bits:BitMatrix ;
    var correctedTopRight:ResultPoint ;

    // Rectanguar symbols are 6x16, 6x28, 10x24, 10x32, 14x32, or 14x44. If one dimension is more
    // than twice the other, it's certainly rectangular, but to cut a bit more slack we accept it as
    // rectangular if the bigger side is at least 7/4 times the other:
    if (4 * dimensionTop >= 7 * dimensionRight || 4 * dimensionRight >= 7 * dimensionTop) {
    	// The matrix is rectangular
    	
      correctedTopRight =
          correctTopRightRectangular(bottomLeft, bottomRight, topLeft, topRight, dimensionTop, dimensionRight);
      if (correctedTopRight == null){
        correctedTopRight = topRight;
      }

	      dimensionTop = transitionsBetween(topLeft, correctedTopRight).getTransitions();
      dimensionRight = transitionsBetween(bottomRight, correctedTopRight).getTransitions();

      if ((dimensionTop & 0x01) == 1) {
        // it can't be odd, so, round... up?
        dimensionTop++;
      }

      if ((dimensionRight & 0x01) == 1) {
        // it can't be odd, so, round... up?
        dimensionRight++;
      }

      bits = sampleGrid(image, topLeft, bottomLeft, bottomRight, correctedTopRight, dimensionTop, dimensionRight);
          
    } else {
    	// The matrix is square
        
    	var dimension:int = Math.min(dimensionRight, dimensionTop);
      // correct top right point to match the white module
      correctedTopRight = correctTopRight(bottomLeft, bottomRight, topLeft, topRight, dimension);
      if (correctedTopRight == null){
        correctedTopRight = topRight;
      }

      // Redetermine the dimension using the corrected top right point
      var dimensionCorrected:int = Math.max(transitionsBetween(topLeft, correctedTopRight).getTransitions(),
                                transitionsBetween(bottomRight, correctedTopRight).getTransitions());
      dimensionCorrected++;
      if ((dimensionCorrected & 0x01) == 1) {
        dimensionCorrected++;
      }

		  bits = sampleGrid(image,
		                    topLeft,
		                    bottomLeft,
		                    bottomRight,
		                    correctedTopRight,
		                    dimensionCorrected,
		                    dimensionCorrected);
		  }
            return new DetectorResult(bits, [topLeft, bottomLeft, bottomRight, correctedTopRight]);
     }


  



 /**
   * Calculates the position of the white top right module using the output of the rectangle detector
   * for a rectangular matrix
   */
  private function correctTopRightRectangular(bottomLeft:ResultPoint ,
		bottomRight:ResultPoint , topLeft:ResultPoint , topRight:ResultPoint ,
		dimensionTop:int , dimensionRight:int ):ResultPoint  {
	  
		var corr:Number = distance(bottomLeft, bottomRight) / dimensionTop;
		var norm:int = distance(topLeft, topRight);
		var cos:Number = (topRight.getX() - topLeft.getX()) / norm;
		var sin:Number = (topRight.getY() - topLeft.getY()) / norm;
		
		var c1:ResultPoint  = new ResultPoint(topRight.getX()+corr*cos, topRight.getY()+corr*sin);
	
		corr = distance(bottomLeft, topLeft) / dimensionRight;
		norm = distance(bottomRight, topRight);
		cos = (topRight.getX() - bottomRight.getX()) / norm;
		sin = (topRight.getY() - bottomRight.getY()) / norm;
		
		var c2:ResultPoint  = new ResultPoint(topRight.getX()+corr*cos, topRight.getY()+corr*sin);

		if (!isValid(c1)){
			if (isValid(c2)){
				return c2;
			}
			return null;
		} else if (!isValid(c2)){
			return c1;
		}
		
		var l1:int = Math.abs(dimensionTop - transitionsBetween(topLeft, c1).getTransitions()) + 
					Math.abs(dimensionRight - transitionsBetween(bottomRight, c1).getTransitions());
		var l2:int = Math.abs(dimensionTop - transitionsBetween(topLeft, c2).getTransitions()) + 
		Math.abs(dimensionRight - transitionsBetween(bottomRight, c2).getTransitions());
		
		if (l1 <= l2){
			return c1;
		}
		
		return c2;
  }

  /**
   * Calculates the position of the white top right module using the output of the rectangle detector
   * for a square matrix
   */
  private function correctTopRight(bottomLeft:ResultPoint ,
                                       bottomRight:ResultPoint,
                                      topLeft:ResultPoint ,
                                      topRight:ResultPoint ,
                                      dimension:int ):ResultPoint {
		
		var corr:Number = distance(bottomLeft, bottomRight) / dimension;
		var norm:int = distance(topLeft, topRight);
		var cos:Number = (topRight.getX() - topLeft.getX()) / norm;
		var sin:Number = (topRight.getY() - topLeft.getY()) / norm;
		
		var c1:ResultPoint  = new ResultPoint(topRight.getX() + corr * cos, topRight.getY() + corr * sin);
	
		corr = distance(bottomLeft, bottomRight) /  dimension;
		norm = distance(bottomRight, topRight);
		cos = (topRight.getX() - bottomRight.getX()) / norm;
		sin = (topRight.getY() - bottomRight.getY()) / norm;
		
		var c2:ResultPoint  = new ResultPoint(topRight.getX() + corr * cos, topRight.getY() + corr * sin);

		if (!isValid(c1)) {
			if (isValid(c2)) {
				return c2;
			}
			return null;
		} else if (!isValid(c2)) {
			return c1;
		}
		
		var l1:int = Math.abs(transitionsBetween(topLeft, c1).getTransitions() -
                      transitionsBetween(bottomRight, c1).getTransitions());
		var l2:int = Math.abs(transitionsBetween(topLeft, c2).getTransitions() -
                      transitionsBetween(bottomRight, c2).getTransitions());

    return l1 <= l2 ? c1 : c2;
  }
  
    private function isValid(p:ResultPoint):Boolean {
	  return p.getX() >= 0 && p.getX() < image.getWidth() && p.getY() > 0 && p.getY() < image.getHeight();
  }

   private static function round(d:Number):int 
   {
    return Math.ceil(d);
  }

	// L2 distance
	  private static function distance(a:ResultPoint, b:ResultPoint):int {
		return Detector.round( Math.sqrt((a.getX() - b.getX())
			* (a.getX() - b.getX()) + (a.getY() - b.getY())
			* (a.getY() - b.getY())));
	  }
  
		/**
	   * Increments the int associated with a key by one.
	   */
	  private static function increment(table:HashTable, key:ResultPoint):void
	  {          	
		if (!table.ContainsKey(key))
		{
			table.Add(key,1);
		}
		else
		{
			table.setValue(key,Math.floor(table.getValueByKey(key) as Number)+1);
		}
	  }

	  private static function sampleGrid(image:BitMatrix ,
                                      topLeft:ResultPoint ,
                                      bottomLeft:ResultPoint ,
                                      bottomRight:ResultPoint ,
                                      topRight:ResultPoint ,
                                      dimensionX:int ,
                                      dimensionY:int ):BitMatrix{

    var sampler:GridSampler  = GridSampler.getGridSamplerInstance();

    return sampler.sampleGrid2(image,
                              dimensionX,
                              dimensionY,
                              0.5,
                              0.5,
                              dimensionX - 0.5,
                              0.5,
                              dimensionX - 0.5,
                              dimensionY - 0.5,
                              0.5,
                              dimensionY - 0.5,
                              topLeft.getX(),
                              topLeft.getY(),
                              topRight.getX(),
                              topRight.getY(),
                              bottomRight.getX(),
                              bottomRight.getY(),
                              bottomLeft.getX(),
                              bottomLeft.getY());
  }

          /**
           * Counts the number of black/white transitions between two points, using something like Bresenham's algorithm.
           */
          private function transitionsBetween( from:ResultPoint, _to:ResultPoint):ResultPointsAndTransitions 
		  {
            // See QR Code Detector, sizeOfBlackWhiteBlackRun()
            var fromX:int = Math.floor(from.getX());
            var fromY:int = Math.floor( from.getY());
            var toX:int =   Math.floor( _to.getX());
            var toY:int =   Math.floor( _to.getY());
            var steep:Boolean = Math.abs(toY - fromY) > Math.abs(toX - fromX);
            if (steep) {
              var temp:int = fromX;
              fromX = fromY;
              fromY = temp;
              temp = toX;
              toX = toY;
              toY = temp;
            }

            var dx:int = Math.abs(toX - fromX);
            var dy:int = Math.abs(toY - fromY);
            var error:int = -dx >> 1;
            var ystep:int = fromY < toY ? 1 : -1;
            var xstep:int = fromX < toX ? 1 : -1;
            var transitions:int = 0;
            var inBlack:Boolean = image._get(steep ? fromY : fromX, steep ? fromX : fromY);
            for (var x:int = fromX, y:int = fromY; x != toX; x += xstep) 
			{
              var isBlack:Boolean = image._get(steep ? y : x, steep ? x : y);
              
              if (isBlack == !inBlack) 
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
            return new ResultPointsAndTransitions(from, _to, transitions);
          }
    }
 
}