package com.google.zxing.qrcode.detector
{
	public class Detector
    { 
		import com.google.zxing.common.BitMatrix;
		import com.google.zxing.common.flexdatatypes.HashTable;
		import com.google.zxing.common.DetectorResult;
		import com.google.zxing.common.GridSampler;
		import com.google.zxing.qrcode.decoder.Version;
		import com.google.zxing.ResultPoint;
		import com.google.zxing.common.BitMatrix;
		import com.google.zxing.ReaderException;
		

          private var image:BitMatrix ;
          
          protected function  getImage():BitMatrix 
          {
    		return image;
 		  }



          public function Detector(image:BitMatrix) {
            this.image = image;
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
          public function detect(hints:HashTable=null):DetectorResult {


            var finder:FinderPatternFinder = new FinderPatternFinder(image);
            var info:FinderPatternInfo = finder.find(hints);

            var topLeft:FinderPattern = info.getTopLeft();
            var topRight:FinderPattern = info.getTopRight();
            var bottomLeft:FinderPattern = info.getBottomLeft();

            var moduleSize:Number = calculateModuleSize(topLeft, topRight, bottomLeft);
            if (moduleSize < 1) {
              throw new ReaderException("Detector : detect : moduleSize < 1");
            }
            var dimension:int = computeDimension(topLeft, topRight, bottomLeft, moduleSize);

            var provisionalVersion:Version = Version.getProvisionalVersionForDimension(dimension);
            var modulesBetweenFPCenters:int = provisionalVersion.getDimensionForVersion() - 7;

            var alignmentPattern:AlignmentPattern  = null;
            // Anything above version 1 has an alignment pattern
            if (provisionalVersion.getAlignmentPatternCenters().length > 0) {

              // Guess where a "bottom right" finder pattern would have been
              var bottomRightX:Number = topRight.getX() - topLeft.getX() + bottomLeft.getX();
              var bottomRightY:Number = topRight.getY() - topLeft.getY() + bottomLeft.getY();

              // Estimate that alignment pattern is closer by 3 modules
              // from "bottom right" to known top left location
              var correctionToTopLeft:Number = 1 - 3 / Number( modulesBetweenFPCenters);
              var estAlignmentX:int = int( (topLeft.getX() + correctionToTopLeft * (bottomRightX - topLeft.getX())));
              var estAlignmentY:int = int( (topLeft.getY() + correctionToTopLeft * (bottomRightY - topLeft.getY())));

              // Kind of arbitrary -- expand search radius before giving up
              for (var i:int = 4; i <= 16; i <<= 1) {
                try {
                  alignmentPattern = findAlignmentInRegion(moduleSize,
                      estAlignmentX,
                      estAlignmentY,
                      Number(i));
                  break;
                } catch (re:ReaderException) {
                  // try next round
                }
              }
              if (alignmentPattern == null) {
                throw new ReaderException("Detector : detect : alignmentPattern == null");
              }

            }

            var bits:BitMatrix  = sampleGrid(image, topLeft, topRight, bottomLeft, alignmentPattern, dimension);

            var points:Array;
            if (alignmentPattern == null) {
              points = [bottomLeft, topLeft, topRight];
            } else {
              points = [bottomLeft, topLeft, topRight, alignmentPattern];
            }
            return new DetectorResult(bits, points);
          }

  protected function processFinderPatternInfo(info:FinderPatternInfo ):DetectorResult 
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
        } catch (re:ReaderException) {
          // try next round
        }
      }
      // If we didn't find alignment pattern... well try anyway without it
    }

    var bits:BitMatrix  = sampleGrid(image, topLeft, topRight, bottomLeft, alignmentPattern, dimension);

    var points:Array;
    if (alignmentPattern == null) {
      points = [bottomLeft, topLeft, topRight];
    } else {
      points = [bottomLeft, topLeft, topRight, alignmentPattern];
    }
    return new DetectorResult(bits, points);
  }



          private static function sampleGrid(image:BitMatrix,
                                              topLeft:ResultPoint,
                                              topRight:ResultPoint,
                                              bottomLeft:ResultPoint,
                                              alignmentPattern:ResultPoint,
                                              dimension:int):BitMatrix  {
            var dimMinusThree:Number = Number( dimension - 3.5);
            var bottomRightX:Number;
            var bottomRightY:Number;
            var sourceBottomRightX:Number;
            var sourceBottomRightY:Number;
            if (alignmentPattern != null) {
              bottomRightX = alignmentPattern.getX();
              bottomRightY = alignmentPattern.getY();
              sourceBottomRightX = sourceBottomRightY = dimMinusThree - 3;
            } else {
              // Don't have an alignment pattern, just make up the bottom-right point
              bottomRightX = (topRight.getX() - topLeft.getX()) + bottomLeft.getX();
              bottomRightY = (topRight.getY() - topLeft.getY()) + bottomLeft.getY();
              sourceBottomRightX = sourceBottomRightY = dimMinusThree;
            }

            var sampler:GridSampler = GridSampler.getGridSamplerInstance();
            return sampler.sampleGrid(
                image,
                dimension,
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
            return (calculateModuleSizeOneWay(topLeft, topRight) +
                calculateModuleSizeOneWay(topLeft, bottomLeft)) / 2;
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
              return moduleSizeEst2;
            }
            if (isNaN(moduleSizeEst2))
            {
              return moduleSizeEst1;
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
            var otherToX:int = fromX - (toX - fromX);
            if (otherToX < 0) {
              // "to" should the be the first value not included, so, the first value off
              // the edge is -1
              otherToX = -1;
            } else if (otherToX >= image.getWidth()) {
              otherToX = image.getWidth();
            }
            var otherToY:int = fromY - (toY - fromY);
            if (otherToY < 0) {
              otherToY = -1;
            } else if (otherToY >= image.getHeight()) {
              otherToY = image.getHeight();
            }
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
            var state:int = 0; // In black pixels, looking for white, first or second time
            var diffX:int =0;
            var diffY:int =0;

            for (var x:int = fromX, y:int = fromY; x != toX; x += xstep) {

              var realX:int = steep ? y : x;
              var realY:int = steep ? x : y;
              if (state == 1) { // In white pixels, looking for black
                if (image._get(realX, realY)) {
                  state++;
                }
              } else {
                if (!image._get(realX, realY)) {
                  state++;
                }
              }

              if (state == 3) { // Found black, white, black, and stumbled back onto white; done
                diffX = x - fromX;
                diffY = y - fromY;
                return Number( Math.sqrt(diffX * diffX + diffY * diffY));
              }
              error += dy;
              if (error > 0) {
                y += ystep;
                error -= dx;
              }
            }

            diffX = toX - fromX;
            diffY = toY - fromY;
            return Number( Math.sqrt(diffX * diffX + diffY * diffY));
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

            var alignmentFinder:AlignmentPatternFinder =
                new AlignmentPatternFinder(
                    image,
                    alignmentAreaLeftX,
                    alignmentAreaTopY,
                    alignmentAreaRightX - alignmentAreaLeftX,
                    alignmentAreaBottomY - alignmentAreaTopY,
                    overallEstModuleSize);
            return alignmentFinder.find();
          }

          /**
           * Ends up being a bit faster than Math.round(). This merely rounds its argument to the nearest int,
           * where x.5 rounds up.
           */
          private static function round(d:Number):int {
            return int(d + 0.5);
          }
    
    }
}