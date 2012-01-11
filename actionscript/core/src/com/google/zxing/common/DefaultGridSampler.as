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

package com.google.zxing.common
{

import com.google.zxing.NotFoundException;

/**
 * @author Sean Owen
 */
public class DefaultGridSampler extends GridSampler 
{

 	

  public function sampleGrid2(image:BitMatrix,
                              dimensionX:int,
                              dimensionY:int,
                              p1ToX:Number,
                              p1ToY:Number,
                              p2ToX:Number, p2ToY:Number,
                              p3ToX:Number, p3ToY:Number,
                              p4ToX:Number, p4ToY:Number,
                              p1FromX:Number, p1FromY:Number,
                              p2FromX:Number, p2FromY:Number,
                              p3FromX:Number, p3FromY:Number,
                              p4FromX:Number, p4FromY:Number):BitMatrix 
   {
    	var transform:PerspectiveTransform  = PerspectiveTransform.quadrilateralToQuadrilateral(
										        p1ToX, p1ToY, p2ToX, p2ToY, p3ToX, p3ToY, p4ToX, p4ToY,
										        p1FromX, p1FromY, p2FromX, p2FromY, p3FromX, p3FromY, p4FromX, p4FromY);
    	return sampleGrid(image, dimensionX, dimensionY, transform);
  }

  public override function sampleGrid(image:BitMatrix ,
                              dimensionX:int,
                              dimensionY:int,
                               transform:PerspectiveTransform):BitMatrix {
    if (dimensionX <= 0 || dimensionY <= 0) {
      throw NotFoundException.getNotFoundInstance();      
    }
    var bits:BitMatrix  = new BitMatrix(dimensionX, dimensionY);
    var points:Array = new Array(dimensionX << 1);
    for (var y:int = 0; y < dimensionY; y++) {
      var max:int = points.length;
      var iValue:Number = Number(y + 0.5);
      for (var x:int = 0; x < max; x += 2) {
        points[x] = Number(int(x >> 1) + 0.5);
        points[x + 1] = iValue;
      }
      transform.transformPoints(points);
      // Quick check to see if points transformed to something inside the image;
      // sufficient to check the endpoints
      checkAndNudgePoints(image, points);
      try {
        for (var x:int = 0; x < max; x += 2) {
          if (image._get(int(points[x]), int( points[x + 1]))) {
            // Black(-ish) pixel
            bits._set(x >> 1, y);
          }
        }
      } catch (aioobe:ArrayIndexOutOfBoundsException) {
        // This feels wrong, but, sometimes if the finder patterns are misidentified, the resulting
        // transform gets "twisted" such that it maps a straight line of points to a set of points
        // whose endpoints are in bounds, but others are not. There is probably some mathematical
        // way to detect this about the transformation that I don't know yet.
        // This results in an ugly runtime exception despite our clever checks above -- can't have
        // that. We could check each point's coordinates but that feels duplicative. We settle for
        // catching and wrapping ArrayIndexOutOfBoundsException.
        throw NotFoundException.getNotFoundInstance();
      }
    }
    return bits;
  }
}
}
