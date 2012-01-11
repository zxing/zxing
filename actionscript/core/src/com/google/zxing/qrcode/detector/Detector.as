package com.google.zxing.qrcode.detector
{
	public class Detector
    { 
		import com.google.zxing.common.BitMatrix;
		import com.google.zxing.common.flexdatatypes.HashTable;
		import com.google.zxing.common.DetectorResult;
		import com.google.zxing.common.GridSampler;
		import com.google.zxing.common.PerspectiveTransform;
		import com.google.zxing.qrcode.decoder.Version;
		import com.google.zxing.ResultPoint;
		import com.google.zxing.common.BitMatrix;
		import com.google.zxing.ReaderException;
		import com.google.zxing.ResultPointCallback;
		import com.google.zxing.DecodeHintType;
		import com.google.zxing.NotFoundException;
		import com.google.zxing.ResultPointCallback;
		

          private var image:BitMatrix ;
          private var resultPointCallback:ResultPointCallback ;
          
          protected function  getImage():BitMatrix 
          {
    		return image;
 		  }



          public function Detector(image:BitMatrix) {
            this.image = image;
          }

		  public function getResultPointCallback():ResultPointCallback 
		  {
		    return resultPointCallback;
		  }
		  
		  /**
           * <p>Detects a QR Code in an image, simply.</p>
           *
           * @return {@link DetectorResult} encapsulating results of detecting a QR Code
           * @throws ReaderException if no QR Code can be found
           */
          //public function detect():DetectorResult{
          //    try{
          //      return detect(null);
          //    }catch(e:Exception){
          //      throw new ReaderException(e.message);
          //    }           
          //}

          /**
           * <p>Detects a QR Code in an image, simply.</p>
           *
           * @param hints optional hints to detector
           * @return {@link DetectorResult} encapsulating results of detecting a QR Code
           * @throws ReaderException if no QR Code can be found
           */
          public function detect(hints:HashTable=null):DetectorResult 
          {
    		var resultPointCallback:ResultPointCallback = hints == null ? null : (hints._get(DecodeHintType.NEED_RESULT_POINT_CALLBACK) as ResultPointCallback);

            var finder:FinderPatternFinder = new FinderPatternFinder(image,resultPointCallback);
            var info:FinderPatternInfo = finder.find(hints);
            
            var result:DetectorResult = processFinderPatternInfo(info);
    		return result;
		}

  public function processFinderPatternInfo(info:FinderPatternInfo ):DetectorResult 
  {

    var topLeft:FinderPattern = info.getTopLeft();
    var topRight:FinderPattern = info.getTopRight();
    var bottomLeft:FinderPattern  = info.getBottomLeft();

    var moduleSize:Number = calculateModuleSize(topLeft, topRight, bottomLeft);
    if (moduleSize < 1) {
      throw new ReaderException();
    }
    var dimension:int = computeDimension(topLeft, topRight, bottomLeft, moduleSize);
    var provisionalVersion:Version = Version.getProvisionalVersionForDimension(dimension);
    var modulesBetweenFPCenters:int  = provisionalVersion.getDimensionForVersion() - 7;

    var alignmentPattern:AlignmentPattern = null;
    // Anything above version 1 has an alignment pattern
    if (provisionalVersion.getAlignmentPatternCenters().length > 0) {

      // Guess where a "bottom right" finder pattern would have been
      var bottomRightX:Number = topRight.getX() - topLeft.getX() + bottomLeft.getX();
      var bottomRightY:Number = topRight.getY() - topLeft.getY() + bottomLeft.getY();

      // Estimate that alignment pattern is closer by 3 modules
      // from "bottom right" to known top left location
      var correctionToTopLeft:Number = 1 - 3 / modulesBetweenFPCenters;
      var estAlignmentX:int = int( (topLeft.getX() + correctionToTopLeft * (bottomRightX - topLeft.getX())));
      var estAlignmentY:int = int( (topLeft.getY() + correctionToTopLeft * (bottomRightY - topLeft.getY())));

      // Kind of arbitrary -- expand search radius before giving up
      for (var i:int = 4; i <= 16; i <<= 1) {
        try {
          alignmentPattern = findAlignmentInRegion(moduleSize,
              estAlignmentX,
              estAlignmentY,
               i);
          break;
        } 
        catch (re:ReaderException) 
        {
          // try next round
        }
      }
      // If we didn't find alignment pattern... well try anyway without it
    }

    var transform:PerspectiveTransform  =
        createTransform(topLeft, topRight, bottomLeft, alignmentPattern, dimension);

    var bits:BitMatrix = sampleGrid(image, transform, dimension);

    var points:Array;
    if (alignmentPattern == null) {
      points = [bottomLeft, topLeft, topRight];
    } else {
      points = [bottomLeft, topLeft, topRight, alignmentPattern];
    }
    return new DetectorResult(bits, points);
  }


  private static function sampleGrid(image:BitMatrix ,
                                      transform:PerspectiveTransform ,
                                      dimension:int ):BitMatrix {

    var sampler:GridSampler  = GridSampler.getGridSamplerInstance();
    return sampler.sampleGrid(image, dimension, dimension, transform);
  }
  
            /**
           * <p>Computes the dimension (number of modules on a size) of the QR Code based on the position
           * of the finder patterns and estimated module size.</p>
           */
          private static function computeDimension(topLeft:ResultPoint,
                                              topRight:ResultPoint,
                                              bottomLeft:ResultPoint,
                                              moduleSize:Number):int {
// note : check custom round function at the bottom
            var tltrCentersDimension:int = round(ResultPoint.distance(topLeft, topRight) / moduleSize);
            var tlblCentersDimension:int = round(ResultPoint.distance(topLeft, bottomLeft) / moduleSize);
            var dimension:int = ((tltrCentersDimension + tlblCentersDimension) >> 1) + 7;
            switch (dimension & 0x03) { // mod 4
              case 0:
                dimension++;
                break;
                // 1? do nothing
              case 2:
                dimension--;
                break;
              case 3:
                throw new ReaderException("Detector : detect : dimension not recognized");
            }
            return dimension;
          }

          /**
           * <p>Computes an average estimated module size based on estimated derived from the positions
           * of the three finder patterns.</p>
           */
          private function calculateModuleSize(topLeft:ResultPoint , topRight:ResultPoint , bottomLeft:ResultPoint ):Number {
            // Take the average
            var num1:Number = calculateModuleSizeOneWay(topLeft, topRight);
            var num2:Number = calculateModuleSizeOneWay(topLeft, bottomLeft);
             var res:Number = (num1+num2) / 2;
            return res;
          }

          /**
           * <p>Estimates module size based on two finder patterns -- it uses
           * {@link #sizeOfBlackWhiteBlackRunBothWays(int, int, int, int)} to figure the
           * width of each, measuring along the axis between their centers.</p>
           */
          private function calculateModuleSizeOneWay(pattern:ResultPoint , otherPattern:ResultPoint ):Number {
            var moduleSizeEst1:Number = sizeOfBlackWhiteBlackRunBothWays(int( pattern.getX()),
                int( pattern.getY()),
                int( otherPattern.getX()),
                int( otherPattern.getY()));
            var moduleSizeEst2:Number = sizeOfBlackWhiteBlackRunBothWays(int( otherPattern.getX()),
                int( otherPattern.getY()),
                int( pattern.getX()),
                int( pattern.getY()));
          
            if (isNaN(moduleSizeEst1)) {
              return moduleSizeEst2 / 7;
            }
            if (isNaN(moduleSizeEst2))
            {
              return moduleSizeEst1 / 7;
            }
            // Average them, and divide by 7 since we've counted the width of 3 black modules,
            // and 1 white and 1 black module on either side. Ergo, divide sum by 14.
            return (moduleSizeEst1 + moduleSizeEst2) / 14;
          }

          /**
           * See {@link #sizeOfBlackWhiteBlackRun(int, int, int, int)}; computes the total width of
           * a finder pattern by looking for a black-white-black run from the center in the direction
           * of another point (another finder pattern center), and in the opposite direction too.</p>
           */
          private function sizeOfBlackWhiteBlackRunBothWays(fromX:int, fromY:int, toX:int,toY:int):Number {

            var result:Number = sizeOfBlackWhiteBlackRun(fromX, fromY, toX, toY);

            // Now count other way -- don't run off image though of course
			var scale:Number = 1;
            var otherToX:int = fromX - (toX - fromX);
            if (otherToX < 0) {
              // "to" should the be the first value not included, so, the first value off
              // the edge is -1
			  scale = fromX / (fromX - otherToX);
              otherToX = 0;
            } else if (otherToX >= image.getWidth()) {
                scale = (image.getWidth() - 1 - fromX) / (otherToX - fromX);
				otherToX = image.getWidth() - 1;
            }
            var otherToY:int = int(fromY - (toY - fromY) * scale);
			
			scale = 1;
            if (otherToY < 0) {
               scale = fromY / (fromY - otherToY);
				otherToY = 0;
            } else if (otherToY >= image.getHeight()) {
               scale = (image.getHeight() - 1 - fromY) /  (otherToY - fromY);
				otherToY = image.getHeight() - 1;
            }
            otherToX = int(fromX + (otherToX - fromX) * scale);
            
            result += sizeOfBlackWhiteBlackRun(fromX, fromY, otherToX, otherToY);
            return result - 1; // -1 because we counted the middle pixel twice
          }

          /**
           * <p>This method traces a line from a point in the image, in the direction towards another point.
           * It begins in a black region, and keeps going until it finds white, then black, then white again.
           * It reports the distance from the start to this point.</p>
           *
           * <p>This is used when figuring out how wide a finder pattern is, when the finder pattern
           * may be skewed or rotated.</p>
           */
          private function sizeOfBlackWhiteBlackRun(fromX:int, fromY:int, toX:int, toY:int):Number {
            // Mild variant of Bresenham's algorithm;
            // see http://en.wikipedia.org/wiki/Bresenham's_line_algorithm
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
            // In black pixels, looking for white, first or second time.
			var state:int = 0;
			// Loop up until x == toX, but not beyond
			var xLimit:int = toX + xstep;
			for (var x:int = fromX, y:int= fromY; x != xLimit; x += xstep) 
			{
				var realX:int = steep ? y : x;
				var realY:int = steep ? x : y;

				// Does current pixel mean we have moved white to black or vice versa?
				var pixl:Boolean = image._get(realX, realY);
				if (((state == 0) && !pixl) ||
				    ((state == 1) && pixl)   ||
				    ((state == 2) && !pixl)) 
				{
					if (state == 2) 
					{
						var diffX:int = x - fromX;
						var diffY:int = y - fromY;
						return Math.sqrt(diffX * diffX + diffY * diffY);
					}
					state++;
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
		// Found black-white-black; give the benefit of the doubt that the next pixel outside the image
		// is "white" so this last point at (toX+xStep,toY) is the right ending. This is really a
		// small approximation; (toX+xStep,toY+yStep) might be really correct. Ignore this.
		if (state == 2) {
		  var diffX1:int = toX + xstep - fromX;
		  var diffY1:int = toY - fromY;
		  return Math.sqrt(diffX1 * diffX1 + diffY1 * diffY1);
		}
		// else we didn't find even black-white-black; no estimate is really possible
		return NaN;
      }

          /**
           * <p>Attempts to locate an alignment pattern in a limited region of the image, which is
           * guessed to contain it. This method uses {@link AlignmentPattern}.</p>
           *
           * @param overallEstModuleSize estimated module size so far
           * @param estAlignmentX x coordinate of center of area probably containing alignment pattern
           * @param estAlignmentY y coordinate of above
           * @param allowanceFactor number of pixels in all directons to search from the center
           * @return {@link AlignmentPattern} if found, or null otherwise
           * @throws ReaderException if an unexpected error occurs during detection
           */
          private function findAlignmentInRegion(overallEstModuleSize:Number,
                                                         estAlignmentX:int,
                                                         estAlignmentY:int,
                                                         allowanceFactor:Number):AlignmentPattern{
            // Look for an alignment pattern (3 modules in size) around where it
            // should be
  
         var allowance:int = int((allowanceFactor * overallEstModuleSize));
            var alignmentAreaLeftX:int = Math.max(0, estAlignmentX - allowance);
            var alignmentAreaRightX:int = Math.min(image.getWidth() - 1, estAlignmentX + allowance);
            if (alignmentAreaRightX - alignmentAreaLeftX < overallEstModuleSize * 3) {
              throw new ReaderException("Detector : findAlignmentInRegion : area smaller than 3 times overallEstModuleSize");
            }

            var alignmentAreaTopY:int = Math.max(0, estAlignmentY - allowance);
            var alignmentAreaBottomY:int = Math.min(image.getHeight() - 1, estAlignmentY + allowance);
	        if (alignmentAreaBottomY - alignmentAreaTopY < overallEstModuleSize * 3) 
			{
				throw NotFoundException.getNotFoundInstance();
			}

            var alignmentFinder:AlignmentPatternFinder =
                new AlignmentPatternFinder(
                    image,
                    alignmentAreaLeftX,
                    alignmentAreaTopY,
                    alignmentAreaRightX - alignmentAreaLeftX,
                    alignmentAreaBottomY - alignmentAreaTopY,
                    overallEstModuleSize,
                    resultPointCallback);
            return alignmentFinder.find();
          }

          /**
           * Ends up being a bit faster than Math.round(). This merely rounds its argument to the nearest int,
           * where x.5 rounds up.
           */
          private static function round(d:Number):int {
            return int(d + 0.5);
          }
    
    public static function createTransform(topLeft:ResultPoint ,
                                                     topRight:ResultPoint ,
                                                     bottomLeft:ResultPoint ,
                                                     alignmentPattern:ResultPoint ,
                                                     dimension:int):PerspectiveTransform {
    var dimMinusThree:Number = dimension - 3.5;
    var bottomRightX:Number;
    var bottomRightY:Number;
    var sourceBottomRightX:Number;
    var sourceBottomRightY:Number;
    if (alignmentPattern != null) {
      bottomRightX = alignmentPattern.getX();
      bottomRightY = alignmentPattern.getY();
      sourceBottomRightX = sourceBottomRightY = dimMinusThree - 3.0;
    } else {
      // Don't have an alignment pattern, just make up the bottom-right point
      bottomRightX = (topRight.getX() - topLeft.getX()) + bottomLeft.getX();
      bottomRightY = (topRight.getY() - topLeft.getY()) + bottomLeft.getY();
      sourceBottomRightX = sourceBottomRightY = dimMinusThree;
    }

    return PerspectiveTransform.quadrilateralToQuadrilateral(
        3.5,
        3.5,
        dimMinusThree,
        3.5,
        sourceBottomRightX,
        sourceBottomRightY,
        3.5,
        dimMinusThree,
        topLeft.getX(),
        topLeft.getY(),
        topRight.getX(),
        topRight.getY(),
        bottomRightX,
        bottomRightY,
        bottomLeft.getX(),
        bottomLeft.getY());
  }
  
    }
}