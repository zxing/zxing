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
	import com.google.zxing.common.detector.MonochromeRectangleDetector;
	import com.google.zxing.common.GridSampler;
	import com.google.zxing.common.BitMatrix;
	import com.google.zxing.common.flexdatatypes.HashTable;
	import com.google.zxing.ResultPoint;
	import com.google.zxing.common.DetectorResult;
	import com.google.zxing.common.flexdatatypes.ArrayList;
	import com.google.zxing.common.BitArray;
	import com.google.zxing.common.BitMatrix;
	import com.google.zxing.ReaderException;
	
	
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
          //private static var INTEGERS:Array = [0, 1, 2, 3, 4];
          

          private  var image:BitMatrix ;
          private var  rectangleDetector:MonochromeRectangleDetector

          public function Detector(image:BitMatrix) 
          {
            this.image = image;
            rectangleDetector = new MonochromeRectangleDetector(image);
          }

          /**
           * <p>Detects a Data Matrix Code in an image.</p>
           *
           * @return {@link DetectorResult} encapsulating results of detecting a QR Code
           * @throws ReaderException if no Data Matrix Code can be found
           */
          public function detect():DetectorResult {

    
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
            var lSideOne:ResultPointsAndTransitions = ResultPointsAndTransitions( transitions.getObjectByIndex(0));
            var lSideTwo:ResultPointsAndTransitions = ResultPointsAndTransitions( transitions.getObjectByIndex(1));

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
              var value:int = pointCount.getValueByIndex(ii) as int;
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
            // or bottom right should work here, but, one will be more reliable since it's traced straight
            // up or across, rather than at a slight angle. We use dot products to figure out which is
            // better to use:
            var dimension:int = Math.min(transitionsBetween(topLeft, topRight).getTransitions(), 
                             transitionsBetween(bottomRight, topRight).getTransitions());
    		if ((dimension & 0x01) == 1) 
    		{
      				// it can't be odd, so, round... up?
      			dimension++;
    		}
    		dimension += 2;



            var bits:BitMatrix = sampleGrid(image, topLeft, bottomLeft, bottomRight, dimension);
            return new DetectorResult(bits, [pointA, pointB, pointC, pointD]);
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
                var value:int = int(table.getValueByKey(key));
                table.setValue(key,value+1);
            }
          }


          private static function sampleGrid(image:BitMatrix ,
                                              topLeft:ResultPoint,
                                              bottomLeft:ResultPoint,
                                              bottomRight:ResultPoint,
                                              dimension:int):BitMatrix {

            // We make up the top right point for now, based on the others.
            // TODO: we actually found a fourth corner above and figured out which of two modules
            // it was the corner of. We could use that here and adjust for perspective distortion.
            var topRightX:Number = (bottomRight.getX() - bottomLeft.getX()) + topLeft.getX();
            var topRightY:Number = (bottomRight.getY() - bottomLeft.getY()) + topLeft.getY();

            // Note that unlike in the QR Code sampler, we didn't find the center of modules, but the
            // very corners. So there is no 0.5f here; 0.0f is right.
            var sampler:GridSampler;
            sampler = GridSampler.getGridSamplerInstance();
            return sampler.sampleGrid(
                image,
                dimension,
                0,
                0,
                dimension,
                0,
                dimension,
                dimension,
                0,
                dimension,
                topLeft.getX(),
                topLeft.getY(),
                topRightX,
                topRightY,
                bottomRight.getX(),
                bottomRight.getY(),
                bottomLeft.getX(),
                bottomLeft.getY());
          }

          /**
           * Counts the number of black/white transitions between two points, using something like Bresenham's algorithm.
           */
          private function transitionsBetween( from:ResultPoint, _to:ResultPoint):ResultPointsAndTransitions {
            // See QR Code Detector, sizeOfBlackWhiteBlackRun()
            var fromX:int = int( from.getX());
            var fromY:int = int( from.getY());
            var toX:int =   int( _to.getX());
            var toY:int =   int( _to.getY());
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
            for (var x:int = fromX, y:int = fromY; x != toX; x += xstep) {
              var isBlack:Boolean = image._get(steep ? y : x, steep ? x : y);
              if (isBlack == !inBlack) {
                transitions++;
                inBlack = isBlack;
              }
              error += dy;
              if (error > 0) {
                y += ystep;
                error -= dx;
              }
            }
            return new ResultPointsAndTransitions(from, _to, transitions);
          }


    }
 
}