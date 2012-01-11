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
/**
 * Implementations of this class can, given locations of finder patterns for a QR code in an
 * image, sample the right points in the image to reconstruct the QR code, accounting for
 * perspective distortion. It is abstracted since it is relatively expensive and should be allowed
 * to take advantage of platform-specific optimized implementations, like Sun's Java Advanced
 * Imaging library, but which may not be available in other environments such as J2ME, and vice
 * versa.
 *
 * The implementation used can be controlled by calling {@link #setGridSampler(GridSampler)}
 * with an instance of a class which implements this interface.
 *
 * @author Sean Owen
 *
 */
   	import com.google.zxing.ReaderException;
   	import com.google.zxing.common.flexdatatypes.IllegalArgumentException;
   	import com.google.zxing.NotFoundException;
  
    public class GridSampler
    {
    	
   		private static var gridSampler:GridSampler = new GridSampler();
   	
	  /**
	   * Sets the implementation of {@link GridSampler} used by the library. One global
	   * instance is stored, which may sound problematic. But, the implementation provided
	   * ought to be appropriate for the entire platform, and all uses of this library
	   * in the whole lifetime of the JVM. For instance, an Android activity can swap in
	   * an implementation that takes advantage of native platform libraries.
	   * 
	   * @param newGridSampler The platform-specific object to install.
	   */
	  public static function setGridSampler(newGridSampler:GridSampler):void {
	    if (newGridSampler == null) {
	      throw new IllegalArgumentException("common : GridSampler : setGridSampler");;
	    }
	    gridSampler = newGridSampler;
	  }
	    /**
	   * @return the current implementation of {@link GridSampler}
	   */


        public static function getGridSamplerInstance():GridSampler
        {
                return gridSampler;
        }

  /**
   * <p>Samples an image for a square matrix of bits of the given dimension. This is used to extract
   * the black/white modules of a 2D barcode like a QR Code found in an image. Because this barcode
   * may be rotated or perspective-distorted, the caller supplies four points in the source image
   * that define known points in the barcode, so that the image may be sampled appropriately.</p>
   *
   * <p>The last eight "from" parameters are four X/Y coordinate pairs of locations of points in
   * the image that define some significant points in the image to be sample. For example,
   * these may be the location of finder pattern in a QR Code.</p>
   *
   * <p>The first eight "to" parameters are four X/Y coordinate pairs measured in the destination
   * {@link BitMatrix}, from the top left, where the known points in the image given by the "from"
   * parameters map to.</p>
   *
   * <p>These 16 parameters define the transformation needed to sample the image.</p>
   *
   * @param image image to sample
   * @param dimension width/height of {@link BitMatrix} to sample from iamge
   * @return {@link BitMatrix} representing a grid of points sampled from the image within a region
   *   defined by the "from" parameters
   * @throws ReaderException if image can't be sampled, for example, if the transformation defined
   *   by the given points is invalid or results in sampling outside the image boundaries
   */

   public function sampleGrid(image:BitMatrix ,
                              dimensionX:int,
                              dimensionY:int,
                               transform:PerspectiveTransform):BitMatrix 
   {
        	// originally in DefaultGridSampler
    if (dimensionX <= 0 || dimensionY <= 0) {
      throw NotFoundException.getNotFoundInstance();      
    }
    var bits:BitMatrix  = new BitMatrix(dimensionX, dimensionY);
    var points:Array = new Array(dimensionX << 1);
    for (var y:int = 0; y < dimensionY; y++) {
      var max:int = points.length;
      var iValue:Number = Number(y + 0.5);
      for (var x:int = 0; x < max; x += 2) {
        points[x] = Number((x >> 1) + 0.5);
        points[x + 1] = iValue;
      }
      transform.transformPoints(points);
      // Quick check to see if points transformed to something inside the image;
      // sufficient to check the endpoints
      checkAndNudgePoints(image, points);
      try {
        for (x = 0; x < max; x += 2) {
          if (image._get(int(points[x]), int( points[x + 1]))) {
            // Black(-ish) pixel
            bits._set(x >> 1, y);
          }
        }
      } catch (aioobe:RangeError) {
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
      
          /**
   * <p>Checks a set of points that have been transformed to sample points on an image against
   * the image's dimensions to see if the point are even within the image.</p>
   *
   * <p>This method will actually "nudge" the endpoints back onto the image if they are found to be
   * barely (less than 1 pixel) off the image. This accounts for imperfect detection of finder
   * patterns in an image where the QR Code runs all the way to the image border.</p>
   *
   * <p>For efficiency, the method will check points from either end of the line until one is found
   * to be within the image. Because the set of points are assumed to be linear, this is valid.</p>
   *
   * @param image image into which the points should map
   * @param points actual points in x1,y1,...,xn,yn form
   * @throws ReaderException if an endpoint is lies outside the image boundaries
   */
    public static function checkAndNudgePoints(image:BitMatrix, points:Array):void
        {
            var width:int = image.getWidth();
            var height:int = image.getHeight();
            // Check and nudge points from start until we see some that are OK:
            var nudged:Boolean = true;
            for (var offset:int = 0; offset < points.length && nudged; offset += 2)
            {
                var x:int = int(points[offset]);
                var y:int = int(points[offset + 1]);
                if (x < -1 || x > width || y < -1 || y > height)
                {
                    throw new ReaderException("common : GridSampler : checkAndNudgePoints : point out of range ("+x+""+y+") max:"+width+" - "+height);
                }
                nudged = false;
                if (x == -1)
                {
                    points[offset] = 0;
                    nudged = true;
                }
                else if (x == width)
                {
                    points[offset] = width - 1;
                    nudged = true;
                }
                if (y == -1)
                {
                    points[offset + 1] = 0;
                    nudged = true;
                }
                else if (y == height)
                {
                    points[offset + 1] = height - 1;
                    nudged = true;
                }
            }
            // Check and nudge points from end:
            nudged = true;
            for (var offset1:int = points.length - 2; offset >= 0 && nudged; offset -= 2)
            {
                var x1:int = int(points[offset1]);
                var y1:int = int(points[offset1 + 1]);
                if (x1 < -1 || x1 > width || y1 < -1 || y1 > height)
                {
                    throw new ReaderException("common : GridSampler : checkAndNudgePoints : out of bounds");
                }
                nudged = false;
                if (x1 == -1)
                {
                    points[offset1] = 0;
                    nudged = true;
                }
                else if (x1 == width)
                {
                    points[offset1] = width - 1;
                    nudged = true;
                }
                if (y1 == -1)
                {
                    points[offset1 + 1] = 0;
                    nudged = true;
                }
                else if (y1 == height)
                {
                    points[offset1 + 1] = height - 1;
                    nudged = true;
                }
            }
        }
    }
}