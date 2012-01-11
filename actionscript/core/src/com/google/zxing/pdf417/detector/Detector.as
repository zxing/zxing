package com.google.zxing.pdf417.detector
{
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



/**
 * <p>Encapsulates logic that can detect a PDF417 Code in an image, even if the
 * PDF417 Code is rotated or skewed, or partially obscured.</p>
 *
 * @author SITA Lab (kevin.osullivan@sita.aero)
 * @author dswitkin@google.com (Daniel Switkin)
 */
public class Detector {

import com.google.zxing.BinaryBitmap;
import com.google.zxing.ReaderException;
import com.google.zxing.ResultPoint;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.DetectorResult;
import com.google.zxing.common.GridSampler;
import com.google.zxing.common.flexdatatypes.HashTable;

  private static var MAX_AVG_VARIANCE:int = int( ((1 << 8) * 0.42));
  private static var MAX_INDIVIDUAL_VARIANCE:int = int((1 << 8) * 0.8);
  private static var SKEW_THRESHOLD:int = 2;

  // B S B S B S B S Bar/Space pattern
  // 11111111 0 1 0 1 0 1 000
  private static var START_PATTERN:Array = [8, 1, 1, 1, 1, 1, 1, 3];

  // 11111111 0 1 0 1 0 1 000
  private static var START_PATTERN_REVERSE:Array = [3, 1, 1, 1, 1, 1, 1, 8];

  // 1111111 0 1 000 1 0 1 00 1
  private static var STOP_PATTERN:Array = [7, 1, 1, 3, 1, 1, 1, 2, 1];

  // B S B S B S B S B Bar/Space pattern
  // 1111111 0 1 000 1 0 1 00 1
  private static var STOP_PATTERN_REVERSE:Array = [1, 2, 1, 1, 1, 3, 1, 1, 7];

  private var image:BinaryBitmap;

  public function Detector(image:BinaryBitmap) {
    this.image = image;
  }

  /**
   * <p>Detects a PDF417 Code in an image, simply.</p>
   *
   * @return {@link DetectorResult} encapsulating results of detecting a PDF417 Code
   * @throws ReaderException if no QR Code can be found
   */
   /*
  public function detect():DetectorResult {
    return detect(null);
  }*/

  /**
   * <p>Detects a PDF417 Code in an image. Only checks 0 and 180 degree rotations.</p>
   *
   * @param hints optional hints to detector
   * @return {@link DetectorResult} encapsulating results of detecting a PDF417 Code
   * @throws ReaderException if no PDF417 Code can be found
   */
  public function detect(hints:HashTable=null ):DetectorResult {
    // Fetch the 1 bit matrix once up front.
    var matrix:BitMatrix = image.getBlackMatrix();

    // Try to find the vertices assuming the image is upright.
    var vertices:Array = findVertices(matrix);
    if (vertices == null) {
      // Maybe the image is rotated 180 degrees?
      vertices = findVertices180(matrix);
      if (vertices != null) {
        correctCodeWordVertices(vertices, true);
      }
    } else {
      correctCodeWordVertices(vertices, false);
    }

    if (vertices == null) 
	{
      throw new ReaderException("pdf417 : Detector : detect : no vertices");
    }
	
      var moduleWidth:Number = computeModuleWidth(vertices);
      if (moduleWidth < 1) {
        throw new ReaderException("pdf417 : Detector : detect : module width < 1");
      }

      var dimension:int = computeDimension(vertices[4], vertices[6],vertices[5], vertices[7], moduleWidth);

      // Deskew and sample image.
      var bits:BitMatrix = sampleGrid(matrix, vertices[4], vertices[5],vertices[6], vertices[7], dimension);
      return new DetectorResult(bits, [vertices[4],vertices[5], vertices[6], vertices[7]]);
  
  }

  /**
   * Locate the vertices and the codewords area of a black blob using the Start
   * and Stop patterns as locators. Assumes that the barcode begins in the left half
   * of the image, and ends in the right half.
   * TODO: Fix this assumption, allowing the barcode to be anywhere in the image.
   * TODO: Scanning every row is very expensive. We should only do this for TRY_HARDER.
   *
   * @param matrix the scanned barcode image.
   * @return an array containing the vertices:
   *           vertices[0] x, y top left barcode
   *           vertices[1] x, y bottom left barcode
   *           vertices[2] x, y top right barcode
   *           vertices[3] x, y bottom right barcode
   *           vertices[4] x, y top left codeword area
   *           vertices[5] x, y bottom left codeword area
   *           vertices[6] x, y top right codeword area
   *           vertices[7] x, y bottom right codeword area
   */
  private static function findVertices(matrix:BitMatrix):Array {
    var height:int = matrix.getHeight();
    var width:int = matrix.getWidth();
    var halfWidth:int = width >> 1;

    var result:Array = new Array(8); for(var kk:int=0;kk<result.length;kk++){result[kk]=0;}
    var found:Boolean = false;

    var loc:Array = null;
    // Top Left
    for (var i:int = 0; i < height; i++) {
      loc = findGuardPattern(matrix, 0, i, width, false, START_PATTERN);
      if (loc != null) {
        result[0] = new ResultPoint(loc[0], i);
        result[4] = new ResultPoint(loc[1], i);
        found = true;
        break;
      }
    }
    // Bottom left
    if (found) { // Found the Top Left vertex
      found = false;
      for (var i2:int = height - 1; i2 > 0; i2--) {
        loc = findGuardPattern(matrix, 0, i2, width, false, START_PATTERN);
        if (loc != null) {
          result[1] = new ResultPoint(loc[0], i2);
          result[5] = new ResultPoint(loc[1], i2);
          found = true;
          break;
        }
      }
    }
    // Top right
    if (found) { // Found the Bottom Left vertex
      found = false;
      for (var i3:int = 0; i3 < height; i3++) {
        loc = findGuardPattern(matrix, 0, i3, width, false, STOP_PATTERN);
        if (loc != null) {
          result[2] = new ResultPoint(loc[1], i3);
          result[6] = new ResultPoint(loc[0], i3);
          found = true;
          break;
        }
      }
    }
    // Bottom right
    if (found) { // Found the Top right vertex
      found = false;
      for (var i4:int = height - 1; i4 > 0; i4--) {
        loc = findGuardPattern(matrix, 0, i4, width, false, STOP_PATTERN);
        if (loc != null) {
          result[3] = new ResultPoint(loc[1], i4);
          result[7] = new ResultPoint(loc[0], i4);
          found = true;
          break;
        }
      }
    }
    return found ? result : null;
  }

  /**
   * Locate the vertices and the codewords area of a black blob using the Start
   * and Stop patterns as locators. This assumes that the image is rotated 180
   * degrees and if it locates the start and stop patterns at it will re-map
   * the vertices for a 0 degree rotation.
   * TODO: Change assumption about barcode location.
   * TODO: Scanning every row is very expensive. We should only do this for TRY_HARDER.
   *
   * @param matrix the scanned barcode image.
   * @return an array containing the vertices:
   *           vertices[0] x, y top left barcode
   *           vertices[1] x, y bottom left barcode
   *           vertices[2] x, y top right barcode
   *           vertices[3] x, y bottom right barcode
   *           vertices[4] x, y top left codeword area
   *           vertices[5] x, y bottom left codeword area
   *           vertices[6] x, y top right codeword area
   *           vertices[7] x, y bottom right codeword area
   */
  private static function findVertices180(matrix:BitMatrix):Array {
    var height:int = matrix.getHeight();
    var width:int = matrix.getWidth();
    var halfWidth:int = width >> 1;

    var result:Array = new Array(8);for(var kk:int=0;kk<result.length;kk++){result[kk]=0;}
    var found:Boolean = false;

    var loc:Array = null;
    // Top Left
    for (var i:int = height - 1; i > 0; i--) {
      loc = findGuardPattern(matrix, halfWidth, i, halfWidth, true, START_PATTERN_REVERSE);
      if (loc != null) {
        result[0] = new ResultPoint(loc[1], i);
        result[4] = new ResultPoint(loc[0], i);
        found = true;
        break;
      }
    }
    // Bottom Left
    if (found) { // Found the Top Left vertex
      found = false;
      for (var i5:int = 0; i5 < height; i5++) {
        loc = findGuardPattern(matrix, halfWidth, i5, halfWidth, true, START_PATTERN_REVERSE);
        if (loc != null) {
          result[1] = new ResultPoint(loc[1], i5);
          result[5] = new ResultPoint(loc[0], i5);
          found = true;
          break;
        }
      }
    }
    // Top Right
    if (found) { // Found the Bottom Left vertex
      found = false;
      for (var i6:int = height - 1; i6 > 0; i6--) {
        loc = findGuardPattern(matrix, 0, i6, halfWidth, false, STOP_PATTERN_REVERSE);
        if (loc != null) {
          result[2] = new ResultPoint(loc[0], i6);
          result[6] = new ResultPoint(loc[1], i6);
          found = true;
          break;
        }
      }
    }
    // Bottom Right
    if (found) { // Found the Top Right vertex
      found = false;
      for (var i7:int = 0; i7 < height; i7++) {
        loc = findGuardPattern(matrix, 0, i7, halfWidth, false, STOP_PATTERN_REVERSE);
        if (loc != null) {
          result[3] = new ResultPoint(loc[0], i7);
          result[7] = new ResultPoint(loc[1], i7);
          found = true;
          break;
        }
      }
    }
    return found ? result : null;
  }

  /**
   * Because we scan horizontally to detect the start and stop patterns, the vertical component of
   * the codeword coordinates will be slightly wrong if there is any skew or rotation in the image.
   * This method moves those points back onto the edges of the theoretically perfect bounding
   * quadrilateral if needed.
   *
   * @param vertices The eight vertices located by findVertices().
   */
  private static function correctCodeWordVertices(vertices:Array , upsideDown:Boolean):void {
      var length:Number;
      var deltax:Number;
      var deltay:Number;
      var correction:Number;

    var skew:Number = vertices[4].getY() - vertices[6].getY();
    if (upsideDown) {
      skew = -skew;
    }
    if (skew > SKEW_THRESHOLD) {
      // Fix v4
      length = vertices[4].getX() - vertices[0].getX();
      deltax = vertices[6].getX() - vertices[0].getX();
      deltay = vertices[6].getY() - vertices[0].getY();
      correction = length * deltay / deltax;
      vertices[4] = new ResultPoint(vertices[4].getX(), vertices[4].getY() + correction);
    } else if (-skew > SKEW_THRESHOLD) {
      // Fix v6
      length = vertices[2].getX() - vertices[6].getX();
      deltax = vertices[2].getX() - vertices[4].getX();
      deltay = vertices[2].getY() - vertices[4].getY();
      correction = length * deltay / deltax;
      vertices[6] = new ResultPoint(vertices[6].getX(), vertices[6].getY() - correction);
    }

    skew = vertices[7].getY() - vertices[5].getY();
    if (upsideDown) {
      skew = -skew;
    }
    if (skew > SKEW_THRESHOLD) {
      // Fix v5
      length = vertices[5].getX() - vertices[1].getX();
      deltax = vertices[7].getX() - vertices[1].getX();
      deltay = vertices[7].getY() - vertices[1].getY();
      correction = length * deltay / deltax;
      vertices[5] = new ResultPoint(vertices[5].getX(), vertices[5].getY() + correction);
    } else if (-skew > SKEW_THRESHOLD) {
      // Fix v7
      length = vertices[3].getX() - vertices[7].getX();
      deltax = vertices[3].getX() - vertices[5].getX();
      deltay = vertices[3].getY() - vertices[5].getY();
      correction = length * deltay / deltax;
      vertices[7] = new ResultPoint(vertices[7].getX(), vertices[7].getY() - correction);
    }
  }

  /**
   * <p>Estimates module size (pixels in a module) based on the Start and End
   * finder patterns.</p>
   *
   * @param vertices an array of vertices:
   *           vertices[0] x, y top left barcode
   *           vertices[1] x, y bottom left barcode
   *           vertices[2] x, y top right barcode
   *           vertices[3] x, y bottom right barcode
   *           vertices[4] x, y top left codeword area
   *           vertices[5] x, y bottom left codeword area
   *           vertices[6] x, y top right codeword area
   *           vertices[7] x, y bottom right codeword area
   * @return the module size.
   */
  private static function computeModuleWidth(vertices:Array):Number {
    var pixels1:Number = ResultPoint.distance(vertices[0], vertices[4]);
    var pixels2:Number = ResultPoint.distance(vertices[1], vertices[5]);
    var moduleWidth1:Number = (pixels1 + pixels2) / (17 * 2.0);
    var pixels3:Number = ResultPoint.distance(vertices[6], vertices[2]);
    var pixels4:Number = ResultPoint.distance(vertices[7], vertices[3]);
    var moduleWidth2:Number = (pixels3 + pixels4) / (18 * 2.0);
    return (moduleWidth1 + moduleWidth2) / 2.0;
  }

  /**
   * Computes the dimension (number of modules in a row) of the PDF417 Code
   * based on vertices of the codeword area and estimated module size.
   *
   * @param topLeft     of codeword area
   * @param topRight    of codeword area
   * @param bottomLeft  of codeword area
   * @param bottomRight of codeword are
   * @param moduleWidth estimated module size
   * @return the number of modules in a row.
   */
  private static function computeDimension(topLeft:ResultPoint, topRight:ResultPoint,bottomLeft:ResultPoint, bottomRight:ResultPoint, moduleWidth:Number):int 
  {
    var topRowDimension:int = Math.round((ResultPoint.distance(topLeft, topRight) / moduleWidth));
    var bottomRowDimension:int = Math.round(ResultPoint.distance(bottomLeft, bottomRight) / moduleWidth);
    return int((((topRowDimension + bottomRowDimension) >> 1) + 8) / 17) * 17;
    /*
    * int topRowDimension = round(ResultPoint.distance(topLeft,
    * topRight)); //moduleWidth); int bottomRowDimension =
    * round(ResultPoint.distance(bottomLeft, bottomRight)); //
    * moduleWidth); int dimension = ((topRowDimension + bottomRowDimension)
    * >> 1); // Round up to nearest 17 modules i.e. there are 17 modules per
    * codeword //int dimension = ((((topRowDimension + bottomRowDimension) >>
    * 1) + 8) / 17) * 17; return dimension;
    */
  }

  private static function sampleGrid(matrix:BitMatrix , topLeft:ResultPoint ,
      bottomLeft:ResultPoint , topRight:ResultPoint , bottomRight:ResultPoint , dimension:int ):BitMatrix
       {

    // Note that unlike the QR Code sampler, we didn't find the center of modules, but the
    // very corners. So there is no 0.5f here; 0.0f is right.
    var sampler:GridSampler = GridSampler.getGridSamplerInstance();

    return sampler.sampleGrid2(
    	matrix, 
        dimension, 
        dimension,
        0, // p1ToX
        0, // p1ToY
        dimension, // p2ToX
        0, // p2ToY
        dimension, // p3ToX
        dimension, // p3ToY
        0, // p4ToX
        dimension, // p4ToY
        topLeft.getX(), // p1FromX
        topLeft.getY(), // p1FromY
        topRight.getX(), // p2FromX
        topRight.getY(), // p2FromY
        bottomRight.getX(), // p3FromX
        bottomRight.getY(), // p3FromY
        bottomLeft.getX(), // p4FromX
        bottomLeft.getY()); // p4FromY
  }

  /**
   * Ends up being a bit faster than Math.round(). This merely rounds its
   * argument to the nearest int, where x.5 rounds up.
   */
  private static function round(d:Number):int {
    return int(d + 0.5);
  }

  /**
   * @param matrix row of black/white values to search
   * @param column x position to start search
   * @param row y position to start search
   * @param width the number of pixels to search on this row
   * @param pattern pattern of counts of number of black and white pixels that are
   *                 being searched for as a pattern
   * @return start/end horizontal offset of guard pattern, as an array of two ints.
   */
 public  static function findGuardPattern( matrix:BitMatrix,  column:int,  row:int,  width:int,
      whiteFirst:Boolean, pattern:Array):Array {
    var patternLength:int = pattern.length;
    // TODO: Find a way to cache this array, as this method is called hundreds of times
    // per image, and we want to allocate as seldom as possible.
    var counters:Array = new Array(patternLength);for (var k:int=0;k<counters.length;k++){counters[k] = 0; }
    var isWhite:Boolean = whiteFirst;

    var counterPosition:int = 0;
    var patternStart:int = column;
    for (var x:int = column; x < column + width; x++) {
      var pixel:Boolean = matrix._get(x, row);
      if (pixel != isWhite) {
        counters[counterPosition]++;
      } else {
        if (counterPosition == patternLength - 1) 
        {
          if (patternMatchVariance(counters, pattern, MAX_INDIVIDUAL_VARIANCE) < MAX_AVG_VARIANCE) 
          {
            return [patternStart, x];
          }
          patternStart += counters[0] + counters[1];
          for (var y:int = 2; y < patternLength; y++) {
            counters[y - 2] = counters[y];
          }
          counters[patternLength - 2] = 0;
          counters[patternLength - 1] = 0;
          counterPosition--;
        } else {
          counterPosition++;
        }
        counters[counterPosition] = 1;
        isWhite = !isWhite;
      }
    }
    return null;
  }

  /**
   * Determines how closely a set of observed counts of runs of black/white
   * values matches a given target pattern. This is reported as the ratio of
   * the total variance from the expected pattern proportions across all
   * pattern elements, to the length of the pattern.
   *
   * @param counters observed counters
   * @param pattern expected pattern
   * @param maxIndividualVariance The most any counter can differ before we give up
   * @return ratio of total variance between counters and pattern compared to
   *         total pattern size, where the ratio has been multiplied by 256.
   *         So, 0 means no variance (perfect match); 256 means the total
   *         variance between counters and patterns equals the pattern length,
   *         higher values mean even more variance
   */
  public static function patternMatchVariance(counters:Array, pattern:Array, maxIndividualVariance:int):int {
    var numCounters:int = counters.length;
    var total:int = 0;
    var patternLength:int = 0;
    for (var i:int = 0; i < numCounters; i++) {
      total += counters[i];
      patternLength += pattern[i];
    }
    if (total < patternLength) {
      // If we don't even have one pixel per unit of bar width, assume this
      // is too small to reliably match, so fail:
      return int.MAX_VALUE;
    }
    // We're going to fake floating-point math in integers. We just need to use more bits.
    // Scale up patternLength so that intermediate values below like scaledCounter will have
    // more "significant digits".
    var unitBarWidth:int = int((total << 8) / patternLength);
    maxIndividualVariance = (maxIndividualVariance * unitBarWidth) >> 8;

    var totalVariance:int = 0;
    for (var x:int = 0; x < numCounters; x++) {
      var counter:int = counters[x] << 8;
      var scaledPattern:int = pattern[x] * unitBarWidth;
      var variance:int = counter > scaledPattern ? counter - scaledPattern : scaledPattern - counter;
      if (variance > maxIndividualVariance) {
        return int.MAX_VALUE;
      }
      totalVariance += variance;
    }
    return int(totalVariance / total);
  }

}
}